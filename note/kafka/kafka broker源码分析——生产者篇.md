当前分析版本是kafka最新版本（版本随时变化，最新分析代码请关注仓库：[https://github.com/coderbruis/kafka](https://github.com/coderbruis/kafka)   **<font style="color:#DF2A3F;">source_code_analysis分支</font>**，底层原理持续更新）

# KafkaRequestHandler核心流程
`KafkaRequestHandler.run()` 是在 **创建 **`**KafkaRequestHandlerPool**`** 时触发的**。

位置在 [KafkaRequestHandler.scala (line 261)]：

```plain
private def createHandler(id: Int): Unit = {
  runnables += new KafkaRequestHandler(...)
  KafkaThread.daemon("data-plane-kafka-request-handler-" + id, runnables(id)).start()
}
```

`KafkaThread.daemon(...).start()` 会启动线程，然后进入：

```plain
KafkaRequestHandler.run()
```

而 `KafkaRequestHandlerPool` 是在 [BrokerServer.startup() (line 515)]：

```plain
dataPlaneRequestHandlerPool = sharedServer.requestHandlerPoolFactory.createPool(...)
```

所以启动链路是：

```plain
BrokerServer.startup
  -> create KafkaApis
  -> create KafkaRequestHandlerPool
    -> create KafkaRequestHandler
    -> KafkaThread.start()
      -> KafkaRequestHandler.run()
```

消费者 `FetchRequest` 从 broker 网络入口到 `KafkaRequestHandler` 的流转是：

```plain
Producer 发送消息到 broker

Acceptor.run
   -> acceptNewConnections()
   -> 接收 TCP 连接
   -> assignNewConnection(...)
   -> Processor.accept(socketChannel)

Processor.run
   -> configureNewConnections()
      注册新连接到 selector

   -> poll()
      从 socket 读取网络数据

   -> processCompletedReceives()
      解析 RequestHeader
      创建 RequestContext
      创建 Request
      requestChannel.sendRequest(req)
```

关键代码在 [SocketServer.scala (line 1000)]：

```plain
requestChannel.sendRequest(req)
selector.mute(connectionId)
```

`sendRequest` 会把请求放进 `RequestChannel` 的阻塞队列：

```plain
requestQueue.put(request)
```

然后：

```plain
KafkaRequestHandler.run
   -> requestChannel.receiveRequest(300)
   -> 取到 Processor 放进来的 Request
   -> apis.handle(request, requestLocal)
```

对应 [KafkaRequestHandler.scala (line 117)]：

```plain
val req = requestChannel.receiveRequest(300)
...
case request: Request =>
  apis.handle(request, requestLocal)
```

然后：

```plain
KafkaApis.handle
   -> 根据 request.header.apiKey 分发
   -> FetchRequest 命中 ApiKeys.FETCH
   -> handleFetchRequest(request)
```

对应 [KafkaApis.scala (line 171)]：

```plain
request.header.apiKey match {
  case ApiKeys.PRODUCE => handleProduceRequest(request)
}
```

简化成一条链：

```plain
Consumer ProduceRequest
  -> SocketServer listener
  -> Acceptor.acceptNewConnections
  -> Processor.accept
  -> Processor.run
  -> processCompletedReceives
  -> RequestChannel.sendRequest
  -> KafkaRequestHandler.run
  -> RequestChannel.receiveRequest
  -> KafkaApis.handle
  -> handleFetchRequest
```

Processor 负责把网络请求解析成 Request 并塞进 RequestChannel；KafkaRequestHandler 线程一直阻塞/轮询 RequestChannel，拿到请求后调用 KafkaApis.handle() 进入业务逻辑。

# KafkaApis.handle()
Kakfa producer在broker端，最核心的调用：

```java
KafkaApis.handleProduceRequest()
  -> ReplicaManager.handleProduceAppend()
    -> ReplicaManager.appendRecords()
      -> ReplicaManager.appendRecordsToLeader()
        -> ReplicaManager.appendToLocalLog()
          -> Partition.appendRecordsToLeader()
            -> UnifiedLog.appendAsLeader()
              -> UnifiedLog.append()
                -> LocalLog.append()
                  -> LogSegment.append()
                    -> FileRecords.append()
                      -> MemoryRecords.writeFullyTo(FileChannel)
```



KafkaApis.handle() 是 broker API 分发入口。

核心源码：

```java
override def handle(request: Request, requestLocal: RequestLocal): Unit = {
  try {
    if (!apiVersionManager.isApiEnabled(request.header.apiKey, request.header.apiVersion)) {
      throw new IllegalStateException(...)
    }

    request.header.apiKey match {
      case ApiKeys.PRODUCE => handleProduceRequest(request, requestLocal)
      case ApiKeys.FETCH => handleFetchRequest(request)
      case ApiKeys.LIST_OFFSETS => handleListOffsetRequest(request)
      case ApiKeys.METADATA => handleTopicMetadataRequest(request)
      case ApiKeys.OFFSET_COMMIT => handleOffsetCommitRequest(request, requestLocal).exceptionally(handleError)
      case ApiKeys.OFFSET_FETCH => handleOffsetFetchRequest(request).exceptionally(handleError)
      case ApiKeys.FIND_COORDINATOR => handleFindCoordinatorRequest(request)
      case ApiKeys.JOIN_GROUP => handleJoinGroupRequest(request, requestLocal).exceptionally(handleError)
      case ApiKeys.HEARTBEAT => handleHeartbeatRequest(request).exceptionally(handleError)
      ...
    }
  }
}
```



## KafkaApis.handleProduceRequest()
KafkaApis.handleProduceRequest() 最核心做了三件事：

+ 校验请求能不能写，校验事务权限、Topic 写权限、Topic/Partition 是否存在、records 格式是否合法。
+ 构造 Produce 响应返回客户端，合并成功/失败结果，处理限流、leader 信息、acks=0 特殊逻辑，然后返回或关闭连接。
+ 把合法数据交给 ReplicaManager 写入日志，调用 replicaManager.handleProduceAppend(...)，真正把消息追加到对应分区副本日志里，并按 acks 等语义等待结果。

总结，handleProduceRequest()是 Broker 处理生产者写消息请求的入口，负责校验、转交副本写入、返回写入结果。



```scala
if (RequestUtils.hasTransactionalRecords(produceRequest)) {
  val isAuthorizedTransactional = produceRequest.transactionalId != null &&
  authHelper.authorize(request.context, WRITE, TRANSACTIONAL_ID, produceRequest.transactionalId)
  if (!isAuthorizedTransactional) {
    requestHelper.sendErrorResponseMaybeThrottle(request, Errors.TRANSACTIONAL_ID_AUTHORIZATION_FAILED.exception)
    return
  }
}
```

事务消息必须先校验 transactionalId 写权限，避免未授权写入进入副本追加流程。



```scala
@nowarn("cat=deprecation")
def sendResponseCallback(responseStatus: Map[TopicIdPartition, PartitionResponse]): Unit = {
  ...
  if (produceRequest.acks == 0) {
    ...
    if (errorInResponse) {
      ...
      requestChannel.closeConnection(request, new ProduceResponse(mergedResponseStatus.asJava).errorCounts)
    } else {
      requestHelper.sendNoOpResponseExemptThrottle(request)
    }  
  } else {
    requestChannel.sendResponse(request, new ProduceResponse(mergedResponseStatus.asJava, maxThrottleTimeMs, nodeEndpoints.values.toList.asJava))
  }
}
```

 构造并发送 Produce 响应，统一合并追加结果和前置校验失败结果。



```scala
if (authorizedRequestInfo.isEmpty)
sendResponseCallback(Map.empty)
else {
  val internalTopicsAllowed = request.header.clientId == "__admin_client"
  val transactionSupportedOperation = AddPartitionsToTxnManager.produceRequestVersionToTransactionSupportedOperation(request.header.apiVersion())
  // 交给 ReplicaManager 追加日志，并按 acks/min.insync.replicas 等语义完成响应。
  replicaManager.handleProduceAppend(
    timeout = produceRequest.timeout.toLong,
    requiredAcks = produceRequest.acks,
    internalTopicsAllowed = internalTopicsAllowed,
    transactionalId = produceRequest.transactionalId,
    entriesPerPartition = authorizedRequestInfo,
    responseCallback = sendResponseCallback,
    recordValidationStatsCallback = processingStatsCallback,
    requestLocal = requestLocal,
    transactionSupportedOperation = transactionSupportedOperation)

  // 追加后清理请求中的 records 引用，避免延迟请求持有大对象影响 GC。
  produceRequest.clearPartitionRecords()
}
```

把合法数据交给 ReplicaManager 写入日志，调用 replicaManager.handleProduceAppend(...)，真正把消息追加到对应分区副本日志里，并按 acks 等语义等待结果。

## ReplicaManager
### ReplicaManager.handleProduceAppend()
handleProduceAppend：处理 Produce请求入口, 这个方法最核心的三件事：

+ 检查请求里有没有事务 batch。
+ 如果是事务消息，先确认这些分区已经加入事务。
+ 处理事务校验失败、重试、错误转换。
+ 校验完成后再调用 `appendRecords()`。



```scala
val transactionalProducerInfo = mutable.HashSet[(Long, Short)]()
val topicPartitionBatchInfo = mutable.Map[TopicPartition, Int]()
val topicIds = entriesPerPartition.keys.map(tp => tp.topic() -> tp.topicId()).toMap
entriesPerPartition.foreachEntry { (topicIdPartition, records) =>
  // 提取事务 batch 的 producer 信息，后面用于校验这些分区是否已经加入事务。
  val transactionalBatches = records.batches.asScala.filter(batch => batch.hasProducerId && batch.isTransactional)
  transactionalBatches.foreach(batch => transactionalProducerInfo.add(batch.producerId, batch.producerEpoch))
  if (transactionalBatches.nonEmpty) topicPartitionBatchInfo.put(topicIdPartition.topicPartition(), records.firstBatch.baseSequence)
}
if (transactionalProducerInfo.size > 1) {
  throw new InvalidPidMappingException("Transactional records contained more than one producer ID")
}
```

检查请求里有没有事务 batch。



```scala
appendRecords(
  timeout = timeout,
  requiredAcks = requiredAcks,
  internalTopicsAllowed = internalTopicsAllowed,
  origin = AppendOrigin.CLIENT,
  entriesPerPartition = entriesWithoutErrorsPerPartition,
  responseCallback = newResponseCallback,
  recordValidationStatsCallback = recordValidationStatsCallback,
  requestLocal = newRequestLocal,
  verificationGuards = verificationGuards
)
```

校验完成后再调用 appendRecords()。

### ReplicaManager.appendRecords()
这个方法核心作用就是：**把已经校验过的 Produce 数据追加到本 broker 作为 leader 的分区日志里，并根据 **`**acks**`** 判断是否需要等待副本复制后再返回。**

拆开就是：

1. **检查写入前置条件**  
分区是否存在、当前 broker 是否 leader、`acks` 是否合法。
2. **写入 leader 本地日志**  
调用分区日志追加逻辑，把 records append 到 log。
3. **处理响应时机**  
如果不需要等待副本，立即回调；如果需要等 ISR 复制，则放入 delayed produce 等待完成或超时。



```scala
 def appendRecords(timeout: Long,
                    requiredAcks: Short,
                    internalTopicsAllowed: Boolean,
                    origin: AppendOrigin,
                    entriesPerPartition: Map[TopicIdPartition, MemoryRecords],
                    responseCallback: util.Map[TopicIdPartition, PartitionResponse] => Unit,
                    recordValidationStatsCallback: Map[TopicIdPartition, RecordValidationStats] => Unit = _ => (),
                    requestLocal: RequestLocal = RequestLocal.noCaching,
                    verificationGuards: Map[TopicPartition, VerificationGuard] = Map.empty,
                    transactionVersion: Short = TransactionVersion.TV_UNKNOWN): Unit = {
    // 核心总结：控制 Produce 追加的整体响应语义，先写 leader 本地日志，再按 acks 决定立即返回或等待复制。
    if (!isValidRequiredAcks(requiredAcks)) {
      sendInvalidRequiredAcksResponse(entriesPerPartition, responseCallback)
      return
    }

    // 先完成 leader 本地追加，得到每个分区的初始写入结果。
    val localProduceResults = appendRecordsToLeader(
      requiredAcks,
      internalTopicsAllowed,
      origin,
      entriesPerPartition,
      requestLocal,
      defaultActionQueue,
      verificationGuards,
      transactionVersion
    )

    val produceStatus = buildProducePartitionStatus(localProduceResults)

    // 回传 record 校验/转换统计，用于 Produce 请求的处理指标。
    recordValidationStatsCallback(localProduceResults.map { case (k, v) =>
      k -> v.logAppendSummary().recordValidationStats()
    })

    // 根据 requiredAcks 判断是否需要进入 delayed produce 等待 ISR 副本追上。
    maybeAddDelayedProduce(
      requiredAcks,
      timeout,
      entriesPerPartition,
      localProduceResults,
      produceStatus,
      responseCallback
    )
  }
```



### ReplicaManager.appendRecordsToLeader()
把一批 Produce records 写入当前 broker 上对应分区的 leader 本地日志，并返回每个分区的本地追加结果；然后调用 appendToLocalLog()，对 entriesPerPartition 逐分区处理，把 records 写到本地 leader replica 的 log。

```scala
val localProduceResultsWithTopicId = appendToLocalLog(
  internalTopicsAllowed = internalTopicsAllowed,
  origin,
  entriesPerPartition,
  requiredAcks,
  requestLocal,
  verificationGuards.toMap,
  transactionVersion
)
```

entriesPerPartition是一个map类型的变量，表示这次请求要写入的哪些分区。遍历entriesPerPartition的过程是在appendToLocalLog()中完成的。



```scala
ddCompletePurgatoryAction(actionQueue, localProduceResultsWithTopicId)
```

这个动作会根据 leader HW 是否推进，唤醒可能已经满足条件的 delayed operations，例如：

    - delayed produce

用于 Produce 请求，典型场景是 acks=all。leader 本地写成功后，还要等 ISR 副本复制到目标 offset。没满足前，请求挂起；满足后返回成功。如果超时，就按当前状态返回，比如 NOT_ENOUGH_REPLICAS_AFTER_APPEND。

    - delayed fetch

用于 Fetch 请求，包括消费者 fetch，也包括 follower 副本从 leader 拉数据。如果当前没有足够数据满足 fetch 条件，比如没有新消息、没达到 fetch.min.bytes、或者 high watermark 还没推进，请求会先挂起。等新数据写入或 HW 推进后，再检查是否可以返回。

    - delayed delete records

用于 DeleteRecords 请求。DeleteRecords 会推进分区的 logStartOffset，但它需要等相关副本状态或 high watermark 条件满足后才能完成。leader 写入或 HW 推进后，可能让挂起的 delete records 请求完成。

    - delayed share fetch

用于 Kafka Share Consumer / share group 相关的 fetch 请求。它和普通 fetch 类似，也是等待有可返回的数据或分区状态变化。区别是它服务的是 share group 的消费模型，而不是传统 consumer group 的普通 fetch。



在 appendRecordsToLeader() 之后唤醒它们，是因为一次 leader append 可能改变了这些条件：

    - LEO 增加
    - HW 推进
    - 有新数据可读
    - 某些等待 offset 的请求满足了

所以 broker 会让这些 delayed operation 重新检查自己是否可以完成。



小结：这个方法处在 Kafka 写入链路的中间层：它不是网络请求入口，也不是底层日志 append 的最终实现，而是 ReplicaManager 层的“批量写 leader 本地副本 + 收集结果 + 唤醒等待操作”的封装。

### ReplicaManager.appendToLocalLog()
遍历 Produce 请求中的各分区 records，找到本 broker 本地的 Partition 并委托给 Partition.appendRecordsToLeader() 执行追加，同时按分区包装返回成功或失败结果。

```scala
entriesPerPartition.map { case (topicIdPartition, records) =>
    ...
    // 找到本地分区，某个分区在这个broker托管的的副本分区，可能是leader副本，也可能是follower副本
    val partition = getPartitionOrException(topicIdPartition)
    // 进入分区级写入，继续检查 leader、本地日志和 min ISR。注意此时并没有判断到哪个partition是否是leader，判断逻辑在下一层
    val info = partition.appendRecordsToLeader(records, origin, requiredAcks, requestLocal,
    verificationGuards.getOrElse(topicIdPartition.topicPartition(), VerificationGuard.SENTINEL), transactionVersion)
    ...
}
```

这个方法其实算一个过渡方法，核心逻辑都委托给了appendRecordsToLeader()执行。

## Partition
### Partition.appendRecordsToLeader()
确认当前 broker 持有该 partition 的 leader replica，然后把 records 以 leader 身份写入本地日志。

```scala
def leaderLogIfLocal: Option[UnifiedLog] = {
  log.filter(_ => isLeader)
}

def isLeader: Boolean = leaderReplicaIdOpt.contains(localBrokerId)
```



```scala
val (info, leaderHWIncremented) = inReadLock(leaderIsrUpdateLock, () => {
  // 判断分区是否是leader
  leaderLogIfLocal match {
    case Some(leaderLog) =>
    val minIsr = effectiveMinIsr(leaderLog)
    val inSyncSize = partitionState.isr.size

    // requiredAcks == -1 表示acks=all
    // acks=all 时必须满足 min.insync.replicas，否则写入即使成功也无法达到复制安全语义。
    if (inSyncSize < minIsr && requiredAcks == -1) {
      throw new NotEnoughReplicasException(s"The size of the current ISR : $inSyncSize " +
                                                 s"is insufficient to satisfy the min.isr requirement of $minIsr for partition $topicPartition, " +
                                                 s"live replica(s) broker.id are : $inSyncReplicaIds")
    }

    // 进入逻辑日志层，以 leader 身份分配 offset/epoch 并追加到本地日志。
    val info = leaderLog.appendAsLeader(records, this.leaderEpoch, origin, requestLocal, verificationGuard, transactionVersion)

    // ISR 可能只剩 leader 自己，写入后需要尝试推进 high watermark。
    (info, maybeIncrementLeaderHW(leaderLog))

    case None =>
    // 当前 broker 不是该分区 leader，不能接收 Produce 写入。
    throw new NotLeaderOrFollowerException("Leader not local for partition %s on broker %d"
                                                 .format(topicPartition, localBrokerId))
  }
})
```

关键点：

1. **加读锁**  
用 `leaderIsrUpdateLock` 保护 leader/ISR 状态，避免写入时 leader 或 ISR 状态并发变化。目的就是保证：一次 leader append 过程中，leader 身份和 ISR 判断是一致的，不会被并发 leader/ISR 更新打断。
2. **必须是本地 leader**  
`leaderLogIfLocal` 有值才允许写。否则抛 `NotLeaderOrFollowerException`。
3. **acks=all 时检查 min ISR**  
只有 `requiredAcks == -1` 时检查：

```plain
inSyncSize < minIsr
```

不满足就抛 `NotEnoughReplicasException`，拒绝写入。

4. **真正追加日志**  
调用：

```plain
leaderLog.appendAsLeader(records, leaderEpoch, origin, ...)
```

这里会做 leader append：分配 offset、写入 log、处理 producer/transaction 相关校验。

5. **尝试推进 HW**  
写完后调用：

```plain
maybeIncrementLeaderHW(leaderLog)
```

如果 ISR 条件允许，可能推进 high watermark。

6. **返回 LogAppendInfo**  
最后把 HW 是否推进写进 `LogAppendInfo`：

```plain
info.copy(if (leaderHWIncremented) LeaderHwChange.INCREASED else LeaderHwChange.SAME)
```



## UnifiedLog
### UnifiedLog.appendAsLeader()
以 leader 身份追加 records，必要时重新校验消息、分配 offset、写入 active segment，并更新幂等/事务相关状态。



```plain
validateAndAssignOffsets = origin != AppendOrigin.RAFT_LEADER
```

validateAndAssignOffsets=true，则以普通leader写入。false则以Raft leader写入。

1. 普通leader写入
    1. UnifiedLog 会从当前 logEndOffset 开始重新分配 offset
    2. 会设置 partition leader epoch
    3. 会更新幂等 producer、事务状态、事务索引
2. Raft leader写入
    1. UnifiedLog 不重新分配 offset
    2. 使用 records 里已经带好的 offset
    3. 只校验这些 offset 是否能合法追加到当前日志末尾

原因是：Raft 日志的 offset/位置由 Raft 层控制。Raft leader 已经决定了日志条目的顺序和 offset，UnifiedLog 不能再改，否则会破坏 Raft 复制一致性。



```scala
return append(records, origin, validateAndAssignOffsets, leaderEpoch, Optional.of(requestLocal),
              verificationGuard, false, RecordBatch.CURRENT_MAGIC_VALUE, transactionVersion);
```

调用append()执行追加核心逻辑。

### UnifiedLog.append()
UnifiedLog.append()是追加日志最核心的方法，leader/follower写入最终都走到这个方法里。



```scala
maybeFlushMetadataFile();
```

首先会调用maybeFlushMetadataFile()，准备元数据，确保 topic metadata 已落盘。

**这里的元数据和KRaft里维护的有什么区别？为什么不直接用KRaft同步了的元数据呢？**

因为KRaft 记录的是集群当前元数据，不是每个 broker 磁盘目录的历史归属。KRaft 负责说明“集群现在应该是什么”；partition.metadata 负责证明“本地磁盘这份数据实际是谁的”。Kafka 写消息前 flush partition.metadata，是为了保证只要日志目录里有真实数据，它就一定有可靠的 topicId 身份，重启后不会把旧 topic 的数据错接到新 topic 上。



```scala
LogAppendInfo appendInfo = analyzeAndValidateRecords(...)
```

校验 record 格式、大小、时间戳、offset 合法性，生成 append 摘要。



```scala
if (appendInfo.firstOrLastOffsetOfFirstBatch() < localLog.logEndOffset())
    throw UnexpectedAppendOffsetException
```

校验 offset 不能倒退，follower 使用 leader 已分配的 offset，不能覆盖本地日志尾部。



```scala
assignEpochStartOffset(...)
```

更新 leader epoch cache。



```scala
maybeRoll(...)
```

检查单次写入大小并可能 roll segment。roll segment就是当前日志段写不下或该换新文件了，Kafka 新建一个新的 log segment，后续消息写到新 segment 里。



```scala
analyzeAndValidateProducerState(...)
```

校验幂等/事务 producer 状态，识别重复 batch、校验 sequence/epoch、收集事务完成信息。



```scala
localLog.append(appendInfo.lastOffset(), validRecords);
```

真正落本地日志。



```scala
updateHighWatermarkWithLogEndOffset();
```

刚写完一批消息，log end offset 变大了，所以 Kafka 顺手让 high watermark 相关的 offset metadata 跟着刷新一下，避免它还指向旧的日志位置信息。



```scala
producerStateManager.update(...)
segment.updateTxnIndex(...)
maybeIncrementFirstUnstableOffset()
```

更新 producer/transaction 状态。



```scala
if (localLog.unflushedMessages() >= config().flushInterval) flush(false);
```

如果距离上次刷盘后，已经写入的消息数达到配置阈值，就触发一次 flush，把日志相关数据从 OS page cache 刷到磁盘。所以日志在刷盘之前，都是一直保存在Page Cache中的。

config().flushInterval)对应配置是：

```scala
log.flush.interval.messages
```

## LocalLog
把 records 追加到当前 active log segment，然后更新本地 log end offset，推进LEO。

代码就两步，核心流程在LogSegment.append()。

```plain
segments.activeSegment().append(lastOffset, records);
updateLogEndOffset(lastOffset + 1);
```

## LogSegment
LogSegment.append() 是 segment 级别的底层追加方法，不负责完整写入语义校验。上层 UnifiedLog 先做 offset、segment roll、producer/事务状态等处理，然后经 LocalLog.append() 调到它。

```scala
public void append(long largestOffset,
                       MemoryRecords records) throws IOException {
  // 核心总结：把 records 追加到当前 segment 的 .log 文件，并按间隔维护 offset/time 索引。
  if (records.sizeInBytes() > 0) {
    LOGGER.trace("Inserting {} bytes at end offset {} at position {}",
                         records.sizeInBytes(), largestOffset, log.sizeInBytes());
    // 记录写入前的物理位置，后续索引会指向这个 .log 文件位置。
    int physicalPosition = log.sizeInBytes();

    // 确认最大 offset 能用当前 segment base offset 表示成相对 offset。
    ensureOffsetInRange(largestOffset);

    // 进入 FileRecords，真正把消息字节写入 .log 文件。
    long appendedBytes = log.append(records);
    LOGGER.trace("Appended {} to {} at end offset {}", appendedBytes, log.file(), largestOffset);

    for (RecordBatch batch : records.batches()) {
      long batchMaxTimestamp = batch.maxTimestamp();
      long batchLastOffset = batch.lastOffset();
      // 维护 segment 内最大时间戳及其 offset，供 time index 使用。
      if (batchMaxTimestamp > maxTimestampSoFar()) {
        maxTimestampAndOffsetSoFar = new TimestampOffset(batchMaxTimestamp, batchLastOffset);
      }

      // 累计写入字节超过索引间隔后，写一条稀疏 offset/time 索引。
      if (bytesSinceLastIndexEntry > indexIntervalBytes) {
        // 将索引写进.index文件：relativeOffset -> physicalPosition
        offsetIndex().append(batchLastOffset, physicalPosition);
        // 将时间索引写进.timeindex文件：timestamp -> offset
        timeIndex().maybeAppend(maxTimestampSoFar(), shallowOffsetOfMaxTimestampSoFar());
        bytesSinceLastIndexEntry = 0;
      }
      var sizeInBytes = batch.sizeInBytes();
      physicalPosition += sizeInBytes;
      bytesSinceLastIndexEntry += sizeInBytes;
    }
  }
}
```

## FileRecords.append()
FileRecords.append() 就是“容量防溢出 + 把内存 record bytes 全量写入 .log 文件 + 更新文件大小计数”。

```scala
public int append(MemoryRecords records) throws IOException {
  // 校验records大小
  if (records.sizeInBytes() > Integer.MAX_VALUE - size.get())
  throw new IllegalArgumentException("Append of size " + records.sizeInBytes() +
                                               " bytes is too large for segment with current file position at " + size.get());

  // 进入 MemoryRecords，把底层 ByteBuffer 内容完整写入文件通道。
  int written = records.writeFullyTo(channel);
  size.getAndAdd(written);
  return written;
}
```

此方法不负责刷盘。append() 只是写到 FileChannel，真正 fsync 是 flush() 里的 channel.force(true)。

## MemoryRecords.writeFullyTo(FileChannel)
循环把底层 ByteBuffer 写入 channel，直到本批 records 的所有字节都写完。

核心逻辑：

```scala
public int writeFullyTo(GatheringByteChannel channel) throws IOException {
  buffer.mark();
  int written = 0;
  while (written < sizeInBytes())
  written += channel.write(buffer);
  // 恢复 buffer 位置，避免本次写入影响后续读取或重复写入。
  buffer.reset();
  return written;
}
```

# 设计思想
经过对KafkaApis.handleProduceRequest()的分析，发现最终到写入磁盘，经过了非常长的调用链路，为什么要这么设计呢？

Kafka 这条链路看起来很深，本质是把 **一次 Produce 写入** 拆成不同层级的职责，每层只处理自己能决定的事。

核心设计原理是：**从网络请求一路收敛到磁盘写入，越往下越接近存储细节，越往上越接近协议、权限、复制和事务语义。**

```java
KafkaApis
  处理协议层：解析请求、权限、错误响应、限流

ReplicaManager
  处理副本层：leader 判断、acks、ISR、副本复制等待、延迟响应

Partition
  处理分区层：当前分区是不是 leader、min ISR、高水位推进

UnifiedLog
  处理逻辑日志层：offset 分配、record 校验、幂等、事务、LSO/HW 状态

LocalLog / LogSegment
  处理物理日志层：segment 选择、滚动、索引维护

FileRecords / MemoryRecords
  处理字节层：ByteBuffer 写入 FileChannel
```

Kafka 的 Produce 写入链路分层，是为了把协议处理、副本一致性、分区状态、日志语义、物理存储和字节写入解耦。这样每层职责清晰，既能保证高性能，又能支撑副本、事务、幂等、索引、恢复这些复杂能力。



它同时要处理：

1. **协议兼容**  
不同版本 Produce 请求、topicId/topicName、acks=0/1/all、错误码响应都不一样。
2. **权限和配额**  
要检查 topic 写权限、transactionalId 权限、带宽限流、请求限流。
3. **副本一致性**  
只能 leader 写；`acks=all` 要等 ISR 副本复制；ISR 不够要拒绝。
4. **分区状态**  
leader epoch、high watermark、log start offset、分区是否在线都要维护。
5. **消息格式校验**  
record batch 格式、大小、压缩、时间戳、magic version 都要校验或转换。
6. **幂等和事务**  
producerId、producerEpoch、sequence、事务状态、transaction index、LSO 都会影响是否能写。
7. **物理存储**  
segment 滚动、offset index、time index、文件大小、FileChannel 写入、flush 策略都属于底层存储问题。

