## 前言
先看下Spring官方文档对于事件以及监听器的解释与说明。
[监听器官方说明](https://docs.spring.io/spring-boot/docs/2.3.1.RELEASE/reference/html/spring-boot-features.html#boot-features-application-events-and-listeners)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200622101432321.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
总结起来就是几点：
1. 除了通常的Spring框架自带的事件例如：ContextRefreshedEvent，SpringApplication还会发送一些额外的事件。
2. 对于事件的监听，需要通过监听器来实现。在SpringBoot中，监听器可以通过三种方式来注册，
	① 通过SpringApplication.addListeners(...)
	② 通过SpringApplicationBuilder.listeners(...)
	③ 通过注册在META-INF/spring.factories中，示例为org.springframework.context.ApplicationListener=com.example.project.MyListener
3. 监听器不能以Bean的形式注册进SpringIOC容器中，因为监听器是在ApplicationContext上下文创建成功之前调用的。
4. Spring应用程序的内置事件会以以下顺序发送：
	① ApplicationStartingEvent
	② ApplicationEnvironmentPreparedEvent 
	③ ApplicationContextInitializedEvent
	④ ApplicationPreparedEvent
	⑤ ApplicationStartedEvent
	⑥ AvailabilityChangeEvent
	⑦ ApplicationReadyEvent
	⑧ AvailabilityChangeEvent
	⑨ ApplicationFailedEvent
	除了上面这些绑定在SpringApplication上的事件外，还有ContextRefreshedEvent和WebServerInitializedEvent、ServletWebServerInitializedEvent、ReactiveWebServerInitializedEvent等事件在ApplicationPreparedEvent和ApplicationStartedEvent事件之间发布。

在介绍完SpringBoot的官方文档后，下面看看SpringBoot源码中监听器是如何实现的。

## 正文

### 1. SpringBoot通过factories注册的监听器
在前面已经讲解过了SpringFactoriesLoader加载spring.factories的原理机制，大家都清楚了SpringFactoriesLoader会加载spring.factories中注册的**初始化器**、**监听器**、**后置处理器**等组件，在SpringApplication的构造方法中会通过JDK的反射工具实例化这些组件。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200622142233204.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
从源码中可以看到，SpringBoot在SpringApplication的构造方法中，会先获取ApplicationListener类型的监听器，并存进SpringApplication对象中，用于后续操作的调用。

下面看看ApplicationListener接口定义信息

```Java
/**
 * Interface to be implemented by application event listeners.
 *
 * <p>Based on the standard {@code java.util.EventListener} interface
 * for the Observer design pattern.
 *
 * <p>As of Spring 3.0, an {@code ApplicationListener} can generically declare
 * the event type that it is interested in. When registered with a Spring
 * {@code ApplicationContext}, events will be filtered accordingly, with the
 * listener getting invoked for matching event objects only.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.event.EventListener
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * Handle an application event.
	 * @param event the event to respond to
	 */
	void onApplicationEvent(E event);

}
```

注释大致意思为：
ApplicationListener接口是用于定义Spring应用程序监听器的接口，是基于Observer（观察者）设计模式的标准接口。从Spring3.0开始，ApplicationListener可以声明感兴趣的事件类型。

注册完spring.factories配置文件中的监听器后，SpringBoot框架是什么时候开始**获取监听器**然后**调用监听器**的监听的事件呢？下面来看SpringApplication#run方法。

**重点需要关注下ApplicationListener#onApplicationEvent方法，这个方法是广播器播放监听器时调用的方法，详情会在下文中讲解。**

**SpringApplication#run**
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020062217104411.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

**SpringApplication#getRunListeners**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200622171133438.png)
这个方法就是通过SpringFactoriesLoader去加载spring.factories配置文件中的文件指定的SpringApplicationRunListener类类型，这里获取的就是EventPublishingRunListener：
```
# Run Listeners
org.springframework.boot.SpringApplicationRunListener=\
org.springframework.boot.context.event.EventPublishingRunListener
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200622171356800.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
EventPublishingRunListener有什么作用？

#### 1.1 EventPublishingRunListener
下面先看下EventPublishingRunListener源码
```Java
/**
 * SpringApplicationRunListener 是用于发布 SpringApplicationEvent的。
 * SpringApplicationRunListener通过内部的ApplicationEventMulticaster在容器刷新之前来触发事件。
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 1.0.0
 */
public class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {

	private final SpringApplication application;

	private final String[] args;
	
	// 事件播放器
	private final SimpleApplicationEventMulticaster initialMulticaster;

	public EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
		// 获取SpringApplicaiton中实例化的事件监听器，添加进事件播放器中。listeners集合最终会存于AbstractApplicationEventMulticaster类中一内部类的一个Set集合中。
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * 通过事件播放器播放ApplicationStartingEvent事件
	 */
	@Override
	public void starting() {
		this.initialMulticaster.multicastEvent(new ApplicationStartingEvent(this.application, this.args));
	}

	/**
	 * 通过事件播放器播放ApplicationEnvironmentPreparedEvent事件
	 */
	@Override
	public void environmentPrepared(ConfigurableEnvironment environment) {
		this.initialMulticaster
				.multicastEvent(new ApplicationEnvironmentPreparedEvent(this.application, this.args, environment));
	}

	/**
	 * 通过事件播放器播放ApplicationContextInitializedEvent事件
	 */
	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {
		this.initialMulticaster
				.multicastEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
	}

	/**
	 * 通过事件播放器播放ApplicationPreparedEvent事件
	 */
	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		for (ApplicationListener<?> listener : this.application.getListeners()) {
			if (listener instanceof ApplicationContextAware) {
				((ApplicationContextAware) listener).setApplicationContext(context);
			}
			context.addApplicationListener(listener);
		}
		this.initialMulticaster.multicastEvent(new ApplicationPreparedEvent(this.application, this.args, context));
	}

	/**
	 * 通过事件播放器播放ApplicationStartedEvent事件
	 */
	@Override
	public void started(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context));
	}

	/**
	 * 通过事件播放器播放ApplicationReadyEvent事件
	 */
	@Override
	public void running(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context));
	}

	/**
	 * 通过事件播放器播放ApplicationFailedEvent事件
	 */
	@Override
	public void failed(ConfigurableApplicationContext context, Throwable exception) {
		ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
		if (context != null && context.isActive()) {
			// Listeners have been registered to the application context so we should
			// use it at this point if we can
			context.publishEvent(event);
		}
		else {
			// An inactive context may not have a multicaster so we use our multicaster to
			// call all of the context's listeners instead
			if (context instanceof AbstractApplicationContext) {
				for (ApplicationListener<?> listener : ((AbstractApplicationContext) context)
						.getApplicationListeners()) {
					this.initialMulticaster.addApplicationListener(listener);
				}
			}
			this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
			this.initialMulticaster.multicastEvent(event);
		}
	}

	private static class LoggingErrorHandler implements ErrorHandler {

		private static final Log logger = LogFactory.getLog(EventPublishingRunListener.class);

		@Override
		public void handleError(Throwable throwable) {
			logger.warn("Error calling ApplicationEventListener", throwable);
		}

	}

}
```
EventPublishingRunListener实现了SpringApplicationRunListener接口，该接口定义了用于监听**SpringApplication生命周期**的一系列接口方法。

```Java
public interface SpringApplicationRunListener {

	/**
	 * 在首次启动run方法时立即调用。 可用于非常早的初始化。
	 */
	default void starting() {
	}

	/**
	 * 在准备SpringIOC容器之后，创建好SpringIOC容器之前调用。
	 */
	default void environmentPrepared(ConfigurableEnvironment environment) {
	}

	/**
	 * 在创建和准备SpringIOC容器之后，但在加载源之前调用。
	 */
	default void contextPrepared(ConfigurableApplicationContext context) {
	}

	/**
	 * 在SpringIOC容器加载完之后但是在其刷新之前调用。
	 */
	default void contextLoaded(ConfigurableApplicationContext context) {
	}

	/**
	 * 上下文已刷新，应用程序已启动，但CommandLineRunner 和ApplicationRunner 尚未被调用。
	 */
	default void started(ConfigurableApplicationContext context) {
	}

	/**
	 * 在刷新应用程序上下文并已调用所有CommandLineRunner和ApplicationRunner后，在run方法完成之前立即调用。 
	 */
	default void running(ConfigurableApplicationContext context) {
	}

	/**
	 * 运行应用程序时发生故障时调用。
	 */
	default void failed(ConfigurableApplicationContext context, Throwable exception) {
	}

}
```

#### 1.2 Event事件
分析完EventPublishingRunListener之后，我们看下在事件Event。在SpringBoot中，EventObject定义了系统事件的顶级父类，其类定义中定义了一个成员变量source，用于存储**事件的初始源头**，对于SpringBoot的系统事件，源头就表示的是SpringIOC容器。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623064526838.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
可以看到SpringApplicationEvent定义的系统事件基本都围绕着SpringBoot框架的启动生命周期。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623064538927.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623064623754.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)


而在EventPublishingRunListener中播放的事件，就是SpringBoot系统事件发送顺序，顺序如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623063407599.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

### 2. SpringBoot中的事件广播器
在EventPublishingRunListener内部中，有一个名为SimpleApplicationEventMulticaster的成员变量，它是一种广播器，专门用于管理SpringBoot中的监听器，并广播指定的类型事件。SpringBoot定义了一个事件广播器接口，用于抽象播放器的接口行为，该接口名称为：ApplicationEventMulticaster，看下它的实现类的层次结构：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623065336310.png)
因此可以知道，在SpringBoot中核心广播器就是SimpleApplicationEventMulticaster，所有管理监听器播放事件的工作都由SimpleApplicationEventMulticaster来完成。下面先来看下接口ApplicationEventMulticaster源码及注释：

```Java
/**
 * ApplicationEventMulticaster接口的实现类用于管理多个ApplicationListener监听器，并对事件进行广播
 *
 * ApplicationEventMulticaster实际上就是作为Spring真正播放事件的一个代理。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ApplicationListener
 */
public interface ApplicationEventMulticaster {

	/**
	 * 添加一个监听器用于触发所有事件
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 添加一个监听器的bean用于触发所有事件
	 */
	void addApplicationListenerBean(String listenerBeanName);

	/**
	 * 从通知列表中移除一个监听器
	 */
	void removeApplicationListener(ApplicationListener<?> listener);

	/**
	 * 从通知列表中移除一个监听器的bean
	 */
	void removeApplicationListenerBean(String listenerBeanName);

	/**
	 * 移除所有注册在广播器上的监听器。
	 */
	void removeAllListeners();

	/**
	 * 在适当的监听器上播放ApplicationEvent事件
	 */
	void multicastEvent(ApplicationEvent event);

	/**
	 * 在适当的监听器上播放ApplicationEvent事件
	 * 可以通过eventType来过滤需要播放的事件类型。
	 */
	void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType);

}
```

接着看AbstractApplicationEventMulticaster抽象类源码，由于AbstractApplicationEventMulticaster方法较多，就挑几个核心方法进行讲解并学习。

AbstractApplicationEventMulticaster类注释可以总结成以下几点：
1. ApplicationEventMulticaster接口的抽象实现，提供了基本的监听器注册功能。
2. 默认情况下，框架不允许同一监听器有多个实例，因为监听器会被存放到set集合中。
3. 通常ApplicationEventMulticaster接口的multicastEvent方法是留给子类SimpleApplicationEventMulticaster实现，它通过multicastEvent方法将所有事件广播到所有已注册的监听器，并在调用线程中播放事件。

下面是AbstractApplicationEventMulticaster类的定义以及成员变量：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623094604505.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
AbstractApplicationEventMulticaster不仅实现了ApplicationEventMulticaster，还实现了BeanClassLoaderAware以及BeanFactoryAware。在SpringBoot中，实现了XXAware，都会获得注入XX的能力，比如常见的ApplicationContextAware，就是拥有注入ApplicationContext的能力；所以AbstractApplicationEventMulticaster即拥有了注入BeanClass和BeanFactory的能力。

成员变量中，defaultRetriever是一个内部类，作为一个存储监听器实例的数据结构，其底层通过Set集合来存储监听器。retrieverCache是一个ConcurrentHashMap实现的内部缓存，用于在下次获取监听器时直接能从缓存中获取。retrievalMutex是一个Object对象，用作对象锁。

AbstractApplicationEventMulticaster已经把监听器存储好了，就等着广播器进行事件广播，而广播的方法就是视SimpleApplicationEventMulticaster#multicastEvent方法。

```Java
@Override
	public void multicastEvent(ApplicationEvent event) {
		// 广播事件
		multicastEvent(event, resolveDefaultEventType(event));
	}

	@Override
	public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
		Executor executor = getTaskExecutor();
		// 获取监听器集合
		for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
			if (executor != null) {
				// 如果有设置线程池，则调用线程池中的线程来调用监听
				executor.execute(() -> invokeListener(listener, event));
			}
			else {
				invokeListener(listener, event);
			}
		}
	}
```
下面看下广播器是如何获取到监听器集合的。

#### 2.1 AbstractApplicationEventMulticaster#getApplicationListeners
既然监听器存放在了播放器里，那么播放器肯定会提供一个获取监听器的方法，那么这个方法就是getApplicationListeners。
**AbstractApplicationEventMulticaster#getApplicationListeners**
```Java
protected Collection<ApplicationListener<?>> getApplicationListeners(
			ApplicationEvent event, ResolvableType eventType) {

		// 获取事件的源，也就是SpringApplication对象。
		Object source = event.getSource();
		// 获取SpringApplication类对象
		Class<?> sourceType = (source != null ? source.getClass() : null);
		ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

		// 从缓存中获取监听器
		ListenerRetriever retriever = this.retrieverCache.get(cacheKey);
		if (retriever != null) {
			return retriever.getApplicationListeners();
		}

		if (this.beanClassLoader == null ||
				(ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader) &&
						(sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
			// 异步地构建以及缓存ListenerRetriever
			synchronized (this.retrievalMutex) {
				retriever = this.retrieverCache.get(cacheKey);
				if (retriever != null) {
					// 从缓存中获取监听器
					return retriever.getApplicationListeners();
				}
				retriever = new ListenerRetriever(true);
				// 获取监听器集合
				Collection<ApplicationListener<?>> listeners =
						retrieveApplicationListeners(eventType, sourceType, retriever);
				// 将存放监听器的retriever存入ConcurrentHashMap中
				this.retrieverCache.put(cacheKey, retriever);
				return listeners;
			}
		}
		else {
			// 没有ListenerRetriever 缓存就没必要进行异步获取
			return retrieveApplicationListeners(eventType, sourceType, null);
		}
	}
```

看下ListenerRetriever#retrieveApplicationListeners源码
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020062311204527.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
类ListenerRetriever的成员变量applicationListeners实际存储的就是spring.factories文件中注册的监听器。那问题来了，spring.factories文件里注册的监听器是什么时候注册进applicationListeners集合中的？

目光得回到EventPublishingRunListener的构造方法中，
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623113045429.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623113524425.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
而EventPublishingRunListener构造方法是通过反射来方法来调用的。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623113906400.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
下图为SpringBoot如何将监听器添加进EventPublishingRunListener中的简易流程图。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623120430115.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
除此之外，还有一个重要方法AbstractApplicationEventMulticaster#supportsEvent，该方法有两个重载方法
```Java
supportsEvent(
			ConfigurableBeanFactory beanFactory, String listenerBeanName, ResolvableType eventType)
```
```Java
supportsEvent(Class<?> listenerType, ResolvableType eventType)
```

二者区别在于第一个supportsEvent方法需要传入beanFactory，用作于获取bean的申明类型。在尝试实例化bean定义的侦听器之前，请先检查其通用声明的事件类型，以对其进行早期过滤。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623141446161.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
这里有一个比较陌生的类，就是ResolvableType，这是啥类？就如其名字，一个ResolvableType对象就代表着一种可解决的java类型，我们可以通过ResolvableType对象可以获取类型携带的信息 ，包括父类型、接口类型、泛型类型等。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623141319889.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)


### 3. SpringBoot中的事件广播器如何播放监听器中的事件？
上文已经分别讲解了事件、监听器、广播器，那么三者之间的调用关系又是怎样的关系呢？说的再好不如debug看下调用栈调用流程来的一清二楚。断点就打在SimpleApplicationEventMulticaster#multicastEvent方法中。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623171859184.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623172201529.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
由于此处EVENT_TYPES类型包含了ApplicationStartingEvents.class，所以是符合类型的事件，因此会把LogginApplicationListener存放进Set集合中。
最终可以看到，对于starting方法，拥有感兴趣的ApplicationStartingEvents事件类型的监听器有下图的四种
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623172602344.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
最终，会在SimpleApplicationEventMulticaster#doInvokeListener中进行监听器的onApplicationEvent调用，最终播放监听器。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623173146188.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
在SimpleApplicationEventMulticaster#multicastEvent方法中，由getApplicationListeners方法返回了拥有感兴趣事件类型的监听器实例集合，上图已经证实，在SpringBoot中对ApplicationStartingEvent事件类型感兴趣的监听器有四种。
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020062317343530.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
因此SpringBoot的广播器会依次播放这四种监听器的onApplicationEvent方法。

从事件调用发起到最终监听器播放事件，可以总结为下图的流程：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623173818841.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

那么经过debug分析，事件、监听器、广播器之间的关系就已经变得明朗起来了。

#### 3.1 事件、监听器、广播器之间的关系
对于广播器SimpleApplicationEventMulticaster，其作用就是存储监听器、根据具体的事件筛选出感兴趣的监听器，然后播放监听器；而监听器的onApplicationEvent方法会传入事件，来进行具体的播放逻辑。当然，广播器是作为EventPublishingRunListener的成员变量。因而在SpringApplication的生命周期中，通过广播器会分别调用对应感兴趣事件的监听器。

### 4. 模仿SpringBoot，实现自定义的事件与监听器
首先，定义一个天气事件抽象类
```Java
public abstract class WeatherEvent {
    public abstract String getWeather();
}
```
定义两个天气事件
```Java
public class RainEvent extends WeatherEvent{
    @Override
    public String getWeather() {
        return "rain...";
    }
}
```
```Java
public class SnowEvent extends WeatherEvent{
    @Override
    public String getWeather() {
        return "snow...";
    }
}
```

接着定义一个事件监听器
```Java
public interface WeatherListener {
	// 类似于SpringBoot监听器的onApplicationEvent方法
    void onWeatherEvent(WeatherEvent event);
}
```

有了监听器接口，那么就要定义实现类
```Java
@Component
public class RainListener implements WeatherListener{
    @Override
    public void onWeatherEvent(WeatherEvent event) {
        if (event instanceof RainEvent) {
            System.out.println("hello " + event.getWeather());
        }
    }
}
```
```Java
@Component
public class SnowListener implements WeatherListener {
    @Override
    public void onWeatherEvent(WeatherEvent event) {
        if (event instanceof SnowEvent) {
            System.out.println("hello " + event.getWeather());
        }
    }
}
```
可以看到，SnowListener和RainListener类的onWeatherEvent方法会依据对应的天气Event进行过滤。

定义完了监听器以及事件之后，就还差广播器以及调用广播器播放事件的XXRunListener了。先定义一个事件广播器，包含了基础的添加监听器、移除监听器、播放事件的功能。
```Java
public interface EventMulticaster {
    void multicastEvent(WeatherEvent event);
    void addListener(WeatherListener weatherListener);
    void removeListener(WeatherListener weatherListener);
}
```
抽象广播器类
```Java
@Component
public abstract class AbstractEventMulticaster implements EventMulticaster{

	// 自动注入所有的天气监听器
    @Autowired
    private List<WeatherListener> listenerList;

	// 播放天气事件
    @Override
    public void multicastEvent(WeatherEvent event) {
        doStart();
        // 遍历监听器，然后播放天气事件
        listenerList.forEach(i -> i.onWeatherEvent(event));
        doEnd();
    }

    @Override
    public void addListener(WeatherListener weatherListener) {
        listenerList.add(weatherListener);
    }

    @Override
    public void removeListener(WeatherListener weatherListener) {
        listenerList.remove(weatherListener);
    }
	// 记录广播器开始调用
    abstract void doStart();
	// 记录广播器结束调用
    abstract void doEnd();
}
```

定义完了广播器，就运行广播器的XXRunListener了，下面定义一个WeatherRunListener，用于播放感兴趣的事件。
```Java
@Component
public class WeatherRunListener {

    @Autowired
    private WeatherEventMulticaster eventMulticaster;

    public void snow() {
        eventMulticaster.multicastEvent(new SnowEvent());
    }

    public void rain() {
        eventMulticaster.multicastEvent(new RainEvent());
    }

}
```

编写测试类测试
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200623224008504.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
### 5. 总结
通过SpringBoot底层源码的分析以及模仿SpringBoot实现的自定义事件广播器，相信大家对广播器、监听器、事件以及调用广播器的XXRunListener运行监听类有了更深入的理解了。另外通过SpringBoot底层源码的分析后可以了解到，SpringBoot事件监听机制用的也是非常的广泛，除了文中介绍的EventPublishingRunListener监听器监听SpringApplication生命周期，还有ConfigFileApplicationListener会监听onApplicationEnvironmentPreparedEvent事件来加载配置文件application.properties的环境变量，所以说SpringBoot事件监听机制是非常重要的一个知识点，在SpringBoot的面试中也会经常面试到。

> 觉得作者写的不错的点个赞并关注作者。
本文代码在面的地址中已收录，文中出现的源码以及流程图都已收录进github中。
> [https://github.com/coderbruis/JavaSourceLearning](https://github.com/coderbruis/JavaSourceLearning)