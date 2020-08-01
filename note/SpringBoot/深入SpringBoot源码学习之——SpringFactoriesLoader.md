## 前言
&emsp;要想深入学习源码，那么就得先学会读懂它的注释，毕竟是一手知识嘛。大家都知道SpringBoot是一款多么优秀的框架，它给Java开发者带来了极大的便利，再也不用去整合SSM了，这里就不继续赞美SpringBoot了。相信大家都会很好奇SpringBoot底层源码是怎么运行的？它是怎么神奇的让我们可以快速开发JAVAEE企业级项目？如何快速整合第三方框架？接下来的深入学习SpringBoot源码系列，让我和各位小伙伴们一同学习SpringBoot底层源码。

这里先学习下 SpringFactoriesLoader，这个类在SpringBoot整个底层源码中起到了非常关键的作用，置于有多关键，等读完整片文章你们就知道了。

**SpringBoot版本：5.2.1.RELEASE**

## 正文

### 1. 话不多说，源码注释解读一波
```
General purpose factory loading mechanism for internal use within the framework.

<p>{@code SpringFactoriesLoader} {@linkplain #loadFactories loads} and instantiates
 factories of a given type from {@value #FACTORIES_RESOURCE_LOCATION} files which
 may be present in multiple JAR files in the classpath. The {@code spring.factories}
 file must be in {@link Properties} format, where the key is the fully qualified
 name of the interface or abstract class, and the value is a comma-separated list of
 implementation class names. For example:
 
```
翻译过来就是：
```
SpringFactoriesLoader是用于Spring框架内部的通用工厂加载机制。

SpringFactoriesLoader通过loadFactories方法来加载并实例化来自FACTORIES_RESOUCE_LOCATION路径中的文件给定的工厂类型，
而这些文件可能包含在类路径的jar包中。这些文件通常都命名为spring.factories，并且都是以properties属性作为格式，文件中key表示
的是接口或者抽象类的全限定名称，而值是以逗号分隔的实现类的名称列表。
```

### 2. 上源码
&emsp;首先，可以看到SpringFactoriesLoader是final类，final修饰的类是不可以被继承，类中的方法都是不可以被覆盖的，且默认都是final修饰的方法，可以猜想到SpringFactoriesLoader类在被设计之初，是不想开发者继承该类并对该类进行扩展。所以，如果在开发中不想让别人对你的类继承或者扩展，那就用final来修饰吧~~

```
public final class SpringFactoriesLoader {
}
```

下面看下SpringFactoriesLoader类有哪些成员变量？

```
	/**
	 * 寻找工厂的位置
	 * 工厂可以存放在多个jar文件中
	 */
public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";
```

   先来看看spring-boot:2.2.1.RELEASEjar包下，spring.factories文件的内容。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200605074613675.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200605110849246.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020060511090234.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)

在spring.factories文件中，有非常多的工厂类，包括了属性源加载器、错误报告器、容器初始化器、容器监听器等，这些工厂类在SpringBoot中都有非常重要的作用，具体的读者可以自行前往查看。

```
// 自定义的用于存储工厂的缓存
private static final Map<ClassLoader, MultiValueMap<String, String>> cache = new ConcurrentReferenceHashMap<>();

```

&emsp;这里的缓存是通过ConcurrentReferenceHashMap类实现的，对于这个类估计很多读者都是第一次见吧...   下面就简单介绍下ConcurrentReferenceHashMap这个数据结构。

通过阅读ConcurrentReferenceHashMap的类注释，可以总结出以下几点：
1. 它是一个ConcurrentHashMap，它的key和value使用ReferenceType.SOFT或者是ReferenceType.WEAK，即软引用和弱引用。
2. 它可以用作Collections.synchronizedMap(new WeakHashMap<K, Reference<V>>())的替代方法。
3. 它遵循ConcurrentHashMap相同的设计约束，但不同的是它还支持null键和null值。
4. 它默认使用的是SoftReference软引用。
5. 使用软引用就意味着，在进行下一次GC时，如果即将发生OOM，GC就会把软引用指向的对象给回收掉。这一特性适合用作缓存处理。


#### 2.1 loadFactories方法
&emsp;下面先来看下loadFactories方法的注释。
```
	/**
	 * Load and instantiate the factory implementations of the given type from
	 * {@value #FACTORIES_RESOURCE_LOCATION}, using the given class loader.
	 * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
	 * <p>If a custom instantiation strategy is required, use {@link #loadFactoryNames}
	 * to obtain all registered factory names.
	 * @param factoryType the interface or abstract class representing the factory
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 * @throws IllegalArgumentException if any factory implementation class cannot
	 * be loaded or if an error occurs while instantiating any factory
	 * @see #loadFactoryNames
	 */
```
翻译过来就是：
&emsp;loadFactories方法通过类加载器来加载并且实例化FACTORIES_RESOURCE_LOCATION路径文件中定义的工厂实现。在返回工厂之前，都会通过AnnotationAwareOrderComparator这个类来进行排序。如果需要自定义实例化策略，请使用loadFactoryNames去获取所有注册的工厂名称。
&emsp;loadFactories方法中，入参factoryType表示工厂类的接口或者抽象类；入参classLoader表示加载工厂的类加载器，如果为空则会使用默认的类加载器。

```
public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
		Assert.notNull(factoryType, "'factoryType' must not be null");
		// 类加载器
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			// 如果为空则用系统默认类加载器
			classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
		}
		// 通过获取所有工厂实现类的名称集合
		List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoaderToUse);
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
		}
		List<T> result = new ArrayList<>(factoryImplementationNames.size());
		for (String factoryImplementationName : factoryImplementationNames) {
			// 实例化工厂实现类，然后添加进result集合中
			result.add(instantiateFactory(factoryImplementationName, factoryType, classLoaderToUse));
		}
		// 通过AnnotationAwareOrderComparator#sort方法对工厂名称进行排序
		AnnotationAwareOrderComparator.sort(result);
		return result;
	}
```

```
	private static <T> T instantiateFactory(String factoryImplementationName, Class<T> factoryType, ClassLoader classLoader) {
		try {
			// 通过classUtils工具类获取工厂实现类的Class对象
			Class<?> factoryImplementationClass = ClassUtils.forName(factoryImplementationName, classLoader);
			if (!factoryType.isAssignableFrom(factoryImplementationClass)) {
				throw new IllegalArgumentException(
						"Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
			}
			// 通过反射工具创建工厂类实例对象
			return (T) ReflectionUtils.accessibleConstructor(factoryImplementationClass).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException(
				"Unable to instantiate factory class [" + factoryImplementationName + "] for factory type [" + factoryType.getName() + "]",
				ex);
		}
	}
```


&emsp;可以看到，loadFactories方法逻辑还是比较简单的，作用也比较明确，即：
① 通过classLoader去加载工厂获取其对应类名称；
② 通过instantiateFactory方法实例化工厂类；
③ 通过AnnotationAwareOrderComparator#sort方法对工厂进行排序；

看下哪些地方用到了loadFactories方法：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200605103200752.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
上图中的ConfigFileApplicationListener类是用于加载application配置文件的监听器类，对于application配置文件的加载，后面会详细讲解~~

#### 2.2 loadFactoryNames方法
&emsp;由于loadFactoryNames方法的注释和loadFactories内容一样，所以这里就不写出来了。

```
	public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
		// 获取到factoryType工厂类型
		String factoryTypeName = factoryType.getName();
		// 加载SpringFactories，如果没有则返回一个空集合
		return loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
	}
```

```
	private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
		// 从缓存中获取已经加载过的SpringFactories
		MultiValueMap<String, String> result = cache.get(classLoader);
		if (result != null) {
			return result;
		}

		try {
			Enumeration<URL> urls = (classLoader != null ?
					// 通过类加载器读取类路径下的spring.factories文件，然后封装成URL存储于Enumeration中
					classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
			result = new LinkedMultiValueMap<>();
			// 遍历urls，再将url封装成UrlResource对象
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				UrlResource resource = new UrlResource(url);
				/**
				 *  通过PropertiesLoaderUtils属性加载器去加载spring.factories中的value值。
				 *  这里的Properties是继承了HashTable的一个属性，key和value就对应着spring.factories文件里的key和value。
				 *  在PropertiesLoaderUtils中，底层是通过IO流读取的文件数据，这里就不细说了。
				 */ 
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				for (Map.Entry<?, ?> entry : properties.entrySet()) {
					String factoryTypeName = ((String) entry.getKey()).trim();
					// 遍历获取工厂实现类名称
					for (String factoryImplementationName : StringUtils.commaDelimitedListToStringArray((String) entry.getValue())) {
						result.add(factoryTypeName, factoryImplementationName.trim());
					}
				}
			}
			// 将获取结果存入缓存中
			cache.put(classLoader, result);
			return result;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load factories from location [" +
					FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
	}
```

通过IDEA来看下有哪些地方用到了loadFactoryName的方法：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200605114254699.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
可以看到上面图片中SpringApplication中是哪个地方用到了loadFactoryNames方法，
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200605114649305.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
进入到SpringApplication的构造器中，会调用setInitializers方法，这个方法是用于设置初始化器，初始化器也是非常重要的一个知识点，后面会详细介绍。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200605114815497.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
可以看到，在SpringApplication中的getSpringFactoriesInstance方法中，调用了SpringFactoriesLoader#loadFacotyNames。SpringFactoriesLoader#loadFactoryNames方法调用完之后就获取到了spring.factories中的value值，并存储到Set集合中，接着就调用createSpringFactoriesInstances方法通过反射工具实例化Set集合中存储的工厂类，经过排序之后再返回给上一层调用。

下面用一张简单的书序图描述下SpringFactoriesLoader在SpringBoot中的调用过程。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200605135730713.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)

## 3. 总结
学习完SpringFactoriesLoader源码之后，算是真正踏入学习SpringBoot源码的大门了，通过SpringFactoriesLoader，SpringBoot得以去加载类路径下jar包中的spring.factories文件，才能够读取该文件中的各种工厂类：包括监听器、初始化器、属性源加载器等，而通过这些工厂类，才得以让SpringBoot变得这么强大！！！接下来几篇关于SpringBoot源码将会对监听器、初始化器、属性源以及配置加载等机制进行深入的分析~~

> 觉得作者写的不错的点个赞，关注作者。
> 本文 Github https://github.com/coderbruis/JavaSourceLearning 已收录，更多源码文章以及源码在github中可以学习。
 