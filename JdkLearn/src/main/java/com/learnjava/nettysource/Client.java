package com.learnjava.nettysource;

import java.io.IOException;
import java.net.Socket;

/**
 * @author LuoHaiYang
 */
public class Client {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8000;
    private static final int SLEEP_TIME = 5000;

    public static void main(String[] args) throws IOException {
        final Socket socket = new Socket(HOST, PORT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("cient start success...");
                while (true) {
                    try {
                        String message = "hello world";
                        System.out.println("client send data : " + message);
                        socket.getOutputStream().write(message.getBytes());
                    } catch (Exception e) {
                        System.out.println("write data error!");
                    }
                    sleep();
                }
            }
        }).start();
    }

    private static void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
