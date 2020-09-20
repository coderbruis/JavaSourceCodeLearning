## 前言 

在Dubbo的SPI机制（上）中，大致介绍了下ExtensionLoader的几个重要的成员变量，本篇文章将重点讲解下ExtensionLoader类、成员变量以及getExtension()等核心方法逻辑。

## 正文

### 1. ExtensionLoader的成员变量

- ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS

    该变量主要作用是扩展带加载器缓存，key表示扩展点类的Class对象，value表示该扩展点类型的ExtensionLoader，表示为ExtensionLoader<T>。
   
- ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES

    该变量表示的是扩展点实现类对象的缓存，key表示扩展点类Class对象，value表示的是扩展点实现类对象实例。
    
- Class<?> type

    该变量表示此时ExtensionLoader<T>的类型，type就是代表的T。ExtensionLoader默认的T类型为ExtensionFactory。
    
- ExtensionFactory objectFactory

    该变量表示的是扩展点加载器的扩展工厂，从ExtensionLoader的构造方法可以清楚，ExtensionLoader<ExtensionFactory>中的objectFactory默认为空。

- ConcurrentMap<Class<?>, String> cachedNames

    这个变量表示的是扩展点类实现名称的缓存，key是对应的T的扩展点实现类Class，value是扩展点名称

- Holder<Map<String, Class<?>>> cachedClasses

    这个变量表示的是扩展点实现类Class缓存 当前T的扩展点名称作为key，value是对应的扩展点实现类Class，这其中cachedNames和cahcedClasses是"KV相反关系"

- Map<String, Object> cachedActivates

    这个变量表示的是@Activate注解的实现类缓存

- Holder<Object> cachedAdaptiveInstance

    这个变量表示的是扩展点适配类型实例对象缓存

- Class<?> cachedAdaptiveClass

    这个变量表示的是适配类型扩展点实现类对象的缓存

- String cachedDefaultName

    这个变量表示的是当前扩展类加载器@SPI注解的value值，即默认的扩展名
    
- LoadingStrategy[] strategies

    这个变量表示的是扩展点配置文件的加载策略

- Set<Class<?>> cachedWrapperClasses

    这个变量表示的是包装类对象缓存
    
在ExtensionLoader中， 依靠以上的各种缓存来实现扩展点实现类加载， 并且大量用到了双重检测，防止指令重排序的原理。

### 2. ExtensionLoader的工作原理

> 由于在本人的github仓库中fork的Dubbo官方源码中，有一SourceCode-of-Translation分支已经标注有详细的注释，所以这里就不粘贴出来了

在ExtensionLoader中，有三个逻辑入口，分别为getExtension、getAdaptiveExtension、getActivateExtension，分别是获取
普通扩展类、获取自适应扩展类、获取自动激活的扩展类。
