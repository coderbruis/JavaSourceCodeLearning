package com.bruis.learnnetty.im.server.handler;

import com.bruis.learnnetty.im.model.GroupMessageRequestPacket;
import com.bruis.learnnetty.im.model.GroupMessageResponsePacket;
import com.bruis.learnnetty.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
@ChannelHandler.Sharable
public class GroupMessageRequestHandler extends SimpleChannelInboundHandler<GroupMessageRequestPacket> {

    public static final GroupMessageRequestHandler INSTANCE = new GroupMessageRequestHandler();

    public GroupMessageRequestHandler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupMessageRequestPacket msg) throws Exception {
        String toGroupId = msg.getToGroupId();
        GroupMessageResponsePacket responsePacket = new GroupMessageResponsePacket();
        responsePacket.setFromGroupId(toGroupId);
        responsePacket.setMessage(msg.getMessage());
        responsePacket.setFromUser(SessionUtil.getSession(ctx.channel()));

        ChannelGroup channelGroup = SessionUtil.getChannelGroup(toGroupId);
        channelGroup.writeAndFlush(responsePacket);
    }
}
