## 前言
前一章已经讲解了SpringBoot的SpringFactoriesLoader类的功能以及作用，即读取spring.factories文件中的工厂类，其中就包括了系统初始化器。在SpringBoot中，系统初始化器名称为ApplicationContextInitializer，它是一个接口，只定义了一个initialize方法。下面将详细介绍下SpringBoot的系统初始化器的原理以及作用，并且自定义一个系统初始化器，并在此基础上讲解下常见的使用场景。

**SpringBoot版本：5.2.1.RELEASE**

## 正文

### 1. 初始ApplicationContextInitializer
  先来看下ApplicationContextInitializer接口的官方注释。
  ```
  /**
 * Callback interface for initializing a Spring {@link ConfigurableApplicationContext}
 * prior to being {@linkplain ConfigurableApplicationContext#refresh() refreshed}.
 *
 * <p>Typically used within web applications that require some programmatic initialization
 * of the application context. For example, registering property sources or activating
 * profiles against the {@linkplain ConfigurableApplicationContext#getEnvironment()
 * context's environment}. See {@code ContextLoader} and {@code FrameworkServlet} support
 * for declaring a "contextInitializerClasses" context-param and init-param, respectively.
 *
 * <p>{@code ApplicationContextInitializer} processors are encouraged to detect
 * whether Spring's {@link org.springframework.core.Ordered Ordered} interface has been
 * implemented or if the @{@link org.springframework.core.annotation.Order Order}
 * annotation is present and to sort instances accordingly if so prior to invocation.
 *
 * @author Chris Beams
 * @since 3.1
 * @param <C> the application context type
 * @see org.springframework.web.context.ContextLoader#customizeContext
 * @see org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * @see org.springframework.web.servlet.FrameworkServlet#setContextInitializerClasses
 * @see org.springframework.web.servlet.FrameworkServlet#applyInitializers
 */

  ```
  
  注释意思可以总结为以下几点：
  1. ApplicationContextInitializer是一个用于在ConfigurableApplicationContext#refresh方法刷新之前，进行回调初始化ConfigurableApplicationContext的一个回调接口。
  2. ApplicationContextInitializer通常用于对WEB环境上下文进行编程式地初始化，例如通过ConfigurableApplicationContext#getEnvironment方法获取容器环境来注册属性源以及激活容器配置。
  3. ApplicationContextInitializer支持@Order注解，在调用初始化器之前系统会对其进行排序。

```
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * 初始化给定的applicationContext
	 */
	void initialize(C applicationContext);

}
```
首先ApplicationContextInitializer是一个接口，实现类都需要实现initialize方法，方法中传入的是需要初始化的Spring容器。对于接口中的**上界泛型定义**<C extends ConfigurableApplicationContext>，表示的是C必须是ConfigurableApplicationContext的子类。
相信读者对ConfigurableApplicationContext都不陌生，它是直接继承至ApplicationContext的一个接口，它在ApplicationContext的基础上还提供了可配置的能力，包括可配置environment、conversionService的能力。

所以对于ApplicationContextInitializer系统初始化器来说，是需要传入Spring容器，对容器进行初始化操作。

#### 1.1 在哪里注册的系统初始化器？
在SpringBoot源码底层，对于系统初始化器的注册是放在了SpringApplication的构造方法里，代码如下图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020060811395969.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
可以看到，在SpringApplication的构造方法中注册了系统初始化器以及监听器，监听器在SpringBoot中也同样重要，下章将会详细讲解。下面翻译下SpringApplication的注释方法：

> SpringApplication构造方法创建了一个SpringApplication实例。 应用程序上下文将从指定的primarySources数据源来加载Bean。可以在调SpringApplication#run（String ...）之前自定义实例。

对于getSpringFactoriesInstances(ApplicationContextInitializer.class)方法，在上一篇文章[深入SpringBoot源码学习之——SpringFactoriesLoader](https://blog.csdn.net/CoderBruis/article/details/106559304)已经介绍了的，getSpringFactoriesInstances方法就是去类路径的jar包或者是resources目录下加载并实例化META-INF/spring.factories的工厂类，**由于入参传的是ApplicationContextInitializer.class，所以只是加载和实例化xxxInitializer的工厂类，即所有的初始化器**，最终以集合的形式返回，然后将返回的集合赋值给SpringApplication的initializers集合。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608115548639.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
SpringBoot框架已经将spring.factories对应所有的初始化器加载到了SpringApplication类的initializers集合数据结构里，那SpringBoot在哪里调用SpringApplication的初始化器呢？

#### 1.2 在哪里调用的初始化器的initialize方法？
如果不知道系统从哪里开始调用的初始化器，可以先个初始化器的initialize方法打个断点，然后debug追中系统的调用栈路径即可，如下图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608120240341.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608120420379.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
可以看到调用路径为：LearnsbApplication#main——>SpringApplication#run——>SpringApplication#prepareContext——>SpringApplication#applyInitializers——>ContextIdApplicationContextInitializer#initialize，这样调用初始化器的地方一幕了然了。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608120713807.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608120932643.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
applyInitializers方法的注释已经注明了方法的作用，即：在SpringIOC容器进行refresh刷新之前，将所有的初始化器应用于SpringIOC容器。传入的context此时还没有进行刷新。

```
	protected void applyInitializers(ConfigurableApplicationContext context) {
		// getInitializers()方法会从SpringApplication中获取所有的已实例化的初始化器
		for (ApplicationContextInitializer initializer : getInitializers()) {
			// 获取初始化器的class类型
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
					ApplicationContextInitializer.class);
			// 对初始化器的class类型进行断言，判断其是否是ConfigurableApplicationContext的子类
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			// 调用初始化器的initialize方法
			initializer.initialize(context);
		}
	}
```

从源码中分析，发现其实系统初始化器的底层源码逻辑比较的简单，但是却很实用。

### 2. 自定义ApplicationContextInitializer
自定义一个系统初始化器首先当然得实现ApplicationContextInitializer接口，然后将逻辑写在initialize方法里。

```
@Order(1)
public class FirstInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
	// 下面将在initialize方法中获取ConfigurableEnviroment对象，并自定义一个map存入其中。
	// 待SpringBoot启动后，在SpringIOC容器中获取这个map。
    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        ConfigurableEnvironment environment = configurableApplicationContext.getEnvironment();
        Map<String, Object> map = new HashMap<>();
        map.put("coderbruis", "https://blog.csdn.net/CoderBruis");
        MapPropertySource mapPropertySource = new MapPropertySource("firstInitializer", map);
        environment.getPropertySources().addLast(mapPropertySource);
        System.out.println("run firstInitializer");
    }
}
```

定义了一个初始化器了，那么该如何让SpringBoot在启动时注册自定义的初始化器呢？首先当然可以在resouces目录下定义一个META-INF/spring.factories文件，如下：
```
# Initializer
org.springframework.context.ApplicationContextInitializer=com.bruis.learnsb.initializer.FirstInitializer
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608144657719.png#pic_center)
这样，SpringBoot就能识别到FirstInitializer这个自定义初始化器了。下面定义一个service并实现ApplicationContextAware，用于注入ApplicationContext对象。
```
@Component
public class TestService implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 将SpringIOC容器注册到该类的成员变量中
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String test() {
        // 从注册进Environment中的map中获取值
        return applicationContext.getEnvironment().getProperty("coderbruis");
    }
 }
```

然后写一个Test类，测试一下：
```
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBeanTest {
    @Autowired
    private TestService testService;

    @Test
    public void testInitializer() {
        System.out.println("Initializer: " + testService.test());
    }
}    
```

启动测试类，查看IDEA控制台输出结果：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608145449179.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)

除了通过META-INF/spring.factories这种方式注册系统初始化器，可以在application.properties以及SpringApplication#addInitializers方法来注册，这里简单介绍下：
1. application.properties
```
context.initializer.classes=com.bruis.learnsb.initializer.FirstInitializer
```

2. SpringApplication#addInitializers
```
@SpringBootApplication
public class LearnsbApplication {

    public static void main(String[] args) {
        // 注册系统初始化器
        SpringApplication springApplication = new SpringApplication(LearnjavaApplication.class);
        springApplication.addInitializers(new FirstInitializer());
        springApplication.run(args);
     }
}       
```

### 3. ApplicationContextInitializer使用场景
下面简单介绍下SpringBoot默认注册的前两个两个系统初始化器
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608150949748.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
**ConfigurationWarningsApplicationContextInitializer**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608151437524.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
ConfigurationWarningsApplicationContextInitializer#initialize方法的作用就是想SpringIOC容器中注册了一个后置处理器，作用就是对于一般配置错误在日志中作出警告，详细逻辑在postPrrocessBeanDefinitionRegistry方法。

**ContextIdApplicationContextInitializer**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608151804666.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
这个初始化器的作用是设置Spring应用上下文的ID。




## 总结
ApplicationContextInitializer是Spring给我们提供的一个在容器刷新之前对容器进行初始化操作的能力，如果有具体的需要在容器刷新之前处理的业务，可以通过ApplicationContextInitializer接口来实现。

> 觉得作者写的不错的点个赞，关注作者。
> 本文代码在 Github https://github.com/coderbruis/JavaSourceLearning 中已收录，更多源码文章以及源码在github中可以学习。