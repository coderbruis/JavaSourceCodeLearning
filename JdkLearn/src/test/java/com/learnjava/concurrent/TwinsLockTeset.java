package com.learnjava.concurrent;

import com.learnjava.concurent.TwinsLock;
import org.junit.Test;

import java.util.concurrent.locks.Lock;

/**
 * @author LuoHaiYang
 */
public class TwinsLockTeset {

    @Test
    public void test() {
        final Lock lock = new TwinsLock();
        class Worker extends Thread {
            public void run() {
                while (true) {
                    lock.lock();
                    try {
                        Thread.sleep(1000);
                        System.out.println(Thread.currentThread().getName());
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }

        // 启动10个线程
        for (int i = 0; i < 10; i++) {
            Worker worker = new Worker();
            worker.setName("Thread-" + i);
            //worker.setDaemon(true);
            worker.start();
        }

        // 每隔10秒换行
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(1000);
                System.out.println();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
