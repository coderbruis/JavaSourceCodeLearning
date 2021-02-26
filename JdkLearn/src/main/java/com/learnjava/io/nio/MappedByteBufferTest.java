package com.learnjava.io.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author LuoHaiYang
 */
public class MappedByteBufferTest {
    public static void main(String[] args) throws Exception {

        RandomAccessFile randomAccessFile = new RandomAccessFile("1.txt", "rw");

        // get channel
        FileChannel channel = randomAccessFile.getChannel();

        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 5);

        mappedByteBuffer.put(0, (byte) 'H');
        mappedByteBuffer.put(3, (byte) '9');

        // IndexOutOfBoundsException
        mappedByteBuffer.put(5, (byte) 'Y');

        randomAccessFile.close();
        System.out.println("change success");
    }

    /**
     *
     * @param from
     * @param to
     * @throws IOException
     */
    public static void mmap4zeroCopy(String from, String to) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new RandomAccessFile(from, "r").getChannel();
            destination = new RandomAccessFile(to, "rw").getChannel();
            MappedByteBuffer inMappedBuf =
                    source.map(FileChannel.MapMode.READ_ONLY, 0, source.size());
            destination.write(inMappedBuf);
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void sendfile4zeroCopy(String from, String to) throws IOException{
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(from).getChannel();
            destination = new FileOutputStream(to).getChannel();
            source.transferTo(0, source.size(), destination);
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}
