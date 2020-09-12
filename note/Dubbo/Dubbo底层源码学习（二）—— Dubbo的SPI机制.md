## 前言 

Dubbo的SPI机制是什么呢？首先，SPI全称为（Service Provider Interface），主要是被框架开发人员使用的一种技术。Dubbo的SPI机制提供的是一种将服务Provider接口定义在META-INF中，当Dubbo服务启动时，
通过SPI加载机制加载文件中的接口，从而使用那些被加载的接口。那么这么做的目的是什么呢？这么做的目的就是为了更好地达到 OCP 原则（即“对扩展开放，对修改封闭”的原则）。这种"对扩展开放，对修改封闭"的原则能
在维持内核功能稳定的基础上，更好的对系统功能进行扩展。换句话说，基于Dubbo SPI加载机制，让整个框架的接口和具体实现完全解耦，从而奠定了整个框架良好可扩展性的基础。

SPI中两个核心就是：扩展点和扩展点实现类。在JDK SPI和Dubbo SPI中，都是通过在配置文件中定义KV键值对来定义对应的扩展点和扩展点实现类，其中Key表示的是扩展点名称，Value表示的是扩展点实现类的全路径名称。

Dubbo SPI是参考了JDK原生的SPI机制，进行了性能优化以及功能增强。

## 正文

### 1. Java SPI

Javs SPI使用的是策略模式，一个接口多种实现。我们只负责声明接口，具体的实现并不在程序中直接确定，而是由程序之外的配置掌控，用于具体实现的装配。

Java SPI的定义及使用步骤如下：
1. 定义一个接口以及对应的方法
2. 编写该接口的一个实现类
3. 在META-INF/services/目录下，创建一个以接口全路径命名的文件，如com.test.spi.PrintService
4. 文件内容为具体实现类的全路径名，如果有多个，则用分行符分隔
5. 在代码中通过java.util.ServiceLoader来加载具体的实现类

在com.test.spi包目录下，定义了一个PrintService接口和一个PrintServiceImpl实现类，然后在resources目录下定义了一个META-INF/services/com.test.spi.PrintService，注意这里定义的是一个
全路径名称的文件。

```
public interface Printservice (
    void printlnfo();
}
```

```
public class PrintServicelmpl implements Printservice { 
    @Override
    public void printlnfo() {
        System.out.println("hello world");
    } 
}
```

```
public static void main(String[] args) ( 
    ServiceLoader<PrintService> serviceServiceLoader =
    ServiceLoader.load(PrintService.class);
    for (Printservice printservice : serviceServiceLoader) ( 
        //此处会输出：hello world 获取所有的SPI实现，循环调用
        printService.printInfo(); printlnfo()方法，会打印出 hello world
    } 
}
```

在JDK SPI中，是通过ServiceLoader来获取所有接口实现的。

最常见的JDK SPI就是com.mysql.cj.jdbc.Driver 接口，它的实现类是有用户通过配置文件来设定的，Driver接口就是一个扩展点。

### 2. Dubbo SPI

Dubbo SPI没有直接使用Java SPI而是在它的思想上又做了一定的改进，形成了一套自己的配置规范和特性。同时，Dubbo SPI又兼容Java SPI服务在启动的时候，Dubbo就会查找这些扩展点的所有实现。

Dubbo SPI之于JDK SPI，做到了三点优化：
1. 不同于JDK SPI会一次性实例化扩展点所有实现，因为JDK SPI有扩展实现，则初始化会很耗时，并且如果没有用上也要加载，则会很浪费资源。而Dubbo SPI只会加载扩展点，而不会对其进行初始化，并且Dubbo SPI中
会根据不同的实现类来缓存到内存中，性能上得到了很大的优化。
2. JDK SPI如果对扩展加载失败，则连扩展的名称都获取不到，并且失败原因会被吃掉，而Dubbo SPI则会将异常堆栈保留下来，方便后序对其异常信息进行分析。
3. Dubbo SPI增加了对IOC和AOP的支持，在Dubbo中，一个扩展点可以通过setter来注入到其他扩展点中。


这里再次统一一下SPI的核心概念：

- 扩展点
- 扩展点的实现类

Dubbo SPI的核心就是围绕着怎么获取扩展点以及扩展点实现类来进行的。那么现在需要先知道，扩展点以及扩展点实现类存放位置？

#### 2.1 Dubbo SPI配置文件及其存放位置

在Dubbo SPI中，按照用途将SPI配置文件分为三类以META-INF开头的目录（META-INF开头目录通常都存放在类路径下）：

- META-INF/services/：该目录的SPI配置文件是用来兼容JDK SPI的
- META-INF/dubbo/：用来存放用户自定义的SPI配置文件
- META-INF/dubbo/internal：该目录用于存放Dubbo内部使用的SPI配置文件

在SPI配置文件中，都是以KV格式存在的配置内容，例如Dubbo源码中的SPI配置文件内容：
```
dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
```

key表示的是扩展点名称，而value表示的是扩展点的实现类的全限定类名。另外，SPI配置文件名称就是扩展点实现类的全限定类名。
![Dubbo-SPI-01](http://note.youdao.com/yws/res/28338/6061E217061B4FEBB4A05588B7DD42BC)

那么，扩展点及其实现类以及存放在SPI文件中了，那么Dubbo应用程序该如何将其加载进行JVM内存中呢？

#### 2.2 Dubbo的ExtensionLoader

ExtensionLoader即扩展点加载器，它是Dubbo SPI的核心，负责加载扩展点即扩展点实现类，先看下其内部重要的几个成员变量：


![Dubbo-SPI-02](http://note.youdao.com/yws/res/28352/F560C187FC8D4B7EB9F54DCE4103A5EB)

![Dubbo-SPI-03](http://note.youdao.com/yws/res/28354/6811B40CCC184FC1B2B2C5E557D6D534)

这里的扩展点加载策略有三种：
- DubboInternalLoadingStrategy（加载内部的SPI）
- DubboLoadingStrategy（加载用户自定义的SPI）
- ServiceLoadingStrategy（加载用于兼容JDK的SPI）

并且其内部默认优先级为：DubboInternalLoadingStrategy > DubboLoadingStrategy > ServiceLoadingStrategy

![image](http://note.youdao.com/yws/res/28367/9F45ECAA6A1F418582172C41137FB3C9)

上图清楚的展示了LoadingStrategy接口及其实现类的关系。LoadingStrategy继承了Prioritized，因而其实现类会有优先级之分，而Dubbo默认是使用的DubboInternalLoadingStrategy，查看其三个类的源码：

```
public class DubboInternalLoadingStrategy implements LoadingStrategy {

    // 表示要加载的目录位置
    @Override
    public String directory() {
        return "META-INF/dubbo/internal/";
    }

    // 获取优先值，用于进行Comparable接口的compareTo优先级比较
    @Override
    public int getPriority() {
        return MAX_PRIORITY;
    }
}
```

```
public class DubboLoadingStrategy implements LoadingStrategy {

    // 表示要加载的目录位置
    @Override
    public String directory() {
        return "META-INF/dubbo/";
    }

    @Override
    public boolean overridden() {
        return true;
    }

    // 获取优先值，用于进行Comparable接口的compareTo优先级比较
    @Override
    public int getPriority() {
        return NORMAL_PRIORITY;
    }


}
```

```
public class ServicesLoadingStrategy implements LoadingStrategy {

    // 表示要加载的目录位置
    @Override
    public String directory() {
        return "META-INF/services/";
    }

    @Override
    public boolean overridden() {
        return true;
    }

    // 获取优先值，用于进行Comparable接口的compareTo优先级比较
    @Override
    public int getPriority() {
        return MIN_PRIORITY;
    }

}
```

这里的MAX_PRIORITY、NORMAL_PRIORITY和MIN_PRIORITY时定义在Prioritized这个接口中的，查看一下Prioritized中定义的值以及实现的compareTo方法：

```
    /**
     * The maximum priority
     */
    int MAX_PRIORITY = Integer.MIN_VALUE;

    /**
     * The minimum priority
     */
    int MIN_PRIORITY = Integer.MAX_VALUE;

    /**
     * Normal Priority
     */
    int NORMAL_PRIORITY = 0;

    /**
     * Get the priority
     *
     * @return the default is {@link #MIN_PRIORITY minimum one}
     */
    default int getPriority() {
        return NORMAL_PRIORITY;
    }

    @Override
    default int compareTo(Prioritized that) {
        return compare(this.getPriority(), that.getPriority());
    }
```

所以在Dubbo中，默认的优先级为：DubboInternalLoadingStrategy > DubboLoadingStrategy > ServiceLoadingStrategy

即优先加载：META-INF/dubbo/internal目录下的SPI配置文件。