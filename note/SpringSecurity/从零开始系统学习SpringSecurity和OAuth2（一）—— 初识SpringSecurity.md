<!-- TOC -->
- [前言](#前言)
- [正文](#正文)
    - [1. 初识SpringSecurity](#1-初识springsecurity)
    - [2. Spring Security项目核心jar包介绍](#2-spring-security项目核心jar包介绍)
    - [3. SpringSecurity核心注解](#3-springsecurity核心注解)        
      - [3.1 @EnableWebSecurity](#31-enablewebsecurity)        
      - [3.2 @EnableGlobalAuthentication](#32-enableglobalauthentication)        
      - [3.3 @EnableGlobalMethodSecurity](#33-enableglobalmethodsecurity)    
    - [4. SpringSecurity核心组件](#4-springsecurity核心组件)        
      - [4.1 认证](#41-认证)        
      - [4.2 授权](#42-授权)    
    - [5. SpringSecurity中的Filter](#5-springsecurity中的filter)        
      - [5.1 FilterChainProxy以及其内部的Filter](#51-filterchainproxy以及其内部的filter)
- [总结](#总结)
- [参考](#参考)
- [相关文章](#相关文章)
<!-- /TOC -->
## 前言
相信了解过SpringSecurity或者是OAuth2的读者，会发现网上会有非常多的相关文章，或是纯概念的，或是带有demo的，无论是哪种类型的文章，本人去阅读之后，对于整个框架的概念还是一知半解，也仅仅是实现了某些功能、某些效果而已，若遇到某些问题时无从下手，只能去百度去Google。这是因为对于SpringSecurity和OAuth2的知识没有一个整体概念的把握，知识体系没有形成系统，遂决定写一个关于SpringSecurity和OAuth2的系列专栏，在构建自己知识体系的同时还希望能帮助有同样困惑的同学。

**本专栏希望通过通俗易懂的语言来达到从入门概念、demo开始，由简入深，最终能达到深谙其底层源码的目的。**

## 正文
### 1. 初识SpringSecurity
对于SpringSecurity，可以由其官网中的介绍了解到其概述：
1. SpringSecurity就是一个功能强大且高定制化的身份验证和访问控制框架，它事实上就是一个保护基于Spring的应用框架安全性的标准。
2. Spring Security是一个重点为Java应用程序提供身份验证和授权的框架。
3. 与所有Spring项目一样，Spring Security的真正强大之处在于它可以很容易地扩展以满足定制需求

对于SpringSecurity的特性，可以总结为以下几点：
1. 对身份认证和授权提供全面和可扩展的支持。
2. 防止会话固定、劫持请求、跨站点请求伪造等攻击。 
3. Servlet API的集成。
4. 与Spring Web MVC的可选集成。

在进行代码编写时，先得对SpringSecurity有一个整体上的认识，等进行coding时才能知道对应代码的对应作用。

### 2. Spring Security项目核心jar包介绍
在SpringSecurity继承的项目中，主要有四个核心的jar包：

- spring-security-core.jar
	SpringSecurity的核心包，任何SpringSecurity的功能都需要此包。
- spring-security-web.jar
	web工程必备，包含过滤器和相关的Web安全基础结构代码。
- spring-security-config.jar 
	用于解析xml配置文件，用到SpringSecurity的xml配置文件的就要用到此包。

由于spring-security-web.jar和spring-security-config.jar 都依赖了spring-security-core.jar，所以只需要导入spring-security-web.jar和spring-security-config.jar 即可。

> 本专栏系列文章相关demo版本：
> SpringBoot: 2.1.14.RELEASE

```
		<!-- 导入SpringSecurity相关依赖 -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-config</artifactId>
            <version>5.1.10.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-web</artifactId>
            <version>5.1.10.RELEASE</version>
        </dependency>
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200712224312526.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

不过，在SpringBoot中的spring-boot-starter-security中，就已经依赖好了spring-security-web.jar和spring-security-config.jar，直接拿来用即可。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200712224733553.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

既然SpringBoot都已经定义好了spring-boot-starter-security，那就用spring-boot-starter-security吧，这样做的好处是能够方便SpringBoot统一SpringSecurity的版本。

### 3. SpringSecurity核心注解
SpringBoot集成SpringSecurity需要配置几个配置文件，并且需要几个核心注解，下面来依次介绍这些注解。

#### 3.1 @EnableWebSecurity
@EnableWebSecurity是Spring Security用于启用Web安全的注解。典型的用法是该注解用在某个Web安全配置类上(实现了接口WebSecurityConfigurer或者继承自WebSecurityConfigurerAdapter)。
```
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
...
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200713110634549.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
从注解源码可以看到，该注解有如下作用：
1. 控制Spring Security是否使用调试模式(通过注解属性debug指定)，默认为false，表示缺省不使用调试模式。
2. 导入 WebSecurityConfiguration，用于配置Web安全过滤器FilterChainProxy，并创建过滤器链springSecurityFilterChain来保护你的Spring应用。
3. 如果是Servlet 环境，导入WebMvcSecurityConfiguration。
4. 如果是OAuth2环境（spring-security-oauth2-client），导入OAuth2ClientConfiguration。
5. 使用注解@EnableGlobalAuthentication启用全局认证机制，即全局的AuthenticationManager，AuthenticationManager会在运行时对请求着进行身份验证。

#### 3.2 @EnableGlobalAuthentication
@EnableGlobalAuthentication是包含在了@EnableWebSecurity注解中的，作用通过启用认证管理器（AuthenticationManager）来启用全局认证机制。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200726025353554.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
核心逻辑就在AuthenticationConfiguration类里。

在AuthenticationConfiguration在SpringSecurity中扮演者非常重要的作用，它内部包含了AuthenticationManager用于核心的认证工作。接下来将会重点讲解该类。


#### 3.3 @EnableGlobalMethodSecurity
Spring Security默认是禁用注解的，要想开启注解，需要在继承WebSecurityConfigurerAdapter的类上加@EnableGlobalMethodSecurity注解，来判断用户对某个控制层的方法是否具有访问权限。

除此之外，还可以在@EnableGlobalMethodSecurity中添加几个属性。

1. @EnableGlobalMethodSecurity(securedEnabled=true)开启@Secured 注解过滤权限。
2. @EnableGlobalMethodSecurity(jsr250Enabled=true)开启@RolesAllowed 注解过滤权限 。
3. @EnableGlobalMethodSecurity(prePostEnabled=true)使用表达式时间方法级别的安全性 4个注解可用。
	- @PreAuthorize 在方法调用之前,基于表达式的计算结果来限制对方法的访问
	- @PostAuthorize 允许方法调用,但是如果表达式计算结果为false,将抛出一个安全性异常
	- @PostFilter 允许方法调用,但必须按照表达式来过滤方法的结果
	- @PreFilter 允许方法调用,但必须在进入方法之前过滤输入值


### 4. SpringSecurity核心组件
在SpringSecurity中，有着许多的组件包括AuthenticationManager、AccessDesicionManager和UsernamePasswordAuthenticationFilter等。

对于SpringSecurity来说，最大的两个问题就是：**认证（Authentication，即你是谁？）和授权（Authorization，允许你做什么？）**。SpringSecurity框架旨在将认证从授权中剥离出来，并也有适用于二者的策略和可扩展的设计。

#### 4.1 认证
在SpringSecurity中，用于认证的主要接口是AuthenticationManager，它只有一个方法：
```
public interface AuthenticationManager {
  Authentication authenticate(Authentication authentication)
    throws AuthenticationException;
}
```

AuthenticationManger最普遍的实现类是ProviderManager，而ProviderManager会将认证委托给AuthenticationProvider。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200714010510964.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200714010535442.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
AuthenticationProvider接口和AuthenticationManager相似，但是它有一个额外的方法允许查询它支持的Authentication方式：
```
public interface AuthenticationProvider {
	Authentication authenticate(Authentication authentication)
			throws AuthenticationException;

	boolean supports(Class<?> authentication);
}
```


#### 4.2 授权 

一旦认证成功，我们就可以进行授权了，它核心的策略就是AccessDecisionManager。同样的，它将授权逻辑全部委托给AccessDecisionVoter来实现。

一个AccessDecisionVoter考虑一个Authentication（代表一个Principal）和一个被ConfigAttributes装饰的安全对象，这里的ConfigAttributes就是一个包含了URL以及这个URL该有权限的对象的集合。
```
boolean supports(ConfigAttribute attribute);

boolean supports(Class<?> clazz);

int vote(Authentication authentication, S object,
        Collection<ConfigAttribute> attributes);
```

现在，已经拥有了认证和授权组件了，那么一个HTTP请求进入SpringSecurity应用时，经过过滤器链中都发生了哪些逻辑？接下来就看下SpringSecurity中过滤器链底层都发生了什么。

### 5. SpringSecurity中的Filter
除了认证和授权外，SpringSecurity的另外一个核心就是Servlet的Filter来实现的。先简单回顾下Servlet中Filter的调用原理。
下图展示了处理单个HTTP请求的经典分层结构图：
  ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200726104520271.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
客户端向服务器发起请求，然后Servlet容器（Tomcat）会根据URI来决定哪个Filter和哪个Servlet适用于这个请求，一个Servlet最多处理一个请求，过滤器是链式的，它们是有顺序的。事实上一个过滤器可以否决接下来的过滤器，上一个过滤器请求处理完就会动过调用Filter的doFilter()向下传递请求，知道请求最终到达Servlet处理完毕后，再过滤器以相反的顺序调用，再以Response返回给客户端。

#### 5.1 FilterChainProxy以及其内部的Filter

回归正题，SpringSecurity在过滤器链中扮演的就是一个Filter，其类型是FilterChainProxy。但它又不是一个普通的Filter，因为FilterChainProxy中包含了额外的过滤器，每个过滤器都发挥特殊的作用。下面用一张图展示下FliterChainProxy中包含的过滤器链。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200726110652665.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
在SpringBoot中，SpringSecurity的FilterChainProxy是以bean的形式注入到Spring容器中的，并且它是默认配置，所以在每次请求中都会存在，所以在SpringSecurity保护的应用中，每次请求都会经过FilterChainProxy。

## 总结
本片文章粗略的讲解了SpringSecurity里的几个核心概念，包括：核心注解、认证和授权的核心组件以及SpringSecurity中的FilterChainProxy，整个SpringSecurity框架就是围绕着这几个核心概念运行，下面几章将会深入分析每个核心概念的底层运行机制。

## 参考
- [《spring-security-architecture》](https://spring.io/guides/topicals/spring-security-architecture)
- [《Spring Security Reference》](https://docs.spring.io/spring-security/site/docs/current/reference/html5/#preface)

## 相关文章
- [从零开始系统学习SpringSecurity和OAuth2（二）—— 安全过滤器FilterChainProxy](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20%E5%AE%89%E5%85%A8%E8%BF%87%E6%BB%A4%E5%99%A8FilterChainProxy.md)
- [从零开始系统学习SpringSecurity和OAuth2（三）—— WebSecurity建造核心逻辑](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%B8%89%EF%BC%89%E2%80%94%E2%80%94%20WebSecurity%E5%BB%BA%E9%80%A0%E6%A0%B8%E5%BF%83%E9%80%BB%E8%BE%91.md)