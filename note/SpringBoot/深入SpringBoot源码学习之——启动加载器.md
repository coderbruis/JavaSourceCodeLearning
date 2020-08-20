## 前言
在SpringBoot中，启动加载器的作用是，当SpringBoot程序执行后立马需要执行其他回调程序时，就可以通过SpringBoot的启动类加载器来实现。

## 正文

### 1. CommandLineRunner

SpringBoot的CommandLineRunner是一个启动加载器的核心，CommandLinerRunner是一个接口，该接口定义如下：


```
@FunctionalInterface
public interface CommandLineRunner {

	// 启动加载器的回调方法
	void run(String... args) throws Exception;

}
```

如何使用SpringBoot启动加载器呢？

实例代码：

```
@Component
@Order(1)
public class FirstCommandLineRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // 给\u001B 给字段加颜色
        System.out.println("\u001B[32m >>> startup first runner<<<");
    }
}
```

启动效果即在SpringBoot启动完成之后，在控制台中输出 startup first runner

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820095619152.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)


小结：
1. 实现CommandLineRunner，重写Run方法
2. 如果有多个CommandLineRunner需要实现，则可以通过@Order来进行排序

### 2. ApplicationRunner
实现SpringBoot启动加载器的第二种方式，就是通过实现ApplicationRunner，先看下ApplicationRunner的源码：

```
@FunctionalInterface
public interface ApplicationRunner {
	void run(ApplicationArguments args) throws Exception;
}
```
可以看到，相比于CommandLineRunner，ApplicationRunner的run方法传入的是ApplicationArguments对象。

先看下ApplicationArguments这个接口定义：

```
public interface ApplicationArguments {
	// 获取源参数，即SpringBootApplication#run方法中传入的args; 这里的source就是SpringBoot对象
	String[] getSourceArgs();
	// 获取SpringBoot对象的properties，然后以Set集合返回
	Set<String> getOptionNames();
	// 判断是否包含指定name的属性
	boolean containsOption(String name);
	// 获取指定name属性的List集合
	List<String> getOptionValues(String name);
	
	List<String> getNonOptionArgs();
}
```
它的实现类是DefaultApplicationArguments，会在SpringApplication中的run方法里创建，这里统一在下文讲解原理时讲解。


实例代码：

```
@Order(1)
@Component
public class FirstApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("\u001B[32m >>> first application runner<<<");
    }
}
```
这里将Order值设置为1，观察下启动结果：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820095756552.png#pic_center)


对于ApplicationRunner进行，有如下小结：
1. 通过实现ApplicationRunner，实现run方法也可以实现SpringBoot启动加载器
2. 通过order值指定顺序，不过如果CommandLineRunner和ApplicationRunner的order值相同，则ApplicationRunner实现优先执行


### 3. SpringBoot启动加载器原理
其实SpringBoot启动加载器原理比较简单，在底层源码调用逻辑比较清除。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820100314418.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
在DefaultApplicationArguments里，有一个不可忽略的类：Source

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820101656164.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
该类在调用构造方法时，会调用父类SimpleCommandLinePropertySource的构造方法，继续跟进查看

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820101854768.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820102111888.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
最后，再调用callRunner方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820100603273.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200820100649546.png#pic_center)



## 总结
1. 在SpringBoot中实现启动加载器有两种方式，分别实现CommandLineRunner和ApplicationRunner；
2. 他们都可以通过@Order来进行优先级排序；
3. 在SpringBoot底层源码中，是先获取ApplicationRunner的bean，然后再去获取CommandLineRunner的bean，因此如果它们的@Order值相同，则是优先调用ApplicationRunner的run方法；
4. 对于CommandLineArgs命令行参数，是通过SimpleCommandLineArgsParser的parse方法来解析的，它会将args解析为key/value对，然后以CommandLineArgus对象返回；

