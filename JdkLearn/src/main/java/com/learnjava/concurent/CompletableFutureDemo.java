package com.learnjava.concurent;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureDemo {

    public static void main(String[] args) throws Exception {
        CompletableFutureDemo completableFutureDemo = new CompletableFutureDemo();
//        completableFutureDemo.test_completed_future();
        completableFutureDemo.test_completed_thenApply();
    }

    public void test_completed_future() throws Exception {
        String expectedValue = "the expected value";
        CompletableFuture<String> alreadyCompleted = CompletableFuture.completedFuture(expectedValue);
        System.out.println(alreadyCompleted.get());
    }

    public void test_completed_thenApply() {
        CompletableFuture<Integer> source = new CompletableFuture<>();
        CompletableFuture<Integer> next =
                source.thenApply(x -> {
                    System.out.println(Thread.currentThread().getName());
                    return x + 1;
                });
        source.complete(10);
        System.out.println(next.join()); // 11
    }
}
