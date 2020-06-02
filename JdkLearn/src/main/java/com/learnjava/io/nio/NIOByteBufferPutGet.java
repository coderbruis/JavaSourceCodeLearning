package com.learnjava.io.nio;

import java.nio.ByteBuffer;

/**
 * @author LuoHaiYang
 */
public class NIOByteBufferPutGet {
    public static void main(String[] args) {
        // create  Buffer
        ByteBuffer buffer = ByteBuffer.allocate(64);

        // put data into buffer
        buffer.putInt(100);
        buffer.putLong(9);
        buffer.putChar('b');
        buffer.putShort((short)4);

        // flip
        buffer.flip();
        System.out.println();

        System.out.println(buffer.getInt());
        System.out.println(buffer.getLong());
        System.out.println(buffer.getChar());
        System.out.println(buffer.getShort());
    }
}
