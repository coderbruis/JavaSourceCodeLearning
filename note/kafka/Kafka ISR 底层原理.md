+ 当前分析版本是kafka最新版本（版本随时变化，最新分析代码请关注仓库：https://github.com/coderbruis/kafka source_code_analysis分支，底层原理持续更新）
+ 转载请标明出处

# Kafka ISR是什么？解决什么问题？
## 是什么？
Kafka ISR是In-Sync Replicas，意思是“与leader保持同步的副本集合”。在Kafka中会有leader副本和follower副本，下面举例：

partition-0有三个副本，replicas = [1, 2, 3]，leader副本是：1，另外两个是follower副本：2, 3。

当网络状况良好，follower副本同步状况良好，都追上了leader副本，则ISR=[1,2,3]，说明1,2,3都是同步副本。可是某一时刻3卡住、宕机、网络慢，长时间追不上leader，则此时ISR=[1,2]。还有一种最差的情况就是2、3网络都慢，导致最后ISR=[1]，ISR就只剩下leader副本了。这里需要注意，leader副本一直都是在ISR中的。

## 解决什么问题？
ISR核心为了解决两个问题：

    1. 判断消息是否足够安全。
    2. 选 leader 时尽量不丢数据。

### 判断消息是否足够安全
当producer配置：acks=all。Kafka不是等所有replicas都写成功，而是等当前 ISR 里的副本都确认。例如：

replicas = [1, 2, 3]，ISR = [1, 2]，此时acks=all只需要leader-1和follower-2写成功，不会等已经落后的follower-3，**<font style="color:#DF2A3F;">这样避免一个慢副本拖垮整个消息写入。</font>**

### 选 leader 时尽量不丢数据
leader挂了以后，Kafka优先从ISR里选新leader。因为ISR里的副本代表“最近是跟上leader的”，更可能拥有已提交的数据。如果选择了非ISR中的副本，则说明可能是缺数据的，非ISR副本当了leader，可能会丢消息。

# Kafka ISR 底层原理与源码链路
已经知道了Kafka ISR的基本概念以及解决什么问题，下文梳理Kafka ISR从初始化、运行时变更、controller 提交确认，到 broker 应用 metadata 的完整链路。

## Kafka中ISR的两层状态
在Kafka中，ISR有两层状态：

    - controlle权威状态：PartitionRegistration.isr
    - leader broker运行时状态：Partition.partitionState.isr

ISR 的初始化以及变更都是是 controller metadata log中实现，leader broker只能根据 follower 复制进度提出 ISR 变更，真正生效必须经过 controller 校验并写入 metadata log。

kafka的ISR是controller说了算，也就是KRaft的controller leader最终控制的。

## Controller Leader 初始化ISR
ISR 的初始值是在controller创建topic/partition时生成的

源码链路：

```latex
ReplicationControlManager.createTopics()
  -> createTopic()
  -> 生成每个 partition 的 replica assignment
  -> 过滤出 active replicas 作为初始 ISR
  -> buildPartitionRegistration(...)
  -> PartitionRegistration.isr
  -> PartitionRegistration.toRecord(...)
  -> PartitionRecord 写入 metadata log
```

关键逻辑在 ReplicationControlManager.createTopic()：

```latex
List<Integer> isr = partitionAssignment.replicas().stream()
    .filter(clusterControl::isActive)
    .toList();
```

这里的含义是：创建 topic 时，controller 会先确定每个 partition 的副本列表 replicas，然后从这些副本里筛出当前可用的 broker，也就是 active replicas，作为这个 partition 的初始 ISR。

然后 controller 用这些信息构造 PartitionRegistration：

```latex
return new PartitionRegistration.Builder()
    .setReplicas(...)
    .setIsr(...)
    .setLeader(isr.get(0))
    .setLeaderEpoch(0)
    .setPartitionEpoch(0)
    .build();
```

这段源码有几个需要注意的点：

    - replicas：这个 partition 的完整副本集合。
    - isr：创建时处于 active 状态的副本集合。
    - leader：默认取isr.get(0)，也就是初始 ISR 里的第一个副本。



PartitionRegistration 会被转成 metadata record：

```latex
records.add(info.toRecord(topicId, partitionIndex, ...));
```

随后在QuorumController.ControllerWriteEvent.run()中被追加到metadata log中。

```latex
long lastOffset = raftClient.prepareAppend(controllerEpoch, records);
```

## Broker 如何应用初始 ISR
Broker 不会自己生成初始 ISR，它是从 metadata log 里读取 controller 写入的PartitionRecord，然后应用到本地分区状态中。

源码链路：

```latex
metadata log 中的 PartitionRecord
  -> MetadataDelta.replay()
  -> TopicsDelta.replay()
  -> TopicDelta.replay(PartitionRecord)
  -> new PartitionRegistration(record)
  -> MetadataImage / TopicsImage 中保存 PartitionRegistration.isr
  -> BrokerMetadataPublisher.onMetadataUpdate()
  -> ReplicaManager.applyDelta()
  -> applyLocalLeadersDelta() / applyLocalFollowersDelta()
  -> Partition.makeLeader() / Partition.makeFollower()
  -> Partition.updateAssignmentAndIsr()
  -> Partition.partitionState.isr
```

关键逻辑在 TopicDelta.replay(PartitionRecord)：

```latex
partitionChanges.put(record.partitionId(), new PartitionRegistration(record));
```

这里的含义是：broker 消费 metadata log 时，遇到 PartitionRecord，会把 record 反序列化成 PartitionRegistration。而 PartitionRegistration 里面就包含 controller 初始化时写入的 ISR。

也就是说，controller 写入的是：PartitionRecord.isr，broker读取后恢复成：PartitionRegistration.isr。

然后 broker 侧的 metadata publisher 会把新的 metadata image 应用到运行时模块：

```latex
replicaManager.applyDelta(topicsDelta, newImage)
```



ReplicaManager.applyDelta() 会判断当前 broker 在这些 partition 里的角色：

```latex
如果当前 broker 是 leader
  -> applyLocalLeadersDelta()
  -> Partition.makeLeader()

如果当前 broker 是 follower
  -> applyLocalFollowersDelta()
  -> Partition.makeFollower()
```

leader 场景下，核心逻辑进入 Partition.makeLeader()：

```latex
partition.makeLeader(info.partition, isNew, offsetCheckpoints, Some(info.topicId), ...)
```

这里传进去的 info.partition 就是 PartitionRegistration，里面带着 controller 写入的初始 ISR。

随后 makeLeader() 会调用：

```latex
updateAssignmentAndIsr(...)
```

把 PartitionRegistration 里的副本信息、ISR、leader epoch、partition epoch 等信息应用到 broker 本地的 Partition 对象中。

最终落到 broker 运行时状态里：**<font style="color:#DF2A3F;">Partition.partitionState.isr</font>**。后续leader副本和follower副本会用这份ISR做不通的逻辑。

    - leader broker 会用本地 ISR 判断 acks=all、min.insync.replicas、HW 推进、ISR 扩缩容。
    - follower broker 也会应用这份 ISR，但 follower 不负责决定 ISR 变更，它主要根据 leader 信息启动 fetch，继续追 leader 数据。



## ISR 如何判断消息是否足够安全
消息是否“足够安全”，核心不是看所有 replicas，而是看 **ISR 里的副本是否满足要求**。

源码链路：

```plain
KafkaApis.handleProduceRequest()
  -> replicaManager.handleProduceAppend()
  -> ReplicaManager.appendRecords()
  -> ReplicaManager.appendRecordsToLeader()
  -> ReplicaManager.appendToLocalLog()
  -> Partition.appendRecordsToLeader()
  -> 判断 ISR 数量是否满足 min.insync.replicas
  -> leader 本地追加消息
  -> 如果 requiredAcks = -1，进入 DelayedProduce 等待 ISR 副本追上
  -> Partition.checkEnoughReplicasReachOffset()
  -> 判断 high watermark 是否达到 requiredOffset
  -> 返回 produce response
```

关键入口在 `KafkaApis.handleProduceRequest()`：

```plain
replicaManager.handleProduceAppend(
  timeout = produceRequest.timeout.toLong,
  requiredAcks = produceRequest.acks,
  ...
)
```

这里的 `requiredAcks` 就是 producer 配置里的 `acks`。

常见情况：

```plain
acks = 0
  -> broker 不需要返回确认

acks = 1
  -> leader 写入成功就返回成功

acks = -1 / all
  -> leader 写入成功后，还要等 ISR 副本复制到足够位置
```

真正判断 ISR 是否足够的逻辑在 `Partition.appendRecordsToLeader()`：

```plain
val minIsr = effectiveMinIsr(leaderLog)
val inSyncSize = partitionState.isr.size

if (inSyncSize < minIsr && requiredAcks == -1) {
  throw new NotEnoughReplicasException(...)
}
```

这里的含义是：如果 producer 使用 `acks=all`，那么 leader 在写入消息之前，会先检查当前 ISR 数量是否小于 `min.insync.replicas`。

如果：

```plain
ISR 数量 < min.insync.replicas
```

那么这条消息连 leader 本地都不会写入，直接返回：

```plain
NOT_ENOUGH_REPLICAS
```

如果 ISR 数量满足要求，leader 才会执行本地追加：

```plain
val info = leaderLog.appendAsLeader(...)
```

但是对于 `acks=all`，leader 本地写成功还不代表请求可以立刻返回成功。因为 `acks=all` 的语义是：消息需要被 ISR 中的副本复制到足够安全的位置。

所以写入之后，会进入 `DelayedProduce` 等待：

```plain
ReplicaManager.maybeAddDelayedProduce(...)
```

判断是否需要等待的条件是：

```plain
requiredAcks == -1
```

也就是只有 `acks=all` 才会走这条等待 ISR 确认的逻辑。



等待过程中，会反复检查：

```plain
partition.checkEnoughReplicasReachOffset(requiredOffset)
```

关键逻辑在 `Partition.checkEnoughReplicasReachOffset()`：

```plain
if (leaderLog.highWatermark >= requiredOffset) {
  if (minIsr <= curMaximalIsr.size)
    (true, Errors.NONE)
  else
    (true, Errors.NOT_ENOUGH_REPLICAS_AFTER_APPEND)
} else {
  (false, Errors.NONE)
}
```

这里的判断重点是：

```plain
leaderLog.highWatermark >= requiredOffset
```

requiredOffset是本次写入消息的下一个 offset。如果 high watermark 已经推进到这个 offset，说明这条消息已经被足够的 ISR 副本复制到了，可以认为消息足够安全。

这段源码有几个需要注意的点：

+ `partitionState.isr.size`：当前 ISR 副本数量。
+ `min.insync.replicas`：topic 或 broker 配置的最小同步副本数。
+ `requiredAcks == -1`：也就是 producer 的 `acks=all`。
+ `requiredOffset`：这次 produce 请求需要等待复制完成的目标 offset。
+ `highWatermark`：Kafka 判断消息是否对消费者可见、是否足够复制的重要边界。



完整判断可以理解成两段：

第一段，写入前判断：

```plain
如果 acks=all，并且 ISR 数量 < min.insync.replicas
  -> 拒绝写入
  -> 返回 NOT_ENOUGH_REPLICAS
```

第二段，写入后判断：

```plain
leader 写入成功
  -> 等待 ISR 副本 fetch 复制
  -> follower 追上后推进 high watermark
  -> high watermark >= requiredOffset
  -> produce 请求返回成功
```

如果消息已经写入 leader，但是等待过程中 ISR 缩小，导致 ISR 数量低于 `min.insync.replicas`，则可能返回：

```plain
NOT_ENOUGH_REPLICAS_AFTER_APPEND
```

也就是：消息已经 append 到 leader 了，但 append 之后 ISR 条件不再满足。

Kafka 判断消息是否足够安全，核心看两个条件：

1. 写入前 ISR 数量是否 >= min.insync.replicas
2. 写入后 high watermark 是否推进到本次 produce 的 requiredOffset

## 选 leader 时尽量不丢数据
Kafka 选 leader 时，核心原则是：优先从 ISR 里选 leader。因为 ISR 里的副本表示：这些副本已经和 leader 保持同步，至少已经复制到了 Kafka 认为安全的位置。所以只要新 leader 来自 ISR，就可以最大程度保证不丢已经提交的数据。

源码链路：

```plain
broker 状态变化 / leader 不可用
  -> ReplicationControlManager.generateLeaderAndIsrUpdates()
  -> PartitionChangeBuilder
  -> electLeader()
  -> electAnyLeader()
  -> isValidNewLeader()
  -> 优先选择 ISR 中可用副本
  -> PartitionChangeRecord.setLeader(...)
  -> 写入 metadata log
```

关键逻辑在PartitionChangeBuilder。它定义了几种选举类型：

```plain
public enum Election {
    PREFERRED,
    ONLINE,
    UNCLEAN
}
```

这几个类型的含义是：

+ `PREFERRED`：优先选 preferred replica，但前提是它在 ISR 中并且可用。
+ `ONLINE`：从 ISR 中选一个可用副本作为 leader。
+ `UNCLEAN`：优先从 ISR 中选；如果 ISR 里没有可用副本，才允许从 ISR 外选。

默认是：Election.ONLINE。也就是说，默认 leader election 是 clean election，只会从 ISR 里选 leader。

真正选 leader 的入口是：

```plain
ElectionResult electLeader() {
    if (election == Election.PREFERRED) {
        return electPreferredLeader();
    }

    return electAnyLeader();
}
```

普通场景走的是 electAnyLeader()：

```plain
private ElectionResult electAnyLeader() {
    if (isValidNewLeader(partition.leader)) {
        return new ElectionResult(partition.leader, false);
    }

    Optional<Integer> onlineLeader = targetReplicas.stream()
        .filter(this::isValidNewLeader)
        .findFirst();

    if (onlineLeader.isPresent()) {
        return new ElectionResult(onlineLeader.get(), false);
    }

    if (election == Election.UNCLEAN) {
        Optional<Integer> uncleanLeader = targetReplicas.stream()
            .filter(isAcceptableLeader::test)
            .findFirst();

        if (uncleanLeader.isPresent()) {
            return new ElectionResult(uncleanLeader.get(), true);
        }
    }

    return new ElectionResult(NO_LEADER, false);
}
```

这段源码有几个需要注意的点：

+ 先判断当前 leader 是否仍然有效。
+ 如果当前 leader 不可用，再从targetReplicas中找一个isValidNewLeader()的副本。
+ isValidNewLeader()的核心约束就是：副本必须有资格成为 leader，正常情况下要在 ISR 里。
+ 只有election == Election.UNCLEAN时，才会从 ISR 外选择 leader。

也就是说，Kafka 默认不会随便找一个 alive replica 当 leader，而是优先找 ISR 里的副本。

这就是“不丢数据”的核心。因为 ISR 里的副本满足：

```plain
它已经跟上 leader，被 controller 认为是 in-sync replica。
```

所以从 ISR 里选 leader，意味着新 leader 拥有已经提交的数据。如果 ISR 里没有可用副本，会发生什么？默认情况下：

```plain
没有可用 ISR 副本
  -> 不选 leader
  -> partition 进入 unavailable
  -> 宁可不可用，也不从 ISR 外选 leader
```

这就是 Kafka 默认偏向一致性的地方。只有开启：

```plain
unclean.leader.election.enable = true
```

才允许 unclean leader election。

源码里是在 ReplicationControlManager.generateLeaderAndIsrUpdates() 中判断：

```plain
if (configurationControl.uncleanLeaderElectionEnabledForTopic(topic.name)) {
    builder.setElection(PartitionChangeBuilder.Election.UNCLEAN);
}
```

配置判断在：

```plain
configurationControl.uncleanLeaderElectionEnabledForTopic(topic.name)
```

如果开启了 unclean election，electAnyLeader()才会走这段：

```plain
Optional<Integer> uncleanLeader = targetReplicas.stream()
    .filter(isAcceptableLeader::test)
    .findFirst();
```

这时选出来的 leader 不一定在 ISR 里，所以可能缺少旧 leader 上已经写入的数据。

Kafka 对这种情况会显式标记：

```plain
if (electionResult.unclean) {
    record.setIsr(List.of(electionResult.node));
    record.setLeaderRecoveryState(LeaderRecoveryState.RECOVERING.value());
}
```

这里的含义是：

+ 如果发生 unclean election，新 leader 会被强制设置成唯一 ISR。
+ 原来的 ISR 会被替换掉。
+ leader recovery state 会变成 `RECOVERING`。
+ 这类选举可能导致数据丢失。

源码注释里也写得很直接：

```plain
// This can result in data loss!
```

所以 Kafka 的策略可以总结成两层：

第一层，默认安全策略：

```plain
只从 ISR 中选 leader
  -> 尽量保证新 leader 拥有已提交数据
  -> 避免数据丢失
```

第二层，可用性兜底策略：

```plain
如果开启 unclean.leader.election.enable
  -> ISR 中无可用副本时，可以从 ISR 外选 leader
  -> partition 可以恢复可用
  -> 但可能丢数据
```

总结：Kafka 选 leader 时尽量不丢数据，是因为默认只允许 ISR 中的副本成为 leader；只有显式开启 unclean leader election，才会牺牲数据安全换取可用性。

