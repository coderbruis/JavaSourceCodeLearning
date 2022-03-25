package com.bruis.learnnetty.im.server.handler;

import com.bruis.learnnetty.im.model.CreateGroupRequestPacket;
import com.bruis.learnnetty.im.model.CreateGroupResponsePacket;
import com.bruis.learnnetty.im.util.IDUtil;
import com.bruis.learnnetty.im.util.SessionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import org.omg.PortableServer.ID_UNIQUENESS_POLICY_ID;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class CreateGroupRequestHandler extends SimpleChannelInboundHandler<CreateGroupRequestPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CreateGroupRequestPacket msg) throws Exception {
        List<String> userIdList = msg.getUserIdList();
        List<String> userNameList = new ArrayList<>();

        ChannelGroup channelGroup = new DefaultChannelGroup(ctx.executor());

        for (String userId : userIdList) {
            Channel channel = SessionUtil.getChannel(userId);
            if (null != channel) {
                channelGroup.add(channel);
                userNameList.add(SessionUtil.getSession(channel).getUserName());
            }
        }

        String groupId = IDUtil.randomUserId();
        CreateGroupResponsePacket createGroupResponsePacket = new CreateGroupResponsePacket();
        createGroupResponsePacket.setSuccess(true);
        createGroupResponsePacket.setGroupId(groupId);
        createGroupResponsePacket.setUserNameList(userNameList);

        channelGroup.writeAndFlush(createGroupResponsePacket);

        System.out.print("群创建成功，id 为[" + createGroupResponsePacket.getGroupId() + "], ");
        System.out.println("群里面有：" + createGroupResponsePacket.getUserNameList());

        SessionUtil.bindChannelGroup(groupId, channelGroup);
    }
}
