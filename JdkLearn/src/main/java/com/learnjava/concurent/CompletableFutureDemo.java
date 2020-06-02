package com.learnjava.concurent;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureDemo {

    public static void main(String[] args) throws Exception {
        CompletableFutureDemo completableFutureDemo = new CompletableFutureDemo();
        completableFutureDemo.test_completed_future();
    }

    public void test_completed_future() throws Exception {
        String expectedValue = "the expected value";
        CompletableFuture<String> alreadyCompleted = CompletableFuture.completedFuture(expectedValue);
        System.out.println(alreadyCompleted.get());
    }

}
