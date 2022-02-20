## NioServerSocketChannel读取数据原理分析

NioServerSocketChannel是AbstractNioMessageChannel的子类，而NioSocketChannel是AbstractNioByteChannel的子类，并且他们都有两个公共的父类：AbstractNioChannel、AbstractChannel。

在Netty中Channel是用来定义对网络IO的读写操作的相关接口，与NIO的Channel接口类似。Channel的功能主要有网络IO的读写、客户端发起的连接、主动关闭连接、关闭链路、获取通信双方的网络地址等。
一些公共的基础方法都在这个AbstractChannel抽象类中实现，几个核心的方法如：channel的注册，channel撤销注册，网络IO的读、写。但对于一些特定的功能则需要不同的实现类去实现，这样最大限度地实现了功能和接口的重用，
就如AbstractNioChannel中主要定义了doRegister()、doConnect()、newDirectBuffer()方法。

## 1. NioServerSocketChannel源码分析

NioServerSocketChannel是AbstractNioMessageChannel的子类，由于它由服务端使用，并且只负责监听Socket的接入，不关心IO的读写，所以与NioSocketChannel相比要简单得多。

NioServerSocketChannel封装了NIO中的ServerSocketChannel，并通过newSocket()方法打开了ServerSocketChannel

NioServerSocketChannel.class

```java
    private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new ChannelException(
                    "Failed to open a server socket.", e);
        }
    }
```

对于NioServerSocketChannel注册至selector上的操作，是在AbstractNioChannel中实现的，源码如下：

```java
    @Override
    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
                selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
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

在ServerSocketChannel的开启，selector上的注册等前期工作完成后，NioServerSocketChannel的开始监听新连接的加入，源码如下：

```java
    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        // 拿到jdk底层channel
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {
                // new出一个NioSocketChannel，将jdk SocketChannel封装成NioSocketChannel，并且这里给NioSocketChannel注册了一个SelectionKey.OP_READ事件
                buf.add(new NioSocketChannel(this, ch));                // 往buf里写入NioSocketChannel
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }
```

上面的源码展示了Netty最终拿到新连接请求后，将jdk底层的SocketChannel封装NioSocketChannel的过程，那么selector是如何获取到accept事件后，调用到这个doReadMessages方法的呢？

为了分析原理的延续，故事还要回到bossGroup的NioEventLoop里，当bossGroup启动，NioServerSocketChannel实例新建并注册到selector之后，Netty的bossGroup就会运行一个NioEventLoop，它的核心工作就是作为一个selector一直去监听客户端发出的accept、connect、read、write等事件。具体逻辑查看NioEventLoop#run()方法，详细的原理请回看之前的NioEventLoop的原理分析，此处只分析NioEventLoop#run()获取到链接事件到调用NioServerSocketChannel#doReadMessages()的链路。

1. NioEventLoop#run()一直轮训，监听这客户端发出的事件，在轮训过程中如果有任务产生，则会优先执行这些任务，调用非阻塞的selectNow()，否则调用select(deadlineNanos)阻塞指定时间去监听客户端事件。
2. 调用NioEventLoop#processSelectedKeys()，Netty默认用的是优化过后的selectedKey，所以调用的是NioEventLoop#processSelectedKeysOptimized()方法。
3. 在processSelectedKeysOptimized方法里会遍历selectedKeys，去拿selectedKeys中的SelectionKey，这个key就是从网络中获取到的感兴趣事件。
4. 先通过SelectionKey获取attachment，及对应的事件channel。由于这里是获取的是accept事件，所以SelectionKey#attachment()获取到的是NioServerSocketChannel对象。
5. 在NioEventLoop#processSelectedKey()方法中，首先拿到NioServerSocketChannel父类AbstractNioMessageChannel中的NioMessageUnsafe对象，接着根据readyOps进行判断，这里当然就是SelectionKey.OP_ACCEPT事件。
6. 调用NioMessageUnsafe#read()方法，最终该方法调用了NioServerSocketChannel#doReadMessages()，完了之后会新建一个对SelectionKey.OP_READ事件感兴趣的NioSocketChannel对象，并存放在readBuf的一个集合中。
7. 接着调用ChannelPipeline#fireChannelRead()方法，目的在于最终调用ServerBootstrapAcceptor#channelRead()方法，调用childGroup#register(child)，把新建的NioSocketChannel对象注册到selector上。

这样，NioServerSocketChannel监听accept事件，接收到客户端连接后，封装客户端的“连接”到NioSocketChannel对象，并注册到selector上，后面的网络IO的读写操作都由这个NioSocketChannel对象来负责处理。

上述核心的6步源码如下：

NioEventLoop.class
```java
    @Override
    protected void run() {
        for (;;) {
            try {
                try {
                    switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
                    // ... 省略
                    case SelectStrategy.SELECT:
                        select(wakenUp.getAndSet(false));
                        // ... 省略
                        if (wakenUp.get()) {
                            selector.wakeup();
                        }
                        // fall through
                    default:
                    }
                } catch (IOException e) {
                    rebuildSelector0();
                    handleLoopException(e);
                    continue;
                }
                // ... 省略
                
                // 步骤1
                processSelectedKeys();
                runAllTasks();

                // ... 省略
            } catch (Throwable t) {
                handleLoopException(t);
                // ... 省略
            }
        }
    }
```

NioEventLoop.class
```java
    // 步骤2
    private void processSelectedKeysOptimized() {
        for (int i = 0; i < selectedKeys.size; ++i) {
            // 步骤3
            final SelectionKey k = selectedKeys.keys[i];
            selectedKeys.keys[i] = null;

            // 步骤4
            final Object a = k.attachment();

            if (a instanceof AbstractNioChannel) {
                // 步骤5
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (needsToSelectAgain) {
                selectedKeys.reset(i + 1);

                selectAgain();
                i = -1;
            }
        }
    }
```

NioEventLoop.class
```java
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                return;
            }
            if (eventLoop != this || eventLoop == null) {
                return;
            }
            unsafe.close(unsafe.voidPromise());
            return;
        }

        try {
            int readyOps = k.readyOps();
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);

                unsafe.finishConnect();
            }

            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                ch.unsafe().forceFlush();
            }
    
            // 步骤5
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }
```

NioServerSocketChannel.class

```java
    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        // 拿到jdk 的SocketChannel，代表着和客户端的一个连接socket
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {
                // 步骤6
                // 封装一个NioSocketChannel对象，并且设置感兴趣事件为：SelectionKey.OP_READ
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }
```

ServerBootstrapAcceptor.class

```java
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final Channel child = (Channel) msg;

            child.pipeline().addLast(childHandler);

            setChannelOptions(child, childOptions, logger);
            setAttributes(child, childAttrs);

            try {
                // 步骤7 
                // 在workerGroup的NioEventLoop上的selector注册了NioSocketChannel
                childGroup.register(child).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            forceClose(child, future.cause());
                        }
                    }
                });
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }
```

以上就是Netty中有关NioServerSocketChannel读取数据的底层原理分析。

下一篇分析NioSocketChannel的发送、读取数据底层原理。