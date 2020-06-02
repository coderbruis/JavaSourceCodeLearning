package com.bruis.learnaop.testaspectJ;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class AspectJTest {

    /**
     * 环绕通知中，在JoinPoint.process()方法前调用的方法，会在@Before调用之前调用；
     * 在JoinPoint.process()方法后调用的方法，会在@After调用之前调用；
     * 如下：
     * @AroundBefore the testaspectJ()...
     * Before the testaspectJ()...
     * testaspectJ()...
     * @AroundAfter the testaspectJ()...
     * After the testaspectJ()...
     *
     * 为啥书上的版本是：
     * Before the testaspectJ()...
     * @AroundBefore the testaspectJ()...
     * testaspectJ()...
     * After the testaspectJ()...
     * @AroundAfter the testaspectJ()...
     */

    /**
     * 定义的切点
     */
    @Pointcut("execution(* *.test(..))")
    public void test(){}

    /**
     * 前置通知
     */
    @Before("test()")
    public void beforeTest() {
        System.out.println("beforeTest()...");
    }

    /**
     * 后置通知
     */
    @After("test()")
    public void after() {
        System.out.println("afterTest()...");
    }

    /**
     * 环绕通知
     * @param p
     * @return
     */
    @Around("test()")
    public Object aroundTest(ProceedingJoinPoint p) {
        System.err.println("beforeTest by @Around...");
        Object o = null;
        try {
            // 处理
            o = p.proceed();
        } catch (Throwable a) {
            a.printStackTrace();
        }
        System.err.println("afterTest by @Around...");
        return o;
    }

}
