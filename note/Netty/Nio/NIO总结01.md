## 前言

NIO知识一直都比较容易遗忘，所以写一篇NIO的总结来方便记忆。

## 正文

======== 复习BIO ======

Java支持三种网络编程模型：BIO/NIO/AIO

BIO（同步并阻塞），服务器实现模式为：
一个连接对应一个线程，即客户端有连接请求时，就需要启动一个线程进行处理。
【一对一的关系】

NIO（同步非阻塞），服务器实现模式为一个线程处理多个请求（连接）。客户端发送的请求会被注册到多路复用器中，如果多路复用器轮询到这个连接是IO请求，则会对这个IO请求进行处理。

client -> Thread -> Selector 

NIO方式适用于连接数目多且连接比较短（轻操作）的架构【连接数多；操作时间短】，比如聊天服务器，弹幕系统，从JDK1.4开始支持。




=========== 复习NIO =========

1. JavaNIO全称为Java-non-blocking IO。NIO相关类都被放在了java.nio及其子包下。

2. NIO中的三大核心：
	- 缓冲区（Buffer）
	- 选择器（Selector）
	- 通道（Channel）

NIO是面向缓冲区编程的，基于Reactor模型来实现的。

NIO的数据流向可以用下图来简单的表示一下：


Client -> Thread -> [Selector]-> Channel -> Buffer -> JVM

一个Selector可以轮询多个Channel

数据总是从Buffer写入Channel；
或者从Channel读取数据到Buffer中；

可以这么理解，Buffer就是一个内存块，直接和JVM关联。


而Channel提供从文件、网络读取数据的渠道。

NIO程序（JVM） <=> 缓冲区 <=> Channel <=> Selector <=> 文件（网络、client）


3. HTTP2.0 使用了多路复用技术，做到同一个连接并发处理多个请求，而且并发请求的数量比HTTP1.1大几个数量级。

4. NIO和BIO比较
	BIO是以流的方式来处理数据的；
	NIO是以缓冲区的方式啦处理数据的；
	BIO是阻塞的；
	NIO是非阻塞的；

5. Channel是会注册到Selector中的；Selector会根据不同的事件来在不同的Channel之间进行切换。

6. Buffer就是一个内存块，底层是数组。
	Buffer既可以读也可以写，是双向的；需要通过flip方法切换。
	channel也是双向的。
