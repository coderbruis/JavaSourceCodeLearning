## 前言

版本：【Spring 5.1.4】、【SpringAOP 5.1.4】

原本想从网上博客学下SpringAOP源码逻辑，结果都看的我一头雾水，知识点都是东一块西一块的，根本都无法了解到SpringAOP源码逻辑的来龙去脉。本人干脆自己通过断点的方式，从SpringAOP源码一步一步打断点来学习SpringAOP源码，并通过系列博文的形式记录一下自己对SpringAOP的理解与总结。

下面来看下一个简单的SpringAOP程序：

用到的类以及配置文件：
- TestBean.java        //测试model
- AspectJTest.java     //AOP实现
- aspectTest.xml       //AOP配置文件
- TestMain            //main测试方法入口

TestBean.java
```Java
public class TestBean {
    private String testStr = "testStr";

    public String getTestStr() {
        return testStr;
    }

    public void setTestStr(String testStr) {
        this.testStr = testStr;
    }

    public void test() {
        System.out.println("test");
    }
}
```

AspectJTest.java
```Java
@Aspect
public class AspectJTest {

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
```

aspectTest.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd http://www.springframework.org/schema/context https://www.springframework.org/schema/context/spring-context.xsd">
    
    <aop:aspectj-autoproxy proxy-target-class="false" expose-proxy="true" />
    <bean id="test" class="com.bruis.learnaop.model.TestBean"/>
    <bean class="com.bruis.learnaop.test.AspectJTest" />


</beans>
```
TestMain.java
```Java
public class TestMain {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("aspectTest.xml");
        TestBean bean = (TestBean)context.getBean("test");
        bean.test();
    }
}
```

运行结果如下：
```
beforeTest by @Around...
beforeTest()...
test
afterTest()...
afterTest by @Around...
```

SpringAOP使用起来就是这么的简单、便捷，然而其内部实现逻辑是什么？相信大家都和我一样的好奇。是时候静下心来好好研究下SpringAOP源码了。




在Spring中，通过短短的一句配置
```
<aop:aspectj-autoproxy/>
```
然后配合着AspectJ的注解就可以使用SpringAOP，入门门槛极低，然而AOP内部的实现机制确实非常复杂，下面就来研究下。

## 正文

### 1、SpringAOP的核心类——AnnotationAwareAspectJAutoProxyCreator

很多小伙伴都会纳闷，这SpringAOP源码的核心类或者入口在哪呢？入口都没找到就更别提研究了.....

其实，只要鼠标点中SpringAOP配置文件里的<aop:aspectj-autoproxy/>，就会调转到spring-aop.xsd，如图：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823101257861.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

对，就是AnnotationAwareAspectJAutoProxyCreator实现了AOP逻辑。

我们通过配置
```
<aop:aspectj-autoproxy/>
```
，可以让Spring自动完成对AnnotationAwareAspectJAutoProxyCreator类的注册，从而实现AOP的功能。在讲解AnnotationAwareAspectJAutoProxyCreator功能之前，先得知道它是怎么被注册到Spring中，并如何被解析到SringIOC中的？下面来看看Spring是如何注册AnnotationAwareAspectJAutoProxyCreator这个类。

### 2、Spring注册AnnotationAwareAspectJAutoProxyCreator

说到Spring注册AnnotationAwareAspectJAutoProxyCreator，那要从SpringIOC加载META-INF/spring.handlers说起了。SpringIOC在初始化时，不仅从XML中获取了bean的定义信息，还从classpath下的META-INF/spring.handlers中获取到对应的Handler，可以从spring-aop的jar包中查看到该文件，文件内容如下：
```
http\://www.springframework.org/schema/aop=org.springframework.aop.config.AopNamespaceHandler
```

断点打在类DefaultNamepsaceHandlerResolver的getHandlerMappings()方法上（具体的Spring源码这里不详细讲解），调试一波，如图：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823101436550.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

无图无真相啊，原来Spring将配置文件中的xmlns配置都解析成了一个一个Java命名解析器。回到我们的关注重点——AopNamespaceHandler，查看源码：
AopNamespaceHandler.class
```Java
public class AopNamespaceHandler extends NamespaceHandlerSupport {
    public AopNamespaceHandler() {
    }

    public void init() {
        this.registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
        this.registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
        this.registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());
        this.registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
    }
}
```
可以看到，在init()方法里，Spring对aspectj-autoproxy也就是AnnotationAwareAspectJAutoProxyCreator进行了注册。在详细了解注册原理之前，先说明下在Spring中，所有的解析器都是对BeanDefinitionParser接口的同一实现：
```Java
public interface BeanDefinitionParser {
    @Nullable
    BeanDefinition parse(Element var1, ParserContext var2);
}
```
解析入口都是从parse方法开始的。
进入AspectJAutoProxyBeanDefinitionParser类中查看parse的实现逻辑：

```Java
class AspectJAutoProxyBeanDefinitionParser implements BeanDefinitionParser {
    ...
    @Nullable
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
        this.extendBeanDefinition(element, parserContext);
        return null;
    }
    ...
}
```



AopNamspaceUtils
```Java
public abstract class AopNamespaceUtils {
    public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";
    private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";
    ...
    public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(ParserContext parserContext, Element sourceElement) {
        //把应用了@Aspect注解修饰的bean注册成BeanDefinition
        BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext.getRegistry(), parserContext.extractSource(sourceElement));
        //处理proxy-target-class和expose-proxy属性
        useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
        //注册组件并通知解析器
        registerComponentIfNecessary(beanDefinition, parserContext);
    }
    
    /*
     * useClassProxyingIfNecessary实现了proxy-target-class和expose-proxy属性的处理逻辑。
     *
     */
    private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, @Nullable Element sourceElement) {
        if (sourceElement != null) {
            boolean proxyTargetClass = Boolean.parseBoolean(sourceElement.getAttribute("proxy-target-class"));
            if (proxyTargetClass) {
                AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
            }

            boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute("expose-proxy"));
            if (exposeProxy) {
                AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
            }
        }

    }
    
```

```Java
public abstract class AopConfigUtils {
    @Nullable
    private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        if (registry.containsBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator")) {
            BeanDefinition apcDefinition = registry.getBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator");
            if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
                int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
                int requiredPriority = findPriorityForClass(cls);
                if (currentPriority < requiredPriority) {
                    apcDefinition.setBeanClassName(cls.getName());
                }
            }

            return null;
        } else {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
            beanDefinition.setSource(source);
            beanDefinition.getPropertyValues().add("order", -2147483648);
            beanDefinition.setRole(2);
            registry.registerBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator", beanDefinition);
            return beanDefinition;
        }
    }
}
```
registerOrEscalateApcAsRequired方法的作用就是获取AnnotationAwareAspectJAutoProxyCreator的BeanDefinition，然后根据优先级来装配这个BeanDefinition。获取到了AnnotationAwareAspectJAutoProxyCreator之后，接下来就要将配置信息和BeanDefinition一起注册到SpringIOC中。
无图无真相，可以看到registerAspectJAnnotationAutoProxyCreatorIfNecessary方法返回的beanDefinition：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823102123876.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)


看看如果proxy-target-class和expose-proxy都为true时，代码的逻辑。
```Java
public abstract class AopConfigUtils {
    ...
    /*
     * 如果proxy-target-class为true，则走该方法的逻辑
     */
    public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator")) {
            BeanDefinition definition = registry.getBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator");
            definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
        }

    }
    
    /*
     * 如果expose-proxy为true，则走该方法的逻辑
     */
    public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator")) {
            BeanDefinition definition = registry.getBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator");
            definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
        }

    }
}
```

这又回到了熟悉的节奏，把bean的定义信息存放到了BeanDefinition中，Spring将设置的proxyTargetClass和exposeProxy的值存放到了BeanDefinition的PropertyValues里。

看到这里，很多小伙伴估计跟我刚学SpringAOP一样，对expose-proxy和proxy-target-class属性作用一头雾水，这里简单介绍下二者的作用：

**proxy-target-class:**
1. 强制使用CGLIB，配置如下属性即可使用CGLIB
    <aop:config proxy-target-class="true"/>
2. 使用CGLIB代理和@AspectJ自动代理支持，配置如下配置即可实现
    <aop:aspectj-autoproxy proxy-target-class="true"/>

**expose-proxy:**
1. 用于那种在在内部调用方法时无法调用切面逻辑的一种强制通知，这个后面做详细解释。

经过了useClassProxyingIfNecessary()方法的调用，ParserContext对象中存放好了注册的额外信息（proxy-target-class、expose-proxy值等），这里暂且将ParserContext称为解析上下文。由上面的源码可知，在AopNamespaceUtils类的registerAspectJAnnotationAutoProxyCreatorIfNecessary方法中，将获取的org.springframework.aop.config.internalAutoProxyCreator的BeanDefinition和解析上下文一起传入registerComponentIfNecessary方法中，进行Component组件注册。

在随后的registerComponentIfNecessary方法中，经过new BeanComponentDefinition()构造方法的调用，已经将AnnotationAwareAspectJAutoProxyCreator的BeanDefinition注册到了SpringIOC中。
```Java
public abstract class AopConfigUtils {
    ...
    private static void registerComponentIfNecessary(@Nullable BeanDefinition beanDefinition, ParserContext parserContext) {
        if (beanDefinition != null) {
            parserContext.registerComponent(new BeanComponentDefinition(beanDefinition, "org.springframework.aop.config.internalAutoProxyCreator"));
        }

    }
}
```
```Java
public class BeanComponentDefinition extends BeanDefinitionHolder implements ComponentDefinition {
    public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName) {
        this(new BeanDefinitionHolder(beanDefinition, beanName));
    }
}
```
```Java
public class BeanDefinitionHolder implements BeanMetadataElement {
    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
        this(beanDefinition, beanName, (String[])null);
    }
    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
        Assert.notNull(beanName, "Bean name must not be null");
        // 此处，将AnnotationAwareAspectJAutoProxyCreator的BeanDefinition赋值给了this.beanDefinition，这个this是包含在SpringIOC中的，这点相信看过
        this.beanDefinition = beanDefinition;
        this.beanName = beanName;
        this.aliases = aliases;
    }
}
```
然后一路返回，将BeanDefinition存放在解析上下文（ParserContext）中，并在AspectJAutoProxyBeanDefinitionParser类的extendBeanDefinition方法中取出。
```Java
class AspectJAutoProxyBeanDefinitionParser implements BeanDefinitionParser {
    private void extendBeanDefinition(Element element, ParserContext parserContext) {
        BeanDefinition beanDef = parserContext.getRegistry().getBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator");
        if (element.hasChildNodes()) {
            this.addIncludePatterns(element, parserContext, beanDef);
        }

    }
}
```
打断点查看parserContext.getRegistry()获取注册清单，发现AnnotationAwareAspectJAutoProxyCreator就注册在了parserContext的注册清单里，而解析上下文又存放在SpringIOC（ApplicationContext，又称为Spring应用容器上下文）中。如图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823102443865.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
跳到AbstractApplicationContext的refresh()方法，发现AnnotationAwareAspectJAutoProxyCreator已经被注册到了SpringIOC容器中了。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823102917270.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

至此，AnnotationAwareAspectJAutoProxyCreator的注册逻辑介绍到这里。下一篇，开始深入AnnotationAwareAspectJAutoProxyCreator学习。

### 3. 总结

1. <aop:aspectj-autoproxy/>的作用是在Spring中注册AnnotationAwareAspectJAutoProxyCreator
2. 注册之后的解析工作是由AspectJAutoProxyBeanDefinitionParser类的.parse方法完成的
3. <aop:aspectj-autoproxy/>的proxy-target-class和expose-proxy属性会被注册到BeanDefinition的PropertyValues里
4. proxy-target-class的作用：
    1）强制使用CGLIB，配置如下属性即可使用CGLIB <aop:config proxy-target-class=“true”/>；
    2）使用CGLIB代理和@AspectJ自动代理支持，配置如下配置即可实现
<aop:aspectj-autoproxy proxy-target-class=“true”/>
5. expose-proxy的作用
    用于那种在在内部调用方法时无法调用切面逻辑的一种强制通知

[深入学习SpringAOP源码（二）—— 深入AnnotationAwareAspectJAutoProxyCreator](https://blog.csdn.net/CoderBruis/article/details/100042081)
[深入学习SpringAOP源码（三）——揭开JDK动态代理和CGLIB代理的神秘面纱](https://blog.csdn.net/CoderBruis/article/details/100083575)
