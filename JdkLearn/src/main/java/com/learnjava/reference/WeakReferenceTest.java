package com.learnjava.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * soft软引用 gc堆内存不够时，可能会回收
 * weak弱引用 gc时，内存一定会回收不可用对象
 * phantom虚引用
 */
public class WeakReferenceTest {
    public static void main(String[] args) throws Exception {
//        test01();
        test02();
    }

    private static void test01() throws InterruptedException {
        List<Reference> refList = new ArrayList<Reference>();

        for (int i = 0; i < 1000; i++) {
            // 10mb
            byte[] buffer = new byte[1024 * 1024 * 10];

            WeakReference weakReference = new WeakReference(buffer);

            refList.add(weakReference);
        }

        // 将buffer直接全部回收了
        System.gc();
        Thread.sleep(1000);
        Iterator<Reference> iterator = refList.iterator();
        while (iterator.hasNext()) {
            Reference ref = iterator.next();
            System.out.println("当前ref引用的对象：" + ref.get());
        }
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

        System.out.println("gc 执行之后， ref引用的对象：" + weakReference.get());

        Reference ref = refQueue.poll();
        System.out.println("gc 执行之后， refQueue中是否有数据？" + (ref != null ? "有" : "没有"));
        System.out.println("referenceQueue 中获取的 ref与 weakReference中的是否一致？" + (ref == weakReference));
    }
}
