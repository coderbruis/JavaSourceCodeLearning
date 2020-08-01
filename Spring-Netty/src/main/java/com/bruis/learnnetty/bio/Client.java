package com.bruis.learnnetty.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author LuoHaiYang
 *
 * 在bio的例子中，主要有server端和client端:
 *
 * 1. Server端开启一个线程通过serverSocket.accept()来监听有没有socket连接进来，之列称之为线程A。
 * 2. 然后在ClientHandler中又开启了一个线程，称为B，用于从socket中获取服务器端传过来的消息，线程B中通过while-true循环来获取。
 * 3. 在client客户端中，还有一个线程称为C，一直去循环获取服务端的数据。
 *
 * 在nio中，NioEventLoop源码中的run方法就对应着线程A的while-true方法以及线程c的while-true方法。而NioEventLoop的select方法就对应着serverSocket的accept操作。
 *
 */
public class Client {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8000;
    // 休眠时间
    private static final int SLEEP_TIME = 5000;
    // 最大数据大小
    public static final int MAX_DATA_LEN = 1024;

    public static void main(String[] args) throws IOException {

        final Socket socket = new Socket(HOST, PORT);
        System.out.println("客户端启动成功!");

        while (true) {
            try {
                Scanner scan = new Scanner(System.in);
                // 读取控制台输入信息
                String clientMessage = scan.nextLine();
                System.out.println("客户端发送数据: " + clientMessage);
                // 将数据写入流中
                socket.getOutputStream().write(clientMessage.getBytes());

                // 将数据从JVM中读取出来，存放在输入流中
                InputStream inputStream = socket.getInputStream();

                byte[] data = new byte[MAX_DATA_LEN];
                int len;
                while ((len = inputStream.read(data)) != -1) {
                    String message = new String(data, 0, len);
                    System.out.println("服务器传来消息: " + message);

                }

            } catch (Exception e) {
                System.out.println("写数据出错!");
            }
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
