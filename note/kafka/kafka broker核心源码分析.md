当前分析版本是kafka最新版本 4.2（版本随时变化，最新分析代码请关注仓库：[https://github.com/coderbruis/kafka](https://github.com/coderbruis/kafka)   **<font style="color:#DF2A3F;">source_code_analysis分支</font>**，底层原理持续更新）

# kafka服务端核心逻辑
目前最新版本kafka已经抛弃了Zookeeper，而是自己维护元数据日志，存放在内部 topic __cluster_metadata。

如果使用默认的配置方式 config/server.properties启动：

```java
bin/kafka-server-start.sh config/server.properties
```

在server.properties配置里，默认角色是：process.roles=broker,controller

完整启动链路如下：

```java
bin/kafka-server-start.sh
 -> Kafka.main(...)
 -> KafkaRaftServer.startup()
 -> ControllerServer.startup()
 -> BrokerServer.startup()
```



配置方式不同，启动链路不同：

```java
rocess.roles=broker
  ...
  KafkaRaftServer.startup()
   -> BrokerServer.startup()

process.roles=controller
  ...
  KafkaRaftServer.startup()
   -> ControllerServer.startup()

process.roles=broker,controller
  ...
  KafkaRaftServer.startup()
   -> ControllerServer.startup()
   -> BrokerServer.startup()
```



> 生产不太推荐 `broker,controller` 混合部署
>

因为 broker 是数据面，压力来自客户端请求、磁盘 IO、网络复制；controller 是控制面，负责元数据变更、分区 leader 选举、broker 上下线协调。混在一个进程里时，broker 的 IO/GC/负载抖动可能影响 controller 稳定性，controller 出问题又会影响整个集群的元数据管理。

****

**broker,controller部署方式适合本地开发。正式生产，适合独立 controller + 独立 broker，大规模生产适合独立 controller quorum集群 + 独立 broker 集群。**

## Kafka.scala
Kafka.scala中main()是 Kafka broker 进程的 JVM 入口，它本身不做具体 broker 组件初始化，核心作用是把进程启动流程串起来，源码如下：

```java
def main(args: Array[String]): Unit = {
      ...    
      // 解析命令行参数，读取server.properties，
      val serverProps = getPropsFromArgs(args)    
      // 构建server实例，主要是场景KafkaRaftServer实例
      val server = buildServer(serverProps)
      ...
      // 启动KafkaRaftServer
      try server.startup()
      ...
      // 阻塞等待关闭，让 main 线程等待服务结束。
      server.awaitShutdown()
}
```

main()核心做了四件事：

+ 解析命令行参数，读取server.properties，如：bin/kafka-server-start.sh config/kraft/server.properties \ --override log.dirs=/tmp/kraft-combined-logs
+ 构建server实例，主要是场景KafkaRaftServer实例
+ 启动KafkaRaftServer
+ 阻塞等待关闭，让 main 线程等待服务结束。

## BrokerServer.startup()
BrokerServer.startup()最核心的几件事：

```java
BrokerServer.startup()
  -> 创建 LogManager
  -> 创建 ReplicaManager
  -> 创建 SocketServer
  -> 创建 KafkaApis
  -> 创建 KafkaRequestHandlerPool
```



```java
logManager = LogManager(config,
                        sharedServer.metaPropsEnsemble.errorLogDirs(),
                        metadataCache,
                        kafkaScheduler,
                        time,
                        brokerTopicStats,
                        logDirFailureChannel)
```

logManager负责本地日志目录、分区日志、恢复、flush、retention 等。但这里先创建，不急着完整恢复，因为要等 metadata catch-up 后才知道最新 topic/broker 配置。broker 最终存消息靠本地 log，LogManager 就是这些 log 的总管。



```java
this._replicaManager = new ReplicaManager(
        config = config,
        metrics = metrics,
        time = time,
        scheduler = kafkaScheduler,
        logManager = logManager,
        remoteLogManager = remoteLogManagerOpt,
        quotaManagers = quotaManagers,
        metadataCache = metadataCache,
        logDirFailureChannel = logDirFailureChannel,
        alterPartitionManager = alterPartitionManager,
        brokerTopicStats = brokerTopicStats,
        delayedRemoteFetchPurgatoryParam = None,
        brokerEpochSupplier = () => lifecycleManager.brokerEpoch,
        addPartitionsToTxnManager = Some(addPartitionsToTxnManager),
        directoryEventHandler = directoryEventHandler,
        defaultActionQueue = defaultActionQueue
      )
```

ReplicaManager，负责副本、分区状态、Produce/Fetch 落盘等数据面核心逻辑。ReplicaManager 是 broker 数据读写的核心对象：

+ Produce 请求最终靠它写入 leader 副本
+ Fetch 请求最终靠它从本地副本读取
+ follower 拉取 leader 数据也会走它
+ ISR、高水位、延迟 produce/fetch 等也和它有关



```java
socketServer = new SocketServer(config,
                                metrics,
                                time,
                                credentialProvider,
                                apiVersionManager,
                                sharedServer.socketFactory,
                                connectionDisconnectListeners)
...
val enableRequestProcessingFuture = socketServer.enableRequestProcessing(authorizerFutures)
```

创建网络入口。此时准备 acceptor 和端口，真正处理请求会延后开启。enableRequestProcessing()开启 SocketServer 请求处理。



```java
dataPlaneRequestProcessor = new KafkaApis(
    requestChannel = socketServer.dataPlaneRequestChannel,
    forwardingManager = forwardingManager,
    replicaManager = replicaManager,
    groupCoordinator = groupCoordinator,
    txnCoordinator = transactionCoordinator,
    shareCoordinator = shareCoordinator,
    autoTopicCreationManager = autoTopicCreationManager,
    brokerId = config.nodeId,
    config = config,
    configRepository = metadataCache,
    metadataCache = metadataCache,
    metrics = metrics,
    authorizerPlugin = authorizerPlugin,
    quotas = quotaManagers,
    fetchManager = fetchManager,
    sharePartitionManager = sharePartitionManager,
    brokerTopicStats = brokerTopicStats,
    clusterId = clusterId,
    time = time,
    tokenManager = tokenManager,
    apiVersionManager = apiVersionManager,
    clientMetricsManager = clientMetricsManager,
    groupConfigManager = groupConfigManager)
```



创建 KafkaApis，作为 broker 数据面请求的核心分发处理器。

```java
dataPlaneRequestHandlerPool = sharedServer.requestHandlerPoolFactory.createPool(
    config.nodeId,
    socketServer.dataPlaneRequestChannel,
    dataPlaneRequestProcessor,
    time,
    config.numIoThreads,
    "broker"
)
```

创建请求处理线程池，从 RequestChannel 拉取请求并调用 KafkaApis。

KafkaApis 是 broker API 层的分发入口。RequestChannel 收到请求后，KafkaRequestHandlerPool 里的线程会取出请求并调用 KafkaApis.handle，再由 KafkaApis 根据 API 类型进入对应业务逻辑。

## SocketServer
Kafka broker 网络层核心是 SocketServer。它里面主要有两类线程：

+ `Acceptor`：接收新 TCP 连接
+ `Processor`：负责连接上的读写 I/O

SocketServer核心调用链路如下：

```java
BrokerServer.startup
  -> SocketServer.enableRequestProcessing
    -> Acceptor.start
      -> Processor.start
        -> Processor.run
          -> configureNewConnections
          -> poll
          -> processCompletedReceives
            -> RequestChannel

      -> Acceptor.run
        -> acceptNewConnections
          -> accept(socket)
          -> assignNewConnection
            -> Processor.accept
              -> 放入 Processor.newConnections 队列
```



### SocketServer.enableRequestProcessing()
这个方法的核心作用就是，开启 SocketServer 管理的所有 endpoint 请求处理。每个 endpoint 会在对应 authorizer ready 后异步启动 acceptor。返回的 future 会在所有 acceptor 启动完成或任一启动失败时完成。

```java
 def enableRequestProcessing(
    authorizerFutures: Map[Endpoint, CompletableFuture[Void]]
  ): CompletableFuture[Void] = this.synchronized {
     ...
    // 为单个 acceptor 绑定对应的 authorizer future，等授权器 ready 后再启动监听。
    def chainAcceptorFuture(acceptor: Acceptor): Unit = {
      // 临时端口会改变 endpoint 对象，所以按 listener 名称匹配 authorizer future。
      val authorizerFuture = authorizerFutures.find {
        case (endpoint, _) => acceptor.endPoint.listener.equals(endpoint.listener())
      } match {
        // 没有专属 future 的 endpoint，等所有 authorizer 完成后再启动。
        case None => allAuthorizerFuturesComplete
        // 有专属 future 的 endpoint，等待自己的 authorizer 完成。
        case Some((_, future)) => future
      }
      authorizerFuture.whenComplete((_, e) => {
        if (e != null) {
          // authorizer 启动失败时，让对应 acceptor 启动结果也失败。
          acceptor.startedFuture.completeExceptionally(e)
        } else {
          // authorizer ready 后启动 acceptor，由 acceptor 自己完成 startedFuture。
          acceptor.start()
        }
      })
    }
     ...
 }
```

正常启动最终会走到acceptor.start()。

而Acceptor类里走到start()又会遍历processors，并调用start。

```java
def start(): Unit = synchronized {
    ...
    processors.foreach(_.start())
    ...
}
```



### Acceptor.acceptNewConnections()
在processor这种启动运行之前，会先执行acceptNewConnections()。这个方法也非常的重要，核心作用就是：

```java
selector 等待 accept 事件
  -> 遍历可处理的 SelectionKey
  -> accept 新 SocketChannel
  -> 做连接 quota / socket 配置
  -> 按 轮询策略 选择 Processor
  -> 把连接交给 Processor 后续处理网络 IO
```

核心代码: 

```java
  private def acceptNewConnections(): Unit = {
    // 阻塞等待新连接事件，最多等待 500ms。
    val ready = nioSelector.select(500)
    if (ready > 0) {
      // 获取本轮 selector 发现的可处理事件。
      val keys = nioSelector.selectedKeys()
      val iter = keys.iterator()
      // 逐个处理可接受的新连接事件。
      while (iter.hasNext && shouldRun.get()) {
        try {
          val key = iter.next
          // 移除已处理 key，避免下轮重复处理。
          iter.remove()

          if (key.isAcceptable) {
            // accept 新连接，成功后交给 Processor 处理后续网络 IO。
            accept(key).foreach { socketChannel =>
              // 按轮询选择可接收连接的 Processor，全部队列满时最后一次允许阻塞等待。
              var retriesLeft = synchronized(processors.length)
              var processor: Processor = null
              do {
                retriesLeft -= 1
                processor = synchronized {
                  // 动态调整网络线程数时，修正下标并原子获取 Processor。
                  currentProcessorIndex = currentProcessorIndex % processors.length
                  processors(currentProcessorIndex)
                }
                currentProcessorIndex += 1
                // 把 Acceptor 接收到的新连接交给某个 Processor 处理。
              } while (!assignNewConnection(socketChannel, processor, retriesLeft == 0))
            }
          } else
            // Acceptor 只关注 accept 事件，其他事件属于异常状态。
            throw new IllegalStateException("Unrecognized key state for acceptor thread.")
        } catch {
          // 单个连接接受失败只记录错误，继续处理其他连接。
          case e: Throwable => error("Error while accepting connection", e)
        }
      }
    }
  }
```



在assignNewConnection中，会将新链接channel交给某个processor处理

```java
  private def assignNewConnection(socketChannel: SocketChannel, processor: Processor, mayBlock: Boolean): Boolean = {
    if (processor.accept(socketChannel, mayBlock, blockedPercentMeter)) {
      debug(s"Accepted connection from ${socketChannel.socket.getRemoteSocketAddress} on" +
        s" ${socketChannel.socket.getLocalSocketAddress} and assigned it to processor ${processor.id}," +
        s" sendBufferSize [actual|requested]: [${socketChannel.socket.getSendBufferSize}|$sendBufferSize]" +
        s" recvBufferSize [actual|requested]: [${socketChannel.socket.getReceiveBufferSize}|$recvBufferSize]")
      true
    } else
      false
  }
```

### Processor.run()
在Acceptor.start()方法中会一次遍历processors，然后调用start()方法启动Processor，所以最终会走到Processor.run()中。

Acceptor：

```java
def start(): Unit = synchronized {
    ...
    processors.foreach(_.start())
    ...
}
```



#### Processor是什么？
在SocketServer中，Processor定义如下：

```plain
private[network] val processors = new ArrayBuffer[Processor]()
```

它表示 某个 listener/endpoint 下的一组网络 IO 线程。在 broker 里，Processor 不是按“请求类型”划分的，不是 ProduceProcessor、FetchProcessor 这种；而是按 listener + 网络线程数 创建的。

```java
DataPlaneAcceptor.configure(...)
  -> addProcessors(num.network.threads)
  -> newProcessor(...）
```



也就是每个 broker listener 都会有一组 Processor：

```java
listener PLAINTEXT
  -> Processor 0
  -> Processor 1
  -> Processor 2
  ...

listener SSL
  -> Processor 3
  -> Processor 4
  -> Processor 5
  ...

listener SASL_SSL
  -> Processor 6
  -> Processor 7
  -> Processor 8
  ...
```

broker 里有多少 Processor，主要取决于 listener 数量 × num.network.threads；每个 Processor 都是同一种网络 IO worker，只是负责不同连接。还需要特别注意的，Processor 只负责网络 IO，不区分 producer 和 consumer；producer/consumer 的业务差异是在 KafkaApis 之后按请求类型区分的。



Processor.run()可以看到Kafka网络层最核心的 IO 编排逻辑：

```java
  override def run(): Unit = {
    try {
      // Processor 主循环持续处理已分配连接上的网络 IO。
      while (shouldRun.get()) {
        try {
          // 注册 Acceptor 分配过来的新连接。
          configureNewConnections()
          // 处理 KafkaRequestHandler 写回的响应，注册写事件或关闭连接。
          processNewResponses()
          // 轮询 selector，读取请求、写出响应并推进连接状态。
          poll()
          // 解析已读完整请求，封装成 Request 后放入 RequestChannel。
          processCompletedReceives()
          // 处理已发送完成的响应，更新指标并恢复连接读取。
          processCompletedSends()
          // 清理 selector 检测到的断开连接。
          processDisconnected()
          // 关闭超过连接配额的连接。
          closeExcessConnections()
        } catch {
          // 捕获异常防止 Processor 线程退出，避免单连接问题扩大成 broker 网络线程不可用。
          case e: Throwable => processException("Processor got uncaught exception.", e)
        }
      }
    } finally {
      // 线程退出时关闭 selector 和该 Processor 管理的连接资源。
      debug(s"Closing selector - processor $id")
      Utils.swallow(this.logger.underlying, Level.ERROR, () => closeAll())
    }
  }
```



在kafka中，除了网络IO编排，还有工作线程编排，业务分发入口，粗略分层为：

```java
Acceptor.run()
  TCP 新连接接入

Processor.run()
  网络 IO 编排，读请求 / 写响应

KafkaRequestHandler.run()
  请求工作线程编排，从 RequestChannel 消费请求

KafkaApis.handle()
  Kafka API 业务分发入口

ReplicaManager / Coordinator / MetadataCache
  具体业务执行
```

## RequestChannel
RequestChannel 是网络线程和请求处理线程之间的队列。

响应发送也会反向通过它回到对应 Processor。

核心源码：

```java
private[network] def sendResponse(response: RequestChannel.Response): Unit = {
  ...
  val processor = processors.get(response.processor)
  if (processor != null) {
    processor.enqueueResponse(response)
  }
}


def receiveRequest(timeout: Long): BaseRequest = {
  val callbackRequest = callbackQueue.poll()
  if (callbackRequest != null)
    callbackRequest
  else {
    // 从requestQueue中弹出请求，然后处理
    val request = requestQueue.poll(timeout, TimeUnit.MILLISECONDS)
    request match {
      case _: WakeupRequest => callbackQueue.poll()
      case _ => request
    }
  }
}
```

请求处理线程从这里拿请求：receiveRequest(timeout)。业务处理完成后，响应通过这里投递给原来的 Processor：sendResponse(response)。



RequestChannel 是 broker 网络线程和业务线程之间的中转站。



## KafkaRequestHandler
KafkaRequestHandler 是真正处理请求的线程，核心逻辑在run()中：

```java
while (!stopped) {
  // 从 RequestChannel 拉取下一个请求，最多等待 300ms。
  val req = requestChannel.receiveRequest(300)
  val endTime = time.nanoseconds

  req match {
    case _: ShutdownRequest =>
      // 收到关闭请求时完成线程清理并退出循环。
      completeShutdown()
      return

    case callback: CallbackRequest =>
      // 处理异步操作完成后重新调度到请求线程的 callback。
      val originalRequest = callback.originalRequest
      ...
      // 执行被重新调度到请求线程的 callback。
      callback.fun().accept(requestLocal)
      ...
      // callback 执行后尝试推进延迟动作。
      apis.tryCompleteActions()
    
    // 处理普通 Kafka 请求，进入 KafkaApis 业务分发。  
    case request: Request =>
      request.requestDequeueTimeNanos(endTime)
      // 标记当前线程正在处理的请求。
      threadCurrentRequest.set(request)
      // 调用 KafkaApis，根据 ApiKey 分发到对应业务逻辑。
      apis.handle(request, requestLocal)
  }
}
```

这个方法核心作用：**不断从 RequestChannel 取请求，然后交给 KafkaApis.handle() 处理。**

****

## KafkaApis.handle()
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







