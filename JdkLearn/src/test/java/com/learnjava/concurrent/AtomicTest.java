package com.learnjava.concurrent;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author LuoHaiYang
 */
public class AtomicTest {

    @Test
    public void testAtomicInteger() {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        System.out.println(atomicInteger.get());

        boolean compareAndSet = atomicInteger.compareAndSet(1, 2);
        System.out.println(compareAndSet);

        System.out.println(atomicInteger.incrementAndGet());

        System.out.println(atomicInteger.get());

    }
}
