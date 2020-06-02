package com.bruis.learnaop.testjdkproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MyInvocationHandler implements InvocationHandler {
    //代理目标对象
    private Object target;
    public MyInvocationHandler(Object target) {
        super();
        this.target = target;
    }
    /**
     * 执行目标对象的方法
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("---------------------------------方法purchase购买前，代理人吃回扣了----------------------------");
        //执行目标对象的方法
        Object result = method.invoke(target, args);
        System.out.println("----------------------------------方法purchase购买后，代理人跑了----------------------------------");
        return result;
    }
    /**
     * 获取目标对象的代理对象
     */
    public Object getProxy() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), target.getClass().getInterfaces(), this);
    }
}
