package com.learnjava.concurent;

/**
 * @author lhy
 * @date 2021/7/27
 */
public class SnowIdDemo {
    public static void main(String[] args) {
        // 通过雪花秀算法获取分布式id
        System.out.println(SnowIdUtils.uniqueLongHex());
    }
}
