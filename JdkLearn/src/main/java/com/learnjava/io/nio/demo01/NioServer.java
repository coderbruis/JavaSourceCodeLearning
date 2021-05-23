package com.learnjava.io.nio.demo01;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author lhy
 * @date 2021/5/22
 */
public class NioServer {
    public static void main(String[] args) throws Exception {
        // 获取服务端通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 切换为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        // 绑定链接
        Selector selector = Selector.open();
        // 将通道注册在selector上，并绑定为读事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 选择器轮训，阻塞
        while (selector.select() > 0) {
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            // 判断是否有事件进来
            while (it.hasNext()) {
                // 获取就绪的事件
                SelectionKey selectionKey = it.next();

                // 读事件
                if (selectionKey.isAcceptable()) {
                    // 就绪的客户端连接事件
                    SocketChannel acceptChannel = serverSocketChannel.accept();
                    acceptChannel.configureBlocking(false);
                    acceptChannel.register(selector, SelectionKey.OP_READ);
                } else if (selectionKey.isReadable()) {
                    // 读就绪事件
                    SocketChannel readAcceptChannel = serverSocketChannel.accept();
                    ByteBuffer allocateBuffer = ByteBuffer.allocate(1024);

                    int len = 0;
                    while ((len = readAcceptChannel.read(allocateBuffer)) > 0) {
                        allocateBuffer.flip();
                        System.out.println(new String(allocateBuffer.array(), 0, len));
                        allocateBuffer.clear();
                    }
                }
            }

            // 取消选择键selectionKey
            it.remove();
        }
    }
}
