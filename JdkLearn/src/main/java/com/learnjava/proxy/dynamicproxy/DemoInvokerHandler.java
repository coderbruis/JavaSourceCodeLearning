package com.learnjava.proxy.dynamicproxy;

import com.learnjava.proxy.staticproxy.RealSubject;
import com.learnjava.proxy.staticproxy.Subject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author bruis
 *
 * jdk动态代理
 *
 */
public class DemoInvokerHandler implements InvocationHandler {

    // 真正的业务对象
    private Object realSubject;

    public DemoInvokerHandler(Object realSubject) {
        this.realSubject = realSubject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("代理预处理操作");

        // 调用真正的代理对象逻辑
        Object result = method.invoke(realSubject, args);

        System.out.println("代理后处理操作");
        return result;
    }

    /**
     * 创建代理对象并返回
     * @return
     */
    public Object getProxy() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                realSubject.getClass().getInterfaces(), this);
    }

    public static void main(String[] args) {
        RealSubject realSubject = new RealSubject();
        DemoInvokerHandler invokerHandler = new DemoInvokerHandler(realSubject);
        Subject proxy = (Subject) invokerHandler.getProxy();

        // 拿到业务对象，执行业务逻辑，此时业务逻辑已经被代理对象代理了
        proxy.operation();
    }
}
