package com.learnjava.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakReferenceTest {
    public static void main(String[] args) throws Exception {
        test02();
    }

    private static void test02() throws InterruptedException {
        ReferenceQueue refQueue = new ReferenceQueue();

        // 10 mb
        byte[] buffer = new byte[1024 * 1024 * 10];
        WeakReference weakReference = new WeakReference(buffer, refQueue);
        // 失去强引用关联
        buffer = null;

        Reference ref0 = refQueue.poll();
        System.out.println("gc 执行之前， refQueue中是否有数据？" + (ref0 != null ? "有" : "没有"));
        System.out.println("gc 执行之前, ref引用的对象：" + weakReference.get());

        System.gc();
        // 确保gc程序执行
        Thread.sleep(1000);

        System.out.println("gc 执行之前， ref引用的对象：" + weakReference.get());

        Reference ref = refQueue.poll();
        System.out.println("referenceQueue 中获取的 ref与 weakReference中的是否一致？" + (ref == weakReference));
    }
}
