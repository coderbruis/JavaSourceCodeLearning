package com.learnjava.concurent;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 演示 TransmittableThreadLocal 在线程池中的上下文传递。
 */
public class TransmittableThreadLocalDemo {

    private static final ThreadLocal<String> NORMAL_THREAD_LOCAL = new ThreadLocal<>();
    private static final InheritableThreadLocal<String> INHERITABLE_THREAD_LOCAL = new InheritableThreadLocal<>();
    private static final TransmittableThreadLocal<String> TTL = new TransmittableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        testThreadLocalInThreadPool();
        testInheritableThreadLocalInThreadPool();
        testTransmittableThreadLocalInThreadPool();
    }

    /**
     * 普通 ThreadLocal 不能跨线程传递值。
     */
    private static void testThreadLocalInThreadPool() throws InterruptedException {
        System.out.println("\n==== 1. normal ThreadLocal ====");
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        NORMAL_THREAD_LOCAL.set("normal-context");
        executorService.execute(() -> printValue("normal", NORMAL_THREAD_LOCAL.get()));

        shutdown(executorService);
        NORMAL_THREAD_LOCAL.remove();
    }

    /**
     * InheritableThreadLocal 只在线程创建时传递，线程池复用线程时容易读到旧值。
     */
    private static void testInheritableThreadLocalInThreadPool() throws InterruptedException {
        System.out.println("\n==== 2. InheritableThreadLocal ====");
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        INHERITABLE_THREAD_LOCAL.set("parent-context-1");
        executorService.execute(() -> printValue("first task", INHERITABLE_THREAD_LOCAL.get()));
        sleepQuietly(200);

        INHERITABLE_THREAD_LOCAL.set("parent-context-2");
        executorService.execute(() -> printValue("second task", INHERITABLE_THREAD_LOCAL.get()));

        shutdown(executorService);
        INHERITABLE_THREAD_LOCAL.remove();
    }

    /**
     * TransmittableThreadLocal 通过包装线程池，在任务提交时捕获上下文，在执行时恢复上下文。
     */
    private static void testTransmittableThreadLocalInThreadPool() throws InterruptedException {
        System.out.println("\n==== 3. TransmittableThreadLocal ====");
        ExecutorService rawExecutorService = Executors.newFixedThreadPool(1);
        ExecutorService ttlExecutorService = TtlExecutors.getTtlExecutorService(rawExecutorService);

        TTL.set("ttl-context-1");
        ttlExecutorService.execute(() -> printValue("first ttl task", TTL.get()));

        TTL.set("ttl-context-2");
        ttlExecutorService.execute(() -> printValue("second ttl task", TTL.get()));

        shutdown(ttlExecutorService);
        TTL.remove();
    }

    private static void printValue(String scene, String value) {
        System.out.printf("%s, thread=%s, value=%s%n", scene, Thread.currentThread().getName(), value);
    }

    private static void shutdown(ExecutorService executorService) throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.SECONDS);
    }

    private static void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
