package com.bruis.learnnetty.im.console;

import io.netty.channel.Channel;

import java.util.Scanner;

/**
 * @Description 指令接口
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public interface ConsoleCommand {
    void exec(Scanner scanner, Channel channel);
}
