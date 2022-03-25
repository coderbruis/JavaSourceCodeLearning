package com.bruis.learnnetty.im.server.handler;

import com.bruis.learnnetty.im.model.JoinGroupRequestPacket;
import com.bruis.learnnetty.im.model.JoinGroupResponsePacket;
import com.bruis.learnnetty.im.util.SessionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class JoinGroupRequestHandler extends SimpleChannelInboundHandler<JoinGroupRequestPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JoinGroupRequestPacket msg) throws Exception {
        // 目标群聊id
        String groupId = msg.getGroupId();
        ChannelGroup channelGroup = SessionUtil.getChannelGroup(groupId);
        JoinGroupResponsePacket responsePacket = new JoinGroupResponsePacket();
        responsePacket.setSuccess(true);
        responsePacket.setGroupId(groupId);
        if (null == channelGroup) {
            responsePacket.setSuccess(false);
            responsePacket.setReason("没有该群聊，请重试...");
        } else {
            channelGroup.add(ctx.channel());
        }
        ctx.channel().writeAndFlush(responsePacket);
    }
}
