package com.learnjava.reference;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

public class PhantomRefTest {

    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue referenceQueue = new ReferenceQueue();

        // 10mb
        byte[] buffer = new byte[1024 * 1024 * 10];

        PhantomReference phantomReference = new PhantomReference(buffer, referenceQueue);

        // 字节数组对象，失去了强引用
        buffer = null;

        Reference ref0 = referenceQueue.poll();

        System.out.println("gc 执行之前， refQueue中是否有数据？" + (ref0 != null ? "有" : "没有"));
        System.out.println("gc 执行之前, ref引用的对象：" + phantomReference.get());

        System.gc();
        // 确保gc程序执行
        Thread.sleep(1000);

        System.out.println("gc 执行之后， ref引用的对象：" + phantomReference.get());

        Reference ref = referenceQueue.poll();
        System.out.println("gc 执行之后， refQueue中是否有数据？" + (ref != null ? "有" : "没有"));
        System.out.println("referenceQueue 中获取的 ref与 weakReference中的是否一致？" + (ref == phantomReference));
    }
}
