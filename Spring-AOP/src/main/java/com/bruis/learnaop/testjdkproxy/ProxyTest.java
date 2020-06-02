package com.bruis.learnaop.testjdkproxy;

public class ProxyTest {
    /**
     * 这个是一个简单的AOP应用，就是在add()方法的执行前后加入了通知。
     * SpringAOP实现其实也就是用了Proxy和InvocationHandler这两个东西，总结一下JDK动态dialing的方式
     * 1. 构造函数，将dialing的对象传入
     * 2. 调用invoke方法，在此方法中实现AOP通知的逻辑
     * 3. 调用getProxy方法，此方法千篇一律必不可少
     */
    public static void main(String[] args) {
        //实例化目标对象
        UserService userService = new UserServiceImpl();
        //实例化InvocationHandler
        MyInvocationHandler invocationHandler = new MyInvocationHandler(userService);
        //根据目标对象生成代理对象
        UserService proxy = (UserService)invocationHandler.getProxy();
        //调用代理对象的方法
        proxy.purchase();
    }
}
