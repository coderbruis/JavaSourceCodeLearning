package com.bruis.learnnetty.netty.connections.shortconnections;

import com.alibaba.fastjson.JSONObject;
import com.bruis.learnnetty.thread.synchronize.RequestFuture;
import com.bruis.learnnetty.thread.synchronize.Response;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;

import java.nio.charset.StandardCharsets;

/**
 * @author lhy
 * @date 2022/2/11
 */
public class NettyClient {
    public static EventLoopGroup group = null;
    public static Bootstrap bootstrap = null;
    static {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }
    public static void main(String[] args) {
        try {
            Promise<Response> promise = new DefaultPromise<>(group.next());
            final ClientHandler clientHandler = new ClientHandler();
            clientHandler.setPromise(promise);
            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                            .addLast(new StringDecoder())
                            .addLast(clientHandler)
                            .addLast(new LengthFieldPrepender(4, false))
                            .addLast(new StringEncoder(StandardCharsets.UTF_8));
                }
            });
            ChannelFuture future = bootstrap.connect("127.0.0.1", 8080).sync();
            RequestFuture request = new RequestFuture();
            request.setId(1);
            request.setRequest("hello world!");
            String requestString = JSONObject.toJSONString(request);
            // 向服务端发送请求
            future.channel().writeAndFlush(requestString);
            // 同步阻塞等待服务端响应请求
            Response response = promise.get();
            System.out.println(JSONObject.toJSONString(response));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
