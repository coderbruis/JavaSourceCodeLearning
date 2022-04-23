package com.bruis.learnnetty.im.console;

import com.bruis.learnnetty.im.model.LoginRequestPacket;
import io.netty.channel.Channel;

import java.util.Scanner;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public class LoginConsoleCommand implements ConsoleCommand {

    @Override
    public void exec(Scanner scanner, Channel channel) {
        LoginRequestPacket loginRequestPacket = new LoginRequestPacket();

        System.out.print("输入用户名登录: ");
        String userIdStr; // 在退出登录logout之后 这里会读取到最后一个回车符 用户名就是空字符串会导致无法退出登录
        while ((userIdStr = scanner.nextLine()).isEmpty()) {
            System.out.println("用户名异常, 请重新输入");
        }
        loginRequestPacket.setUserName(userIdStr);
        loginRequestPacket.setPassword("pwd");

        // 发送登录数据包
        channel.writeAndFlush(loginRequestPacket);
        waitForLoginResponse();
    }

    private static void waitForLoginResponse() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }
}
