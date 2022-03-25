package com.bruis.learnnetty.im.console;

import com.bruis.learnnetty.im.util.SessionUtil;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public class ConsoleCommandManager implements ConsoleCommand {

    private Map<String, ConsoleCommand> consoleCommandMap;

    public ConsoleCommandManager() {
        consoleCommandMap = new HashMap<>();
        consoleCommandMap.put("sendToUser", new SendToUserConsoleCommand());
        consoleCommandMap.put("logout", new LoginConsoleCommand());
        consoleCommandMap.put("createGroup", new CreateGroupConsoleCommand());
        consoleCommandMap.put("joinGroup", new JoinGroupConsoleCommand());
        consoleCommandMap.put("quitGroup", new QuitGroupConsoleCommand());
        consoleCommandMap.put("listGroup", new ListGroupMembersConsoleCommand());
        consoleCommandMap.put("sendToGroup", new SendToGroupConsoleCommand());
    }

    @Override
    public void exec(Scanner scanner, Channel channel) {
        String command = scanner.next();
        if (!SessionUtil.hasLogin(channel)) {
            return;
        }
        ConsoleCommand consoleCommand = consoleCommandMap.get(command);
        if (null != consoleCommand) {
            consoleCommand.exec(scanner, channel);
        } else {
            System.err.println("无法识别[" + command + "]指令，请重新输入");
        }
    }
}
