package com.learnjava.concurent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 *
 * 同时有两个线程获取锁
 *
 * @author LuoHaiYang
 */
public class TwinsLock implements Lock {

    private final Sync sync = new Sync(2);

    private static final class Sync extends AbstractQueuedSynchronizer {
        Sync(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("count must larger than zero.");
            }
            setState(count);
        }

        /**
         * 尝试共享式获取同步状态
         * @param reduceCount
         * @return
         */
        @Override
        public int tryAcquireShared(int reduceCount) {
            for (;;) {
                // 获取同步状态
                int current = getState();
                int newCount = current - reduceCount;

                if (newCount < 0 || compareAndSetState(current, newCount)) {
                    return newCount;
                }
            }
        }

        /**
         * 共享式释放同步状态
         * @param returnCount
         * @return
         */
        @Override
        public boolean tryReleaseShared(int returnCount) {
            for (;;) {
                int current = getState();
                int newCount = current + returnCount;
                if (compareAndSetState(current, newCount)) {
                    return true;
                }
            }
        }
    }

    /**
     * 加锁
     */
    @Override
    public void lock() {
        sync.acquireShared(1);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        sync.releaseShared(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
