# Netty 服务端启动分析

在Java中，网络通信是通过Socket来进行的，那么在Netty中，服务端的Socket是在哪里进行初始化的？并且在哪里进行accept连接？ Netty里的Channel是啥，有啥作用呢？带着这三个问题，进入本文的Netty服务端启动分析。

本文分析将分为四大步：

1. 创建服务端Channel；
2. 初始化服务端Channel；
3. 注册selector；
4. 端口绑定；

## 1. 创建服务端Channel

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

服务端构建好ServerBootstrap之后，通过bind()方法进行绑定。进入ServerBootstrap的父类AbstractBootstrap后，一路调用来到AbstractBootstrap#doBind()方法，首先就是初始化并注册Channel。

![netty01png](https://coderbruis.github.io/javaDocs/img/netty/source/netty01_01.png)

在initAndRegister处channelFactory是ReflectiveChannelFactory，具体赋值处是在ServerBootstrap#channel()方法中定义的，并且传入的channel是：NioServerSocketChannel，上图中可以见。

查看到ReflectiveChannelFactory#newChannel()方法，实际就是调用的NioServerSocketChannel#newInstance()方法反射构建一个channel对象。


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

总结下服务端创建Channel的三件重要事情：

1. 通过反射来创建JDK底层的channel；
2. 设置Channel为非阻塞模式ch.configureBlocking(false);
3. 创建一个pipeline对象；



## 2. 初始化服务端Channel

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

## 3. 注册selector

整个注册selector过程可以分为以下几步：

- AbstractChannel#register(channel) 入口
	- this.eventLoop = eventLoop 绑定线程
	- register0() 实际注册 
	  	- doRegister() 调用jdk底层进行注册
	  	- invokeHandlerAddedIfNeeded()
		- fireChannelRegistered() 传播注册成功的事件
	

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

## 4. 端口绑定

端口绑定流程如下：

- AbstractUnsafe#bind() 入口
  	- dobind()
  		- javaChannel().bind() jdk底层绑定
	- pipeline.fireChanelActive() 传播事件
		- HeadContext.readIfIsAutoRead()
	

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
  
