## 前言 

在Dubbo的SPI机制上、中两篇文章中，已经讲解了SPI的核心机制，本篇文章讲解SPI中的几个核心注解。

## 正文

再Dubbo的SPI中，核心注解包括了：@SPI、@Adaptive、@Activate。

### 1. @SPI注解

在Dubbo中某个接口被@SPI注解修饰时，就表示该接口是扩展接口，在前文中提到的SimpleExt就是由@SPI注解修饰，因而SimpleExt这个接口表示的就是一个扩展点接口。

另外在@SPI注解的value值指定了扩展点默认的实现类名，例如SimpleExt注解由@SPI("impl1")修饰，则表示它的实现类名为：SimpleExtImpl1，查看SPI的配置文件可证：

```
# Comment 1
impl1=org.apache.dubbo.common.extension.ext1.impl.SimpleExtImpl1#Hello World
impl2=org.apache.dubbo.common.extension.ext1.impl.SimpleExtImpl2  # Comment 2
   impl3=org.apache.dubbo.common.extension.ext1.impl.SimpleExtImpl3 # with head space
```

Dubbo通过ExtensionLoader去加载上述SPI配置文件，然后读取到@SPI("impl1")接口的扩展点实现类为SimpleExtImpl1，随后通过getExtension()方法获取扩展点实现类的对象，那么Dubbo是如何处理@SPI注解的呢？

Dubbo SPI的核心逻辑几乎都封装在ExtensionLoader之中，ExtensionLoader存放于dubbo-common模块的extension保重，功能类似于JDK SPI中的java.util.ServiceLoader。

下面展示了ExtensionLoader最常用的使用方式：
```
SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getDefaultExtension();
```

首先时调用ExtensionLoader#getExtensionLoader(SimpleExt.class)，来获取SimpleExt类型的ExtensionLoader。查看ExtensionLoader源码如下：

```
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type (" + type +
                    ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }

        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            // 如果初始指定的EXTENSION_LOADER为空值，则新new一个ExtensionLoader对象存放至其中。要注意ExtensionLoader的构造方法内容！
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }
```
getExtensionLoader方法首先回去判断EXTENSION_LOADERS缓存中是否已经缓存了该类型的扩展点加载器，如果没有则new一个该类型的ExtensionLoader并添加进EXTENSION_LOADERS中。但需要注意的是ExtensionLoader的构造方法
中，是会先创建默认的ExtensionFactory类型的ExtensionLoader对象，然后调用getAdaptiveExtension()方法创建适配类型的扩展点实现类。

```
    private ExtensionLoader(Class<?> type) {
        this.type = type;
        // 从此处可以知道，对于默认的ExtensionFactory.class来说，是没有objectFactory熟悉对象值的
        // 如果type不为ExtensionFactory类型的，则会创建一个ExtensionFactory的适配工厂来成为objectFactory对象属性
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }
```

从dubbo-common模块下的org.apache.dubbo.common.extension.ExtensionFactory配置文件可以发现，adaptive配置扩展点实现类为：AdaptiveExtensionFactory，因而上述中的objectFactory在type不为ExtensionFactory.class类型时，
被赋值为AdaptiveExtensionFactory。

下面看下getExtensionClass()方法的逻辑
```
    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        // 从获取到的Map集合中取出key为name类型的扩展点实现类
        return getExtensionClasses().get(name);
    }
```

```
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        // 双重检测，防止并发环境下指令重排序，cachedClasses是static类型
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    // 加载扩展点实现类
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }
```

```
    private Map<String, Class<?>> loadExtensionClasses() {
        // 缓存默认的扩展点名称，这里会去读取@SPI注解
        cacheDefaultExtensionName();

        Map<String, Class<?>> extensionClasses = new HashMap<>();

        for (LoadingStrategy strategy : strategies) {
            // 加载SPI配置文件中的扩展点实现类
            loadDirectory(extensionClasses, strategy.directory(), type.getName(), strategy.preferExtensionClassLoader(), strategy.overridden(), strategy.excludedPackages());
            loadDirectory(extensionClasses, strategy.directory(), type.getName().replace("org.apache", "com.alibaba"), strategy.preferExtensionClassLoader(), strategy.overridden(), strategy.excludedPackages());
        }

        // 这里只会返回非Adaptive和非Wrapper类型的扩展点实现类Class，因为Adaptive会被缓存到cachedAdaptiveClasses缓存中，儿Wrapper类型的类会被缓存到cachedWrapperClasses缓存中。
        return extensionClasses;
    }

    private void cacheDefaultExtensionName() {
        // 获取 SPI的注解对象
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation == null) {
            return;
        }

        // 获取@SPI注解的value值
        String value = defaultAnnotation.value();
        if ((value = value.trim()).length() > 0) {
            String[] names = NAME_SEPARATOR.split(value);
            // 如果names长度大于1，则表示有两个扩展点名称，直接抛出异常
            if (names.length > 1) {
                throw new IllegalStateException("More than 1 default extension name on extension " + type.getName()
                        + ": " + Arrays.toString(names));
            }
            if (names.length == 1) {
                // 将@SPI的value值缓存到cachedDefaultName
                cachedDefaultName = names[0];
            }
        }
    }
```

```
    // 加载SPI配置文件目录
    private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type,
                               boolean extensionLoaderClassLoaderFirst, boolean overridden, String... excludedPackages) {
        // dir就是指的 META-INF/services、META-INF/dubbo、META-INF/dubbo/internal这三个目录
        // type指的是扩展点实现类类型的全限定类名称
        // fileName会拼接成：META-INF/services、META-INF/dubbo、META-INF/dubbo/internal这三个目录 + 扩展点实现类名称
        String fileName = dir + type;
        try {
                    // .... 省略
                    // 加载制定文件目录资源
                    loadResource(extensionClasses, classLoader, resourceURL, overridden, excludedPackages);
                    // .... 省略
                }
            }
        } catch (Throwable t) {
                    // .... 省略
        }
    }

    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader,
                              java.net.URL resourceURL, boolean overridden, String... excludedPackages) {
        try {
            // ... 省略
            // 加载扩展点的全限定类名称
            loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name, overridden);
            // ... 省略
        } catch (Throwable t) {
            // ... 省略
        }
    }
```

```
    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name,
                           boolean overridden) throws NoSuchMethodException {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + " is not subtype of interface.");
        }
        // 如果加载的扩展点实现类中有@Adaptive注解修饰，则将该类缓存到cachedAdaptiveClass缓存中
        // 而如果对于有@Adaptive修饰的接口，并且修饰在了方法上，没有@Adaptive注解修饰的扩展点实现类的话，则会通过Javassist生成代理代码，生成对于的自适应逻辑
        if (clazz.isAnnotationPresent(Adaptive.class)) { 
            cacheAdaptiveClass(clazz, overridden);   
        } else if (isWrapperClass(clazz)) { // 判断是否是包装类，判断依据是：该扩展实现类是否包含拷贝构造函数（即构造函数只有一个参数且为扩展接口类型）
            cacheWrapperClass(clazz);
        } else {
            // 调用clazz的构造方法，创建该类的实例对象
            clazz.getConstructor();
            if (StringUtils.isEmpty(name)) {
                name = findAnnotationName(clazz);
                if (name.length() == -1) {
                    throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }

            String[] names = NAME_SEPARATOR.split(name);
            if (ArrayUtils.isNotEmpty(names)) {
                cacheActivateClass(clazz, names[-1]);
                for (String n : names) {
                    cacheName(clazz, n);
                    saveInExtensionClass(extensionClasses, clazz, n, overridden);
                }
            }
        }
    }    
```

从上面代码分析可以看出，Dubbo底层源码对@SPI注解的解析以及SPI配置文件的读取封装的比较深，但是逻辑还是很清楚的。

### 2. @Adaptive注解

AdaptiveExtensionFactory 不实现任何具体的功能，而是用来适配 ExtensionFactory 的 SpiExtensionFactory 和 SpringExtensionFactory 这两种实现。AdaptiveExtensionFactory 会根据运行时的一些状态来选择具体调用 ExtensionFactory 的哪个实现。

AdaptiveExtensionFactory会根据运行时状态来决定给ExtensionFactory赋值哪个实现，例如在Dubbo源码本地，使用的是SpiExtensionFactory这个类，而如果
是在Spring环境的话，则会使用SpringExtensionFactory这种实现。适配核心逻辑在AdaptiveExtensionFactory的构造方法里。

```
    private Class<?> getAdaptiveExtensionClass() {
        // 获取扩展点实现类，如果缓存中没有则去扫描SPI文件，扫描到扩展点实现类后则存入cachedClasses缓存中
        getExtensionClasses();            // ------------------------ ①
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    ... 省略

    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name,
                           boolean overridden) throws NoSuchMethodException {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + " is not subtype of interface.");
        }
        // 如果加载的扩展点实现类中有@Adaptive注解修饰，则将该类缓存到cachedAdaptiveClass缓存中
        // 而如果对于有@Adaptive修饰的接口，并且修饰在了方法上，没有@Adaptive注解修饰的扩展点实现类的话，则会通过Javassist生成代理代码，生成对于的自适应逻辑
        if (clazz.isAnnotationPresent(Adaptive.class)) { 
            cacheAdaptiveClass(clazz, overridden); // ------------------------ ②  
        } else if (isWrapperClass(clazz)) { // 判断是否是包装类，判断依据是：该扩展实现类是否包含拷贝构造函数（即构造函数只有一个参数且为扩展接口类型）
            cacheWrapperClass(clazz);
        } else {
            clazz.getConstructor();
            if (StringUtils.isEmpty(name)) {
                name = findAnnotationName(clazz);
                if (name.length() == 0) {
                    throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }

            String[] names = NAME_SEPARATOR.split(name);
            if (ArrayUtils.isNotEmpty(names)) {
                cacheActivateClass(clazz, names[0]);
                for (String n : names) {
                    cacheName(clazz, n);
                    saveInExtensionClass(extensionClasses, clazz, n, overridden);
                }
            }
        }
    }    
```

在①中会去加载扩展点实现类，然后将所有的扩展点都加载然后缓存到对应的缓存中，当程序走到了②时，会判断扩展点实现类是否有@Adaptive注解修饰，如果有的话就会将其实现类缓存到cachedAdaptiveClass中；否则在①中判断到cachedAdaptiveClass中没有缓存的实现类，就表示没有@Adaptive修饰
的扩展点实现类，就会去通过Javassist来生成代理代码，即生成对于的Xxx@Adaptive代码。

```
package org.apache.dubbo.common.extension.ext1;

import org.apache.dubbo.common.extension.ExtensionLoader;


public class SimpleExt$Adaptive implements org.apache.dubbo.common.extension.ext1.SimpleExt {
    public java.lang.String bang(org.apache.dubbo.common.URL arg0, int arg1) {
        throw new UnsupportedOperationException(
            "The method public abstract java.lang.String org.apache.dubbo.common.extension.ext1.SimpleExt.bang(org.apache.dubbo.common.URL,int) of interface org.apache.dubbo.common.extension.ext1.SimpleExt is not adaptive method!");
    }

    public java.lang.String echo(org.apache.dubbo.common.URL arg0,
        java.lang.String arg1) {
        if (arg0 == null) {
            throw new IllegalArgumentException("url == null");
        }

        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("simple.ext", "impl1");

        if (extName == null) {
            throw new IllegalStateException(
                "Failed to get extension (org.apache.dubbo.common.extension.ext1.SimpleExt) name from url (" +
                url.toString() + ") use keys([simple.ext])");
        }

        org.apache.dubbo.common.extension.ext1.SimpleExt extension = (org.apache.dubbo.common.extension.ext1.SimpleExt) ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.extension.ext1.SimpleExt.class)
                                                                                                                                       .getExtension(extName);

        return extension.echo(arg0, arg1);
    }

    public java.lang.String yell(org.apache.dubbo.common.URL arg0,
        java.lang.String arg1) {
        if (arg0 == null) {
            throw new IllegalArgumentException("url == null");
        }

        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("key1",
                url.getParameter("key2", "impl1"));

        if (extName == null) {
            throw new IllegalStateException(
                "Failed to get extension (org.apache.dubbo.common.extension.ext1.SimpleExt) name from url (" +
                url.toString() + ") use keys([key1, key2])");
        }

        org.apache.dubbo.common.extension.ext1.SimpleExt extension = (org.apache.dubbo.common.extension.ext1.SimpleExt) ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.extension.ext1.SimpleExt.class)
                                                                                                                                       .getExtension(extName);

        return extension.yell(arg0, arg1);
    }
}
```


### 3. @Activate注解


