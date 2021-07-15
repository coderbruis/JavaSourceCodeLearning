package com.bruis.learnnetty.netty.fastthreadlocal;

/**
 * @author lhy
 * @date 2021/7/14
 */
public class ThreadLocalTest {

    private static ThreadLocal<Object> threadLocal0 = new ThreadLocal<>();

    private static ThreadLocal<Object> threadLocal1 = new ThreadLocal<>();

    public static void main(String[] args) {
        // 线程外
        System.out.println("main线程1: " + threadLocal0.get());
        Object o = new Object();
        threadLocal0.set(o);

        new Thread(() -> {
            Object threadObject = threadLocal0.get();
            System.out.println("线程内: " + threadObject);
            if (threadObject == null) {
                Object newObject = new Object();
                System.out.println("新new一个对象：" + newObject);
                threadLocal0.set(newObject);
            }
            try {
                Thread.sleep(1000);
                System.out.println("休眠了一秒");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("线程内，从ThreadLocal获取：" + threadLocal0.get());
        }).start();

        System.out.println("main线程2: " + threadLocal0.get());
    }
}
