package com.bruis.learnnetty.netty.demo01;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;

/**
 * @author LuoHaiYang
 */
public class Server {
    public static void main(String[] args) throws Exception {

        // bossGroup对应着socket编程中的服务端的Thread，用于监听是否有client连接
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // workGroup对应着socket编程中的数据读取，读取server端读到的数据
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                    // 给服务端channel设置SocketChannel类型
                    .channel(NioServerSocketChannel.class)
                    // 给每个客户端连接设置TCP基本属性
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 绑定属性
                    .childAttr(AttributeKey.newInstance("childAttr"), "childAttrValue")
                    // handler表示客户端的一个处理逻辑
                    .handler(new ServerHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            //channel.pipeline().addLast(new AuthHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(8888).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}
