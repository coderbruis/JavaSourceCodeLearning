package com.bruis.learnnetty.im.console;

import com.bruis.learnnetty.im.model.JoinGroupRequestPacket;
import io.netty.channel.Channel;

import java.util.Scanner;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class JoinGroupConsoleCommand implements ConsoleCommand{

    @Override
    public void exec(Scanner scanner, Channel channel) {
        JoinGroupRequestPacket requestPacket = new JoinGroupRequestPacket();
        System.out.println("输入groupId, 加入群聊：");
        String groupId = scanner.next();
        requestPacket.setGroupId(groupId);
        channel.writeAndFlush(requestPacket);
    }
}
