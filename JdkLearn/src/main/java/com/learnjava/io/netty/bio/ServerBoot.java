package com.learnjava.io.netty.bio;


/**
 * @author LuoHaiYang
 */
public class ServerBoot {
    public static final int PORT = 8000;

    public static void main(String[] args) {
        Server server = new Server(PORT);

    }
}
