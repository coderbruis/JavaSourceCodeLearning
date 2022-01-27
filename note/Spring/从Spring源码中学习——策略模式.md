<!-- TOC -->
- [前言](#前言)
- [正文](#正文)    
    - [一、先看看初学者都会的多重if-else判断](#一先看看初学者都会的多重if-else判断)    
    - [二、策略模式实现](#二策略模式实现)        
        - [2.1 定义一个策略接口: Strategy.class](#21-定义一个策略接口-strategyclass)        
        - [2.2 创建接口的实现类](#22-创建接口的实现类)        
        - [2.3 创建Context类](#23-创建context类)        
        - [2.4 创建实现类](#24-创建实现类)    
    - [三、策略模式的优缺点](#三策略模式的优缺点)        
        - [3.1 优点](#31-优点)        
        - [3.2 缺点](#32-缺点)        
        - [3.3 策略模式中的三个角色](#33-策略模式中的三个角色)    
    - [四、在Spring源码中应用的策略模式](#四在spring源码中应用的策略模式)        
        - [4.1 Spring源码中的BeanDefinitionReader](#41-spring源码中的beandefinitionreader)
        - [4.2 Spring源码中ResourceLoader](#42-spring源码中resourceloader)    
    - [五、总结](#五总结)<!-- /TOC -->

## 前言

策略模式听起来高大上，吓退了很多初学小伙伴，下面就来揭开策略模式的神秘面纱，并看看在Spring源码中是如何应用到策略模式的。

## 正文

### 一、先看看初学者都会的多重if-else判断

```Java
public int count(int num1, int num2, String operation) {
    if (operation.equals("+")) {
        return num1 + num2;
    } else if (operation.equals("-")) {
        return num1 - num2;
    } else if (operation.equals("*")) {
        return num1 * num2;
    } else if (operation.equals("/")) {
        return num1 / num2;
    }
}
```

问题分析：

有没有觉得这些算法都耦合在一起了，如果需要改某个算法，而要改动整个count()方法，如果其中某个算法出错了，会影响整个count()方法。

### 二、策略模式实现

#### 2.1 定义一个策略接口: Strategy.class
```Java
public interface Strategy {
    public int doOperation(int num1, int num2);
}
```

#### 2.2 创建接口的实现类

Add.java
```Java
public class Add implements Strategy{
   @Override
   public int doOperation(int num1, int num2) {
      return num1 + num2;
   }
}
```

Substract.java
```Java
public class Substract implements Strategy{
   @Override
   public int doOperation(int num1, int num2) {
      return num1 - num2;
   }
}
```

Multiply.java
```Java
public class Multiply implements Strategy{
   @Override
   public int doOperation(int num1, int num2) {
      return num1 * num2;
   }
}
```

Divide.java
```Java
public class Divide implements Strategy{
   @Override
   public int doOperation(int num1, int num2) {
      return num1 / num2;
   }
}
```

#### 2.3 创建Context类

```Java
public class Context {
   private Strategy strategy;
   
   public SetStrategy(Strategy strategy){
      this.strategy = strategy;
   }
 
   public int executeStrategy(int num1, int num2){
      return strategy.doOperation(num1, num2);
   }
}
```

#### 2.4 创建实现类
```Java
public class StrategyPatternDemo {
   public static void main(String[] args) {
      Context context = new Context();    
      context.SetStrategy(new Add());    
      System.out.println("10 + 5 = " + context.executeStrategy(10, 5));
      context.SetStrategy(new Substract());    
      System.out.println("10 - 5 = " + context.executeStrategy(10, 5));
      context.SetStrategy(new Multiply());  
      System.out.println("10 * 5 = " + context.executeStrategy(10, 5));
      context.SetStrategy(new Divide());  
      System.out.println("10 / 5 = " + context.executeStrategy(10, 5));
    }
}
```


### 三、策略模式的优缺点

#### 3.1 优点

- 策略模式实现类的算法或行为可以自由切换
- 避免使用多重条件判断
- 扩展性能好

#### 3.2 缺点

- 策略类增多
- 所有的策略类需要向外暴露

#### 3.3 策略模式中的三个角色

1. Context封装角色：也叫上下文角色，起到封装作用，屏蔽了高层模块对策略的直接访问
2. 策略抽象角色：定义策略实现的接口
3. 策略实现类：实现策略接口，实现具体的策略算法或行为内容并向外界暴露

### 四、在Spring源码中应用的策略模式

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190801164743575.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)


在上图中，BeanDefinition、ResourceLoader、BeanNameGenerator三个接口作为策略接口，而其他的实现类都分别实现了各自的行为用于针对不同的业务场景，那么还有一个Context封装对象，在这里就是ApplicationContext——AbstractXmlApplicationContext里。

#### 4.1 Spring源码中的BeanDefinitionReader

在学习BeanDefinitionReader之前，要先了解一下什么是BeanDefinition

接口BeanDefinition.java
```Java
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;
    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;
    ...
    boolean isSingleton();
    boolean isPrototype();
    boolean isAbstract();
    ...
}
```

可以看到BeanDefinition作为一个接口，主要是用于存储从XML配置文件读取Bean信息到JVM内存的一个载体，具体是存储在了BeanDefinition的实现类——RootBeanDefinition中，下面来看看RootBeanDefinition。

```Java
public class RootBeanDefinition extends AbstractBeanDefinition {
    @Nullable
	private BeanDefinitionHolder decoratedDefinition;
	
	@Nullable
	volatile ResolvableType targetType;
	
	// 用于缓存给定bean定义的解析类型确定类
	@Nullable
	volatile Class<?> resolvedTargetType;
	...
}
```
可以看到RootBeanDefinition不是真正存储Bean信息的载体，继续查看BeanDefinitionHolder
```Java
public class BeanDefinitionHolder implements BeanMetadataElement {

    private final BeanDefinition beanDefinition;

	private final String beanName;

	@Nullable
	private final String[] aliases;
}
```
终于，看到了存储bean用的beanName和别名aliases数组了。具体其他bean信息存储在哪了？本片不在这里进行讲解，感兴趣的小伙伴可以去查看本人写的对Spring解析的博文。

回到正题，我们已经知道了BeanDefinition就是一个存储XML配置文件中bean信息的一个载体，那么这个过程是如何实现的呢？答案就在BeanDefinitionReader的实现类————XmlBeanDefinitionReader里面。

XmlBeanDefinitionReader就是一个策略的具体实现，表示的是一种可以从Xml中获取Bean配置信息的策略，除了这种策略外，还有PropertiesBeanDefinitionReader这种从Properties配置文件获取Bean配置信息的策略。第三节中总结提到在策略模式中有三种角色，1）Context封装角色；2）策略抽象角色；3）策略实现角色。在这里我们已经找到了策略抽象角色——BeanDefinitionReader和策略实现角色——XmlBeanDefinitionReader和PropertiesBeanDefinitionReader，就差Context封装角色了，那么Spring中哪个类充当了这个角色呢？

答案就是——AbstractXmlApplicationContext类

无图无真相，请看下图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190801164812944.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

可以看到，策略实现类XmlBeanDefinitionReader在AbstractXmlApplicationContext中执行了具体的策略执行，也就是后面复杂的从Xml配置文件读取bean配置信息的操作。

#### 4.2 Spring源码中ResourceLoader

下面先看看ResourceLoader的源码，然后再来简单介绍下其作用
```
public interface ResourceLoader {
    
    String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

    Resource getResource(String location);
    
    ClassLoader getClassLoader();
}
```

这里的ResourceLoader就是一个Resource加载器，而Resource是将URL、URI等资源抽象为一个Resource资源对象，方便Spring进一步操作。

下面先来分析下三种角色：
1. AbstractBeanDefinitionReader 作为Context封装角色
2. ResourceLoader作为策略的抽象
3. DefaultResourceLoader和ResourcePatternResolver就是具体的执行策略


上源码
![\[外链图片转存失败(img-3ha67xsg-1564649270087)(A2B888FAB6364C2BB9E772E86AD70D69)\]](https://img-blog.csdnimg.cn/20190801164907970.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190801165001118.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

这里ResourceLoader的实现类是ClassPathXmlApplicationContext，其实ClassPathXMLApplicationContext是DefaultResourceLoader的子类。如下图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190801165041615.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

由于```resourceLoader instanceof ResourcePatternResolver为true```，所以走如下逻辑：

AbstractBeanDefinitionReader.java
```Java
Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
int count = loadBeanDefinitions(resources);
```

AbstractApplicationContext.java
```Java
    @Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}
```

PathMatchingResourcePatternResolver.java
```Java
    @Override
	public Resource[] getResources(String locationPattern) throws IOException {
		
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			
			if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
				
				return findPathMatchingResources(locationPattern);
			}
			else {
				
				return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
			}
		}
		else {
			
			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
					locationPattern.indexOf(':') + 1);
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				
				return findPathMatchingResources(locationPattern);
			}
			else {
				
				return new Resource[] {getResourceLoader().getResource(locationPattern)};
			}
		}
	}
```

由于Spring IOC源码篇幅过长，源码部分就暂时介绍到这里。

### 五、总结

本篇文章通过几个例子介绍了下策略模式，并且深入Spring源码中查看了策略模式的应用场景，然而对于策略模式的精髓学习任然需要持续深入，不然学习到的都只是皮毛而已。

