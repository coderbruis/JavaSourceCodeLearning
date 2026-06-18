![JavaSourceCodeLearningImage](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/images/JavaSourceCodeLearningImage.png)

<p align="center">
  <a href="https://www.oracle.com/cn/java/technologies/javase/javase-jdk8-downloads.html">
    <img src="https://img.shields.io/badge/JDK-1.8.0__77-brightgreen" alt="JDK">
  </a>
  <a href="https://spring.io/">
    <img src="https://img.shields.io/badge/Spring-5.2.1.RELEASE-green" alt="Spring">
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/SpringBoot-2.2.1.RELEASE-yellowgreen" alt="SpringBoot">
  </a>
  <a href="https://spring.io/projects/spring-security">
    <img src="https://img.shields.io/badge/SpringSecurity-5.1.0.RELEASE-orange" alt="SpringSecurity">
  </a>
  <a href="https://spring.io/projects/spring-security-oauth">
    <img src="https://img.shields.io/badge/Spring--Security--OAuth2-2.3.5.RELEASE-red" alt="Spring-Security-OAuth2">
  </a>
  <a href="https://netty.io/">
    <img src="https://img.shields.io/badge/Netty-4.1.43.Final-blue" alt="Netty">
  </a>
  <a href="https://rocketmq.apache.org/">
    <img src="https://img.shields.io/badge/RocketMQ-4.9.0-green" alt="rocketmq">
  </a>
  <a href="https://kafka.apache.org/">
    <img src="https://img.shields.io/badge/kafka-4.0.2-yellowgreen" alt="rocketmq">
  </a>
</p>

Java流行框架源码分析，学习以及总结。项目持续更新中，不建议直接Fork，欢迎star、watch。

对于框架底层源码的学习，需要反复、认真思考，并做到温故而知新，这样才能将底层原理吸收得更加牢固。

框架包括：

✅  JDK源码

✅  JUC源码

✅  Spring源码

✅  SpringBoot源码

✅  SpringAOP源码

✅  SpringSecurity源码

✅  SpringSecurity OAuth2源码

✅  JDK源码

✅  Dubbo源码

✅  Netty源码

✅  RocketMQ源码

✅  kafka源码

> 为什么要分析、学习源码？

学习一个框架的源码，不仅在实际使用时如果出现问题，可以快速定位出问题，找到问题原因并解决，同时还可以学习到框架的架构思想以与设计模式。当然，学习框架底层源码还可以提升我们自身的水平，在大厂面试时能够在众多面试者中脱颖而出。因此学习框架虽然枯燥乏味，但罗马并非一日建成的，所以平时之余就要多学习框架底层源码，
这样在用到的时候就能游刃有余。

# 目录

-   项目导入
    将整个JavaSourceCodeLearning导入IDEA中，然后选中项目pom.xml文件右键，open as maven project然后等待maven下载相应jar包即可。

-   JDK源码学习
    - JDK版本：1.8.0_77
    - [深入学习String源码与底层（一）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0String%E6%BA%90%E7%A0%81%E4%B8%8E%E5%BA%95%E5%B1%82%EF%BC%88%E4%B8%80%EF%BC%89.md)
    - [深入学习String源码与底层（二）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0String%E6%BA%90%E7%A0%81%E4%B8%8E%E5%BA%95%E5%B1%82%EF%BC%88%E4%BA%8C%EF%BC%89.md)
    - [深入解读CompletableFuture源码与原理](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E8%A7%A3%E8%AF%BBCompletableFuture%E6%BA%90%E7%A0%81%E4%B8%8E%E5%8E%9F%E7%90%86.md)
    - [一篇文章快速深入学习ThreadLocal](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E4%B8%80%E7%AF%87%E6%96%87%E7%AB%A0%E5%BF%AB%E9%80%9F%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0ThreadLocal.md)
    - [深入学习Java volatile关键字](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0Java%20volatile%E5%85%B3%E9%94%AE%E5%AD%97.md)
    - [深入学习Thread底层原理](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0Thread%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81.md)
    - [深入学习JDK1.7、8 HashMap扩容原理]()
    - [开源项目里那些看不懂的位运算分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/%E5%BC%80%E6%BA%90%E9%A1%B9%E7%9B%AE%E9%87%8C%E9%82%A3%E4%BA%9B%E7%9C%8B%E4%B8%8D%E6%87%82%E7%9A%84%E4%BD%8D%E8%BF%90%E7%AE%97%E5%88%86%E6%9E%90.md)
    - [ThreadPoolExecutor源码分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E8%A7%A3%E6%9E%90ThreadPoolExecutor%E5%BA%95%E5%B1%82%E5%8E%9F%E7%90%86.md)

-   Spring源码学习
    - Spring版本：5.2.1.RELEASE
    
    - [深入Spring源码系列（一）——在IDEA中构建Spring源码](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%E5%9C%A8IDEA%E4%B8%AD%E6%9E%84%E5%BB%BASpring%E6%BA%90%E7%A0%81.md)
    - [深入Spring源码系列（二）——深入Spring容器，通过源码阅读和时序图来彻底弄懂Spring容器（上）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%E6%B7%B1%E5%85%A5Spring%E5%AE%B9%E5%99%A8%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB%E5%92%8C%E6%97%B6%E5%BA%8F%E5%9B%BE%E6%9D%A5%E5%BD%BB%E5%BA%95%E5%BC%84%E6%87%82Spring%E5%AE%B9%E5%99%A8%EF%BC%88%E4%B8%8A%EF%BC%89.md)
    - [深入Spring源码系列（二）——深入Spring容器，通过源码阅读和时序图来彻底弄懂Spring容器（下）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%E6%B7%B1%E5%85%A5Spring%E5%AE%B9%E5%99%A8%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB%E5%92%8C%E6%97%B6%E5%BA%8F%E5%9B%BE%E6%9D%A5%E5%BD%BB%E5%BA%95%E5%BC%84%E6%87%82Spring%E5%AE%B9%E5%99%A8%EF%BC%88%E4%B8%8B%EF%BC%89.md)
    - [深入Spring源码系列（补充篇）——程序调用Spring源码](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E8%A1%A5%E5%85%85%E7%AF%87%EF%BC%89%E2%80%94%E2%80%94%E7%A8%8B%E5%BA%8F%E8%B0%83%E7%94%A8Spring%E6%BA%90%E7%A0%81.md)
    - [从Spring源码中学习——策略模式](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E4%BB%8ESpring%E6%BA%90%E7%A0%81%E4%B8%AD%E5%AD%A6%E4%B9%A0%E2%80%94%E2%80%94%E7%AD%96%E7%95%A5%E6%A8%A1%E5%BC%8F.md)

-   SpringAOP源码学习
    - Spring版本：5.2.1.RELEASE 
    
    - [深入学习SpringAOP源码（一）——注册AnnotationAwareAspectJAutoProxyCreator](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringAOP/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0SpringAOP%E6%BA%90%E7%A0%81%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%E6%B3%A8%E5%86%8CAnnotationAwareAspectJAutoProxyCreator.md) 
    - [深入学习SpringAOP源码（二）—— 深入AnnotationAwareAspectJAutoProxyCreator](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringAOP/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0SpringAOP%E6%BA%90%E7%A0%81%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20%E6%B7%B1%E5%85%A5AnnotationAwareAspectJAutoProxyCreator.md)
    - [深入学习SpringAOP源码（三）——揭开JDK动态代理和CGLIB代理的神秘面纱](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringAOP/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0SpringAOP%E6%BA%90%E7%A0%81%EF%BC%88%E4%B8%89%EF%BC%89%E2%80%94%E2%80%94%E6%8F%AD%E5%BC%80JDK%E5%8A%A8%E6%80%81%E4%BB%A3%E7%90%86%E5%92%8CCGLIB%E4%BB%A3%E7%90%86%E7%9A%84%E7%A5%9E%E7%A7%98%E9%9D%A2%E7%BA%B1.md)
        
-   SpringBoot源码学习
    - SpringBoot版本：2.2.1.RELEASE
    
    - [深入浅出SpringBoot源码——SpringFactoriesLoader](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94SpringFactoriesLoader.md) 
    - [深入浅出SpringBoot源码——监听器与事件机制](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94%E7%9B%91%E5%90%AC%E5%99%A8%E4%B8%8E%E4%BA%8B%E4%BB%B6%E6%9C%BA%E5%88%B6.md)
    - [深入浅出SpringBoot源码——系统初始化器](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94%E7%B3%BB%E7%BB%9F%E5%88%9D%E5%A7%8B%E5%8C%96%E5%99%A8.md)
    - [深入浅出SpringBoot源码——启动加载器](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94%E5%90%AF%E5%8A%A8%E5%8A%A0%E8%BD%BD%E5%99%A8.md)
    
-   SpringSecurity&OAuth2源码学习
    - SpringSecurity版本：5.1.0.RELEASE
    - [深入浅出SpringSecurity和OAuth2（一）—— 初识SpringSecurity](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%20%E5%88%9D%E8%AF%86SpringSecurity.md)   
    - [深入浅出SpringSecurity和OAuth2（二）—— 安全过滤器FilterChainProxy](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20%E5%AE%89%E5%85%A8%E8%BF%87%E6%BB%A4%E5%99%A8FilterChainProxy.md)
    - [深入浅出SpringSecurity和OAuth2（三）—— WebSecurity建造核心逻辑](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%B8%89%EF%BC%89%E2%80%94%E2%80%94%20WebSecurity%E5%BB%BA%E9%80%A0%E6%A0%B8%E5%BF%83%E9%80%BB%E8%BE%91.md)
    - [深入浅出SpringSecurity和OAuth2（四）—— FilterChainProxy过滤器链中的几个重要的过滤器](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E5%9B%9B%EF%BC%89%E2%80%94%E2%80%94%20FilterChainProxy%E8%BF%87%E6%BB%A4%E5%99%A8%E9%93%BE%E4%B8%AD%E7%9A%84%E5%87%A0%E4%B8%AA%E9%87%8D%E8%A6%81%E7%9A%84%E8%BF%87%E6%BB%A4%E5%99%A8.md)
    
-   Dubbo底层源码解析
    - Dubbo底层源码版本：2.7.8
    - [Dubbo底层源码学习—— 源码搭建](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Dubbo/Dubbo%E6%BA%90%E7%A0%81%E6%90%AD%E5%BB%BA.md)
    - [Dubbo底层源码学习（一）—— Dubbo的URL](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Dubbo/Dubbo%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%20Dubbo%E7%9A%84URL.md)
    - [Dubbo底层源码学习（二）—— Dubbo的SPI机制（上）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Dubbo/Dubbo%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20Dubbo%E7%9A%84SPI%E6%9C%BA%E5%88%B6%EF%BC%88%E4%B8%8A%EF%BC%89.md)
    - [Dubbo底层源码学习（二）—— Dubbo的SPI机制（中）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Dubbo/Dubbo底层源码学习%EF%BC%88二%EF%BC%89——%20Dubbo的SPI机制%EF%BC%88中%EF%BC%89.md
)
    - [Dubbo底层源码学习（二）—— Dubbo的SPI机制（下）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Dubbo/Dubbo%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20Dubbo%E7%9A%84SPI%E6%9C%BA%E5%88%B6%EF%BC%88%E4%B8%8B%EF%BC%89.md)
    - Dubbo底层源码学习（三）—— Dubbo的注册中心
    - Dubbo底层源码学习（四）—— Dubbo的注册中心缓存机制
    - Dubbo底层源码学习（五）—— Dubbo的注册中心重试机制
    - [Dubbo底层源码学习（六）—— Dubbo的服务暴露](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Dubbo/Dubbo%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E2%80%94%E2%80%94%E6%9C%8D%E5%8A%A1%E6%9A%B4%E9%9C%B2.md)
    - Dubbo底层源码学习（七）—— Dubbo的服务消费
    
-   Netty底层源码解析    
    - Netty版本：4.1.43.Final
    - [二进制运算以及源码、反码以及补码学习](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Netty/%E4%BA%8C%E8%BF%9B%E5%88%B6.md) 
    - [Netty源码包结构](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Netty/Netty%E6%BA%90%E7%A0%81%E5%8C%85%E7%BB%93%E6%9E%84.md)
    - [Netty底层源码解析-EventLoopGroup](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Netty/Netty%E4%B8%AD%E7%9A%84EventLoopGroup%E6%98%AF%E4%BB%80%E4%B9%88.md)
    - [Netty底层源码解析-初始Netty及其架构](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-%E5%88%9D%E5%A7%8BNetty%E5%8F%8A%E5%85%B6%E6%9E%B6%E6%9E%84.md)
    - [Netty底层源码解析-Netty服务端启动分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-Netty%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%90%AF%E5%8A%A8%E5%88%86%E6%9E%90.md)
    - [Netty底层源码解析-NioEventLoop原理分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-NioEventLoop%E5%8E%9F%E7%90%86%E5%88%86%E6%9E%90.md)
    - [Netty底层源码解析-ChannelPipeline分析（上）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-ChannelPipeline%E5%88%86%E6%9E%90%EF%BC%88%E4%B8%8A%EF%BC%89.md)
    - [Netty底层源码解析-ChannelPipeline分析（下）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-ChannelPipeline%E5%88%86%E6%9E%90%EF%BC%88%E4%B8%8B%EF%BC%89.md)
    - [Netty底层源码解析-NioServerSocketChannel接受数据原理分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-NioServerSocketChannel%E6%8E%A5%E5%8F%97%E6%95%B0%E6%8D%AE%E5%8E%9F%E7%90%86%E5%88%86%E6%9E%90.md)
    - Netty底层源码解析-NioSocketChannel接受、发送数据原理分析
    - Netty底层源码解析-FastThreadLocal原理分析
    - Netty底层源码解析-内存分配原理分析 
    - Netty底层源码解析-RocketMQ底层使用到的Netty
    - [Netty底层的优化总结]()
    - [实战+原理效果更佳！强烈推荐闪电侠大佬实战课：《Netty 入门与实战：仿写微信 IM 即时通讯系统》](https://juejin.cn/book/6844733738119593991)
    
Netty实战课相关点位于：Spring-Netty，com/bruis/learnnetty/im包下，有需要的读者可前往查看。


-   RocketMQ底层源码解析    
    - RocketMQ版本：4.9.0
    - RocketMQ底层源码解析-RocketMQ环境搭建
    - RocketMQ底层源码解析-本地调试RocketMQ源码
    - RocketMQ底层源码解析-NameServer分析



-   kafka源码分析
    - kafka版本：4.2
    - [kafka broker核心源码分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/kafka%20broker%E6%A0%B8%E5%BF%83%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md)
    - [kafka消费者核心源码分析（一）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/kafka%E6%B6%88%E8%B4%B9%E8%80%85%E6%A0%B8%E5%BF%83%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90%EF%BC%88%E4%B8%80%EF%BC%89.md)

  持续更新中...


    
# 支持

  原创不易，各位帅哥美女star支持下...


