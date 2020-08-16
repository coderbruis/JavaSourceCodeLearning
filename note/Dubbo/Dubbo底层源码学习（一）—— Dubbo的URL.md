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

### 2. Dubbo的URL源码

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

## 参考

- [拉钩教育——Dubbo源码解析](https://kaiwu.lagou.com/course/courseInfo.htm?courseId=393#/detail/pc?id=4258)