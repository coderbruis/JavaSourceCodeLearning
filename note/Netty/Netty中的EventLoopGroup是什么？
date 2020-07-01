## 前言

在Netty中，抽象出线程组Group，它的类型为EventLoopGroup。在Netty服务端，有两组EventLoopGroup，一个是
parentGroup，用于负责客户端的连接；另一个是childGroup，用于负责网络的读写。

## 1. EventLoopGroup

在Netty中，抽象出一种线程池EventLoopGroup，底层是ScheduledExecutorService。那么EventLoopGroup的作用是什么呢？

首先这里需要注意的是，EventLoopGroup是一个接口，接口中定义了register方法，用于注册通道Channel。那么EventLoopGroup作为线程池，当然是用于存储EventLoop的，这里可以大胆的猜想，EventLoop的作用类似于一个线程，接下来就去证实一下我们的猜想。EventLoop是什么？

### 1.1 EventLoop

这里先给出一个结论：EventLoop表示一个不断循环的执行处理任务的线程，在Netty中用的比较多的是NioEventLoop，每个NioEventLoop都有一个selector，用于监听绑定在其上的socket的网络编程。

每个NioEventLoop内部都有一个selector, 用于监听绑定在其上的socket的网络通讯。

NioEventLoop循环执行三件事：
1. 轮询accept事件；
2. 处理accept事件，与client建立连接，生成NioSocketChannel，并将其注册到某个worker NIOEventLoop上的selector。
3. 处理任务队列的任务，即runAllTasks

任务队列中的task有3中典型使用场景：
1. 自定义普通任务；
2. 自定义定时任务；
3. 非当前Reactor线程调用Channel的各种方法；

NioEventLoop内部采用串行化设计，从设计的读取->解码->处理->编码->发送，始终由IO线程NioEventLoop负责。

每个NioEventLoop中包含有一个Selector，一个taskQueue，taskQueue是在父类SingleThreadEventLoop中。

每个NioEventLoop的Selector上可以注册多个NioChannel。

每个NioChannel只会绑定在唯一的NioEventLoop上。

每个NioChannel都绑定有一个自己的ChannelPipeline。

## 2. 