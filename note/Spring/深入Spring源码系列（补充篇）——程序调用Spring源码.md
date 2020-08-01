## 前言

前段时间一直忙于其他事情，没空下来继续研究Spring源码。最近有空又重拾Spring源码，打算继续深入研究。

之前下载好了Spring源码之后，并成功导入到IDEA中了，可是光导入源码但是没有调用Spring源码就显得特别的菜了，且不利于深入学习Spring源码。本人花了点时间来尝试使用IDEA程序调用Spring源码，遂写下这篇文章来记录下实现过程。

## 正文

在系列文章第一篇中，已经成功下载好了Spring源码并使用Gradle导入源码，具体步骤这里不再赘述。

### 一、使用Spring源码的test测试文件来尝试启动Spring源码

如图操作：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190626095214471.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
启动test()方法，尝试调用Spring源码。结果会发现报错了
```
CoroutinesUtils cannot be resolved，没有CoroutinesUtils类
```
如下图位置：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190626095615609.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
（由于本人已经处理了这个错误，所以上图没有报出错误）

### 二、解决问题

查了一下错误，发现是因为CoroutinesUtils是kotlin的一个工具类，Spring源码包读取不到。

使用如下方法即可解决该工具类读不到的问题：

 1. 找到spring-framework-master\spring-core-coroutines\build\libs 下面的spring-core-coroutines-5.2.0.BUILD-SNAPSHOT.jar包
 2. 选中改jar包右键——> Add as Library，将jar包导入依赖
 3. 选中工具栏Build——> Rebuild Project
 等Spring源码重新构建之后，就可以使用本地程序调用Spring源码了。

### 三、验证是否成功调用Spring源码
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190626100508414.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
可以看到，在源码AbstractApplicationContext.java位置，本人添加了中文注释，一会儿使用Debug模式调用测试方法，看是否能进入该源码类的方法里，能进入则证明调用Spring源码成功。

![在这里插入图片描述](https://img-blog.csdnimg.cn/2019062610114672.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190626101205966.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190626101218898.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190626101234310.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
这样，程序就成功地调用了Spring源码。接下来就可以愉快的研究Spring源码了：）