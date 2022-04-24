package com.learnjava.concurent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/4/24
 */
public class ThreadPoolExecutorDemo {

    public static void main(String[] args) {
        testThreadPoolExecutorBinaryCalc();
    }


    /**
     * 验证ThreadPoolExecutor中的二进制位运算操作
     */
    private static void testThreadPoolExecutorBinaryCalc() {
//        System.out.println(ctl.get());
//        System.out.println(Integer.toBinaryString(ctlOf(RUNNING, 0)));
//        System.out.println(Integer.toBinaryString(RUNNING));
        // 修改线程状态-STOP
        System.out.println(Integer.toBinaryString(~runStateOf(ctlOf(STOP, 10))));
        // 修改线程状态-TERMINATED
//        System.out.println(runStateOf(3));
//        System.out.println(Integer.toBinaryString(~CAPACITY));
    }

    private static final int COUNT_BITS = Integer.SIZE - 3;

    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    private static final int RUNNING = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;

    private static AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    private static int runStateOf(int c) { return c & ~CAPACITY; }

    private static int workerCountOf(int c) { return c & CAPACITY; }

    private static int ctlOf(int rs, int wc) { return rs | wc; }
}
