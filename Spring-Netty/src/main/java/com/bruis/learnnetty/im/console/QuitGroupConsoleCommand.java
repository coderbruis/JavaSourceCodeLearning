package com.bruis.learnnetty.im.console;

import com.bruis.learnnetty.im.model.QuitGroupRequestPacket;
import io.netty.channel.Channel;

import java.util.Scanner;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class QuitGroupConsoleCommand implements ConsoleCommand {

    @Override
    public void exec(Scanner scanner, Channel channel) {
        QuitGroupRequestPacket quitGroupRequestPacket = new QuitGroupRequestPacket();

        System.out.print("输入 groupId，退出群聊：");
        String groupId = scanner.next();

        quitGroupRequestPacket.setGroupId(groupId);
        channel.writeAndFlush(quitGroupRequestPacket);
    }
}
