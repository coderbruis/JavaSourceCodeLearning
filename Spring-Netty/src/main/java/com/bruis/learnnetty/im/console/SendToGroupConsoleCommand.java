package com.bruis.learnnetty.im.console;

import com.bruis.learnnetty.im.model.GroupMessageRequestPacket;
import io.netty.channel.Channel;

import java.util.Scanner;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class SendToGroupConsoleCommand implements ConsoleCommand {

    @Override
    public void exec(Scanner scanner, Channel channel) {
        System.out.print("发送消息给某个某个群组：");

        String toGroupId = scanner.next();
        String message = scanner.next();
        channel.writeAndFlush(new GroupMessageRequestPacket(toGroupId, message));

    }
}
