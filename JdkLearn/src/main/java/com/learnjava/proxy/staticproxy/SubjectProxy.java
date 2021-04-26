package com.learnjava.proxy.staticproxy;

public class SubjectProxy implements Subject {

    private RealSubject realSubject;

    public SubjectProxy() {
        realSubject = new RealSubject();
    }

    @Override
    public void operation() {
        System.out.println("代理预处理逻辑");
        realSubject.operation();
        System.out.println("代理后处理逻辑");
    }

    public static void main(String[] args) {
        SubjectProxy proxy = new SubjectProxy();
        proxy.operation();
    }
}
