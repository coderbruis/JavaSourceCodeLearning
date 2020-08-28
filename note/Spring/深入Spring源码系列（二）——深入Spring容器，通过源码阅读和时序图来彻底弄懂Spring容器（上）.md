<!-- TOC -->
- [前言](#前言)
- [进入正题](#进入正题)
    - [DefaultListableBeanFactory](#defaultlistablebeanfactory)
	- [XmlBeanDefinitionReader](#xmlbeandefinitionreader)
	- [通过xml配置文件来初始化容器](#通过xml配置文件来初始化容器)    
	- [提问环节](#提问环节)
	    - [深入理解Spring容器如何创建bean和初始化bean](#深入理解spring容器如何创建bean和初始化bean)
		- [在web容器中初始化spring容器](#在web容器中初始化spring容器)
- [参考](#参考)
		<!-- /TOC -->
## 前言
**Spring版本: Spring5-0-3**

Spring容器就相当于一个大的水桶，里面装着很多水——bean对象。bean对象就是一个普通的pojo对象。这里有一个很重要的概念，就是IOC——Invertion of Control，即反转控制。通俗点将就是将创建并且绑定数据bean的权利赋予给了Spring容器（或Spring IOC容器，下文Spring容器代指Spring IOC容器），在bean生成或初始化的时候，Spring容器就会将数据注入到bean中，又或者通过将对象的引用注入到对象数据域中的方式来注入对方法调用的依赖。

## 进入正题

在Spring容器的设计中，有两个主要的容器系列，一个是实现BeanFactory接口的简单容器系列，这个接口实现了容器最基本的功能；另一个是ApplicationContext应用上下文，作为容器的高级形态而存在，它用于扩展BeanFactory中现有的功能。ApplicationContext和BeanFactory两者都是用于加载Bean的，但是相比之下，ApplicationContext提供了更多的扩展功能，简单一点说：ApplicationContext包含BeanFactory的所有功能。绝大多数“典型”的企业应用和系统，ApplicationContext就是你需要使用的。下面展示一下分别使用BeanFactory和ApplicationContext读取xml配置文件的方式：
```
BeanFactory bf = new XmlBeanFactory(new ClassPathResource("applicationContext.xml"));
```
```
ApplicationContext bf = new ClassPathXmlApplicationContext("applicationContext.xml");
```
下面先介绍Spring最核心的两个类。

### DefaultListableBeanFactory
DefaultListableBeanFactory是整个bean加载的核心部分，是Spring注册及加载bean的默认实现。下面看看DefaultListableBeanFactory的层次结构图。

![图片1](https://note.youdao.com/yws/api/personal/file/A91C9C5BB33B48A4B501435C157FFD99?method=download&shareKey=2b9a7ef7fd42d051fec83fe3f5eef7a8)
从上往下开始介绍各个类以及接口的作用：
- AliasRegistry（接口）：alias指的是bean的别名，而aliasRegistry定义了对alias的增删改查等操作。
- SimpleAliasRegistry（类）：主要使用map作为alias的缓存，并对接口AliasRegistry进行实现。
- SingletonBeanRegistry（接口）：定义对单例的注册及获取。
- BeanFactory（接口）：定义获取bean及bean的各种属性。
- DefaultSingleTonBeanRegistry（接口）：实现了SingletonBeanRegistry的方法，同时继承了SimpleAliasRegistry。
- HierarchicalBeanFactory（接口）：继承BeanFactory，也就是在BeanFactory定义功能的基础上增加了对parantFactory的支持。
- BeanDefinitionRegistry（接口）：定义对BeanDefinition的增删改查功能，BeanDefinition就是描述一个bean的实例，包含了属性值（scope、bean的name、lazy加载模式等），构造参数以及其他更多的实现信息。
- FactoryBeanRegistrySupport（类）：在DefaultSingleTonBeanRegistry基础上增加了对FactoryBean的特殊功能。
- ConfigurableBeanFactory（接口）：提供配置Factory的各种方法，包括设置父工厂、bean的加载器、添加BeanPostProcessor等功能。
- ListableBeanFactory（接口）：根据各种条件获取bean的配置清单，可以根据bean的name、type等条件获得bean配置清单。
- AbstractBeanFactory（类）：综合FactoryBeanRegistrySupport和ConfigurableBeanFactory的功能。
- AutowireCapableBeanFactory（接口）：提供创建Bean、自动注入、初始化以及应用bean的后置处理器。
- AbstractAutowireCapableBeanFactory（类）：综合AbstractBeanFactory并对接口AutowireCapableBeanFactory进行实现
- ConfigurableListableBeanFactory：BeanFactory配置清单，指定忽略类型及接口等。
- DefaultListableBeanFactory：综合上面所有功能，主要对Bean注册后的处理。

### XmlBeanDefinitionReader
XML配置文件的读取是Spring中最重要的功能，因为Spring的大部分功能都是以配置作为切入点的，XmlBeanDefinitionReader实现了对资源文件的读取、解析以及注册。先看一下XmlBeanDefinitionReader的层次结构图。
![图片2](https://note.youdao.com/yws/api/personal/file/477FF4A409A94CBB8CF9A05A16D8F7D4?method=download&shareKey=de7d24b623d4c5bb7e65bb440438e271)

- EnvironmentCapable（接口）：定义获取Environment方法，Environment代表了配置文件。
- BeanDefinitionReader（接口）：主要定义资源文件读取并转换为BeanDefinition的各个功能。
- AbstractBeanDefinitionReader（类）：对EnvironmentCapable、BeanDefinitionReader类定义的功能进行实现。
- ResourceLoader（接口）：定义资源加载器，主要应用于根据给定的资源文件地址返回对应的Resource。
- BeanDefinitionReader：主要定义资源文件读取并转换为BeanDefinition的各个功能。

学习完Spring最核心的两个类之后，让我们进入coding时间。

## 通过xml配置文件来初始化容器

下面演示一个使用ApplicationContext接口获取xml配置，从而实现一个helloword级别的spring程序：

applicationContext.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="person" class="com.bruis.learnspring.context.Person">
        <property name="age" value="23"/>
        <property name="name" value="Bruis"/>
    </bean>
    
</beans>
```
测试类
```
public class SpringMain {
    public static void main(String[] args) {
        //使用spring容器
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        Person person = (Person)context.getBean("person");
        System.out.println(person);
    }
}

运行结果：
Person{name='Bruis', age=23}
```
运行完程序，读者有没有想过，配置文件是怎么被加载到Spring容器，然后初始化Bean，最终打印到控制台的？我想大部分读者都会很好奇并想去一探究竟，但是碍于Spring源码难以阅读，debug跳来跳去眼花缭乱。下面就跟着笔者一起深入Spring源码，看看Spring容器是如何从xml配置文件中获取配置信息并且打印出bean的信息。



** 前方高能 **

![图片3](https://note.youdao.com/yws/api/personal/file/219238FD61C146C99E137E303D52EA66?method=download&shareKey=d5e5aaa1e9fa782eeb056b89119c3565)

通过在断点debug，跟踪程序运行。

1. SpringMain.class
```
ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
```
2. ClassPathXmlApplicationContext.class
```
public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
    this(new String[]{configLocation}, true, (ApplicationContext)null);
}
/*
使用给定的父类容器创建新的ClassPathXmlApplicationContext，然后从给定的XML文件加载定义，加载所有bean定义并且创建所有的单例，在进一步配置上下文后调用refresh。换句话说xml文件的读取，bean的创建和实例化都是在refresh()方法中进行的，refresh()函数中包含了几乎所有的ApplicationContext中提供的全部功能。
*/
public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent) throws BeansException {
    super(parent);
    //设置配置路径
    this.setConfigLocations(configLocations);
    if (refresh) {
        //refresh Spring容器
        this.refresh();
    }
}
```
3. AbstractRefreshableConfigApplicationContext.class
```
//给configLocations字符串数组设置值，支持多个配置文件已数组方式同时传入。
public void setConfigLocations(String... locations) {
    if (locations != null) {
        Assert.noNullElements(locations, "Config locations must not benull");
        this.configLocations = new String[locations.length];
        for(int i = 0; i < locations.length; ++i) {
            this.configLocations[i] = this.resolvePath(locations[i]).trim();
        }
    } else {
        this.configLocations = null;
    }
}
```

### 提问环节

- Spring容器的生命周期分为几个阶段？Spring容器的初始化发生在什么时候，发生了什么？Spring容器的销毁过程又发生了什么？
- Spring容器什么时候读取xml配置文件？并且把配置信息读取？Spring容器用什么数据结构存储用于创建bean的K/V信息？
- Spring容器获取了用于创建bean的K/V信息后，在什么时候去创建并且初始化bean？

下面我们来重点看看refresh()过程。

![Image](https://note.youdao.com/yws/api/personal/file/76AE8FEDAFF54B6881C336B056AC5B0A?method=download&shareKey=430f5263180efd8467df6e6434456f3d)

1. AbstractApplicationContext.class
```
/*
    简单来说，Spring容器的初始化时右refresh()方法来启动的，这个方法标志着IOC容器的正式启动。具体来说，这里的启动包括了BeanDefinition和Resource的定位、载入和注册三个基本过程。
*/
@Override
public void refresh() throws BeansException, IllegalStateException {
	synchronized (this.startupShutdownMonitor) {
		//准备刷新容器（上下文环境）
		prepareRefresh();
		//通知子类刷新内部bean工厂，初始化BeanFactory并进行XML文件的解析、读取
		ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
		//准备bean工厂在此上下文中使用，并对BeanFactory进行各种功能填充
		prepareBeanFactory(beanFactory);
		try {
			// 允许在上下文子类中对bean工厂进行后处理
			postProcessBeanFactory(beanFactory);
			// 在上下文中调用注册并激活BeanFactory的处理器，就是这个方法中的处理器注册了bean
			invokeBeanFactoryPostProcessors(beanFactory);
			// 注册拦截bean创建的bean处理器，这里只是注册，真正的调用Bean是在getBean方法的时候
			registerBeanPostProcessors(beanFactory);
			// 初始化此上下文的消息源，及不同语言的消息体，例如国际化处理
			initMessageSource();
			// 初始化此上下文的事件多播器
			initApplicationEventMulticaster();
			// 在特定上下文子类中初始化其他特殊bean.
			onRefresh();
			// 检查监听器bean并注册它们
			registerListeners();
			// 实例化所有剩余（非惰性的）单例
			finishBeanFactoryInitialization(beanFactory);
			// 完成刷新过程，通知生命周期处理器lifecycleProcessor刷新过程，同时发出ContextRefreshEvent通知别人
			finishRefresh();
		}
		catch (BeansException ex) {
		    ...
			// 摧毁已经创建的单例以避免资源浪费
			destroyBeans();
			// 重置'有效'标志
			cancelRefresh(ex);
			// Propagate exception to caller.
			throw ex;
		}
		finally {
			resetCommonCaches();
		}
	}
}
```
2. AbstractRefreshableApplicationContext.class
```
/*
    通知子类刷新内部bean工厂，初始化BeanFactory并进行XML文件的解析、读取。obtain就是指获得的含义，这个方法obtaiinFreshBeanFactory正是实现BeanFactory的地方，也就是经过这个方法，ApplicationContext就已经拥有了BeanFactory的全部功能（也就是BeanFactory包含在了Spring容器里了）。
*/
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    this.refreshBeanFactory();
    ...
    return beanFactory;
}
/*
此实现执行此上下文的基础bean工厂的实际刷新，关闭先前的bean工厂（如果有）并初始化上一个生命周期的下一阶段的新bean工厂。

经过debug，最后从refreshBeanFactory()方法返回后，this也就是AbstractRefreshableApplicationContext实例对象

*/
@Override
protected final void refreshBeanFactory() throws BeansException {
    //如果有bean工厂，则关闭该工厂
	if (hasBeanFactory()) {
		destroyBeans();
		closeBeanFactory();
	}
	try {
	    //创建一个新bean工厂，这里的DefaultListableBeanFactory就是前面笔者将的Spring核心类，这个类真的很重要！
		DefaultListableBeanFactory beanFactory = createBeanFactory();
		//为了序列化指定ID，如果需要的话，让这个BeanFactory从ID反序列化掉BeanFactory对象
		beanFactory.setSerializationId(getId());
		//定制beanFactory，设置相关属性，包括是否允许覆盖同名称的不同定义的对象以及循环依赖以及设置@Autowired和@Qualifier注解解析器QualifierAnnotationAutowireCandidateResolver
		customizeBeanFactory(beanFactory);
		//加载bean定义信息，这一步实际上就从XML配置文件里的bean信息给读取到了Factory里了。
		loadBeanDefinitions(beanFactory);
		synchronized (this.beanFactoryMonitor) {
			this.beanFactory = beanFactory;
		}
	}
	catch (IOException ex) {
	    ...
	}	
}
```
这里先看看上面代码的loadBeanDefinitions()方法运行完后的结果
![图片](https://note.youdao.com/yws/api/personal/file/59FBCD3CC1B54136A05309EA6B88FEB3?method=download&shareKey=80bdcfcbde0362b73eb633390c5b1042)
![图片](https://note.youdao.com/yws/api/personal/file/E258907852284A6F93A2C305319EBB64?method=download&shareKey=7e1dba96d3b53ca9b6af017552f8fd31)
从图中可以知道，loadBeanDefinitions()方法运行完后，在beanFactory变量里面存放着一个ConcurrentHashMap变量，用于存放着person这个KV键值对，Key为person，Value为一个ArrayList的变量，里面存放着person的两个属性：age、name。

那么，person的属性是怎么被封装到beanFactory里面的呢？请看下面的源码解析。

3. AbstractXmlApplicationContext.class
```
protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
    //为给定的BeanFactory创建一个新的XmlBeanDefinitionReader
    XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
    beanDefinitionReader.setEnvironment(this.getEnvironment());
    beanDefinitionReader.setResourceLoader(this);
    beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
    //允许子类提供reader的自定义初始化
    this.initBeanDefinitionReader(beanDefinitionReader);
    //真正加载bean定义信息
    this.loadBeanDefinitions(beanDefinitionReader);
}
protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
	Resource[] configResources = getConfigResources();
	if (configResources != null) {
		reader.loadBeanDefinitions(configResources);
	}
	String[] configLocations = getConfigLocations();
	if (configLocations != null) {
		reader.loadBeanDefinitions(configLocations);
	}
}
```
首先在refreshBeanFactory()方法中已经初始化了DefaultListableBeanFactory，对于读取XML配置文件，还需要使用XmlBeanDefinitionReader。所以在上述loadBeanDefinitions()中就需要初始化XmlBeanDefinitionReader。在DefaultListableBeanFactory和XmlBeanDefinitionReader后就可以进行配置文件的读取了。要注意的地方时，在XmlBeanDefinitionReader初始化时就已经把DefaultListableBeanFactory给注册进去了，所以在XmlBeanDefinitionReader读取的BeanDefinition都会注册到DefaultListableBeanFactory中，也就是经过上述的loadingBeanDefinitions()，类型DefaultListableBeanFactory的变量beanFactory就已经包含了所有**解析好的配置**了。

4. AbstractBeanDefinitionReader.class
```
@Override
public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
	Assert.notNull(locations, "Location array must not be null");
	int count = 0;
	for (String location : locations) {
		count += loadBeanDefinitions(location);
	}
	return count;
}

/*
从指定的资源位置加载bean定义,他的位置也可以是位置模式，前提是此bean定义读取器的ResourceLoader是ResourcePatternResolver。
*/
public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
	ResourceLoader resourceLoader = getResourceLoader();
	if (resourceLoader == null) {
	    ...
	}

	if (resourceLoader instanceof ResourcePatternResolver) {
		// 资源模式匹配可用
		try {
			Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
			int count = loadBeanDefinitions(resources);
			if (actualResources != null) {
				Collections.addAll(actualResources, resources);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
			}
			return count;
		}
		catch (IOException ex) {
		    ...
		}
	}
	else {
		// 只能通过绝对URL加载单个资源
		Resource resource = resourceLoader.getResource(location);
		int count = loadBeanDefinitions(resource);
		if (actualResources != null) {
			actualResources.add(resource);
		}
		...
	return count;
}
```
5. PathMatchingResourcePatternResolver.class
```
@Override
public Resource[] getResources(String locationPattern) throws IOException {
	Assert.notNull(locationPattern, "Location pattern must not be null");
	if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
		// 类路径资源（可能有多个同名资源）
		if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
			// 类路径资源模式
			return findPathMatchingResources(locationPattern);
		}
		else {
			// 具有给定名称的所有类路径资源
			return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
		}
	}
	else {
		// 通常只在前缀后面查找一个模式，并且在Tomcat之后只有“* /”分隔符之后的“war：”协议。
		int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
				locationPattern.indexOf(':') + 1);
		if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
			// 文件模式
			return findPathMatchingResources(locationPattern);
		}
		else {
			// 具有给定名称的单个资源
			return new Resource[] {getResourceLoader().getResource(locationPattern)};
		}
	}
}
```
6. XmlBeanDefinitionReader.class
```
/*
    从XML配置文件中获取bean定义信息
*/
public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
	Assert.notNull(encodedResource, "EncodedResource must not be null");
    ...
	Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
	if (currentResources == null) {
		currentResources = new HashSet<>(4);
		this.resourcesCurrentlyBeingLoaded.set(currentResources);
	}
	...
	try {
		InputStream inputStream = encodedResource.getResource().getInputStream();
		try {
			InputSource inputSource = new InputSource(inputStream);
			if (encodedResource.getEncoding() != null) {
				inputSource.setEncoding(encodedResource.getEncoding());
			}
			//获取到读取xml配置文件的InputStream流后，进行BeanDefinitions的加载
			return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
		}
		finally {
			inputStream.close();
		}
	}
	catch (IOException ex) {
	    ...
	}
	finally {
		currentResources.remove(encodedResource);
		if (currentResources.isEmpty()) {
			this.resourcesCurrentlyBeingLoaded.remove();
		}
	}
}

/*
    真正从xml配置文件中加载Bean定义信息
*/
protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
		throws BeanDefinitionStoreException {

	try {
	    //获取xml的配置信息并封装为Document对象
		Document doc = doLoadDocument(inputSource, resource);
		return this.registerBeanDefinitions(doc, resource);
	}
	catch (BeanDefinitionStoreException ex) {
	    ...
	}
}
```

下面，继续深入registerBeanDefinitions方法。

![图片](https://note.youdao.com/yws/api/personal/file/861658D89B0D4B48A7ED56B554CF3028?method=download&shareKey=c3bc974e751495bac74d9ac9ec56cb75)

1. XmlBeanDefinitionReader.class 
```
/*
    注册给定DOM文档中包含的bean定义
*/
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
    BeanDefinitionDocumentReader documentReader = this.createBeanDefinitionDocumentReader();
    int countBefore = this.getRegistry().getBeanDefinitionCount();
    documentReader.registerBeanDefinitions(doc, this.createReaderContext(resource));
    return this.getRegistry().getBeanDefinitionCount() - countBefore;
}
```
2. DefaultBeanDefinitionDocumentReader.class
```
/*
    此实现根据“spring-beans”XSD解析bean定义
*/
public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
    this.readerContext = readerContext;
    this.logger.debug("Loading bean definitions");
    Element root = doc.getDocumentElement();
    this.doRegisterBeanDefinitions(root);
}

/*
    任何嵌套的<beans>元素都将导致此方法的递归。为了正确传播和保留beans的default属性，需要跟踪当前（父）委托，该委托可以为null。创建新的（子）委托时，需要引用父项以进行回退，然后最终将this.delegate重置为其原始（父）引用。
*/
protected void doRegisterBeanDefinitions(Element root) {
    BeanDefinitionParserDelegate parent = this.delegate;
    this.delegate = this.createDelegate(this.getReaderContext(), root, parent);
    if (this.delegate.isDefaultNamespace(root)) {
        String profileSpec = root.getAttribute("profile");
        if (StringUtils.hasText(profileSpec)) {
            String[] specifiedProfiles = StringUtils.tokenizeToStringArray(profileSpec, ",; ");
            if (!this.getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("Skipped XML bean definition file due to specified profiles [" + profileSpec + "] not matching: " + this.getReaderContext().getResource());
                }

                return;
            }
        }
    }
    
    this.preProcessXml(root);
    //解析beanDefinitions信息，经过这个方法，beanFactory中就会保存从xml配置文件中解析而来的信息
    this.parseBeanDefinitions(root, this.delegate);
    this.postProcessXml(root);
    this.delegate = parent;
}

/*
    解析文档中根级别的元素：import、alias、bean
*/
protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
    if (delegate.isDefaultNamespace(root)) {
        NodeList nl = root.getChildNodes();
        for(int i = 0; i < nl.getLength(); ++i) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element ele = (Element)node;
                if (delegate.isDefaultNamespace(ele)) {
                    //解析默认元素
                    this.parseDefaultElement(ele, delegate);
                } else {
                    delegate.parseCustomElement(ele);
                }
            }
        }
    } else {
        delegate.parseCustomElement(root);
    }

}

private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
    if (delegate.nodeNameEquals(ele, "import")) {
        this.importBeanDefinitionResource(ele);
    } else if (delegate.nodeNameEquals(ele, "alias")) {
        this.processAliasRegistration(ele);
    } else if (delegate.nodeNameEquals(ele, "bean")) {
        //读取到xml配置文件的<bean>节点
        this.processBeanDefinition(ele, delegate);
    } else if (delegate.nodeNameEquals(ele, "beans")) {
        this.doRegisterBeanDefinitions(ele);
    }
}
 
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
    BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
    ...
}

```

2. BeanDefinitionParserDelegate.class
```
/*
    解析bean定义本身，而不考虑名称或别名，如果解析期间出错则返回null。
*/
@Nullable
public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
    String id = ele.getAttribute("id");
    String nameAttr = ele.getAttribute("name");
    List<String> aliases = new ArrayList();
    if (StringUtils.hasLength(nameAttr)) {
        String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, ",; ");
        aliases.addAll(Arrays.asList(nameArr));
    }

    String beanName = id;
    if (!StringUtils.hasText(id) && !aliases.isEmpty()) {
        beanName = (String)aliases.remove(0);
        ...
    }

    if (containingBean == null) {
        this.checkNameUniqueness(beanName, aliases, ele);
    }
    
    //终于，这里要解析beanDefinition了
    AbstractBeanDefinition beanDefinition = this.parseBeanDefinitionElement(ele, beanName, containingBean);
    if (beanDefinition != null) {
        if (!StringUtils.hasText(beanName)) {
            try {
                if (containingBean != null) {
                    beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, this.readerContext.getRegistry(), true);
                } else {
                    beanName = this.readerContext.generateBeanName(beanDefinition);
                    String beanClassName = beanDefinition.getBeanClassName();
                    if (beanClassName != null && beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() && !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                        aliases.add(beanClassName);
                    }
                }
                ...
            } catch (Exception var9) {
                this.error(var9.getMessage(), ele);
                return null;
            }
        }
        
        //别名数组
        String[] aliasesArray = StringUtils.toStringArray(aliases);
        return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
    } else {
        return null;
    }
}

@Nullable
public AbstractBeanDefinition parseBeanDefinitionElement(Element ele, String beanName, @Nullable BeanDefinition containingBean) {
    this.parseState.push(new BeanEntry(beanName));
    String className = null;
    if (ele.hasAttribute("class")) {
        className = ele.getAttribute("class").trim();
    }

    String parent = null;
    if (ele.hasAttribute("parent")) {
        parent = ele.getAttribute("parent");
    }

    try {
        //创建BeanDefinition
        AbstractBeanDefinition bd = this.createBeanDefinition(className, parent);
        this.parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
        bd.setDescription(DomUtils.getChildElementValueByTagName(ele, "description"));
        this.parseMetaElements(ele, bd);
        this.parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
        this.parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
        
        //通过构造器解析参数值
        this.parseConstructorArgElements(ele, bd);
        //通过property的value解析值吗，本文的程序xml就是通过property属性设置bean的值的，最终被这一方法所解析出来。
        this.parsePropertyElements(ele, bd);
        
        this.parseQualifierElements(ele, bd);
        bd.setResource(this.readerContext.getResource());
        bd.setSource(this.extractSource(ele));
        AbstractBeanDefinition var7 = bd;
        return var7;
    } catch (ClassNotFoundException var13) {
        ...
    }
    return null;
}


public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
    NodeList nl = beanEle.getChildNodes();
    for(int i = 0; i < nl.getLength(); ++i) {
        Node node = nl.item(i);
        if (this.isCandidateElement(node) && this.nodeNameEquals(node, "property")) {
            //解析出参数值来，这里就真正的讲age的23，和name的bruis值解析出来并防止在一个组装的类里面存放着。因为这里有两个bean，所以要循环调用两次parsePropertyElement()方法
            this.parsePropertyElement((Element)node, bd);
        }
    }
}

public void parsePropertyElement(Element ele, BeanDefinition bd) {
    String propertyName = ele.getAttribute("name");
    if (!StringUtils.hasLength(propertyName)) {
        this.error("Tag 'property' must have a 'name' attribute", ele);
    } else {
        this.parseState.push(new PropertyEntry(propertyName));

        try {
            if (!bd.getPropertyValues().contains(propertyName)) {
                Object val = this.parsePropertyValue(ele, bd, propertyName);
                PropertyValue pv = new PropertyValue(propertyName, val);
                this.parseMetaElements(ele, pv);
                pv.setSource(this.extractSource(ele));
                
                //就是这一步，将K为age、name，值分别为23、bruis的KV对存放在了Spring容器里。
                bd.getPropertyValues().addPropertyValue(pv);
                return;
            }
        } finally {
            this.parseState.pop();
        }

    }
}

```
![Images](https://note.youdao.com/yws/api/personal/file/75CAC9D21AD64BAB89B0D25C8BBE7598?method=download&shareKey=89e73cf46fe18b1b85aecf8d58006f8e)
![Images](https://note.youdao.com/yws/api/personal/file/CF65BB80EB934EBEBA49466CFAB261A0?method=download&shareKey=8b9f0078cf5a3171dfd69d00d9ba55f6)

然后，就会一路返回到refresh()方法里的加载bean定义信息的方法——loadBeanDefinitions()，此时beanFactory里面就会存在一个带有KV对的ConcurrentHashMap，而这个beanFactory会存放在Spring容器里面。
```
DefaultListableBeanFactory beanFactory = createBeanFactory();
beanFactory.setSerializationId(getId());
customizeBeanFactory(beanFactory);
//加载bean定义信息
loadBeanDefinitions(beanFactory);
```
再看看DefaultListableBeanFactory里面的内容
![Images](https://note.youdao.com/yws/api/personal/file/59FBCD3CC1B54136A05309EA6B88FEB3?method=download&shareKey=80bdcfcbde0362b73eb633390c5b1042)
![Images](https://note.youdao.com/yws/api/personal/file/E258907852284A6F93A2C305319EBB64?method=download&shareKey=7e1dba96d3b53ca9b6af017552f8fd31)

上面的过程，就已经完成了Spring容器的初始化过程，相信读者也已经对Spring容器的初始化有了一个大致的了解。下面总结一下Spring容器的初始化：
- 第一个过程是Resource定位过程。这个Resource定位过程指的是BeanDefinition的资源定位，它由ResourceLoader通过统一的Resource接口来完成，这个Resource对各种形式的BeanDefinition的使用都提供了统一接口。这个定位过程类似于容器寻找数据的过程，就像使用水桶装水先要把水找到一样。
- 第二个过程是BeanDefinition的载入。这个载入过程是把用户定义好的Bean表示成IOC容器内部的数据结构，而这个容器内部的数据结构就是BeanDefinition。下面介绍这个数据结构的详细定义。具体来说，这个BeanDefinition实际上就是POJO对象在IOC容器的抽象，通过这个BeanDefinition定义的数据结构，使IOC能够方便地对POJO对象进行管理。
- 第三个过程是向IOC容器注册这些BeanDefinition的过程，这个过程是通过调用BeanDefinitionRegistry接口的实现来完成的。这个注册过程把载入过程中解析到的BeanDefinition向IOC容器进行注册。通过上面的分析，我们知道IOC内部将BeanDefinition注册到了ConcurrentHashMap中。 

经过前面源码分析，我们终于弄清楚了Spring对XML配置文件的解析的过程，接下来就是要弄清楚Spring容器对bean的创建和加载了。
下面就介绍如何将Map里的bean的信息是如何被添加至person实例中。

### 深入理解Spring容器如何创建bean和初始化bean
bean的创建和初始化过程是在refresh方法里的invokeBeanFactoryPostProcessors()方法里实现的。

下面先简单总结一下在IOC中bean的生命周期：
- Bean实例的创建
- 为Bean实例设置属性
- 调用Bean的初始化方法
- 应用可以通过IOC容器使用Bean
- 当容器关闭时，调用Bean的销毁方法

下面先看看创建bean和初始化bean的时序图。
![Images](https://note.youdao.com/yws/api/personal/file/8B415614A97D45B481925159264C344F?method=download&shareKey=1083828cfcea581b0aa5cae56e3f3090)

1. AbstractApplicationContext.class
```
public void refresh() throws BeansException, IllegalStateException {
    ...
    // 实例剩余的（非懒加载）的单例
    finishBeanFactoryInitialization(beanFactory);
    ...
}

/*
    返程上下文bean工厂的实例化过程，实例化所有剩余的单例类
*/
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
	// 初始化上下文的 conversion service 
	if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
			beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
		beanFactory.setConversionService(
				beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
	}

	// Register a default embedded value resolver if no bean post-processor
	// (such as a PropertyPlaceholderConfigurer bean) registered any before:
	// at this point, primarily for resolution in annotation attribute values.
	if (!beanFactory.hasEmbeddedValueResolver()) {
		beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
	}

	// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
	String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
	for (String weaverAwareName : weaverAwareNames) {
		getBean(weaverAwareName);
	}

	// 停止使用类型匹配的临时类加载器 
	beanFactory.setTempClassLoader(null);

	// 允许缓存所有bean定义元数据，而不期待进一步的更改
	beanFactory.freezeConfiguration();

	// 实例剩余的（非懒加载）的单例
	beanFactory.preInstantiateSingletons();
}

```
这里的懒加载的意思，指的是bean单例不是在Spring容器初始化的时候就创建的，而是在要使用该bean的时候，才会创建该bean。

2. DefaultListableBeanFactory.class
```
// 实例剩余的（非懒加载）的单例
@Override
public void preInstantiateSingletons() throws BeansException {
	if (this.logger.isDebugEnabled()) {
		this.logger.debug("Pre-instantiating singletons in " + this);
	}

    // 获取所有的bean定义的名字，并保存在集合List里面。
	List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

	// 触发所有非延迟单例bean的初始化...
	for (String beanName : beanNames) {
	    // 触发所有适用bean的后初始化回调
		RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
		if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
			if (isFactoryBean(beanName)) {
				Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
				if (bean instanceof FactoryBean) {
					final FactoryBean<?> factory = (FactoryBean<?>) bean;
					boolean isEagerInit;
					if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
						isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
										((SmartFactoryBean<?>) factory).isEagerInit(),
								getAccessControlContext());
					}
					else {
						isEagerInit = (factory instanceof SmartFactoryBean &&
								((SmartFactoryBean<?>) factory).isEagerInit());
					}
					if (isEagerInit) {
						getBean(beanName);
					}
				}
			}
			else {
				getBean(beanName);
			}
		}
	}

	// 触发所有适用bean的后置初始化回调方法...
	for (String beanName : beanNames) {
		Object singletonInstance = getSingleton(beanName);
		if (singletonInstance instanceof SmartInitializingSingleton) {
			final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					smartSingleton.afterSingletonsInstantiated();
					return null;
				}, getAccessControlContext());
			}
			else {
				smartSingleton.afterSingletonsInstantiated();
			}
		}
	}
}
```

3. AbstractBeanFactory.class 
```
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
			@Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {
        // 去除name上存在的工厂bean的前缀
		final String beanName = transformedBeanName(name);
		Object bean;

		// 快速判断单例缓存中是否存在该bean，如果存在则返回单例bean；否则返回null
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
		    ...
		    // 从单例缓存中获取单例bean
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}
		else {
			// 判断该bean是否存在父工厂
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				//如果存在父工厂，则按照父工厂的实现来创建该实例
			}
    
            // 标记该bean已经被创建
			if (!typeCheckOnly) {
				markBeanAsCreated(beanName);
			}

			try {
			    // 返回bean的定义信息，包括bean的scope、依赖、是否进行懒加载等定义
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				checkMergedBeanDefinition(mbd, beanName, args);

				// 确保初始化所有依赖这个bean的bean
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dep : dependsOn) {
					
					    ...
					
						registerDependentBean(dep, beanName);
						getBean(dep);
					}
				}

				// 开始创建这个bean，注意这里使用的lambda表达式，传入的是一个AbstracBeanFactory
				if (mbd.isSingleton()) {
					sharedInstance = getSingleton(beanName, () -> {
						try {
							return createBean(beanName, mbd, args);
						}
						catch (BeansException ex) {
						    ...
						}
					});
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}

				else if (mbd.isPrototype()) {
					// 该bean定义了原型模式，创建该bean
					Object prototypeInstance = null;
					try {
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						afterPrototypeCreation(beanName);
					}
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				else {
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
					    ...
					}
					try {
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							}
							finally {
								afterPrototypeCreation(beanName);
							}
						});
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
					    ...
					}
				}
			}
			catch (BeansException ex) {
			    ...
			}
		}

		// Check if required type matches the type of the actual bean instance.
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
				    ...
				}
				return convertedBean;
			}
			catch (TypeMismatchException ex) {
			    ...
			}
		}
		return (T) bean;
	}
```

4. DefaultSingletonBeanRegistry.class
```
/*
    尝试从缓存中获取单例对象，如果缓存中有该单例对象，并且该对象正在被创建，则从缓存中获取。
*/
@Nullable
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // singletonObjects是一个单例缓存，是一个ConcurrentHashMap
	Object singletonObject = this.singletonObjects.get(beanName);
	// 从缓存中获取单例对象、判断对象是否正在被创建
	if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
		synchronized (this.singletonObjects) {
			singletonObject = this.earlySingletonObjects.get(beanName);
			if (singletonObject == null && allowEarlyReference) {
				ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
				if (singletonFactory != null) {
					singletonObject = singletonFactory.getObject();
					this.earlySingletonObjects.put(beanName, singletonObject);
					this.singletonFactories.remove(beanName);
				}
			}
		}
	}
	return singletonObject;
}

/*
    
*/
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
	Assert.notNull(beanName, "Bean name must not be null");
	// 可以看到，对于单例bean的获取，是要获得“单例缓存”的锁的，否则无法操作bean的获取。
	synchronized (this.singletonObjects) {
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null) {
			
			...
			
			// bean创建之前的回调方法
			beforeSingletonCreation(beanName);
			boolean newSingleton = false;
			
			...
			
			try {
			    // 尝试从单例工厂获取单例bean
			    // 注意，这里的singletonFactory是由方法参数传进来的一个lambda表达式，这个singletonFactory是一个AbstractAutowireCapableBeanFactory实例
				singletonObject = singletonFactory.getObject();
				newSingleton = true;
			}
			catch (IllegalStateException ex) {
			    ...
			}
			catch (BeanCreationException ex) {
			    ...
			}
			finally {
		        
		        ...
		        
				// 单例bean创建过后的回调方法
				afterSingletonCreation(beanName);
			}
			if (newSingleton) {
				addSingleton(beanName, singletonObject);
			}
		}
		return singletonObject;
	}
}
```

无图无真相：
![Images](https://note.youdao.com/yws/api/personal/file/4C30C0DA143E422FBD27E50AE71AC179?method=download&shareKey=2f4dff65df0e9761ede47d26782dd977)

5. AbstractAutowireCapableBeanFactory.class
```
/*
    该类的中心方法：创建bean实例，实例化bean实例，应用bean的后置处理器

*/
@Override
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
		throws BeanCreationException {

	RootBeanDefinition mbdToUse = mbd;

	try {
		// 调用实例化前的后置处理器，在这个后置处理器中可以对bean进行处理，可以返回由后置处理器处理的bean而不是被实例化的bean。
		// 换句话说就是这里可以拦截住bean的实例化
		// Spring的后置处理器后面有机会再专门写一篇博文来总结学习一下
		Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
		if (bean != null) {
			return bean;
		}
	}
	catch (Throwable ex) {
	    ...
	}
	try {
		Object beanInstance = doCreateBean(beanName, mbdToUse, args);
		...
		return beanInstance;
	}
	catch (BeanCreationException ex) {
	    ...
	}
	catch (ImplicitlyAppearedSingletonException ex) {
	    ...
	}
	catch (Throwable ex) {
	    ...
	}
}

/*
    正在的创建一个bean，并且按照配置文件的配置来实例化该bean。如果没有初始化前的后置处理器的调用，则调用该方法。
*/
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
		throws BeanCreationException {
	    // 创建一个包装类，用于包装真正要创建的bean。
	BeanWrapper instanceWrapper = null;
	if (mbd.isSingleton()) {
		instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
	}
	if (instanceWrapper == null) {
	    // 使用适当的实例化策略例如：工厂方法、构造函数自动装配或者简单实例化 来为指定bean创建新的实例。这里仅仅是简单的实例化，为bean设置默认初始值
	    // 也就是name为null，age为0。此时instanceWrapper任然还只是一个包装bean，并不是一个真正意义上的person类bean。
		instanceWrapper = createBeanInstance(beanName, mbd, args);
	}
	
    // 终于，我们千辛万苦打断点调试来到了这一步，就是这一步， 获得了我们想要的person类bean。
    // 只需要在BeanWrapper里取出WrapperInstance即可。
    // 接下来就是要拿这个创建好的bean和BeanDefinition进行实例化了。
	final Object bean = instanceWrapper.getWrappedInstance();
	
	// 获取bean的类型
	Class<?> beanType = instanceWrapper.getWrappedClass();
	if (beanType != NullBean.class) {
		mbd.resolvedTargetType = beanType;
	}

	// 调用后置处理器去修改bean的定义信息。
	synchronized (mbd.postProcessingLock) {
		if (!mbd.postProcessed) {
			try {
				applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
			}
			catch (Throwable ex) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Post-processing of merged bean definition failed", ex);
			}
			mbd.postProcessed = true;
		}
	}

	boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
			isSingletonCurrentlyInCreation(beanName));
	if (earlySingletonExposure) {
	    ...
	    // 先快速的给bean实例化，然后将bean添加到单例工厂中。这里的单例工厂实际上就是一个Map
		addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
	}

	// 实例化一个真正使用的bean。
	Object exposedObject = bean;
	try {
	    
		populateBean(beanName, mbd, instanceWrapper);
		exposedObject = initializeBean(beanName, exposedObject, mbd);
	}
	catch (Throwable ex) {
	    ...
	}

	if (earlySingletonExposure) {
		Object earlySingletonReference = getSingleton(beanName, false);
		if (earlySingletonReference != null) {
			if (exposedObject == bean) {
				exposedObject = earlySingletonReference;
			}
			else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
				String[] dependentBeans = getDependentBeans(beanName);
				Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
				for (String dependentBean : dependentBeans) {
					if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
						actualDependentBeans.add(dependentBean);
					}
				}
				if (!actualDependentBeans.isEmpty()) {
				    ...
				}
			}
		}
	}

	// Register bean as disposable.
	try {
		registerDisposableBeanIfNecessary(beanName, bean, mbd);
	}
	catch (BeanDefinitionValidationException ex) {
	    ...
	}
	return exposedObject;
}

/*
    对创建出的Bean进行赋值填充。
*/
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    ...        
    // 取出BeanDefinition里的属性值
	PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
    
    // 如果设置的是自动装配模式，则由自动装配来进行赋值
	if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
			mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
		MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

        // 通过bean名自动装配
		if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
			autowireByName(beanName, mbd, bw, newPvs);
		}

        // 通过bean类型自动装配
		if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
			autowireByType(beanName, mbd, bw, newPvs);
		}

		pvs = newPvs;
	}

	boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
	boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);

	if (hasInstAwareBpps || needsDepCheck) {
		if (pvs == null) {
			pvs = mbd.getPropertyValues();
		}
		PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
		if (hasInstAwareBpps) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
					if (pvs == null) {
						return;
					}
				}
			}
		}
		if (needsDepCheck) {
			checkDependencies(beanName, mbd, filteredPds, pvs);
		}
	}

    // 这里的PropertyValues已经包含了bean字段属性的设置值了
	if (pvs != null) {
	    // 对bean进行赋值
		applyPropertyValues(beanName, mbd, bw, pvs);
	}
}


/*
    应用给定的属性值，并使用深拷贝对bean进行填充赋值。
*/
protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
	if (pvs.isEmpty()) {
		return;
	}

	MutablePropertyValues mpvs = null;
	
	// 源属性值
	List<PropertyValue> original;

	if (System.getSecurityManager() != null) {
		if (bw instanceof BeanWrapperImpl) {
			((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
		}
	}

	if (pvs instanceof MutablePropertyValues) {
		mpvs = (MutablePropertyValues) pvs;
		if (mpvs.isConverted()) {
			try {
				bw.setPropertyValues(mpvs);
				return;
			}
			catch (BeansException ex) {
			    ...
			}
		}
		original = mpvs.getPropertyValueList();
	}
	else {
		original = Arrays.asList(pvs.getPropertyValues());
	}

	TypeConverter converter = getCustomTypeConverter();
	if (converter == null) {
		converter = bw;
	}
	BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

	// 拷贝值；创建一个深拷贝副本，应用于任何bean引用此bean的情况。
	List<PropertyValue> deepCopy = new ArrayList<>(original.size());
	boolean resolveNecessary = false;
	for (PropertyValue pv : original) {
		if (pv.isConverted()) {
			deepCopy.add(pv);
		}
		else {
			String propertyName = pv.getName();
			Object originalValue = pv.getValue();
			Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
			Object convertedValue = resolvedValue;
			boolean convertible = bw.isWritableProperty(propertyName) &&
					!PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
			if (convertible) {
				convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
			}
			// 可能在合并的bean定义中存储转换后的值，以避免为每个创建的bean实例重新转换。
			if (resolvedValue == originalValue) {
				if (convertible) {
					pv.setConvertedValue(convertedValue);
				}
				deepCopy.add(pv);
			}
			else if (convertible && originalValue instanceof TypedStringValue &&
					!((TypedStringValue) originalValue).isDynamic() &&
					!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
				pv.setConvertedValue(convertedValue);
				deepCopy.add(pv);
			}
			else {
				resolveNecessary = true;
				deepCopy.add(new PropertyValue(pv, convertedValue));
			}
		}
	}
	if (mpvs != null && !resolveNecessary) {
		mpvs.setConverted();
	}

	// 将深拷贝属性数组填充到beanWrapper中。这里就真正的将属性值填充到了bean上，实现了
	try {
		bw.setPropertyValues(new MutablePropertyValues(deepCopy));
	}
	catch (BeansException ex) {
        ...		
	}
}
```

经过上面的分析，就知道真正的对bean赋值填充是在AbstractAutowireCapableBeanFactory.class类里的applyPropertyValues方法里的，并且是通过对原属性值进行了一次深拷贝，然后将深拷贝后的属性值填充到bean里的。

## 在web容器中初始化spring容器
[深入Spring源码系列（二）——深入Spring容器，通过源码阅读和时序图来彻底弄懂Spring容器（下）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%E6%B7%B1%E5%85%A5Spring%E5%AE%B9%E5%99%A8%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB%E5%92%8C%E6%97%B6%E5%BA%8F%E5%9B%BE%E6%9D%A5%E5%BD%BB%E5%BA%95%E5%BC%84%E6%87%82Spring%E5%AE%B9%E5%99%A8%EF%BC%88%E4%B8%8B%EF%BC%89.md)


## 参考
- 《Spring源码深度分析》
- 《Spring技术内幕》

