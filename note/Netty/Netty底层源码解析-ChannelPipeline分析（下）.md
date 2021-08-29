## 1. 概览

上篇已经讲解了ChannelPipeline以及ChannelHandler的关系以及对应的类继承关系图，本节来详细分析一下inbound和outbound的原理。

## 2. DefaultChannelPipeline源码分析

在DefaultChannelPipeline中，定义了一个head“头结点”和一个tail“尾结点”，它们都是AbstractChannelhandlerContext类的节点，我们都知道在ChannelPipeline中AbstractChannelHandlerContext就是节点元素的抽象类实现，而这个handlerContext持有ChannelHandler。

在Netty中我们还需要知道inbound和outbound类型的ChannelHandler节点的执行顺序。

下面来先看下一个Netty的demo

该Netty的demo中，分别定义了六个Handler，分为两组，一组是inboundHandler，另一组是outboundHandler。


InBoundHandlerA
```java
public class InBoundHandlerA extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("InBoundHandlerA: " + msg);
        ctx.fireChannelRead(msg);
    }
}
```

InBoundHandlerB
```java
public class OutBoundHandlerB extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("OutBoundHandlerB: " + msg);
        ctx.write(msg, promise);
    }


    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        ctx.executor().schedule(() -> {
            ctx.channel().write("ctx.channel().write -> hello world");
            ctx.write("hello world");
        }, 3, TimeUnit.SECONDS);
    }
}
```

InBoundHandlerC
```java
public class InBoundHandlerC extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("InBoundHandlerC: " + msg);
        ctx.fireChannelRead(msg);
    }
}
```

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
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new InBoundHandlerA());
                            ch.pipeline().addLast(new InBoundHandlerB());
                            ch.pipeline().addLast(new InBoundHandlerC());
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

执行结果如下：
```
InBoundHandlerA: hello world
InBoundHandlerB: hello world
InBoundHandlerC: hello world
```

可以发现Netty中，对于inboundHandler来说是按照顺序执行操作的。

接着在看看outboundHandler定义如下

OutBoundHandlerA
```java
public class OutBoundHandlerA extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("OutBoundHandlerA: " + msg);
        ctx.write(msg, promise);
    }
}
```

OutBoundHandlerB
```java
public class OutBoundHandlerB extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("OutBoundHandlerB: " + msg);
        ctx.write(msg, promise);
    }
}
```

OutBoundHandlerC
```java
public class OutBoundHandlerC extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("OutBoundHandlerC: " + msg);
        ctx.write(msg, promise);
    }
}
```


然后修改Server类为如下，

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
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new OutBoundHandlerA());
                            ch.pipeline().addLast(new OutBoundHandlerB());
                            ch.pipeline().addLast(new OutBoundHandlerC());
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

执行结果如下：
```
OutBoundHandlerC: ctx.channel().write -> hello world
OutBoundHandlerB: ctx.channel().write -> hello world
OutBoundHandlerA: ctx.channel().write -> hello world
OutBoundHandlerA: hello world
```

可以看到在Netty中对于ountboundHandler来说，是倒序执行的。

整个Netty执行ChannelHandler可以用下图来描述。

![channelPipeline事件传播图](https://coderbruis.github.io/javaDocs/img/netty/source/ChannelPipeline事件传播图.png)


上图描述的Head节点顺序执行，Tail节点逆序执行的源码是在DefaultChannelPipeline中，在《Netty-ChannelPipeline-上》文章开头就已经说明了，对于inboundHandler类型的Handler，主要还是用于监听Channel的read、register、active、exceptionCaught等事件，而对于outboundHandler类型来说，主要是用于bind、connect、write、flush等事件，回顾了这一点后，我们在继续看DefaultChannelPipeline源码

```java
public class DefaultChannelPipeline implements ChannelPipeline {
    ... 省略

    @Override
    public final ChannelPipeline fireChannelRead(Object msg) {
        AbstractChannelHandlerContext.invokeChannelRead(head, msg);
        return this;
    }

    @Override
    public final ChannelFuture write(Object msg) {
        return tail.write(msg);
    }

    ... 省略
}
```

分别以inbound类型的channelRead和outbound类型的write来分析。

DefaultChannelPipeline.java
```java
    @Override
    public final ChannelPipeline fireChannelRead(Object msg) {
        AbstractChannelHandlerContext.invokeChannelRead(head, msg);
        return this;
    }
```
在AbstractChannelHandlerContext#invokeChannelRead方法中，传入了一个重要的入参：head，这里就是传入的Head头结点，这一重要调用得以让inbound类型handler在ChannelPipeline中按顺序执行。

AbstractChannelHandlerContext.java
```java
    static void invokeChannelRead(final AbstractChannelHandlerContext next, Object msg) {
        final Object m = next.pipeline.touch(ObjectUtil.checkNotNull(msg, "msg"), next);
        EventExecutor executor = next.executor();
        // 在NioEventLoop线程内，next这里传入的是head头结点
        if (executor.inEventLoop()) {
            next.invokeChannelRead(m);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelRead(m);
                }
            });
        }
    }

    private void invokeChannelRead(Object msg) {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelRead(this, msg);
            } catch (Throwable t) {
                invokeExceptionCaught(t);
            }
        } else {
            fireChannelRead(msg);
        }
    }
    
```

ChannelInboundHandler#channelRead的调用，会最终来到InBoundHandlerA里的channelRead方法。
```java
public class InBoundHandlerA extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("InBoundHandlerA: " + msg);
        ctx.fireChannelRead(msg);
    }
}
```

经过AbstractChannelHandlerContext#fireChannelRead，会在ChannelPipeline中寻找下一个inbound，然后继续执行channelRead。

```java
    @Override
    public ChannelHandlerContext fireChannelRead(final Object msg) {
        invokeChannelRead(findContextInbound(MASK_CHANNEL_READ), msg);
        return this;
    }
```


细看OutBoundHandlerB#handlerAdded方法由两个write，一个是ctx.channel.write，另一个是ctx.write，这两个有啥区别呢？为啥输出结果是三条：ctx.channel().write -> hello world，一条hello world呢？

启动Server启动类之后，再cmd窗口输入连接socket的命令debug之后分析得

```
telnet 127.0.0.1 8888
```

在客户端socket连接进Netty之后，会先注册channel并init初始化，这时会调用Server类里ServerBootstrap注入的ChannelInitilizer的initChannel方法，最终得以向ChannelPipeline里添加进OutBoundHandlerA、OutBoundHandlerB、OutBoundHandlerC，随后调用

```java
ch.pipeline().addLast(new xxxx)
```
只有会触发DefaultChannelPipeline#callHandlerAdded0()方法，最终来到OutBoundHandler里的handlerAdded()方法，并向Netty的定时任务队列里添加了一个匿名内部任务，也就是：

```java
    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        ctx.executor().schedule(() -> {
            ctx.channel().write("ctx.channel().write -> hello world");
            ctx.write("hello world");
        }, 3, TimeUnit.SECONDS);
    }
```

随后完成客户端Socket的初始化工作。此时服务端的selector继续执行for死循环，执行到任务队列，此时发现任务队列中有一个定时任务需要执行，则拿出任务并执行任务，执行过程会跳转到上面的匿名内部类，并依次执行ctx.channel().write()和ctx.write()两个方法。

```java
ctx.channel().write()
```
方法会从ChannelPipeline的尾部tail开始执行（上文已经总结过，outboundHandler都是从tail节点开始执行handler） ，所以字符串“ctx.channel().write -> hello world”就会按outboundHandlerC、outboundHandlerB、outboundHandlerC这个顺序开始执行，执行完head节点之后会一路往上返回到Ctx.channel().write()
方法，并最后去执行ctx.write()方法，而ctx.write()方法会从当前的handler节点开始向前执行，所以当前outboundHandlerB的前节点是outboundHandlerA，所以最终控制台打印出：
```
OutBoundHandlerC: ctx.channel().write -> hello world
OutBoundHandlerB: ctx.channel().write -> hello world
OutBoundHandlerA: ctx.channel().write -> hello world
OutBoundHandlerA: hello world
```

整个过程比较复杂，也比较绕，下面用一张流程图来描述整个过程。

![NettyChannelPipeline流程图1](https://coderbruis.github.io/javaDocs/img/netty/source/NettyChannelPipeline流程图1.png)


- TODO ChannelPipeline优化？MASK
- TODO SimpleChannelInboundHandler源码分析
