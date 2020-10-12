## 前言 

在Dubbo的SPI机制上、中两篇文章中，已经讲解了SPI的核心机制，本篇文章讲解SPI中的几个核心注解。

## 正文

再Dubbo的SPI中，核心注解包括了：@SPI、@Adaptive、@Activate。

### 1. @SPI注解

### 2. @Adaptive注解

AdaptiveExtensionFactory 不实现任何具体的功能，而是用来适配 ExtensionFactory 的 SpiExtensionFactory 和 SpringExtensionFactory 这两种实现。AdaptiveExtensionFactory 会根据运行时的一些状态来选择具体调用 ExtensionFactory 的哪个实现。

AdaptiveExtensionFactory会根据运行时状态来决定给ExtensionFactory赋值哪个实现，例如在Dubbo源码本地，使用的是SpiExtensionFactory这个类，而如果
是在Spring环境的话，则会使用SpringExtensionFactory这种实现。适配核心逻辑在AdaptiveExtensionFactory的构造方法里。
### 3. @Activate注解


