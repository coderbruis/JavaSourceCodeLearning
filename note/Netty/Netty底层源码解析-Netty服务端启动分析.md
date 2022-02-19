# Netty 服务端启动分析

在Java中，网络通信是通过Socket来进行的，那么在Netty中，服务端要用到的Socket是在哪里进行初始化的？并且在哪里进行accept接受客户端连接的？ Netty里的Channel是啥，有啥作用呢？带着这三个问题，进入本文的Netty服务端启动分析。

本文分析将分为五大步：

1. Netty中的Channel；
2. 创建服务端Channel；
3. 初始化服务端Channel；
4. 注册selector；
5. 端口绑定；

## 1. Netty中的Channel

在Netty中的Channel是用来定义对网络IO进行读/写的相关接口，与NIO中的Channel接口类似。Channel的功能主要有网络IO的读写、客户端发起的连接、主动关闭连接、关闭链路、获取通信双方的网络地址等。Channel接口下有一个重要的抽象类————AbstractChannel，一些公共的基础方法都在这个抽象类中实现，但对于一些特定的功能则需要不同的实现类去实现，这样最大限度地实现了功能和接口的重用。

在AbstractChannel中的网络IO模型和协议种类比较多，除了TCP协议，Netty还支持了HTTP2协议，如：AbstractHttp2StreamChannel。

Netty对于不同的网络模型以及IO模型，在AbstractChannel的基础上又抽象出了一层，如：AbstractNioChannel、AbstractEpollChannel、AbstractHttp2StreamChannel。

## 2. 创建服务端Channel

创建服务端Channel又可以分为四步，如下：

1. ServerBootstrap#bind() 用户代码入口；
2. initAndRegister() 初始化并注册；
3. newChannel() 创建服务端channel；


首先看下下图简易版的Netty服务端启动代码。

```java
public final class Server {

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childAttr(AttributeKey.newInstance("childAttr"), "childAttrValue")
                    .handler(new ServerHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new AuthHandler());
                            //..

                        }
                    });

            ChannelFuture f = b.bind(8888).sync();

            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

服务端构建好ServerBootstrap之后，通过bind()方法进行绑定。进入ServerBootstrap的父类AbstractBootstrap后，线程经过调用栈的调用后来到AbstractBootstrap#doBind()方法，首先就是初始化并注册Channel。

AbstractBootstrap#doBind()
```java
    private ChannelFuture doBind(final SocketAddress localAddress) {
        // 注册channel
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        // regFuture如果完成了，则isDone为true，否则给regFuture添加一个监听器，当完成的时候再进行doBind0的操作
        if (regFuture.isDone()) {
            // 此时我们已经知道NioServerSocketChannel已经完成了注册
            ChannelPromise promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);

            // 给regFuture添加一个监听器，当注册chanel完成的时候，会回调进来
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            // 拿到ReflectiveChannelFactory，然后通过其newChannel生成一个服务端Channel，底层就是通过反射newInstance()获取实例
            // 这里自然是NioServerSocketChannel实例对象
            channel = channelFactory.newChannel();
            // 初始化channel
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        /**
         * config() -> ServerBootstrapConfig
         * group() -> NioEventLoopGroup，返回的是MultithreadEventLoopGroup
         * register() -> 就是通过chooser选取到NioEventLoop对象
         */
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }
        return regFuture;
    }    
```

在initAndRegister处channelFactory是ReflectiveChannelFactory，具体赋值处是在ServerBootstrap#channel()方法中定义的，并且传入的channel是：NioServerSocketChannel。

ReflectiveChannelFactory#newChannel
```java
    @Override
    public T newChannel() {
        try {
            return constructor.newInstance();
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
        }
    }
```

查看到ReflectiveChannelFactory#newChannel()方法，T的类型是NioServerSocketChannel，所以实际就是调用的NioServerSocketChannel#newInstance()方法反射构建一个channel对象。


那么，我们看下NioServerSocketChannel底层是如何获取通过反射创建服务端Channel的呢？


以下部分源码均在NioServerSocketChannel类中

```java
	public NioServerSocketChannel() {
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }
```

而newSocket()方法是一个静态方法

```java
	private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
        	// 通过SelectorProvider来获取一个ServerSocketChannel，SelectorProvider是通过SPI来获取的
        	// 此处返回一个ServerSocketChannelImpl对象
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new ChannelException(
                    "Failed to open a server socket.", e);
        }
    }
```

```java
	public NioServerSocketChannel(ServerSocketChannel channel) {
		// 调用抽象父类AbstractNioChannel构造方法，注意此处服务端Channel注册的是OP_ACCEPT事件
        super(null, channel, SelectionKey.OP_ACCEPT);
        // TCP参数配置类
        config = new NioServerSocketChannelConfig(this, javaChannel().socket());
    }
```

AbstractNioChannel类
```java
	protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
		// 调用父类AbstractChannel的构造方法
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
        	// 关闭blocking，关闭阻塞模式：比较重要
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                ch.close();
            } catch (IOException e2) {
                if (logger.isWarnEnabled()) {
                    logger.warn(
                            "Failed to close a partially initialized socket.", e2);
                }
            }

            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }
```

AbstractChannel类
```java
    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        // 每个channel的唯一标识
        id = newId();
        // 底层io操作工具类
        unsafe = newUnsafe();
        // channel里的逻辑链pipeline（非常重要）
        pipeline = newChannelPipeline();
    }
```

通过源码阅读，可以总结出Netty服务端创建Channel的三件重要事情：

1. 通过反射来创建JDK底层的channel；
2. 设置Channel为非阻塞模式ch.configureBlocking(false);
3. 创建一个pipeline对象；


## 3. 初始化服务端Channel

初始化服务端Channel可以分为如下的几步：

1. set ChannelOptions，ChannelAttrs 设置options和attrs
2. set Child Options，ChildAttrs，为服务端创建的子链接创建options和attrs
3. config handler，配置服务端pipeline
4. add ServerBootstrapAcceptor，添加连接器


ServerBoostrap端初始化过程
```java
	void init(Channel channel) throws Exception {
		// 获取用户配置的options
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            channel.config().setOptions(options);
        }

		// 配置attrs
        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(childOptions.size()));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(childAttrs.size()));
        }

        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                // 添加ServerBootstrapAccetor
                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
```

## 4. 将Channel注册到selector

整个注册selector过程可以分为以下几步：

1. AbstractChannel$AbstractUnsafe#register(channel)
2. AbstractUnsafe#register0()
3. AbstractUnsafe#doRegister()

AbstractChannel
```java
		@Override
        public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }
            if (isRegistered()) {
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            if (!isCompatible(eventLoop)) {
                promise.setFailure(
                        new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

			// 设置AbstractChannel的eventLoop
            AbstractChannel.this.eventLoop = eventLoop;

            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }
```

AbstractChannel.AbstractUnsafe#register0()
```java
	private void register0(ChannelPromise promise) {
            try {
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                doRegister();
                neverRegistered = false;
                registered = true;

                // 调用handlerAdd事件回调
                pipeline.invokeHandlerAddedIfNeeded();

                safeSetSuccess(promise);
                // 调用register事件回调
                pipeline.fireChannelRegistered();
                
                if (isActive()) {
                    if (firstRegistration) {
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        beginRead();
                    }
                }
            } catch (Throwable t) {
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
```


AbstractNioChannel.java
```java
    @Override
    protected void doRegister() throws Exception {
        boolean selected = false;
        // 这里是个小技巧，for(;;)比while(true)效率要高很多
        for (;;) {
            try {
            	// 将通道channel注册到selector上
                selectionKey = javaChannel().register(eventLoop().selector, 0, this);
                return;
            } catch (CancelledKeyException e) {
                if (!selected) {
                    eventLoop().selectNow();
                    selected = true;
                } else {
                    throw e;
                }
            }
        }
    }
```

就这样，NioServerSocketChannel就以Accept事件注册到了Selector上了。

这里需要注意一点，javaChannel()返回的是AbstractSelectableChannel，调用其register方法用于在给定的selector上注册这个通道channel，并返回一个选这件selectionKey。传入的操作位为0表示对任何事件都不感兴趣，仅仅是完成注册操作。

## 5. 端口绑定

端口绑定流程如下：

1. AbstractBootstrap#bind()
2. AbstractBootstrap#dobind()
3. AbstractChannel#bind()
4. NioServerSocketChannel#doBind()
	

AbstractChannel.AbstractUnsafe#bind()
```java
        @Override
        public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
            assertEventLoop();

            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            // See: https://github.com/netty/netty/issues/576
            if (Boolean.TRUE.equals(config().getOption(ChannelOption.SO_BROADCAST)) &&
                localAddress instanceof InetSocketAddress &&
                !((InetSocketAddress) localAddress).getAddress().isAnyLocalAddress() &&
                !PlatformDependent.isWindows() && !PlatformDependent.isRoot()) {
                // Warn a user about the fact that a non-root user can't receive a
                // broadcast packet on *nix if the socket is bound on non-wildcard address.
                logger.warn(
                        "A non-root user can't receive a broadcast packet if the socket " +
                        "is not bound to a wildcard address; binding to a non-wildcard " +
                        "address (" + localAddress + ") anyway as requested.");
            }

			// 是否active
            boolean wasActive = isActive();
            try {
            	// 调用jdk底层代码进行绑定
                doBind(localAddress);
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
                return;
            }

            if (!wasActive && isActive()) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireChannelActive();
                    }
                });
            }
            safeSetSuccess(promise);
        }
```

NioServerSocketChannel.java
```java
    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
    	// 判断jdk版本
        if (PlatformDependent.javaVersion() >= 7) {
            javaChannel().bind(localAddress, config.getBacklog());
        } else {
            javaChannel().socket().bind(localAddress, config.getBacklog());
        }
    }
```


## 总结

Netty服务端核心启动流程主要是为了创建NioServerSocketChannel，然后将其注册在Selector上，总结下核心步骤如下：

- NioServerSocket#newSocket() 获取服务端channel
- ServerBootstrap#init() 对服务端channel进行初始化
- AbstractChannel.AbstractUnsafe#register() 将服务端Channel注册到Selector上
- AbstractChannel.AbstractUnsafe#doBind() 注册端口号
  

