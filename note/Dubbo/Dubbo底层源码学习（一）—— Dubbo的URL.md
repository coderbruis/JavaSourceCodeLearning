## 前言

在Dubbo中，URL扮演者非常重要的作用，可以说，理解了URl，就理解了一般的Dubbo了。在Dubbo源码中，可随处看到URL的使用踪迹。
URL 是整个 Dubbo 中非常基础，也是非常核心的一个组件，阅读源码的过程中你会发现很多方法都是以 URL 作为参数的，在方法内部解析传入的 URL 得到有用的参数，所以有人将 URL 称为Dubbo 的配置总线。

## 正文

### 1. 什么是URL？URL有哪些组成部分？

URL全称（Uniform Resource Locator，统一资源定位符），它是互联网的统一资源定位标志，也就是指网络地址。

一个标准的URL是这样的：

```
protocol://username:password@host:port/path?key=value&key=value
```
