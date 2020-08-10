<!-- TOC -->
 - [前言](#前言)
 - [正文](#正文)
     - [1. AbstractConfiguredSecurityBuilder中安全配置类](#1-abstractconfiguredsecuritybuilder中安全配置类)    
	 - [2. AbstractConfiguredSecurityBuilder的doBuild()方法](#2-abstractconfiguredsecuritybuilder的dobuild方法)
	 - [3. WebSecurity中的performBuild()方法](#3-websecurity中的performbuild方法)
- [总结](#总结)
- [相关文章](#相关文章)
<!-- /TOC -->
## 前言
相信了解过SpringSecurity或者是OAuth2的读者，会发现网上会有非常多的相关文章，或是纯概念的，或是带有demo的，无论是哪种类型的文章，本人去阅读之后，对于整个框架的概念还是一知半解，也仅仅是实现了某些功能、某些效果而已，若遇到某些问题时无从下手，只能去百度去Google。这是因为对于SpringSecurity和OAuth2的知识没有一个整体概念的把握，知识体系没有形成系统，遂决定写一个关于SpringSecurity和OAuth2的系列专栏，在建造自己知识体系的同时还希望能帮助有同样困惑的同学。

============== 分割线 ==============

上一章已经讲解到了SpringSecurity用到了建造者模式来建造FilterChainProxy。本片就来详细分析下WebSecurity的核心逻辑以及AbstractConfiguredSecurityBuilder的doBuild()方法。

## 正文

### 1. AbstractConfiguredSecurityBuilder中安全配置类
SpringSecurity通过SecurityConfigurer来建造FilterChainProxy，建造前还需要进行配置。因此AbstractConfiguredSecurityBuilder还需要注入配置组件SecurityConfigurer，初始化配置组件SecurityConfigurer，调用SecurityConfigurer的configure方法。

在AbstractConfiguredSecurityBuilder类中，看下安全配置类的定义：‘

```
private final LinkedHashMap<Class<? extends SecurityConfigurer<O, B>>, List<SecurityConfigurer<O, B>>> configurers = new LinkedHashMap<Class<? extends SecurityConfigurer<O, B>>, List<SecurityConfigurer<O, B>>>();
```
这是定义的安全配置器的子类Map集合，这个configurers就是用于初始化以及配置FilterChainProxy中的filters用的。Map集合中，Key是SecurityConfigurer的子类的Class类型，Value是SecurityConfigurer的list集合。

作为一个成员变量，自然会有方法从外部注入安全配置类。在AbstractConfiguredSecurityBuilder的类中，定义了add方法。

```
	private <C extends SecurityConfigurer<O, B>> void add(C configurer) throws Exception {
		Assert.notNull(configurer, "configurer cannot be null");
		// 获取安全配置类的Class类型
		Class<? extends SecurityConfigurer<O, B>> clazz = (Class<? extends SecurityConfigurer<O, B>>) configurer
				.getClass();
		// 同步去操作安全配置类集合
		synchronized (configurers) {
			// 查看建造状态是否是已经配置
			if (buildState.isConfigured()) {
				throw new IllegalStateException("Cannot apply " + configurer
						+ " to already built object");
			}
			// 如果allowConfigurersOfSameType为true，则从configurers集合中获取clazz类型的安全配置类集合
			List<SecurityConfigurer<O, B>> configs = allowConfigurersOfSameType ? this.configurers
					.get(clazz) : null;
			if (configs == null) {
				// 初始化安全配置类结合
				configs = new ArrayList<SecurityConfigurer<O, B>>(1);
			}
			// 将安全配置类添加至configs的list集合中
			configs.add(configurer);
			// 以clazz为key，configs为value存入configurers的LinkedHashMap集合中
			this.configurers.put(clazz, configs);
			if (buildState.isInitializing()) {
				this.configurersAddedInInitializing.add(configurer);
			}
		}
	}

```

通过IDEA来查看下哪些地方调用了add方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020080918440196.png)

看下apply方法
```
	// 传入的C是SecurityConfigurerAdapter的子类，
	public <C extends SecurityConfigurerAdapter<O, B>> C apply(C configurer)
			throws Exception {
		// 传入objectPostProcessor，该对象用于创建各种“实例”，具体什么作用下问会讲解，请留意
		configurer.addObjectPostProcessor(objectPostProcessor);
		// 将当前对象设置为建造者
		configurer.setBuilder((B) this);
		// 调用add方法，向configurers集合中添加configurer
		add(configurer);
		return configurer;
	}

```

继续查看apply方法有哪些地方调用了的
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200809224904457.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
图上的HttpSecurity的getOrApply方法值得注意一下，查看其方法
```
	private <C extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>> C getOrApply(
			C configurer) throws Exception {
		// 从configurers集合中获取安全配置类
		C existingConfig = (C) getConfigurer(configurer.getClass());
		if (existingConfig != null) {
			// 如果存在则直接返回该安全配置类
			return existingConfig;
		}
		// 如果不存在则调用apply方法去应用该安全配置类，并缓存到configurers集合中
		return apply(configurer);
	}

```

getOrApply方法主要是从configurers集合中获取配置类，如果存在则直接返回，否则则应用该配置类。

继续查看getOrApply有哪些地方在调用。这下终于看到了安全配置类了。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200809225003886.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
这里有一个问题值得思考，这些配置类到底是干嘛用的？这里就以ExpressionUrlAuthorizationConfigurer配置类为例，看下其类内部configure方法逻辑。

找了一圈，发现configure的实现是在ExpressionUrlAuthorizationConfigurer的抽象父类AbstractInterceptUrlConfigurer定义的。

```
	@Override
	public void configure(H http) throws Exception {
		// 创建元数据，该抽象方法由ExpressionUrlAuthorizationConfigurer定义，返回一个ExpressionBasedFilterInvocationSecurityMetadataSource对象
		FilterInvocationSecurityMetadataSource metadataSource = createMetadataSource(http);
		// 如果配置失败，则数据源配置失败
		if (metadataSource == null) {
			return;
		}
		// 创建一个FilterSecurityInterceptor对象
		FilterSecurityInterceptor securityInterceptor = createFilterSecurityInterceptor(
				http, metadataSource, http.getSharedObject(AuthenticationManager.class));
		if (filterSecurityInterceptorOncePerRequest != null) {
			securityInterceptor
					.setObserveOncePerRequest(filterSecurityInterceptorOncePerRequest);
		}
		// 通过objectPostProcessor创建一个securityInterceptor实例对象
		securityInterceptor = postProcess(securityInterceptor);
		http.addFilter(securityInterceptor);
		// 将实例对象存入SharedObject缓存中
		http.setSharedObject(FilterSecurityInterceptor.class, securityInterceptor);
	}

```

从ExpressionUrlAuthorizationConfigurer的抽象父类AbstractInterceptUrlConfigurer可以看出，configure方法中调用了一个postProcess方法，该方法用于生成FilterSecurityInterceptor对象，在本系列文章前面第一章以及列出来的FilterChainProxy拦截器链对象，可以知道FilterSecurityInterceptor对象就属于FilterChainProxy拦截器链中的对象，并且是处在最后一个位置。

到此处，安全配置类的作用已经提现出来了，就是向sharedObject中添加过滤器，并最终注入到FilterChainProxy中。

### 2. AbstractConfiguredSecurityBuilder的doBuild()方法
随着configurers集合元素的注入，下面就是进行建造工作，调用doBuild()方法。

```
	@Override
	protected final O doBuild() throws Exception {
		synchronized (configurers) {
			// 设置建造状态为初始化中
			buildState = BuildState.INITIALIZING;

			// 进行初始化前的工作
			beforeInit();
			// 初始化
			init();

			// 设置建造状态为配置中
			buildState = BuildState.CONFIGURING;

			// 配置前的工作
			beforeConfigure();
			// 调用配置
			configure();

			// 设置建造状态为建造中
			buildState = BuildState.BUILDING;

			// 执行建造核心逻辑
			O result = performBuild();

			// 设置建造状态为已建造
			buildState = BuildState.BUILT;

			return result;
		}
	}

```
beforeInit()和beforeConfigure()是一个空方法体，没有逻辑。
```
	protected void beforeInit() throws Exception {
	}
	protected void beforeConfigure() throws Exception {
	}
```

```
	private void init() throws Exception {
		// 调用getConfigurers()方法获取this.configurers的所有value值，并以List集合的形式返回
		Collection<SecurityConfigurer<O, B>> configurers = getConfigurers();
		// 遍历configurers，并依次调用安全配置类的init方法
		for (SecurityConfigurer<O, B> configurer : configurers) {
			// 调用安全配置类的init初始化方法
			configurer.init((B) this);
		}

		for (SecurityConfigurer<O, B> configurer : configurersAddedInInitializing) {
			configurer.init((B) this);
		}
	}
```

这需要注意的是，init和configure方法是有接口SecurityConfigurer接口定义的，但其实现以由SecurityConfigurerAdapter这个抽象的适配器类实现了，所以最终的安全配置类可重写init()和configure()，也可以不重写。所以可以发现，很多安全配置类是没有重写init()方法的。


**接着就是configure()方法的调用**

```
	private void configure() throws Exception {
		// 调用getConfigurers()方法获取this.configurers的所有value值，并以List集合的形式返回
		Collection<SecurityConfigurer<O, B>> configurers = getConfigurers();

		for (SecurityConfigurer<O, B> configurer : configurers) {
			// 遍历configurers，然后依次调用其configure方法
			configurer.configure((B) this);
		}
	}
```

在前面已经已经以ExpressionUrlAuthorizationConfigurer为例，去查看安全配置类configure方法的作用，都知道安全配置类的configure方法是用于将过滤器添加到sharedObject缓存中。

经过init()和configure()方法的执行，以及可以开始进行建造工作了，因而调用performBuild()方法执行建造过程。

```
	protected abstract O performBuild() throws Exception;
```
可以看到在AbstractConfiguredSecurityBuilder中，performBuild是以抽象方法的形式存在的，所以实现逻辑都在其子类中。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200810112226524.png)

这里HttpSecurity和WebSecurity类名这么像，有什么异同呢？

> HttpSecurity

HttpSecurity允许为特定的http请求配置基于Web的安全特性。默认情况下，HttpSecurity将应用于所有请求，但是如果需要限制请求，则可以通过RequestMatcher或其他类似方法来进行限制。

> WebSecurity

而WebSecurity是通过WebSecurityConfiguration创建的，用于去创建类FilterChainProxy。在SpringSecurity中，WebSecurity通过HttpSecurity来对某些请求进行拦截限制。

> 区别

WebSecurity用于建造FilterChainProxy，WebSecurity是包含HttpSecurity的一个更大的概念，而HttpSecurity仅是用于建造FilterChainProxy中的一个SecurityFilterChain。


### 3. WebSecurity中的performBuild()方法
WebSecurity重写了AbstractConfiguredSecurityBuilder的perfomBuild()方法，核心逻辑如下：

```
	@Override
	protected Filter performBuild() throws Exception {
		Assert.state(
				!securityFilterChainBuilders.isEmpty(),
				() -> "At least one SecurityBuilder<? extends SecurityFilterChain> needs to be specified. "
						+ "Typically this done by adding a @Configuration that extends WebSecurityConfigurerAdapter. "
						+ "More advanced users can invoke "
						+ WebSecurity.class.getSimpleName()
						+ ".addSecurityFilterChainBuilder directly");
		// 获取chain大小
		int chainSize = ignoredRequests.size() + securityFilterChainBuilders.size();
		// 设置安全过滤器链集合
		List<SecurityFilterChain> securityFilterChains = new ArrayList<>(
				chainSize);
		// 遍历igoredRequests集合，该集合表示存储着忽略拦截的请求
		for (RequestMatcher ignoredRequest : ignoredRequests) {
			securityFilterChains.add(new DefaultSecurityFilterChain(ignoredRequest));
		}
		for (SecurityBuilder<? extends SecurityFilterChain> securityFilterChainBuilder : securityFilterChainBuilders) {
			// 先调用securityFilterChainBuilder执行build()方法，然后添加进securityFilterChains集合中。之列build()方法是去迭代执行doBuild()逻辑。
			securityFilterChains.add(securityFilterChainBuilder.build());
		}
			
		// 创建一个FilterChainProxy对象，传入一个securityFilterChains对象
		FilterChainProxy filterChainProxy = new FilterChainProxy(securityFilterChains);
		if (httpFirewall != null) {
			// 设置http防火墙属性
			filterChainProxy.setFirewall(httpFirewall);
		}
		filterChainProxy.afterPropertiesSet();

		Filter result = filterChainProxy;
		if (debugEnabled) {
			logger.warn("\n\n"
					+ "********************************************************************\n"
					+ "**********        Security debugging is enabled.       *************\n"
					+ "**********    This may include sensitive information.  *************\n"
					+ "**********      Do not use in a production system!     *************\n"
					+ "********************************************************************\n\n");
			result = new DebugFilter(filterChainProxy);
		}

		// 执行一个线程
		postBuildAction.run();
		return result;
	}

```

在performBuild()方法中，会执行securityFilterChainBuilder的build()方法，该方法又会去迭代执行doBuild()方法，最终返回一个SecurityFilterChain对象。


综上，本文对WebSecurity的建造逻辑进行了概括性的讲解，更加深入的内容还需要进行代码debug跟踪深入查看，这样才能加深对于WebSecurity的理解。

## 总结
- 安全配置类是在HttpSecurity、XXConfigurerAdapter等类中进行添加
- WebSecurity和HttpSecurity区别在于，WebSecurity是用于建造FilterChainProxy的，它是包含HttpSecurity；而HttpSecurity是用于对请求进行限制，同时还用于建造DefaultSecurityFilterChain
- 安全配置类主要是通过configure方法向sharedObject缓存对象中添加过滤器，并最终添加进FilterChainProxy过滤器链中
- WebSecurity建造FilterChainProxy的核心逻辑可以笼统的分为三步：安全配置了初始化、安全配置类configure的调用、performBuild的调用

## 相关文章
- [从零开始系统学习SpringSecurity和OAuth2（一）—— 初识SpringSecurity](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%20%E5%88%9D%E8%AF%86SpringSecurity.md)
- [从零开始系统学习SpringSecurity和OAuth2（二）—— 安全过滤器FilterChainProxy](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20%E5%AE%89%E5%85%A8%E8%BF%87%E6%BB%A4%E5%99%A8FilterChainProxy.md)