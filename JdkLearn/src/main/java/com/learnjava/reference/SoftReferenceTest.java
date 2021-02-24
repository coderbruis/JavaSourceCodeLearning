package com.learnjava.reference;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 软引用
 */
public class SoftReferenceTest {
    public static void main(String[] args) throws InterruptedException {
        List<Reference> refList = new ArrayList<Reference>();

        for (int i = 0; i < 1000; i++) {
            // 10mb
            byte[] buffer = new byte[1024 * 1024 * 10];

            SoftReference softReference = new SoftReference(buffer);

            refList.add(softReference);
        }

        System.gc();
        Thread.sleep(1000);

        Iterator<Reference> it = refList.iterator();

        while (it.hasNext()) {
            Reference ref = it.next();
            System.out.println("当前ref引用的对象：" + ref.get());
        }
    }
}
