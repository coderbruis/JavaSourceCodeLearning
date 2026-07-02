+ 当前分析版本是kafka最新版本（版本随时变化，最新分析代码请关注仓库：https://github.com/coderbruis/kafka source_code_analysis分支，底层原理持续更新）
+ 转载请标明出处

# kafka消费者核心入口poll
kafka消费者核心入口在KafkaConsumer.poll()：

```java
    @Override
    public ConsumerRecords<K, V> poll(final Duration timeout) {
        return delegate.poll(timeout);
    }
```

此处用的委托模式，该方法由接口ConsumerDelegate进行委托，会依赖配置来决定委托到ClassicKafkaConsumer或者AsynckafkaConsumer，配置group.protocol默认为classic，也就是ClassicKafkaConsumer。

kafka配置默认代码定义在ConsumerConfig中，如下：

```java
public static final String GROUP_PROTOCOL_CONFIG = "group.protocol";
public static final String DEFAULT_GROUP_PROTOCOL =
    GroupProtocol.CLASSIC.name().toLowerCase(Locale.ROOT);
```



先提前了解下两种不同protocol实现类型的区别：

**<font style="color:rgb(26, 28, 31);">ClassicKafkaConsumer</font>**

+ <font style="color:rgb(26, 28, 31);">老实现。</font>
+ <font style="color:rgb(26, 28, 31);">用户线程直接执行大部分 consumer 逻辑。</font>
+ <font style="color:rgb(26, 28, 31);">poll() 里同步处理网络、协调器、fetch、commit 等流程。</font>
+ <font style="color:rgb(26, 28, 31);">代码路径更直接，但用户线程承担的事情更多。</font>

**AsyncKafkaConsumer 新实现**

+ <font style="color:rgb(26, 28, 31);">引入后台线程处理网络 IO 和部分 consumer 内部事件。</font>
+ <font style="color:rgb(26, 28, 31);">用户线程更多是提交事件、等待结果、取数据。</font>
+ <font style="color:rgb(26, 28, 31);">目标是减少用户线程阻塞，改善响应性和内部职责拆分。</font>

<font style="color:rgb(26, 28, 31);"></font>

# <font style="color:rgb(26, 28, 31);">ClassicKafkaConsumer.poll()</font>
ClassicKafkaConsumer.poll()方法入口传入的是Timer对象，作为一个时间上下文对象，

核心作用：<font style="color:rgb(26, 28, 31);">控制这次 </font><font style="color:rgb(26, 28, 31);">poll()</font><font style="color:rgb(26, 28, 31);"> 最多能阻塞多久，并在多个内部步骤之间共享剩余时间。</font>

```java
    @Override
    public ConsumerRecords<K, V> poll(final Duration timeout) {
        return poll(time.timer(timeout));
    }
```

poll() 中最可能导致阻塞的两个阶段：

1. 消费组协调（JoinGroup / SyncGroup / Heartbeat）
2. Fetch 数据等待

在看下poll()封装的流程，这个方法核心作用总结：**在用户给定的超时时间内，完成消费组协调、分区位置准备、发送/接收 fetch 请求，并把拉到的消息返回给用户。**

```java
    private ConsumerRecords<K, V> poll(final Timer timer) {
        acquireAndEnsureOpen();
        try {
            // 统计同一线程两次 poll() 调用之间隔了多久
            this.kafkaConsumerMetrics.recordPollStart(timer.currentTimeMs());

            // 消费者必须订阅主题，并且自动分区或者手动指定分区
            if (this.subscriptions.hasNoSubscriptionOrUserAssignment()) {
                throw new IllegalStateException("Consumer is not subscribed to any topics or assigned any partitions");
            }

            // 在超时时间范围内，只要没拿到数据就持续多轮poll
            do {
                // 在可能发生阻塞操作前响应用户通过 wakeup() 发出的唤醒请求。
                client.maybeTriggerWakeup();
                // 在拉取消息前，先确保消费者已经完成入组和分区分配，并准备好各分区接下来应该从哪个 offset 开始消费。
                updateAssignmentMetadataIfNeeded(timer, false);
                // 尝试从 fetch buffer 取数据；如果没有，就发送 fetch 请求并等待 broker 响应。
                final Fetch<K, V> fetch = pollForFetches(timer);
                // 如果本轮已经拿到可返回的数据，准备返回给用户
                if (!fetch.isEmpty()) {
                    // 提前发送下一轮 fetch 请求。用户处理当前 records 的时候，下一批数据已经在路上，提高吞吐，形成 pipeline。
                    if (sendFetches() > 0 || client.hasPendingRequests()) {
                        client.transmitSends();
                    }

                    if (fetch.records().isEmpty()) {
                        log.trace("Returning empty records from `poll()` "
                                + "since the consumer's position has advanced for at least one topic partition");
                    }
                    // 把拉到的数据交给 consumer interceptor 处理，然后包装成 ConsumerRecords 返回给用户。
                    return this.interceptors.onConsume(new ConsumerRecords<>(fetch.records(), fetch.nextOffsets()));
                }
            } while (timer.notExpired());
            return ConsumerRecords.empty();
        } finally {
            // 释放 consumer 使用权
            release();
            // 记录 poll 结束指标
            this.kafkaConsumerMetrics.recordPollEnd(timer.currentTimeMs());
        }
    }
```



下面逐渐分析poll()的几个核心流程

## 2.1 maybeTriggerWakeup()
在ClassicKafkaConsumer.poll()流程中，client.maybeTriggerWakeup()这个方法有点奇怪吧，什么场景会发起wakeup唤醒呢？

client.maybeTriggerWakeup()实际调用的是ConsumerNetworkClient的maybeTriggerWakeup方法

```java
    public void maybeTriggerWakeup() {
        if (!wakeupDisabled.get() && wakeup.get()) {
            log.debug("Raising WakeupException in response to user wakeup");
            wakeup.set(false);
            throw new WakeupException();
        }
    }
```

上述方法可以看到，当wakeupDisabled为false，并且有用户发起了wakeup，则会抛出WakeupException这个异常。<font style="color:rgb(26, 28, 31);">wakeup()</font><font style="color:rgb(26, 28, 31);"> 会让正在阻塞的 </font><font style="color:rgb(26, 28, 31);">poll()</font><font style="color:rgb(26, 28, 31);"> “尽快抛出” </font><font style="color:rgb(26, 28, 31);">WakeupException</font><font style="color:rgb(26, 28, 31);">，从而提前结束本次 poll，不必等待 timeout 或消息返回。常用于关闭 consumer 时唤醒消费者线程，让它跳出消费循环并执行后续关闭流程。</font>

可以总结为如下几个场景：

1）Spring容器关闭时，Spring Kafka listener container 停止，提前唤醒kafka结束poll()阻塞流程，提前结束poll()；

2）JVM执行shutdown hook，调用 consumer.wakeup() 或触发框架关闭流程，提前唤醒kafka结束poll()阻塞流程，提前结束poll()；



## 2.2 updateAssignmentMetadataIfNeeded()
updateAssignmentMetadataIfNeeded()这个方法是消费者组重平衡的核心入口，并且拉消费位置，为后续的fetch发起准备。

这个方法核心作用总结：**在拉取消息前，先确保消费者已经完成入组和分区分配，并准备好各分区接下来应该从哪个 offset 开始消费。**



ClassicKafkaConsumer.updateAssignmentMetadataIfNeeded()

```java
    boolean updateAssignmentMetadataIfNeeded(final Timer timer, final boolean waitForJoinGroup) {
        // 判断消费者组协调是否完成（加入消费者组、重平衡、心跳协调）
        if (coordinator != null && !coordinator.poll(timer, waitForJoinGroup)) {
            return false;
        }

        // 准备各分区的拉取位置，即consumer该从哪个offset开始拉消息
        return updateFetchPositions(timer);
    }
```

TODO 重平衡是kafka的重点核心逻辑，重平衡分析逻辑用另外一篇重点分析。



ClassicKafkaConsumer.updateFetchPositions()源码如下，这个方法核心作用总结：**确保当前分配到的每个分区，都有一个明确的“下一次从哪里开始拉消息”的位置。**

```java
private boolean updateFetchPositions(final Timer timer) {
        // 检查已有的 fetch position 是否还有效。
        offsetFetcher.validatePositionsIfNeeded();

        // 检查当前订阅状态里，所有已分配分区是否都有有效的 fetch position。
        cachedSubscriptionHasAllFetchPositions = subscriptions.hasAllFetchPositions();
        // 如果所有分区都有位置了，就不需要再查 committed offset，也不需要 reset，直接返回成功。
        if (cachedSubscriptionHasAllFetchPositions) return true;

        // 如果存在 consumer coordinator，就尝试为“还没有 fetch position 的分区”读取已提交 offset。也就是问 coordinator：这些分区之前消费到哪里了？
        // 如果在 timer 时间内没有查到 committed offset，就返回 false，本轮先不继续。这里 coordinator != null 是因为：
        // 1)自动分配分区时通常有 coordinator。
        // 2)手动 assign() 的消费者也可能不依赖 coordinator。
        // 3)如果没有 coordinator，就跳过 committed offset 初始化，后面走 reset 策略。
        if (coordinator != null && !coordinator.initWithCommittedOffsetsIfNeeded(timer)) return false;

        // 对仍然没有 fetch position 的分区，按默认 offset reset 策略准备重置位置。
        // 比如：
        // 1)earliest：准备从最早 offset 开始。
        // 2)latest：准备从最新 offset 开始。
        // 3)none：没有可用 offset 时直接抛 NoOffsetForPartitionException。
        subscriptions.resetInitializingPositions();

        // Finally send an asynchronous request to look up and update the positions of any
        // partitions which are awaiting reset.
        // 对刚才标记为需要 reset 的分区，异步发送 ListOffsets 请求，去 broker 查询真正的 earliest/latest offset，并更新 fetch position。
        offsetFetcher.resetPositionsIfNeeded();

        return true;
    }
```

OffsetFetcher.validatePositionsIfNeeded();是一个很重要的校验逻辑，源码如下，这个方法核心作用总结：**检查已有的 fetch position 是否还有效。主要场景是 leader 发生变化后，分区日志可能被截断，原来的 offset 可能已经不合法，所以需要异步发请求去校验。**

```java
    public void validatePositionsIfNeeded() {
        Map<TopicPartition, SubscriptionState.FetchPosition> partitionsToValidate =
                offsetFetcherUtils.refreshAndGetPartitionsToValidate();
        validatePositionsAsync(partitionsToValidate);
    }
```

validatePositionsAsync中最核心的一段代码，这个方法核心代码作用总结：**<font style="color:rgb(26, 28, 31);">这段代码就是“发起 offset 校验请求，并注册成功/失败处理逻辑”；成功就判断是否发生日志截断，失败就安排重试或记录异常。</font>**

```java
 RequestFuture<OffsetForEpochResult> future =
                    offsetsForLeaderEpochClient.sendAsyncRequest(node, fetchPositions);
            future.addListener(new RequestFutureListener<>() {
                @Override
                public void onSuccess(OffsetForEpochResult offsetsResult) {
                    offsetFetcherUtils.onSuccessfulResponseForValidatingPositions(fetchPositions,
                            offsetsResult);
                }

                @Override
                public void onFailure(RuntimeException e) {
                    offsetFetcherUtils.onFailedResponseForValidatingPositions(fetchPositions, e);
                }
            });
```

此处最重要的一步就是走的：offsetFetcherUtils.onSuccessfulResponseForValidatingPositions(fetchPositions,offsetsResult)，这个方法回调核心作用总结：**<font style="color:rgb(26, 28, 31);">处理 </font>****<font style="color:rgb(26, 28, 31);">OffsetsForLeaderEpoch</font>****<font style="color:rgb(26, 28, 31);"> 校验请求的成功响应，判断当前消费 offset 是否仍然有效，是否发生日志截断，并据此完成校验、触发重试、offset reset，或者缓存异常。</font>**

<font style="color:rgb(26, 28, 31);">（由于篇幅原因，此处不深究校验逻辑，具体原理在源码仓库中，在OffsetFetcherUtils.onSuccessfulResponseForValidatingPositions()中）</font>

<font style="color:rgb(26, 28, 31);"></font>

## <font style="color:rgb(26, 28, 31);">2.3 pollForFetches()</font>
这个方法核心代码作用总结：**先从本地已完成的 fetch 结果里取数据；没有数据就发送新的 fetch 请求；再通过 client.poll() 推进网络 I/O，等待 broker 响应；最后再取一次数据返回。**

```java
    private Fetch<K, V> pollForFetches(Timer timer) {
        // 计算这次最多阻塞多久。
        long pollTimeout = coordinator == null ? timer.remainingMs() :
                Math.min(coordinator.timeToNextPoll(timer.currentTimeMs()), timer.remainingMs());
        // 先看本地是否已经有可返回的数据。
        final Fetch<K, V> fetch = fetcher.collectFetch();
        if (!fetch.isEmpty()) {
            return fetch;
        }

        // fetchBuffer中没有数据，发起fetch请求
        sendFetches();

        // 如果当前订阅的分区还不是全部都有有效 fetch position，就不要长时间阻塞等 fetch。
        // 因为 position 不完整时，可能还在查 committed offset、reset offset、校验 leader epoch，或者刚失败进入 backoff。
        if (!cachedSubscriptionHasAllFetchPositions && pollTimeout > retryBackoffMs) {
            // 这个时候等太久没意义，所以最多等 retryBackoffMs，尽快回到外层循环继续推进 position 准备流程。
            pollTimeout = retryBackoffMs;
        }

        log.trace("Polling for fetches with timeout {}", pollTimeout);

        Timer pollTimer = time.timer(pollTimeout);
        // client.poll(...) 会推进 Kafka 网络 I/O：发送请求、接收响应、执行回调。
        client.poll(pollTimer, () -> {
            // 这个匿名参数是继续阻塞的条件：只要 fetcher 还没有可用 fetch，就可以继续等；一旦有 fetch 响应完成并进入可取状态，就提前结束等待，不必把 pollTimeout 用完。
            return !fetcher.hasAvailableFetches();
        });
        timer.update(pollTimer.currentTimeMs());
        // fetch请求之后，再从fetcher收集一次数据。
        return fetcher.collectFetch();
    }
```



首次进入poll时，fetchBuffer通常为空。请求发送会在sendFetches()中执行，也就是ClassicKafkaConsumer.sendFetches()，代码如下：

```java
    private int sendFetches() {
        offsetFetcher.validatePositionsOnMetadataChange();
        return fetcher.sendFetches();
    }
```

Fetcher.sendFetches()，代码如下：

```java
    public synchronized int sendFetches() {
        // 准备fetch请求
        final Map<Node, FetchSessionHandler.FetchRequestData> fetchRequests = prepareFetchRequests();
        // 发起fetch请求，并注册回调handler
        sendFetchesInternal(
                fetchRequests,
                (fetchTarget, data, clientResponse) -> {
                    synchronized (Fetcher.this) {
                        handleFetchSuccess(fetchTarget, data, clientResponse);
                    }
                },
                (fetchTarget, data, error) -> {
                    synchronized (Fetcher.this) {
                        handleFetchFailure(fetchTarget, data, error);
                    }
                });
        return fetchRequests.size();
    }
```



prepareFetchRequests()是一个非常重要的方法，它核心作用有两个：(此方法逻辑嵌套太深，篇幅原因，不在本篇深入分析)

1）它负责决定这次要向哪些 broker 发 FetchRequest、每个 broker 要拉哪些分区、从哪个 offset 开始拉。

2）让每个 broker 的 fetch 请求进入它自己的 fetch session 管理流程，后续可以利用 fetch session 做增量 fetch，减少请求体大小，也避免重复传完整分区列表。



Fetcher.sendFetchesInternal代码如下：

```java
 private List<RequestFuture<ClientResponse>> sendFetchesInternal(Map<Node, FetchSessionHandler.FetchRequestData> fetchRequests,
                                                                    ResponseHandler<ClientResponse> successHandler,
                                                                    ResponseHandler<Throwable> errorHandler) {
        final List<RequestFuture<ClientResponse>> requestFutures = new ArrayList<>();

        for (Map.Entry<Node, FetchSessionHandler.FetchRequestData> entry : fetchRequests.entrySet()) {
            final Node fetchTarget = entry.getKey();
            final FetchSessionHandler.FetchRequestData data = entry.getValue();
            // 封装一层FetchRequest
            final FetchRequest.Builder request = createFetchRequest(fetchTarget, data);
            // 发起fetch请求
            final RequestFuture<ClientResponse> responseFuture = client.send(fetchTarget, request);

            responseFuture.addListener(new RequestFutureListener<>() {
                @Override
                public void onSuccess(ClientResponse resp) {
                    // broker响应fetch请求成功回调
                    successHandler.handle(fetchTarget, data, resp);
                }

                @Override
                public void onFailure(RuntimeException e) {
                    // broker响应fetch请求失败回调
                    errorHandler.handle(fetchTarget, data, e);
                }
            });
            requestFutures.add(responseFuture);
        }

        return requestFutures;
    }
```



final RequestFuture<ClientResponse> responseFuture = client.send(fetchTarget, request); 这个调用最终会走到ConsumerNetworkClient.send()，源码如下：

```java
    public RequestFuture<ClientResponse> send(Node node,
                                              AbstractRequest.Builder<?> requestBuilder,
                                              int requestTimeoutMs) {
        long now = time.milliseconds();
        RequestFutureCompletionHandler completionHandler = new RequestFutureCompletionHandler();
        ClientRequest clientRequest = client.newClientRequest(node.idString(), requestBuilder, now, true,
            requestTimeoutMs, completionHandler);
        // 存一份待发送请求
        unsent.put(node, clientRequest);
        // 唤醒kafka client，发起fetch请求
        client.wakeup();
        return completionHandler.future;
    }
```

这个方法核心作用就两个，存一份fetch请求，唤醒kafka的网络请求client。后续被唤醒的kafka网络请求会从unsent中获取fetch请求，向broker发起真正的请求。

此处client.wakeup()也是非常重要的，send() 把请求放进 unsent 队列后，叫醒正在 poll() 里阻塞等待网络事件的底层网络客户端，让它赶紧回来看看有没有新请求要发。如果不wakeup，请求可能被延迟发送。新请求入队后，立刻叫醒网络 poll，让它尽快把请求从 unsent 发出去，而不是傻等 poll 超时。



并且此处的wakeup和上文讨论的consumer.wakeup()不是一个逻辑，此处wakeup底层是调用的NetworkClient.wakeup()，最终调用的是selector.wakeup()：

```java
    @Override
    public void wakeup() {
        this.selector.wakeup();
    }
```



那么unsent最终会在哪里被调用呢？答案就是ConsumerNetworkClient.trySend()，源码如下：

```java
long trySend(long now) {
        long pollDelayMs = maxPollTimeoutMs;

        // 遍历带发送的fetch请求
        for (Node node : unsent.nodes()) {
            Iterator<ClientRequest> iterator = unsent.requestIterator(node);
            if (iterator.hasNext())
                // 获取 poll 超时时间
                pollDelayMs = Math.min(pollDelayMs, client.pollDelayMs(node, now));

            while (iterator.hasNext()) {
                ClientRequest request = iterator.next();
                if (client.ready(node, now)) {
                    // 向broker发起请求
                    client.send(request, now);
                    iterator.remove();
                } else {
                    // try next node when current node is not ready
                    break;
                }
            }
        }
        return pollDelayMs;
    }
```

经过遍历unsent拿到待发送的fetch请求，然后调用client.send()之后，最终会走到NetworkClient.doSend()中，源码如下：

```java
    private void doSend(ClientRequest clientRequest, boolean isInternalRequest, long now, AbstractRequest request) {
        String destination = clientRequest.destination();
        RequestHeader header = clientRequest.makeHeader(request.version());
        if (log.isDebugEnabled()) {
            log.debug("Sending {} request with header {} and timeout {} to node {}: {}",
                clientRequest.apiKey(), header, clientRequest.requestTimeoutMs(), destination, request);
        }
        Send send = request.toSend(header);
        InFlightRequest inFlightRequest = new InFlightRequest(
                clientRequest,
                header,
                isInternalRequest,
                request,
                send,
                now);
        // 将fetch请求存放到inFlightRequests中
        this.inFlightRequests.add(inFlightRequest);
        // 把这个 fetch 请求对应的 NetworkSend 挂到目标 broker 连接对应的 KafkaChannel 上。
        selector.send(new NetworkSend(clientRequest.destination(), send));
    }
```

doSend()就两个核心作用：

1）将fetch请求存放到inFlightRequests中。

2）把这个 fetch 请求对应的 NetworkSend 挂到目标 broker 连接对应的 KafkaChannel 上。



> KafkaConsumer 内部的 NetworkClient/Selector 会给它需要连接的每个 broker 建一个连接，对应一个 KafkaChannel
>

关系图

```java
一个 KafkaConsumer
  -> 一个 ConsumerNetworkClient
  -> 一个 NetworkClient
  -> 一个 Selector
  -> 多个 KafkaChannel
       broker-1 一个
       broker-2 一个
       broker-3 一个
```



在pollForFetches()方法最后，会调用：

```java
        client.poll(pollTimer, () -> {
            // 这个匿名参数是继续阻塞的条件：只要 fetcher 还没有可用 fetch，就可以继续等；一旦有 fetch 响应完成并进入可取状态，就提前结束等待，不必把 pollTimeout 用完。
            return !fetcher.hasAvailableFetches();
        });
```

也就是ConsumerNetworkClient.poll()，源码如下，这个方法核心作用总结：

**它是 consumer 侧网络推进器：把 unsent 里的请求尽量发给 NetworkClient，驱动底层网络 I/O，处理响应/断连/超时/wakeup，最后把完成结果通知给上层 future。**

```java
public void poll(Timer timer, PollCondition pollCondition, boolean disableWakeup) {
        // 方法一进来就调用，上一次 poll() 过程中可能已经有请求完成了，但回调没有在当时立刻执行，而是先放进了 pendingCompletion。
        firePendingCompletedRequests();

        lock.lock();
        try {
            // 先执行上一次已经完成、但还没通知上层的请求回调。
            handlePendingDisconnects();

            // 尝试把 unsent 里的请求发出去。
            long pollDelayMs = trySend(timer.currentTimeMs());

            // 判断这次是否真的需要阻塞等网络。
            if (pendingCompletion.isEmpty() && (pollCondition == null || pollCondition.shouldBlock())) {
                // 计算底层 NetworkClient.poll() 最多阻塞多久。
                long pollTimeout = Math.min(timer.remainingMs(), pollDelayMs);
                if (client.inFlightRequestCount() == 0)
                    // 如果没有任何 in-flight 请求，说明现在没请求在路上，那就不要长时间阻塞，最多等 retryBackoffMs。
                    pollTimeout = Math.min(pollTimeout, retryBackoffMs);
                // 真正驱动底层网络 I/O 的地方：
                client.poll(pollTimeout, timer.currentTimeMs());
            } else {
                // 非阻塞地推进一下网络 I/O
                client.poll(0, timer.currentTimeMs());
            }
            timer.update();

            // 检查 unsent 请求对应的连接是否已经失败。
            checkDisconnects(timer.currentTimeMs());
            // 如果允许 wakeup，就检查是否有其他线程调用了 consumer.wakeup()
            if (!disableWakeup) {
                maybeTriggerWakeup();
            }

            maybeThrowInterruptException();

            // 再尝试发送一次 unsent 请求。
            // 为什么要第二次？ 因为刚才 client.poll(...) 可能完成了连接、释放了 buffer、清理了 in-flight 请求，使一些之前不能发送的请求现在可以发了。
            trySend(timer.currentTimeMs());

            failExpiredRequests(timer.currentTimeMs());
            
            // 清理 unsent 内部空集合，避免 map 一直膨胀。
            unsent.clean();
        } finally {
            lock.unlock();
        }

        // 执行完成回调。
        firePendingCompletedRequests();

        metadata.maybeThrowAnyException();
    }
```

trySend()上文已经分析过了。client.poll()就是kafka的网络驱动核心，真正发起网络请求的方法。而这里还有一个重要的方法需要注意，那就是：firePendingCompletedRequests()，这个方法最终会触发回调，这个回调也在上文中提到过的sendFetches()方法中注册的handler：

```java
 (fetchTarget, data, clientResponse) -> {
                    synchronized (Fetcher.this) {
                        handleFetchSuccess(fetchTarget, data, clientResponse);
                    }
                },
                (fetchTarget, data, error) -> {
                    synchronized (Fetcher.this) {
                        handleFetchFailure(fetchTarget, data, error);
                    }
                });
```

handleFetchSuccess()方法又是一长串复杂的调用链路，这个逻辑将在下一篇文章重点分析，用一句话总结就是：**handleFetchSuccess() 处理 broker 返回的 Fetch 成功响应，把每个分区返回的数据包装成 CompletedFetch 放进本地 fetchBuffer，同时维护 fetch session、leader 变更、offset 校验和 fetch 请求状态。**

****

经过这么复杂的逻辑之后，pollForFetches()也拿到了想要的结果，最终从<font style="color:rgb(26, 28, 31);">ClassicKafkaConsumer.</font>poll()返回fetch结果。

