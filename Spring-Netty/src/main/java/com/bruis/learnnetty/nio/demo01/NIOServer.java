package com.bruis.learnnetty.nio.demo01;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author LuoHaiYang
 */
public class NIOServer {

    public static void main(String[] args) throws Exception {

        // 创建ServerSocketChannel -> ServerSocket
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        // 得到一个Selector对象
        Selector selector = Selector.open();

        // 绑定一个端口：6666，在服务端监听
        serverSocketChannel.socket().bind(new InetSocketAddress(6666));

        // 设置为非阻塞
        serverSocketChannel.configureBlocking(false);

        // 把ServerSocketChannel注册到selector， 事件为：OP_ACCEPT
        SelectionKey registerSelectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Register to selector, selectionKey is OP_ACCEPT, return selectionKey = " + registerSelectionKey + ", 注册后selectionKey 数量 = " + selector.keys().size());

        // 循环等待客户端连接
        while (true) {
            // 这里我们等待1秒， 如果没有事件发生，返回
            if (selector.select(1000) == 0) {
                // 没有事件发生
                System.out.println("服务器等待了1秒，无连接");
                continue;
            }

            // 如果返回结果 > 0, 就获取到相关的selectionKey集合
            // 1. 如果返回 > 0，表示已经获取到关注的事件
            // 2. selector.selectedKeys() 返回关注事件的集合
            // 通过selectionKeys反向获取通道
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            // 通过迭代器遍历Set<SelectionKey>
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                // 获取Selectionkey
                SelectionKey key = keyIterator.next();
                // 根据key对应的通道发生的事件做响应处理
                if (key.isAcceptable()) {
                    // 如果key是OP_ACCEPT，有新的客户端连接
                    // 通过ServerSocketChannel生成一个socketChannel
                    SocketChannel socketChannel = serverSocketChannel.accept();

                    System.out.println("Register to selector, selectionKey is OP_ACCEPT, return selectionKey = " + registerSelectionKey + ", 注册后selectionKey 数量 = " + selector.keys().size());

                    // 将SocketChannel设置为非阻塞
                    socketChannel.configureBlocking(false);

                    // 将socketChannel注册到selector，关注事件为OP_READ，同时给socketChannel关联一个buffer
                    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

                }

                System.out.println("isReadable = " + key.isReadable() + ", isWritable = " + key.isWritable() + ", isValid = " + key.isValid());

                if (key.isReadable()) {
                    // 如果key是OP_READ
                    // 通过key反向获取到对应channel
                    SocketChannel channel = (SocketChannel) key.channel();

                    // 获取该channel关联的buffer
                    ByteBuffer buffer = (ByteBuffer)key.attachment();

                    // 从buffer中读取数据到channel中
                    channel.read(buffer);

                    System.out.println("from 客户端 " + new String(buffer.array()));
                }

                // 手动从集合中移动当前的selectionKey，防止重复操作
                keyIterator.remove();
            }

        }
    }
}
