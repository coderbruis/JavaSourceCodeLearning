package com.learnjava.concurrent;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFutureDemo {

    /**
     * completedFuture()：
     *
     * 该方法会返回一个已经完成的带有返回值的CompletableFuture
     *
     * @throws Exception
     */
    @Test
    public void test_completed_future() throws Exception {
        String expectedValue = "the expected value";
        CompletableFuture<String> alreadyCompleted = CompletableFuture.completedFuture(expectedValue);
        System.out.println(alreadyCompleted.get());
    }

    /**
     * runAsync():
     * 异步运行
     *
     */
    @Test
    public void runAsyncExample() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletableFuture cf = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName());
        }, executorService);
        System.out.println(Thread.currentThread().getName());
        while (true) {
            if (cf.isDone()) {
                System.out.println("CompletedFuture...isDown");
                break;
            }
        }
    }


    /**
     * 1. supplyAsync()
     *   如果没有指定线程池，supplyAsync()方法会通过ForkJoinPool.commonPool()中的线程来
     *   异步完成任务，并通过给定Supplier获得值。
     * 2. thenAccept()
     *   接收CompletableFuture结果并进行处理。
     * 3. thenAcceptAsync()
     *   异步接收CompletableFuture结果
     * @throws Exception
     */
    @Test
    public void thenApply() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        /*Supplier<String> sp = () -> {
            return "";
        };
        Supplier<String> spp = String::new;*/

        CompletableFuture cf = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("supplyAsync " + Thread.currentThread().getName());
            return "hello ";
        },executorService).thenAccept(s -> {
            try {
                thenApply_test(s + "world");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println(Thread.currentThread().getName());
        while (true) {
            if (cf.isDone()) {
                System.out.println("CompletedFuture...isDown");
                break;
            }
        }

        /** 运行结果：
         main
         supplyAsync pool-1-thread-1
         thenApply_test hello world
         thenApply_test pool-1-thread-1
         */
    }

    @Test
    public void thenApplyAsync() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CompletableFuture cf = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("supplyAsync " + Thread.currentThread().getName());
            return "hello ";
        },executorService)/*.thenAcceptAsync(s -> {
            try {
                thenApply_test(s + "world");
            } catch (Exception e) {
                e.printStackTrace();
            }
        },executorService)*/;

        System.out.println(Thread.currentThread().getName());
        while (true) {
            if (cf.isDone()) {
                System.out.println("CompletedFuture...isDown result = " + cf.get());
                break;
            }
        }

        /** 运行结果：
         main
         supplyAsync pool-1-thread-1
         thenApply_test hello world
         thenApply_test pool-1-thread-2
         */
    }

    private void thenApply_test(String msg) {
        System.out.println("thenApply_test " + msg);
        System.out.println("thenApply_test " + Thread.currentThread().getName());
    }

    @Test
    public void thenAcceptBoth() {
        CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "hello";
        }).thenAcceptBoth(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "world";
        }), (s1, s2) -> {
            System.out.println("thenAcceptBoth " + Thread.currentThread().getName());
            System.out.println(s1 + " " + s2);
        }); // hello world
        System.out.println(Thread.currentThread().getName());
        while (true){}
    }

    @Test
    public void exceptionally() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletableFuture cf = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (1 == 1) {
                throw new RuntimeException("测试exceptionally...");
            }
            return "s1";
        }, executorService).exceptionally(e -> {
            System.out.println(e.getMessage());
            return "helloworld " + e.getMessage();
        });
        cf.thenAcceptAsync(s -> {
            System.out.println("thenAcceptAsync: " + s);
        });
        System.out.println("main: " + Thread.currentThread().getName());
        while (true) {
            if (cf.isDone()) {
                System.out.println("CompletableFuture is Down..." + cf.get());
                break;
            }
        }
    }

    /**
     * join()方法会阻塞等待CompletableFuture获取完结果
     *
     * 可以看到supplyAsync使用的是线程池中的线程，而main线程会阻塞等待completableFuture任务完成。
     *
     */
    @Test
    public void exceptionallyByJoin() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        String result = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName());
            if (1 == 1) {
                throw new RuntimeException("测试exceptionally...");
            }
            return "s1";
        }, executorService).exceptionally(e -> {
            System.out.println(e.getMessage());
            return "helloworld " + e.getMessage();
        }).join();
        System.out.println(result + "," + Thread.currentThread().getName());
    }
}
