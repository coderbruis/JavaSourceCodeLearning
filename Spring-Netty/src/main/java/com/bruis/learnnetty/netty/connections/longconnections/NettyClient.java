package com.bruis.learnnetty.netty.connections.longconnections;

import com.alibaba.fastjson.JSONObject;
import com.bruis.learnnetty.netty.connections.longconnections.ClientHandler;
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
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;

/**
 * @author lhy
 * @date 2022/2/16
 */
public class NettyClient {
    public static EventLoopGroup group = null;
    public static Bootstrap bootstrap = null;
    public static ChannelFuture future = null;
    static {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ClientHandler clientHandler = new ClientHandler();
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
                        0, 4, 0, 4));
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(clientHandler);
                ch.pipeline().addLast(new LengthFieldPrepender(4, false));
                ch.pipeline().addLast(new StringEncoder(Charset.forName("utf-8")));
            }
        });
        try {
            future = bootstrap.connect("127.0.0.1", 8080).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 说明：对于这个长连接的例子中，使用了静态化，即单链接、长连接，如果是多链接的话不可使用静态化，需使用线程池。
     * @param msg
     * @return
     */
    public Object sendRequest(Object msg) {
        try {
            RequestFuture request = new RequestFuture();
            request.setRequest(msg);
            String requestStr = JSONObject.toJSONString(request);
            future.channel().writeAndFlush(requestStr);
            myselfPrint("我阻塞了", null);
            return request.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    public static void main(String[] args) {
        NettyClient nettyClient = new NettyClient();
        for (int i = 0; i < 10; i++) {
            Object result = nettyClient.sendRequest("hello");
            myselfPrint("拿到结果了", result);
        }
    }

    public static void myselfPrint(String description, Object value) {
        StringBuilder builder = new StringBuilder();
        builder.append(Thread.currentThread().getName());
        if (!StringUtils.isEmpty(description)) {
            builder.append("-").append(description);
        }
        if (!StringUtils.isEmpty(value)) {
            builder.append("-").append(value);
        }
        System.out.println(builder.toString());
    }
}
