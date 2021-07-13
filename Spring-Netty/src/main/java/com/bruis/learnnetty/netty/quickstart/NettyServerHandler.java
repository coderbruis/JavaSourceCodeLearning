package com.bruis.learnnetty.netty.quickstart;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

/**
 * @author LuoHaiYang
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("channelActive");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        System.out.println("channelRegistered...");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        System.out.println("handlerAdded");
    }

    /**
     * 读取实际数据，这里就是我们读取客户端发送的消息
     * @param ctx 上下文对象，包含了管道pipeline，通道channel，地址
     * @param msg 就是客户端发送的数据  默认Object
     * @throws Exception
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
/*
        System.out.println("服务器读取线程 " + Thread.currentThread().getName());
        System.out.println("server ctx = " + ctx);
        System.out.println("看看Channel和pipeline的关系");
        Channel channel = ctx.channel();
        // 本质是一个双向连接，出站入站
        ChannelPipeline pipeline = ctx.pipeline();
        // 将msg转成一个ByteBuf
        // ByteBuf是Netty提供的，不是NIO的ByteBuffer
        ByteBuf byteBuf = (ByteBuf)msg;
        System.out.println("客户端发送消息是：" + byteBuf.toString(CharsetUtil.UTF_8));
        System.out.println("客户端地址：" + channel.remoteAddress());
*/

        // 自定义任务
/*
        ctx.channel().eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, client... task", CharsetUtil.UTF_8));
                    System.out.println("channel hashcode = " + ctx.channel().hashCode());
                } catch (Exception e) {
                    System.out.println("Exception" + e.getMessage());
                }
            }
        });

        ctx.channel().eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, client2... task", CharsetUtil.UTF_8));
                    System.out.println("channel hashcode = " + ctx.channel().hashCode());
                } catch (Exception e) {
                    System.out.println("Exception" + e.getMessage());
                }
            }
        });
*/

        // 自定义定时任务
        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, client3... task", CharsetUtil.UTF_8));
                    System.out.println("channel hashcode = " + ctx.channel().hashCode());
                } catch (Exception e) {
                    System.out.println("error" + e);
                }
            }
        }, 5, TimeUnit.SECONDS);

    }

    /**
     * 数据读取完毕
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // writeAndFlush 是write + flush
        // 将数据写入到缓存，并刷新
        // 我们对这个发送的数据会进行编码
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello, client...", CharsetUtil.UTF_8));
    }

    /**
     * 处理异常，一般是需要关闭通道
     * @param ctx 上下文
     * @param cause 异常
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
