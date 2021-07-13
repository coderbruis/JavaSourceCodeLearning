package com.bruis.learnnetty.netty.fastthreadlocal;

import io.netty.util.concurrent.FastThreadLocal;

/**
 * FastThreadLocal测试类
 *
 * @author lhy
 * @date 2021/7/13
 */
public class FastThreadLocalTest {

    /**
     * FastThreadLocal对象1
     */
    private static FastThreadLocal<Object> threadLocal0 = new FastThreadLocal<Object>() {
        @Override
        protected Object initialValue() throws Exception {
            Object o = new Object();
            System.out.println("threadLocal0 initialValue: " + o);
            return o;
        }

        @Override
        protected void onRemoval(Object value) throws Exception {
            System.out.println("onRemoval");
        }
    };

    private static FastThreadLocal<Object> threadLocal1 = new FastThreadLocal<Object>() {
        @Override
        protected Object initialValue() throws Exception {
            Object o = new Object();
            System.out.println("threadLocal1 initialValue: " + o);
            return o;
        }
    };

    public static void main(String[] args) {
        new Thread(() -> {
            Object object0 = threadLocal0.get();
            System.out.println(Thread.currentThread().getName() + "---> " + object0);

            threadLocal0.set(new Object());
        }) .start();

        new Thread(() -> {
            Object object0 = threadLocal0.get();
            System.out.println(Thread.currentThread().getName() + "---> " + object0);

            while (true) {
                System.out.println(Thread.currentThread().getName() + "---> " + (threadLocal0.get() == object0));
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
