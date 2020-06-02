package com.learnjava.io.nio;

import java.nio.ByteBuffer;

/**
 * @author LuoHaiYang
 */
public class ReadOnlyBuffer {
    public static void main(String[] args) {
        // create buffer
        ByteBuffer buffer = ByteBuffer.allocate(64);

        for (int i = 0; i < 64; i++) {
            buffer.put((byte)i);
        }

        // flip
        buffer.flip();

        // get readOnlyBuffer
        ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
        System.out.println(readOnlyBuffer.getClass());

        // read
        while (readOnlyBuffer.hasRemaining()) {
            System.out.println(readOnlyBuffer.get());
        }

        // throw ReadOnlyBufferException
        readOnlyBuffer.put((byte)100);

    }
}
