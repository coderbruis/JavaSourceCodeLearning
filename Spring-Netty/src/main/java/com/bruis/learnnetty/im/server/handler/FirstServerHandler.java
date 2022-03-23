package com.bruis.learnnetty.im.server.handler;

import com.bruis.learnnetty.im.model.MessageRequestPacket;
import com.bruis.learnnetty.im.model.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.Date;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public class FirstServerHandler extends SimpleChannelInboundHandler<MessageRequestPacket> {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;

        System.out.println(new Date() + ": 服务端读到数据 -> " + byteBuf.toString(Charset.forName("utf-8")));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageRequestPacket msg) throws Exception {
        System.out.println(new Date() + ": 服务端读到数据 -> " + msg.getMessage());
    }
}
