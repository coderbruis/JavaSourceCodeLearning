package com.bruis.learnnetty.im.client.handler;

import com.bruis.learnnetty.im.model.MessageRequestPacket;
import com.bruis.learnnetty.im.model.MessageResponsePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public class FirstClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        for (int i = 0; i < 5; i++) {
//            ByteBuf buffer = getByteBuf(ctx);
            MessageRequestPacket packet = new MessageRequestPacket();
            packet.setMessage("你好啊，测试一下Netty的通讯！");
            ctx.channel().writeAndFlush(packet);
        }
    }

    private ByteBuf getByteBuf(ChannelHandlerContext ctx) {
//        byte[] bytes = "你好，欢迎关注我的微信公众号，《闪电侠的博客》!".getBytes(Charset.forName("utf-8"));
        byte[] bytes = "你好".getBytes(Charset.forName("utf-8"));
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(bytes);

        return buffer;
    }
}
