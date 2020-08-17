## 前言

在Dubbo中，URL扮演者非常重要的作用，可以说，理解了URl，就理解了一般的Dubbo了。在Dubbo源码中，可随处看到URL的使用踪迹。
URL 是整个 Dubbo 中非常基础，也是非常核心的一个组件，阅读源码的过程中你会发现很多方法都是以 URL 作为参数的，在方法内部解析传入的 URL 得到有用的参数，所以有人将 URL 称为Dubbo 的配置总线。

## 正文

### 1. 什么是URL？URL有哪些组成部分？Dubbo中如何使用URL的？

URL全称（Uniform Resource Locator，统一资源定位符），它是互联网的统一资源定位标志，也就是指网络地址。

一个标准的URL是这样的：

```
protocol://username:password@host:port/path?key=value&key=value
```
- protocol：URL 的协议。我们常见的就是 HTTP 协议和 HTTPS 协议，当然，还有其他协议，如 FTP 协议、SMTP 协议等。
- username/password：用户名/密码。 HTTP Basic Authentication 中多会使用在 URL 的协议之后直接携带用户名和密码的方式。
- host/port：主机/端口。在实践中一般会使用域名，而不是使用具体的 host 和 port。
- path：请求的路径。
- parameters：参数键值对。一般在 GET 请求中会将参数放到 URL 中，POST 请求会将参数放到请求体中。

URL 是整个 Dubbo 中非常基础，也是非常核心的一个组件，阅读源码的过程中你会发现很多方法都是以 URL 作为参数的，在方法内部解析传入的 URL 得到有用的参数，所以有人将 URL 称为Dubbo 的配置总线。

下面来看下Dubbo中的URL组成部分：

Dubbo 中任意的一个实现都可以抽象为一个 URL，Dubbo 使用 URL 来统一描述了所有对象和配置信息，并贯穿在整个 Dubbo 框架之中。这里我们来看 Dubbo 中一个典型 URL 的示例，如下：

```
dubbo://172.17.32.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&pid=32508&release=&side=provider&timestamp=1593253404714dubbo://172.17.32.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&pid=32508&release=&side=provider&timestamp=1593253404714
```

### 2. Dubbo中的URL

先看下Dubbo中org.apache.dubbo.common包下的URL类源码：

```
public /*final**/
class URL implements Serializable {

    private static final long serialVersionUID = -1985165475234910535L;

    private final String protocol;

    private final String username;

    private final String password;

    // by default, host to registry
    private final String host;

    // by default, port to registry
    private final int port;

    private final String path;

    private final Map<String, String> parameters;

    private final Map<String, Map<String, String>> methodParameters;
    
    ...省略
}
```

可以看到URL中定义了协议、用户名、密码、host、端口等信息，和上述讲解的一致。

看下在Dubbo中典型的URL。

```
dubbo://192.168.1.6:20880/moe.cnkirito.sample.HelloService?timeout=3000
描述一个 dubbo 协议的服务

zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-consumer&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=1214&qos.port=33333&timestamp=1545721981946
描述一个 zookeeper 注册中心

consumer://30.5.120.217/org.apache.dubbo.demo.DemoService?application=demo-consumer&category=consumers&check=false&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=1209&qos.port=33333&side=consumer&timestamp=1545721827784
描述一个消费者

```

可以说，任意的一个领域中的一个实现都可以认为是一类 URL，dubbo 使用 URL 来统一描述了元数据，配置信息，贯穿在整个框架之中。

### 3. Dubbo中URL是如何生成的呢？

一个URL对象是如何在Dubbo中生成的呢？它的协议、参数等都是如何生成的？这可以概括为URL的生命周期是怎样的。

这里可以概括为三点：

1. 解析服务
2. 暴露服务
3. 引用服务

> 解析服务
    
![DubboNamespaceHandler](https://github.com/coderbruis/JavaSourceCodeLearning/tree/master/note/images/Dubbo/dubbo01.png)

在Dubbo的jar包中的META-INF/spring.handlers配置，配置了DubboNamespaceHandler处理类，在遇到dubbo命名空间时，会进行解析。

可以看到DubboNamespaceHandler处理类中，init方法注册了很多DubboBeanDefinitionParser对象，该对象的parse方法就是对所有的Dubbo标签都进行解析，
将新建的bean注册到spring中，并解析xml中的属性，并添加到该bean中。

（DubboNamespaceHandler的parse主要步骤分为：1）初始化RootBeanDefinition；2）获取BeanId；3）将获取到的Bean注册到Spring中；4）将xml中配置的信息放到beandefinition的ProvertyValues中；）

接着，在 ServiceConfig.export() 或 ReferenceConfig.get() 初始化时，将 Bean 对象转换 URL 格式，所有 Bean 属性转成 URL 的参数。

然后将 URL 传给协议扩展点，基于扩展点自适应机制，根据 URL 的协议头，进行不同协议的服务暴露或引用。


> 暴露服务

1. 只暴露服务端口：

在没有注册中心，直接暴露提供者的情况下，ServiceConfig 解析出的 URL 的格式为：dubbo://service-host/com.foo.FooService?version=1.0.0。

基于扩展点自适应机制，通过 URL 的 dubbo:// 协议头识别，直接调用 DubboProtocol的 export() 方法，打开服务端口。

2. 向注册中心暴露服务：

在有注册中心，需要注册提供者地址的情况下，ServiceConfig 解析出的 URL 的格式为: registry://registry-host/org.apache.dubbo.registry.RegistryService?export=URL.encode("dubbo://service-host/com.foo.FooService?version=1.0.0")，

基于扩展点自适应机制，通过 URL 的 registry:// 协议头识别，就会调用 RegistryProtocol 的 export() 方法，将 export 参数中的提供者 URL，先注册到注册中心。

再重新传给 Protocol 扩展点进行暴露： dubbo://service-host/com.foo.FooService?version=1.0.0，然后基于扩展点自适应机制，通过提供者 URL 的 dubbo:// 协议头识别，就会调用 DubboProtocol 的 export() 方法，打开服务端口。

3. ServiceConfig的doExportUrlsFor1Protocol方法 

在该方法中，创建出URL，然后将URL传递给协议扩展点，基于扩展点自适应机制进行服务暴露。通俗点说就是创建出URL后，通过DubboProtocol来暴露服务。

ServiceConfig这个类非常复杂，后面专门拿一篇来讲解。

> 引用服务

1. 直连引用服务：

在没有注册中心，直连提供者的情况下，ReferenceConfig 解析出的 URL 的格式为：dubbo://service-host/com.foo.FooService?version=1.0.0。

基于扩展点自适应机制，通过 URL 的 dubbo:// 协议头识别，直接调用 DubboProtocol 的 refer() 方法，返回提供者引用。

2. 从注册中心发现引用服务：

在有注册中心，通过注册中心发现提供者地址的情况下，ReferenceConfig 解析出的 URL 的格式为：registry://registry-host/org.apache.dubbo.registry.RegistryService?refer=URL.encode("consumer://consumer-host/com.foo.FooService?version=1.0.0")。

基于扩展点自适应机制，通过 URL 的 registry:// 协议头识别，就会调用 RegistryProtocol 的 refer() 方法，基于 refer 参数中的条件，查询提供者 URL，如： dubbo://service-host/com.foo.FooService?version=1.0.0。

基于扩展点自适应机制，通过提供者 URL 的 dubbo:// 协议头识别，就会调用 DubboProtocol 的 refer() 方法，得到提供者引用。

然后 RegistryProtocol 将多个提供者引用，通过 Cluster 扩展点，伪装成单个提供者引用返回。

### 4. Dubbo中使用的URL

1. Dubbo在SPI中的应用

2. Dubbo中的服务暴露

3. Dubbo中的服务引用

## 参考

