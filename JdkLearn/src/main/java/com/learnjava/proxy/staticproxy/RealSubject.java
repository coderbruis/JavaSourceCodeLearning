package com.learnjava.proxy.staticproxy;

public class RealSubject implements Subject {

    @Override
    public void operation() {
        System.out.println("这是真正的业务类");
    }

}
