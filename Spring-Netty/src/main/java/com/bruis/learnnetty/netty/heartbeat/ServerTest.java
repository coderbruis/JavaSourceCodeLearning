package com.bruis.learnnetty.netty.heartbeat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author lhy
 * @date 2021/8/19
 */
public class ServerTest {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup=new NioEventLoopGroup();
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        serverBootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                //设置线程队列中等待连接的个数
                .option(ChannelOption.SO_BACKLOG,128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, 65535)
                .childOption(ChannelOption.SO_RCVBUF, 65535)
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                                .addLast(new NettyEncoder(),
                                        new NettyDecoder(),
                                        new ConnectServerHandler());

                    }
                });

        ChannelFuture future = serverBootstrap.bind(9090).sync();
        future.channel().closeFuture().sync();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public static class ConnectServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            System.out.println("Server服务端：" + new String(msg.getBody()));
            RemotingCommand response = new RemotingCommand();
            response.setBody("接受到请求了".getBytes());
            response.setCode(0);
            ctx.writeAndFlush(response);
        }
    }
}
