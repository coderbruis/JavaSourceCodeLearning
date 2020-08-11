<!-- TOC -->
- [前言](#前言)
- [在web容器中初始化spring容器](#在web容器中初始化spring容器)
    - [SpringIOC容器和Web容器](#springioc容器和web容器)
	- [ContextLoaderListener](#contextloaderlistener)
- [总结](#总结)
<!-- /TOC -->

## 前言

继上一篇文章深入了解了在普通的Spring应用程序中如何创建并启动SpringIOC容器的，下面，深入学习一下在web容器中，是如何创建、初始化并启动SpringIOC容器的。

**Spring版本：Spring5-0-3**

## 在web容器中初始化spring容器

相信读者都能够用IDE搭建出基于Spring的web应用程序，例如SSM框架（不会的百度一下吧，这里就不讲解了）。

在搭建SSM框架的时候，引入SpringMVC配置文件有两种方式，如下：

**方式一：**

在web容器初始化过程中，会在WEB-INF文件夹下寻找名为[servlet-name]-servlet.xml的配置文件作为SpringMVC的配置文件，如下springMVC的配置文件就是放在WEB-INF下名为dispatcherServlet-servlet.xml的配置文件
```
<servlet>
    <servlet-name>dispatcherServlet</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>dispatcherServlet</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```
**方式二：**

直接在类路径下配置SpringMVC配置文件。
```
<servlet>  
    <servlet-name>DispatcherServlet</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>  
    <init-param>  
        <param-name>contextConfigLocation</param-name>  
        <param-value>classpath:springmvcconfig.xml</param-value>  
    </init-param>  
    <load-on-startup>1</load-on-startup>
    </servlet>  
    <servlet-mapping>  
        <servlet-name>DispatcherServlet</servlet-name>  
        <url-pattern>/</url-pattern>
    </servlet-mapping>  
```

在web.xml下可以看到DispatcherServlet是SpringMVC的核心，下面将重点讲解DispatcherServlet的原理以及作用。

### SpringIOC容器和Web容器
SpringIOC容器是如何在Web环境中被加载并起作用的？SpringIOC容器是何时创建的？何时初始化的？

**首先，必须知道的一点是：SpringIOC是一个独立的模块，它并不是直接在Web容器中发挥作用的。**

如果要在Web容器中使用IOC容器，需要Spring为IOC设计一个启动过程，把IOC容器导入，并将Web容器中建立起来。具体来说，SpringIOC容器的启动过程是和Web容器的启动过程集成在一起的。在这个启动过程中，一方面处理Web容器的启动，另一方面处理SpringIOC容器的启动过程，对于SpringIOC容器的启动过程需要设计特定的Web容器拦截器，将SpringIOC容器集成到Web容器中，并将其初始化。完成了上述过程，SpringIOC容器才能正常工作，而SpringMVC是建立在IOC容器的基础上的，这样才能建立起MVC框架的运行机制，从而响应从容器传递的HTTP请求。


启动Spring的容器，让项目一启动，就启动SpringIOC容器，下面是web.xml配置文件中的配置信息：
```
<context-param>
    <param-name>contextConfigLocation</param-name>
    <!-- spring的配置文件 -->
    <param-value>classpath:applicationContext.xml</param-value>
</context-param>
<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>
```

### ContextLoaderListener

**ContextLoaderListener作为一个监听器，不仅负责完成IOC容器在Web容器中的启动工作，并且还是SpringMVC的启动类。**
IOC容器的启动过程就是建立Spring上下文的过程，该上下文是与ServletContext相伴而生的，同时也是IOC容器在Web应用环境中的具体表现之一。由ContextLoaderListener启动的上下文为根上下文。在根上下文的基础上，还有一个与Web MVC相关的上下文应用来保存控制器（DispatcherServlet）需要的MVC对象，**作为根上下文的子上下文**，构成一个层次化的上下文体系，这个与Web MVC相关的上下文——WebApplicationContext。在Web容器中启动Spring应用程序时，首先建立根上下文，然后建立这个上下文体系，这个上下文体系的建立是由ContextLoader来完成的。简单点说，ContextLoaderListener的作用就是启动Web容器时，自动装配ApplicationContext的配置信息。

先看看Web程序启动到SpringIOC容器创建和初始化的整个过程。
[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-1df3mrUq-1596181541865)(https://note.youdao.com/yws/api/personal/file/9755412D703C4DE287B26AF2396E57BD?method=download&shareKey=52783ceb34f405ad47140c22da34275e)]

结合着时序图，再去调试源码，思路会清晰很多。

ContextLoaderListener.class
```
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

	public ContextLoaderListener() {
	}

	public ContextLoaderListener(WebApplicationContext context) {
		super(context);
	}

	/**
	 * 初始化WebApplicationContext
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		initWebApplicationContext(event.getServletContext());
	}

	/**
	 * 关闭WebApplicationContext
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		closeWebApplicationContext(event.getServletContext());
		ContextCleanupListener.cleanupAttributes(event.getServletContext());
	}

}

```
这里的ContextLoaderListener是Spring的类，但实现了ServletContextListener接口。这个接口是Servlet API中定义的，提供了与Servlet生命周期结合的回调，也就是说Servlet调用contextInitialized()方法初始化容器时，会回调ContextLoaderListener中实现的contextInitialized()方法，Servlet中的contextDestroyed()方法也同理。观察源码可知，在Web容器中，建立WebApplicationContext的过程是在contextInitialized()方法中完成的。

ContextLoader.class
```
public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
       ...
    // 判断在web容器中是否存在WebApplicationContext，因为在配置中只允许申明一次ServletContextListener，多次声明会扰乱Spring的执行逻辑。
    if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
		throw new IllegalStateException(
				"Cannot initialize context because there is already a root application context present - " +
				"check whether you have multiple ContextLoader* definitions in your web.xml!");
	}   

	try {
		// 创建WebApplicationContext，将上下文存储在本地实例变量中，以保证它在ServletContext关闭时可用。
		if (this.context == null) {
			this.context = createWebApplicationContext(servletContext);
		}
		
		// 确保该容器是可配置的web容器
		if (this.context instanceof ConfigurableWebApplicationContext) {
			ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
			if (!cwac.isActive()) {
				// 上下文尚未刷新 - >提供诸如设置父上下文，设置应用程序上下文ID等服务
				if (cwac.getParent() == null) {
				    // 在Web容器中建立起双亲IOC容器
					ApplicationContext parent = loadParentContext(servletContext);
					cwac.setParent(parent);
				}
				// 经过上面两个步骤，现在开始配置并初始化WebApplicationContext。
				configureAndRefreshWebApplicationContext(cwac, servletContext);
			}
		}
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		if (ccl == ContextLoader.class.getClassLoader()) {
			currentContext = this.context;
		}
		else if (ccl != null) {
			currentContextPerThread.put(ccl, this.context);
		}
		...

		return this.context;
	}
	catch (RuntimeException ex) {
        ...		    
	}
}
```
由ContextLoader的源码可知，SpringIOC的载入过程是在ContextLoader类的initWebApplicationContext()方法中完成的。

这里还要介绍一个重要的接口——WebApplicationContext
```
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * 用于在成功启动时将根WebApplicationContext绑定到的Context属性。
	 */
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

	/**
	 * 获取Web容器的上下文，也就是ServletContext对象，这里相当于提供了一个Web容器级别的全局变量。
	 */
	ServletContext getServletContext();

}
```
而WebApplicationContext接口是由XMLWebApplicationContext来实现具体的功能，然后再通过ApplicationContext接口与BeanFactory接口对接，完成Spring容器的功能。然而对于具体的一些Spring容器的实现都是在AbstractRefreshableWebApplicationContext中完成的，这一点和**上篇**讲解的AbstractRefreshableConfigApplicationContext功能类似。initWebApplicationContext()方法最后返回的是一个WebApplicationContext接口，而实际返回的就是XMLWebApplicationContext实现类。XMLWebApplicationContext在基本的ApplicationContext功能的基础上，增加了对**Web环境**和XML配置定义的处理。在XMLWebApplicationContext的初始化过程中，Web容器中的IOC容器被建立起来，从而再整个Web容器中建立起Spring应用。
```
public class XmlWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	/** 默认读取Spring配置文件的根路径，如果指定其他配置文件，则从这个默认的根路径读取。 */
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

	/** 默认的配置文件位置在/WEB-INF/目录下 */
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

	/** 默认的配置文件后缀.xml文件 */
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";


	/**
	 * 熟悉的loadBeanDefinitions方法，相信看过上篇的读者，应该不会陌生。这里的功能就不在赘述了。
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.setEnvironment(getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				reader.loadBeanDefinitions(configLocation);
			}
		}
	}
	@Override
	protected String[] getDefaultConfigLocations() {
		if (getNamespace() != null) {
			return new String[] {DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace() + DEFAULT_CONFIG_LOCATION_SUFFIX};
		}
		else {
			return new String[] {DEFAULT_CONFIG_LOCATION};
		}
	}

}
```
从源码中可以看到，XMLWebApplicationContext中成员变量存放着默认的读取Spring配置文件的根目录，在生成IOC容器过程中，就会从默认路径/WEB-INF/applicationContext.xml配置文件中或者指定的配置文件路径获取，然后再通过熟悉的loadBeanDefinitions()方法来获取Bean定义信息，最终完成整个上下文的初始化过程。

ContextLoader.class
```
protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
    // 这里判断使用什么样的类在Web容器中作为IOC容器
	Class<?> contextClass = determineContextClass(sc);
	if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
		throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
				"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
	}
	// 直接实例化需要产生的IOC容器，并设置IOC容器的各个参数，然后通过refresh启动容器的初始化。refresh的过程相信读者并不陌生。
	return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
}

protected Class<?> determineContextClass(ServletContext servletContext) {
	String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
	// 判断是否存在指定的IOC
	if (contextClassName != null) {
		try {
			return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException ex) {
		    ...
		}
	}
	else {
	    // 如果没有指定的IOC容器，则properties中获取默认的IOC容器，也就是XMLWebApplicationContext。
		contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
		try {
			return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
            ...		    
		}
	}
}

```

下面看看默认的IOC容器是什么。有图有真相：
[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-9m6BDvfX-1596181541884)(https://note.youdao.com/yws/api/personal/file/AB1007BC2A7549D7898417D6231AE4E3?method=download&shareKey=e851d344aedd461f319dba3b8e2c6fe8)]

```
protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
	if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
		String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
		if (idParam != null) {
			wac.setId(idParam);
		}
		else {
			wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
					ObjectUtils.getDisplayString(sc.getContextPath()));
		}
	}

    // 设置ServletContext的引用
	wac.setServletContext(sc);
	String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
	if (configLocationParam != null) {
		wac.setConfigLocation(configLocationParam);
	}

	ConfigurableEnvironment env = wac.getEnvironment();
	if (env instanceof ConfigurableWebEnvironment) {
		((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
	}

    // 自定义上下文
	customizeContext(sc, wac);
	// 调用SpringIOC容器的refresh()方法。
	wac.refresh();
}
```

## 总结

对于Spring承载的Web应用而言，可以指定在Web应用程序启动时载入IOC容器（WebApplicationContext）。这个载入的功能是通过ContextLoaderListener来实现的，它是一个Web容器的监听器，而ContextLoaderListener又通过ContextLoader来完成实际的WebApplicationContext的初始化，也就是IOC的初始化。换句话说，ContextLoader就像Spring应用在Web容器中的启动器。

