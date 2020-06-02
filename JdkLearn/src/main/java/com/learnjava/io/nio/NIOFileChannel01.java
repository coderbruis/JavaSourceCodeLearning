package com.learnjava.io.nio;

import java.awt.print.Pageable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author LuoHaiYang
 */
public class NIOFileChannel01 {

    public static void main(String[] args) throws Exception {
        // write to disk
        // write("hello nio");
        //read("/Users/tianbao/file01.log");
        //readAndWrite("/Users/tianbao/file01.log", "/Users/tianbao/file02.log");
        useTransferFrom("/Users/tianbao/file03.log", "/Users/tianbao/file02.log");
    }

    public static void useTransferFrom(String toPath, String fromPath) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(fromPath);
        FileOutputStream fileOutputStream = new FileOutputStream(toPath);

        FileChannel sourceCh = fileInputStream.getChannel();
        FileChannel destCh = fileOutputStream.getChannel();

        // use transferFrom to copy
        destCh.transferFrom(sourceCh, 0, sourceCh.size());
        // clouse
        sourceCh.close();
        destCh.close();

        fileInputStream.close();
        fileOutputStream.close();
    }

    // read and write in common buffer
    public static void readAndWrite(String fromPath, String toPath) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(fromPath);
        FileChannel fileChannel01 = fileInputStream.getChannel();

        FileOutputStream fileOutputStream = new FileOutputStream(toPath);
        FileChannel fileChannel02 = fileOutputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.allocate(512);

        while (true) {
            byteBuffer.clear();
            // disk -> channel -> bytebuffer
            int read = fileChannel01.read(byteBuffer);
            System.out.println("read = " + read);
            if (read == -1) {
                break;
            }
            // change index
            byteBuffer.flip();
            // bytebuffer -> channel -> disk
            fileChannel02.write(byteBuffer);
        }

        fileInputStream.close();
        fileOutputStream.close();
    }

    public static void read(String path) throws Exception {
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);

        FileChannel fileChannel = fileInputStream.getChannel();
        // create buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)file.length());

        // disk -> channel -> buffer
        fileChannel.read(byteBuffer);

        // buffer -> jvm
        System.out.println(new String(byteBuffer.array()));

        // disk -> channel -> buffer -> jvm
        fileChannel.close();
        fileInputStream.close();
    }

    public static void write(String str) throws Exception {
        File file;
        // jvm -> output -> disk
        FileOutputStream fileOutputStream = new FileOutputStream("/Users/tianbao/file01.log");

        FileChannel fileChannel = fileOutputStream.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        // set str into bytebuffer
        byteBuffer.put(str.getBytes());
        byteBuffer.flip();

        // write to channel
        fileChannel.write(byteBuffer);
        fileOutputStream.close();

        // jvm -> buffer -> channel -> disk
    }

}
