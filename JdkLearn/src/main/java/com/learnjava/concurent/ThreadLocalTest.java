package com.learnjava.concurent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadLocalTest {
    public static void main(String[] args) throws InterruptedException {
//        testThreadIsolation();
//        testInitialValue();
//        testSetGetRemove();
//        testThreadLocalAsMapKey();
//        testThreadPoolShouldRemove();
        testHashIncrementDistribution();
    }

    /**
     * 核心结果：同一个ThreadLocal对象，在不同线程中保存的是不同副本。
     */
    public static void testThreadIsolation() throws InterruptedException {
        System.out.println("\n==== 1. thread isolation ====");
        int threads = 3;
        CountDownLatch countDownLatch = new CountDownLatch(threads);

        InnerClass innerClass = new InnerClass();
        for (int i = 1; i <= threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < 4; j++) {
                    innerClass.add(String.valueOf(j));
                    innerClass.print();
                }

                innerClass.set("hello world");
                countDownLatch.countDown();
            }, "thread - " + i).start();
        }
        countDownLatch.await();
    }

    /**
     * initialValue只会在当前线程第一次get且没有值时触发。
     */
    public static void testInitialValue() throws InterruptedException {
        System.out.println("\n==== 2. initialValue per thread ====");
        ThreadLocal<StringBuilder> local = new ThreadLocal<StringBuilder>() {
            @Override
            protected StringBuilder initialValue() {
                System.out.printf("%s init value\n", Thread.currentThread().getName());
                return new StringBuilder(Thread.currentThread().getName());
            }
        };

        Runnable task = () -> {
            System.out.printf("%s first get: %s\n", Thread.currentThread().getName(), local.get());
            System.out.printf("%s second get: %s\n", Thread.currentThread().getName(), local.get());
        };

        Thread threadA = new Thread(task, "thread-A");
        Thread threadB = new Thread(task, "thread-B");
        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();
    }

    /**
     * set/get/remove是ThreadLocal最常用的生命周期。
     */
    public static void testSetGetRemove() {
        System.out.println("\n==== 3. set get remove ====");
        ThreadLocal<String> local = new ThreadLocal<String>() {
            @Override
            protected String initialValue() {
                return "init";
            }
        };

        System.out.println("first get: " + local.get());
        local.set("changed");
        System.out.println("after set: " + local.get());
        local.remove();
        System.out.println("after remove, get again: " + local.get());
    }

    /**
     * 原理要点：值不是存在ThreadLocal对象里，而是存在当前线程的ThreadLocalMap里。
     * ThreadLocal实例本身作为key，所以同一个线程可以给不同ThreadLocal保存不同值。
     */
    public static void testThreadLocalAsMapKey() {
        System.out.println("\n==== 4. ThreadLocal instance as key ====");
        ThreadLocal<String> userLocal = new ThreadLocal<String>();
        ThreadLocal<String> traceLocal = new ThreadLocal<String>();

        userLocal.set("user-1001");
        traceLocal.set("trace-abc");

        System.out.printf("same thread:%s, userLocal:%s, traceLocal:%s\n",
                Thread.currentThread().getName(),
                userLocal.get(),
                traceLocal.get());

        userLocal.remove();
        traceLocal.remove();
    }

    /**
     * 线程池会复用线程；如果任务结束后不remove，后续任务可能读到上一个任务遗留的值。
     */
    public static void testThreadPoolShouldRemove() throws InterruptedException {
        System.out.println("\n==== 5. thread pool should remove ====");
        ThreadLocal<String> requestIdLocal = new ThreadLocal<String>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {
            requestIdLocal.set("request-1");
            System.out.printf("%s set value: %s\n",
                    Thread.currentThread().getName(),
                    requestIdLocal.get());
        });

        executorService.execute(() -> {
            System.out.printf("%s read old value: %s\n",
                    Thread.currentThread().getName(),
                    requestIdLocal.get());
            requestIdLocal.remove();
        });

        executorService.execute(() -> System.out.printf("%s after remove: %s\n",
                Thread.currentThread().getName(),
                requestIdLocal.get()));

        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.SECONDS);

    }

    /**
     * 验证ThreadLocal中0x61c88647这个hash增量的分布效果。
     * ThreadLocalMap长度是2的幂，定位下标时使用 hash & (len - 1)。
     */
    public static void testHashIncrementDistribution() {
        System.out.println("\n==== 6. hash increment distribution ====");
        int hashIncrement = 0x61c88647;

        printHashIndexSequence(hashIncrement, 16);
        printHashIndexSequence(hashIncrement, 32);
    }

    private static void printHashIndexSequence(int hashIncrement, int len) {
        int hash = 0;

        System.out.printf("len = %d, HASH_INCREMENT & (len - 1) = %d\n",
                len,
                hashIncrement & (len - 1));

        for (int i = 0; i < len; i++) {
            int index = hash & (len - 1);
            System.out.printf("%2d -> hash: 0x%08x, index: %2d\n", i, hash, index);
            hash += hashIncrement;
        }
    }

    private static class InnerClass {
        public void add(String newStr) {
            //利用Counter类来实例化StringBuilder
            StringBuilder stringBuilder = Counter.counter.get();

            //将newStr存进ThreadLocal里
            Counter.counter.set(stringBuilder.append(newStr));
        }

        /**
         * 打印ThreadLocal的信息
         */
        public void print() {
            System.out.printf("Thread name:%s , ThreadLocal hashcode:%s, Instance hashcode:%s, Value:%s\n",
                    Thread.currentThread().getName(),
                    Counter.counter.hashCode(),
                    Counter.counter.get().hashCode(),
                    Counter.counter.get().toString());
        }

        public void set(String word) {
            Counter.counter.set(new StringBuilder(word));
            System.out.printf("Set, Thread name:%s , ThreadLocal hashcode:%s,  Instance hashcode:%s, Value:%s\n",
                    Thread.currentThread().getName(),
                    Counter.counter.hashCode(),
                    Counter.counter.get().hashCode(),
                    Counter.counter.get().toString());
        }
    }

    private static class Counter {
        private static ThreadLocal<StringBuilder> counter = ThreadLocal.withInitial(StringBuilder::new);
    }
}
