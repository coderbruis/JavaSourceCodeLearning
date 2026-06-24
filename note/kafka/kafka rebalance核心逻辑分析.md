当前分析版本是kafka最新版本（版本随时变化，最新分析代码请关注仓库：[https://github.com/coderbruis/kafka](https://github.com/coderbruis/kafka)   **<font style="color:#DF2A3F;">source_code_analysis分支</font>**，底层原理持续更新）

# Kafka Rebalance 核心流程
`KafkaConsumer.poll()` 是消费者触发 rebalance 的主要入口。

位置在 [KafkaConsumer.java (line 916)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java:916)：

```plain
public ConsumerRecords<K, V> poll(final Duration timeout) {
    return delegate.poll(timeout);
}
```

在 classic consumer 实现里，会进入 [ClassicKafkaConsumer.poll() (line 641)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:641)：

```plain
KafkaConsumer.poll
  -> ClassicKafkaConsumer.poll
    -> updateAssignmentMetadataIfNeeded
      -> ConsumerCoordinator.poll        
      -> updateFetchPositions
    -> pollForFetches
```

也就是说，consumer 每次 `poll()` 拉数据之前，都会先确认自己是否已经完成消费组协调、分区分配和 fetch offset 准备。

`ClassicKafkaConsumer.poll()` 最核心做了三件事：

+ **推进消费组协调**  
调用 `updateAssignmentMetadataIfNeeded(...)`，确保自动分配分区的 consumer 已经加入 group，并且 rebalance 已经完成。
+ **准备 fetch 位置**  
如果分区还没有有效 position，会根据 committed offset、auto offset reset 等规则确定从哪里开始拉。
+ **拉取并返回消息**  
从 fetch buffer 取数据，或者发送新的 FetchRequest，并把拿到的 records 返回给用户。

对应 [ClassicKafkaConsumer.java (line 665)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:665)：

```plain
updateAssignmentMetadataIfNeeded(timer, false)
pollForFetches(timer)
sendFetches()
```

再精简一句：**rebalance 不是后台自动完整完成的，classic consumer 的 group rebalance 主要是在用户线程调用 **`poll()`** 时被推进的。**

---

## ConsumerCoordinator.poll()
`ConsumerCoordinator.poll()` 是 consumer 端消费组协调的入口。

位置在 [ConsumerCoordinator.java (line 530)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:530)：

```plain
public boolean poll(Timer timer, boolean waitForJoinGroup)
```

这个方法最核心做了五件事：

+ **更新订阅元数据**  
调用 `maybeUpdateSubscriptionMetadata()`，如果 metadata version 变了，就更新本地 `metadataSnapshot`。
+ **执行已完成的 offset commit 回调**  
异步提交的 callback 不一定在请求完成时立即由用户线程执行，这里统一触发。
+ **维护心跳和 coordinator 连接**  
刷新 heartbeat 的 last poll 时间；如果不知道 coordinator，就先找 coordinator。
+ **必要时触发重新入组**  
如果 `rejoinNeededOrPending()` 为 true，就调用 `ensureActiveGroup(...)` 推进 JoinGroup/SyncGroup。
+ **处理自动提交**  
如果开启 auto commit，并且提交周期到了，就异步提交当前消费位点。

核心链路是：

```plain
ConsumerCoordinator.poll
  -> maybeUpdateSubscriptionMetadata
  -> invokeCompletedOffsetCommitCallbacks
  -> pollHeartbeat
  -> coordinatorUnknownAndUnreadySync
  -> rejoinNeededOrPending
  -> ensureActiveGroup
  -> maybeAutoCommitOffsetsAsync
```

`waitForJoinGroup` 决定这次是否等待 rebalance 完成：

+ `true`：使用用户传进来的 timer，可以阻塞等待入组完成。
+ `false`：使用 0ms timer，只推进一次流程，不强等完成。

这也是为什么 `poll()` 可能先返回空数据：不是没有数据，而是本轮还在推进 rebalance 或准备 assignment。

### invokeCompletedOffsetCommitCallbacks()为什么要在poll()里执行？
invokeCompletedOffsetCommitCallbacks的核心作用是把已经完成的异步 offset commit 结果取出来，并调用用户传入的 OffsetCommitCallback.onComplete(...)。offset commit提交的是：某个consumer group对某个 topic-partition已经消费到哪里，它写入的是内部topic __consumer_offsets。invokeCompletedOffsetCommitCallbacks放在poll()里有几个原因：

1. 保证 callback 在用户线程执行

Kafka 不希望在内部网络处理路径里直接跑用户 callback，因为用户 callback 可能很慢、可能抛异常、可能调用 consumer API。

2. 避免阻塞内部协议处理

如果在 response handler 里直接执行用户 callback，callback 慢会拖慢 coordinator response 处理，影响 rebalance、heartbeat、commit 等内部状态推进。

3. 及时暴露 fenced 异常

如果异步 commit 遇到 FencedInstanceIdException，代码会设置 asyncCommitFenced，下一次 invokeCompletedOffsetCommitCallbacks() 会抛出 fenced 异常。这也是为什么它要在 poll() 这种高频入口前置检查。fenced 可以理解成 身份被抢占后的隔离机制。配了 group.instance.id 后，consumer 变成静态成员。同一个 group.id + group.instance.id 同一时间只能有一个实例有效，fenced就是排查是否有重复的 group.instance.id 实例在运行。



### rejoinNeededOrPending（）
这个方法命名就很有意思“是否需要重新加入消费组，或者重新加入消费组的流程仍在进行中”。首先来看请求第一次进来。

核心链路：

```plain
ConsumerCoordinator.rejoinNeededOrPending
  -> 判断是否是自动分配分区
  -> 判断metadataSnapshot
  -> 判断subscriptions
```



如果用户是手动分配分区，就不需要rebalance了。

```java
if (!subscriptions.hasAutoAssignedPartitions())
    return false;
```



上次 assignment 使用的 metadata 和当前 metadata 不匹配，或者说集群 topic/partition 等元数据变化，需要重新分配。

```java
if (assignmentSnapshot != null && !assignmentSnapshot.matches(metadataSnapshot)) {
    ...
    return true;
}
```



上次 JoinGroup 提交的 subscription 和当前 subscription 不一致，或者说用户订阅或正则匹配结果变化，需要重新入组。

```java
if (joinedSubscription != null && !joinedSubscription.equals(subscriptions.subscription())) {
    ...
    return true;
}
```



这里两处判断为啥都是上次的JoinGroup？因为这是在构造这一次JoinGroup之前，所以得那上次的JoinGroup的元数据和订阅信息和当前准备构造JoinGroup的数据进行对比。



如果上面两个判断都为false，则会走最后一个判断rejoinNeededOrPending

```java
    protected synchronized boolean rejoinNeededOrPending() {
        return rejoinNeeded || joinFuture != null;
    }
```

第一次发起JoinGroup，joinFuture=null。而rejoinNeeded为true是什么场景呢？这主要是通过AbstractCoordinator.requestRejoin()来触发

```java
public synchronized void requestRejoin(final String shortReason,
                                       final String fullReason) {
    log.info("Request joining group due to: {}", fullReason);
    this.rejoinReason = shortReason;
    this.rejoinNeeded = true;
}
```

而requestRejoin()的调用场景有：broker 返回 REBALANCE_IN_PROGRESS / ILLEGAL_GENERATION / UNKNOWN_MEMBER_ID。



## Rebalance触发条件？
是否需要加入或重新加入消费组，主要由 `rejoinNeededOrPending()` 判断。

位置在 [ConsumerCoordinator.java (line 953)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:953)：

```plain
public boolean rejoinNeededOrPending()
```

常见触发条件：

+ **第一次加入消费组**  
consumer 还没有 generation/member id，必须 JoinGroup。
+ **订阅发生变化**  
比如用户调用 `subscribe()` 改了 topic 列表。
+ **metadata 发生变化**  
topic 新增分区、删除分区、正则订阅匹配结果变化等。
+ **成员变化**  
有 consumer 加入、退出、session timeout、max.poll.interval.ms 超时。
+ **cooperative rebalance 需要第二轮确认**  
如果本轮 assignment 要 revoke 部分分区，consumer 会先 revoke，然后 request rejoin，下一轮再完成新分配。

---

## ensureActiveGroup()
`ensureActiveGroup()` 是“确保消费组已经可用”的核心方法。

位置在 [AbstractCoordinator.java (line 422)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:422)：

```plain
boolean ensureActiveGroup(final Timer timer) {
    if (!ensureCoordinatorReady(timer)) {
        return false;
    }
    startHeartbeatThreadIfNeeded();
    return joinGroupIfNeeded(timer);
}
```

这个方法最核心做了三件事：

+ **找到 coordinator**  
如果还不知道 group coordinator 是哪个 broker，就通过 FindCoordinator 找到它。
+ **启动 heartbeat 线程**  
heartbeat 线程负责维持 session，避免 broker 认为该 member 死掉。
+ **必要时加入 group**  
调用 `joinGroupIfNeeded(...)` 推进 JoinGroup 和 SyncGroup。

简化链路：

```plain
ensureActiveGroup
  -> ensureCoordinatorReady
  -> startHeartbeatThreadIfNeeded
  -> joinGroupIfNeeded
```

注意：heartbeat 线程负责维持成员活性，但 rebalance 的 JoinGroup/SyncGroup 主流程仍然由调用 `poll()` 的用户线程推进。

---

## joinGroupIfNeeded()
`joinGroupIfNeeded()` 是 consumer 端 rebalance 状态机的核心。

位置在 [AbstractCoordinator.java (line 476)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:476)：

```plain
boolean joinGroupIfNeeded(final Timer timer)
```

核心流程：

```plain
joinGroupIfNeeded
  -> while rejoinNeededOrPending
    -> ensureCoordinatorReady
    -> onJoinPrepare
    -> initiateJoinGroup
      -> sendJoinGroupRequest
      -> JoinGroupResponseHandler
      -> sendSyncGroupRequest
      -> SyncGroupResponseHandler
    -> client.poll(future, timer)
    -> onJoinComplete
```

这个方法最核心做了四件事：

+ **入组前准备**  
调用 `onJoinPrepare(...)`，处理 auto commit、revoke/lost callback、清理旧 assignment。
+ **发送 JoinGroup**  
把自己的订阅信息、支持的 assignor、当前拥有的 partitions 发给 broker coordinator。
+ **发送 SyncGroup**  
如果自己是 leader，先计算整个 group 的 assignment，再发 SyncGroup；如果是 follower，发送空 assignment 等 leader 的结果。
+ **应用新 assignment**  
SyncGroup 成功后，调用 `onJoinComplete(...)`，更新本地分配并触发 assigned callback。

关键点：`joinGroupIfNeeded()` 会循环执行，直到 rebalance 成功、超时、被 wakeup，或者遇到不可恢复错误。

---

## JoinGroup 请求携带什么
JoinGroup 请求里最关键的是 `metadata()` 生成的 subscription metadata。

位置在 [ConsumerCoordinator.java (line 303)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:303)：

```plain
protected JoinGroupRequestProtocolCollection metadata()
```

每个 assignor 都会生成一份协议元数据，里面包括：

+ 当前订阅的 topics。
+ assignor 自己的 user data。
+ 当前 consumer 已经拥有的 partitions。
+ 当前 generation id。
+ rack id。

核心代码：

```plain
Subscription subscription = new Subscription(
    topics,
    assignor.subscriptionUserData(joinedSubscription),
    subscriptions.assignedPartitionsList(),
    generation().generationId,
    rackId
);
```

所以 JoinGroup 不是只告诉 broker “我订阅了哪些 topic”，还会告诉 broker “我上一次拥有哪些 partition”。这对 cooperative rebalance 非常关键。

---

## Broker 侧 JoinGroup / SyncGroup
broker 网络入口仍然是 `KafkaApis.handle()`。

位置在 [KafkaApis.scala (line 180)](/Users/apple/Documents/git_code_pr/kafka/core/src/main/scala/kafka/server/KafkaApis.scala:180)：

```plain
case ApiKeys.JOIN_GROUP => handleJoinGroupRequest(request, requestLocal).exceptionally(handleError)
case ApiKeys.SYNC_GROUP => handleSyncGroupRequest(request, requestLocal).exceptionally(handleError)
```

`handleJoinGroupRequest()` 会做 group 权限校验，然后交给 `groupCoordinator.joinGroup()`。

位置在 [KafkaApis.scala (line 1381)](/Users/apple/Documents/git_code_pr/kafka/core/src/main/scala/kafka/server/KafkaApis.scala:1381)：

```plain
groupCoordinator.joinGroup(
  request.context,
  joinGroupRequest.data,
  requestLocal.bufferSupplier
)
```

`GroupCoordinatorService.joinGroup()` 最终调度 classic group 的 join 操作。

位置在 [GroupCoordinatorService.java (line 1053)](/Users/apple/Documents/git_code_pr/kafka/group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupCoordinatorService.java:1053)：

```plain
runtime.scheduleWriteOperation(
    "classic-group-join",
    topicPartitionFor(request.groupId()),
    coordinator -> coordinator.classicGroupJoin(context, request, responseFuture)
)
```

`syncGroup()` 类似，会调度 classic group sync。

位置在 [GroupCoordinatorService.java (line 1125)](/Users/apple/Documents/git_code_pr/kafka/group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupCoordinatorService.java:1125)：

```plain
runtime.scheduleWriteOperation(
    "classic-group-sync",
    topicPartitionFor(request.groupId()),
    coordinator -> coordinator.classicGroupSync(context, request, responseFuture)
)
```

broker coordinator 的职责可以概括成：

+ 维护 group 成员、generation、leader、protocol。
+ 收集所有成员的 JoinGroup metadata。
+ 选出 leader。
+ 等 leader 通过 SyncGroup 提交完整 assignment。
+ 把每个 member 自己的 assignment 返回给它。

broker 不负责具体分区分配算法。classic consumer 的分配算法是在 leader consumer 本地执行的。

---

## Leader 如何执行分配
当 JoinGroupResponse 返回后，如果当前 consumer 被 broker 选为 leader，会进入：

```plain
JoinGroupResponseHandler.handle
  -> onLeaderElected(joinResponse)
    -> ConsumerCoordinator.onLeaderElected(...)
```

位置在 [AbstractCoordinator.java (line 724)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:724)：

```plain
if (joinResponse.isLeader()) {
    onLeaderElected(joinResponse).chain(future);
} else {
    onJoinFollower().chain(future);
}
```

leader 的核心逻辑在 [ConsumerCoordinator.onLeaderElected() (line 686)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:686)：

```plain
protected Map<String, ByteBuffer> onLeaderElected(...)
```

这个方法最核心做了六件事：

+ **反序列化所有成员的订阅信息**  
从 JoinGroupResponse 的 `members` 里解析每个 member 的 `Subscription`。
+ **收集 group 订阅的所有 topic**  
用于更新 leader 的 metadata 监听范围。
+ **收集 owned partitions**  
保存每个 member 上报的旧 assignment。
+ **调用 assignor 执行分配**  
`assignor.assign(metadata.fetch(), new GroupSubscription(subscriptions))`。
+ **校验 cooperative assignment**  
对自定义 cooperative assignor，调用 `validateCooperativeAssignment(...)` 防止直接抢占仍被别人持有的 partition。
+ **序列化 assignment**  
把每个 member 的 assignment 序列化，准备放进 leader SyncGroup。

核心代码在 [ConsumerCoordinator.java (line 724)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:724)：

```plain
Map<String, Assignment> assignments =
    assignor.assign(metadata.fetch(), new GroupSubscription(subscriptions)).groupAssignment();
```

leader 计算完 assignment 后，会在 [AbstractCoordinator.java (line 836)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:836) 构造 leader SyncGroup：

```plain
SyncGroupRequest.Builder requestBuilder =
    new SyncGroupRequest.Builder(
        new SyncGroupRequestData()
            .setAssignments(groupAssignmentList)
    );
```

简化链路：

```plain
JoinGroupResponse leader
  -> deserialize all subscriptions
  -> update group subscription metadata
  -> assignor.assign(...)
  -> validate cooperative assignment
  -> serialize assignment per member
  -> SyncGroupRequest(assignments)
```

---

## Follower 做什么
follower 不计算 assignment。

位置在 [AbstractCoordinator.java (line 801)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:801)：

```plain
private RequestFuture<ByteBuffer> onJoinFollower()
```

follower 只发送一个空 assignment 的 SyncGroup：

```plain
setAssignments(Collections.emptyList())
```

意思是：follower 告诉 broker “我已经进入 sync 阶段了，具体分配结果等 leader 提交”。

最终 broker 会在 SyncGroupResponse 里把属于这个 follower 的 assignment 返回给它。

---

## SyncGroupResponse 如何完成 Rebalance
SyncGroupResponse 成功后，会进入 `SyncGroupResponseHandler.handle(...)`。

位置在 [AbstractCoordinator.java (line 871)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:871)：

```plain
if (!hasGenerationReset(generation) && state == MemberState.COMPLETING_REBALANCE) {
    state = MemberState.STABLE;
    rejoinNeeded = false;
    future.complete(ByteBuffer.wrap(syncResponse.data().assignment()));
}
```

这里最核心做了三件事：

+ 校验 generation 没有被 heartbeat 线程重置。
+ 将成员状态改成 `STABLE`。
+ 把 broker 返回的 assignment 放进 future。

future 成功后，`joinGroupIfNeeded()` 会调用 `onJoinComplete(...)` 应用 assignment。

---

## onJoinPrepare()
`onJoinPrepare()` 是入组前的清理阶段。

位置在 [ConsumerCoordinator.java (line 785)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:785)：

```plain
protected boolean onJoinPrepare(Timer timer, int generation, String memberId)
```

这个方法最核心做了三件事：

+ **入组前自动提交 offset**  
如果开启 auto commit，会先尝试异步提交当前消费位点。
+ **触发 revoke 或 lost callback**  
如果 generation/member id 已经被重置，说明之前的分区已经 lost；否则按 EAGER/COOPERATIVE 协议触发 revoke。
+ **清理旧 group subscription 状态**  
重置 leader 标记和 group subscription，为下一轮 JoinGroup 做准备。

EAGER 协议下：

```plain
revoke all currently assigned partitions
subscriptions.assignFromSubscribed(emptySet)
```

COOPERATIVE 协议下：

```plain
only revoke partitions whose topics are no longer subscribed
keep still-owned partitions locally
```

这就是 EAGER 和 COOPERATIVE 的核心差异：

+ EAGER：每次 rebalance 先全部撤销，再重新分配。
+ COOPERATIVE：尽量保留仍然合法的分区，只撤销需要迁移的分区。

---

## onJoinComplete()
`onJoinComplete()` 是本地应用 assignment 的阶段。

位置在 [ConsumerCoordinator.java (line 380)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:380)：

```plain
protected void onJoinComplete(...)
```

这个方法最核心做了五件事：

+ **反序列化 SyncGroupResponse 里的 assignment**  
得到当前 member 最终分到的 partitions。
+ **校验 assignment 是否匹配当前订阅**  
如果 assignment 和当前 subscription 不一致，说明订阅可能变化了，直接 request rejoin。
+ **计算 added/revoked partitions**  
和本地当前 owned partitions 比较，得出新增和撤销集合。
+ **COOPERATIVE 下处理第二阶段 revoke**  
如果有 revoked partitions，先触发 revoke callback，然后 request rejoin，下一轮再完成迁移。
+ **应用新 assignment 并触发 assigned callback**  
调用 `subscriptions.assignFromSubscribed(...)`，然后触发 `onPartitionsAssigned(...)`。

核心代码在 [ConsumerCoordinator.java (line 421)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:421)：

```plain
SortedSet<TopicPartition> addedPartitions = new TreeSet<>(COMPARATOR);
addedPartitions.addAll(assignedPartitions);
addedPartitions.removeAll(ownedPartitions);
```

COOPERATIVE 下如果发现 revoked partitions：

```plain
invokePartitionsRevoked(revokedPartitions)
requestRejoin("need to revoke partitions and re-join", ...)
```

最后应用 assignment：

```plain
subscriptions.assignFromSubscribed(assignedPartitions)
invokePartitionsAssigned(addedPartitions)
```

也就是说，`onJoinComplete()` 才是用户真正看到分区变化的地方。

---

## validateCooperativeAssignment()
`validateCooperativeAssignment()` 只用于 COOPERATIVE rebalance。

位置在 [ConsumerCoordinator.java (line 757)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:757)：

```plain
private void validateCooperativeAssignment(...)
```

它的作用是校验自定义 cooperative assignor 是否违反规则。

COOPERATIVE 的基本规则是：

```plain
如果 partition 当前还被 member A 拥有，
assignor 不能在同一轮 rebalance 里直接把它分给 member B。
```

正确流程必须是：

```plain
第一轮 rebalance:
  A 先 revoke partition
  该 partition 暂时不分配给任何人

第二轮 rebalance:
  partition 已经没人 owned
  再把它分给 B
```

`validateCooperativeAssignment()` 的判断方式：

```plain
for each member:
  added = newAssignment - oldOwnedPartitions
  revoked = oldOwnedPartitions - newAssignment

if totalAdded intersects totalRevoked:
  illegal assignment
```

如果有交集，说明某个 partition 一边被旧 owner revoke，一边又被新 owner added。这就是“没有先释放，直接转移”，违反 COOPERATIVE 协议。

内置 `CooperativeStickyAssignor` 会跳过这个校验，位置在 [ConsumerCoordinator.java (line 726)](/Users/apple/Documents/git_code_pr/kafka/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:726)：

```plain
if (protocol == RebalanceProtocol.COOPERATIVE &&
    !assignorName.equals(COOPERATIVE_STICKY_ASSIGNOR_NAME)) {
    validateCooperativeAssignment(ownedPartitions, assignments);
}
```

原因是内置 cooperative sticky assignor 自己已经处理了 owned partition 的 generation 语义。

---

# EAGER 和 COOPERATIVE 的差异
## EAGER
EAGER rebalance 的特点是简单直接。

```plain
onJoinPrepare:
  revoke all partitions
  clear local assignment

leader assign:
  assign all partitions again

onJoinComplete:
  assign new partitions
  trigger onPartitionsAssigned
```

优点：

+ 实现简单。
+ assignment 一次完成。

缺点：

+ 每次 rebalance 都会短暂停止所有分区消费。
+ 即使某些 partition 仍然分给同一个 consumer，也会先 revoke 再 assign。

## COOPERATIVE
COOPERATIVE rebalance 的特点是渐进迁移。

```plain
onJoinPrepare:
  keep still-owned partitions
  revoke only obviously invalid partitions

leader assign:
  do not immediately reassign partitions still owned by others

onJoinComplete:
  revoke partitions that need migration
  request another rejoin if needed

next rebalance:
  assign released partitions to new owners
```

优点：

+ 减少分区停止消费的范围。
+ 尽量保持稳定 assignment。
+ 更适合大 group、大 partition 数量的场景。

缺点：

+ 可能需要多轮 rebalance。
+ assignor 必须遵守“先释放，再转移”的规则。
+ 状态机更复杂。

---

# 一条完整 Rebalance 链路
以自动订阅 consumer 触发 rebalance 为例：

```plain
KafkaConsumer.poll
  -> ClassicKafkaConsumer.poll
    -> updateAssignmentMetadataIfNeeded
      -> ConsumerCoordinator.poll
        -> maybeUpdateSubscriptionMetadata
        -> pollHeartbeat
        -> rejoinNeededOrPending
        -> ensureActiveGroup
          -> ensureCoordinatorReady
          -> startHeartbeatThreadIfNeeded
          -> joinGroupIfNeeded
            -> onJoinPrepare
              -> maybe auto commit
              -> revoke/lost old partitions
            -> sendJoinGroupRequest
              -> ConsumerCoordinator.metadata
              -> JoinGroupRequest
            -> broker KafkaApis.handleJoinGroupRequest
              -> GroupCoordinatorService.joinGroup
              -> select generation / leader / protocol
            -> JoinGroupResponseHandler
              -> leader:
                   ConsumerCoordinator.onLeaderElected
                     -> deserialize subscriptions
                     -> assignor.assign
                     -> validateCooperativeAssignment
                     -> serialize group assignment
                   leader SyncGroupRequest(assignments)
                 follower:
                   follower SyncGroupRequest(empty assignments)
            -> broker KafkaApis.handleSyncGroupRequest
              -> GroupCoordinatorService.syncGroup
              -> return each member assignment
            -> SyncGroupResponseHandler
              -> state = STABLE
              -> future.complete(assignment)
            -> onJoinComplete
              -> deserialize assignment
              -> compute added/revoked partitions
              -> cooperative may request another rejoin
              -> subscriptions.assignFromSubscribed
              -> onPartitionsAssigned
      -> updateFetchPositions
    -> pollForFetches
```

再压缩成一句：

```plain
poll()
  -> 确认是否要 rebalance
  -> JoinGroup 上报订阅和 owned partitions
  -> broker 选 leader
  -> leader 本地执行 assignor.assign
  -> SyncGroup 广播 assignment
  -> 每个 member 应用自己的 assignment
  -> 准备 offset 后开始 fetch
```

---

# 总结
Kafka classic consumer rebalance 的核心可以理解成三层协作：

+ **ConsumerCoordinator 负责客户端状态机**  
判断是否需要 rejoin、入组前 revoke、发送 JoinGroup/SyncGroup、应用 assignment。
+ **GroupCoordinator 负责服务端 group 管理**  
维护成员、generation、leader、protocol，并把 leader 提交的 assignment 分发给各个 member。
+ **ConsumerPartitionAssignor 负责分区分配算法**  
真正决定 topic partitions 分给哪些 consumer。

最关键的几个方法：

```plain
ClassicKafkaConsumer.poll
ConsumerCoordinator.poll
AbstractCoordinator.ensureActiveGroup
AbstractCoordinator.joinGroupIfNeeded
ConsumerCoordinator.onJoinPrepare
ConsumerCoordinator.metadata
ConsumerCoordinator.onLeaderElected
ConsumerCoordinator.validateCooperativeAssignment
AbstractCoordinator.SyncGroupResponseHandler
ConsumerCoordinator.onJoinComplete
```

EAGER 和 COOPERATIVE 的本质区别是：

+ EAGER 是“先全部释放，再重新分配”。
+ COOPERATIVE 是“能不动就不动，要迁移的分区先释放，下一轮再分给别人”。

所以看 rebalance 源码时，最重要的是抓住这条主线：

```plain
是否需要重平衡
  -> 入组前怎么处理旧分区
  -> JoinGroup 带了哪些成员信息
  -> leader 如何计算 assignment
  -> SyncGroup 如何下发 assignment
  -> member 如何应用 assignment
```

只要这条链路清楚，`ConsumerCoordinator`、`AbstractCoordinator`、`GroupCoordinatorService` 里的大部分逻辑都能对应到具体阶段。

