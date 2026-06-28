package com.learnjava.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;;

/**
 * @Author : haiyang.luo
 * @Date : 2026/6/28 21:44
 * @Description :
 */
@Timeout(30)
public class CompletableFuturePrincipleTest {

    @Test
    public void testCompleteTriggersDependentStageRegisteredBeforeCompletion() {
        CompletableFuture<Integer> source = new CompletableFuture<>();
        CompletableFuture<Integer> dependent = source.thenApply(value -> value + 1);

        assertFalse(dependent.isDone());

        assertTrue(source.complete(10));

        assertEquals(11, dependent.join());
        assertTrue(dependent.isDone());
    }

    @Test
    public void testDependentStageRegisteredAfterCompletionRunsOnCallerThread() {
        CompletableFuture<Integer> source = CompletableFuture.completedFuture(10);
        String callerThreadName = Thread.currentThread().getName();
        AtomicReference<String> callbackThreadName = new AtomicReference<>();

        CompletableFuture<Integer> dependent = source.thenApply(value -> {
            callbackThreadName.set(Thread.currentThread().getName());
            return value + 1;
        });

        assertEquals(11, dependent.join());
        assertEquals(callerThreadName, callbackThreadName.get());
    }

    @Test
    public void testAsyncDependentStageRunsOnExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("completable-future-principle-test");
            return thread;
        });
        try {
            CompletableFuture<Integer> source = new CompletableFuture<>();
            AtomicReference<String> callbackThreadName = new AtomicReference<>();

            CompletableFuture<Integer> dependent = source.thenApplyAsync(value -> {
                callbackThreadName.set(Thread.currentThread().getName());
                return value + 1;
            }, executor);

            assertTrue(source.complete(10));

            assertEquals(11, dependent.join());
            assertEquals("completable-future-principle-test", callbackThreadName.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testThenComposeCompletesAfterReturnedFutureCompletes() {
        CompletableFuture<Integer> source = new CompletableFuture<>();
        CompletableFuture<Integer> inner = new CompletableFuture<>();

        CompletableFuture<Integer> composed = source.thenCompose(value -> inner.thenApply(innerValue -> value + innerValue));

        assertTrue(source.complete(10));
        assertFalse(composed.isDone());

        assertTrue(inner.complete(5));

        assertEquals(15, composed.join());
    }

    @Test
    public void testExceptionalCompletionSkipsThenApplyAndCanRecover() {
        CompletableFuture<Integer> source = new CompletableFuture<>();
        AtomicBoolean thenApplyCalled = new AtomicBoolean(false);

        CompletableFuture<Integer> recovered = source
                .thenApply(value -> {
                    thenApplyCalled.set(true);
                    return value + 1;
                })
                .exceptionally(exception -> 0);

        RuntimeException failure = new RuntimeException("boom");
        assertTrue(source.completeExceptionally(failure));

        assertEquals(0, recovered.join());
        assertFalse(thenApplyCalled.get());
    }

    @Test
    public void testOnlyFirstCompletionWins() {
        CompletableFuture<String> future = new CompletableFuture<>();

        assertTrue(future.complete("first"));
        assertFalse(future.complete("second"));
        assertFalse(future.completeExceptionally(new RuntimeException("boom")));

        assertEquals("first", future.join());
    }

    @Test
    public void testJoinWaitsForCompletion() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        CountDownLatch joinStarted = new CountDownLatch(1);
        AtomicReference<String> joinedValue = new AtomicReference<>();

        Thread waitingThread = new Thread(() -> {
            joinStarted.countDown();
            joinedValue.set(future.join());
        });
        waitingThread.start();

        assertTrue(joinStarted.await(5, TimeUnit.SECONDS));
        assertFalse(future.isDone());

        assertTrue(future.complete("done"));

        waitingThread.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse(waitingThread.isAlive());
        assertEquals("done", joinedValue.get());
    }

    @Test
    public void testJoinWrapsFailureInCompletionException() {
        CompletableFuture<String> future = new CompletableFuture<>();
        RuntimeException failure = new RuntimeException("boom");

        assertTrue(future.completeExceptionally(failure));

        CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertSame(failure, exception.getCause());
    }
}
