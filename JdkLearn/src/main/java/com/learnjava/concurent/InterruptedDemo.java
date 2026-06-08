package com.learnjava.concurent;

import java.util.concurrent.TimeUnit;

/**
 * @Author : haiyang.luo
 * @Date : 2026/6/8 00:56
 * @Description :
 */
public class InterruptedDemo {
    public static void main(String[] args) throws Exception {
        // SleepThread 不停地尝试睡眠
        Thread sleepThread = new Thread(new SleepRunner(), "SleepThread");
        sleepThread.setDaemon(true);
        // BusyThread 不停地运行
        Thread busyThread = new Thread(new BusyRunner(), "BusyThread");
        busyThread.setDaemon(true);
        sleepThread.start();
        busyThread.start();
        // 休眠 5s，让 SleepThread 和 BusyThread 充分运行
        TimeUnit.SECONDS.sleep(5);
        sleepThread.interrupt();
        busyThread.interrupt();
        // 休眠 1s，确保主线程能够看到结果
        TimeUnit.SECONDS.sleep(1);
        System.out.println("SleepThread interrupted is " + sleepThread.isInterrupted());
        System.out.println("BusyThread interrupted is " + busyThread.isInterrupted());
        // 防止 sleepThread 和 busyThread 立刻退出
        TimeUnit.SECONDS.sleep(2);
    }
    static class SleepRunner implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    // 捕获中断异常，需要重新标记中断
                    Thread.currentThread().interrupt();
//                    throw new RuntimeException(e);
                    while (true) {
                        // 让线程保持存活，方便 main 线程观察中断状态
                    }
                }

            }
        }
    }
    static class BusyRunner implements Runnable {
        @Override
        public void run() {
            while (true) {
            }
        }
    }
}
