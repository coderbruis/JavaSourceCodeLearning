package com.learnjava.io.bio;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author LuoHaiYang
 */
public class BIOServer {
    /**
     *
     * 玩法就是启动main线程去创建ServerSocket，然后监听6666端口等待连接。
     *
     * 客户端的话，可以在terminal里使用命令：telnet 127.0.0.1 6666 就可以连接到ServerSocket 了。
     *
     */
    public static void main(String[] args) throws Exception {
        // 创建一个无界线程池
        // 如果有客户端连接，就创建线程与之通信
        ExecutorService cached = Executors.newCachedThreadPool();

        // 创建ServerSocket
        ServerSocket serverSocket = new ServerSocket(6666);

        System.out.println("Server started....");

        while (true) {
            System.out.println("Thread's id = " + Thread.currentThread().getId() + " Thread's name = " + Thread.currentThread().getName());
            // listen, wait for client
            System.out.println("Wating for connect...");

            // connect
            final Socket socket = serverSocket.accept();
            System.out.println("Client connect...");

            cached.execute(new Runnable() {
                @Override
                public void run() {
                    // do something...
                    handle(socket);
                }
            });
        }

    }

    public static void handle(Socket socket) {
        try {
            System.out.println("Thread's id = " + Thread.currentThread().getId() + " Thread's name = " + Thread.currentThread().getName());
            byte[] bytes = new byte[1024];
            // get InputStream by socket
            InputStream inputStream = socket.getInputStream();

            while (true) {
                System.out.println("Thread's id = " + Thread.currentThread().getId() + " Thread's name = " + Thread.currentThread().getName());
                System.out.println("reading...");

                int read = inputStream.read(bytes);
                if (read != -1) {
                    System.out.println(new String(bytes, 0, read));
                } else {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
