package com.bruis.learnaop.testcglibaop;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class EnhancerDemo {

    /**
     * CGLIB是一个强大的高性能的代码生成包，注意这里是“代码生成包”。
     * 它被广泛的用在AOp框架中，例如Spring AOP，CGLIB为他们提供了方法的拦截（Interception）。
     * CGLIB包的底层通过使用一个小而块的字节码处理框架ASM，来转换字节码并生成新的类，除了
     * CGLIB包，脚本语言例如Groovy和BeanShell，也是使用ASM来生成Java的字节码，当然不鼓励直接使用
     * ASM，因为它要求你必须对JVM内部结构包括class文件的格式指令集都很熟悉。
     *
     * 下面这个CGLIBdemo，可以看到运行结果，除了test()方法之外，System.out.pringln(demo)也被代理了。
     * demo首先调用了toString()方法，然后又调用了hashCode，生成的对象为EnhancerDemo的实例，这个类是
     * 运行时由CGLIB产生的。
     */

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(EnhancerDemo.class);
        enhancer.setCallback(new MethodInterceptorImpl());

        EnhancerDemo demo = (EnhancerDemo) enhancer.create();
        demo.test();
        System.out.println(demo);
    }

    public void test() {
        System.out.println("EnhancerDemo testaspectJ()");
    }

    private static class MethodInterceptorImpl implements MethodInterceptor {
        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            System.err.println("Before invoke " + method);
            Object result = methodProxy.invokeSuper(o, objects);
            System.out.println("After invoke" + method);
            return result;
        }
    }
}
