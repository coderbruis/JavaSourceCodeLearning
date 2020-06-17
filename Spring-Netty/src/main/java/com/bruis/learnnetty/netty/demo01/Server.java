package com.bruis.learnnetty.netty.demo01;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author LuoHaiYang
 */
public class Server {
    public static void main(String[] args) throws Exception {

        /**
         * 1. 创建两个线程组：bossGroup和workerGroup
         * 2. bossGroup只是处理连接请求，真正的和客户端业务处理的话，会交给workerGroup
         * 3. bossGroup和workerGroup含有的子线程（NioEventLoop）的个数，默认实际为CPU核数 * 2
         *
         * bossGroup对应着socket编程中的服务端的Thread，用于监听是否有client连接
         * workGroup对应着socket编程中的数据读取，读取server端读到的数据
         *
         */
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            // 创建服务器端的启动对象，配置参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 使用链式编程来进行设置, group设置两个线程组
            bootstrap.group(bossGroup, workGroup)
                    // 使用NioServerSocketChannel来作为服务器的通道实现
                    .channel(NioServerSocketChannel.class)
                    // 设置线程队列得到的连接数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 设置保持活动链接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 创建一个通道测试对象
                    // 给workerGroup的EventLoop对于的管道设置处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        // 给pipeline设置处理器
                        @Override
                        public void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(new NettyServerHandler());
                        }
                    });

            System.out.println("...服务器 is ready...");

            // 绑定一个端口并且同步，生成一个ChannelFuture对象
            ChannelFuture channelFuture = bootstrap.bind(8888).sync();
            // 对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}
