## 前言

想要深入学习Dubbo，最好的方式就是阅读并调用Dubbo源码，接下来先来动手搭建一个Dubbo源码环境。

## 正文

### 1. 下载源码

步骤：

1. 先从dubbo源码官网github中fork一份到自己的github仓库中。
    
    ```
        git clone git@github.com:xxxxxxxx/dubbo.git 
    ```
    
2. 使用命令：git branch v2.7.8。 切换到分支2.7.8。

    ```
        git checkout -b dubbo-2.7.8 dubbo-2.7.8
    ```

3. 导入方式（IDEA导入方式）
    
    可以通过IDEA ——> File ——> Open ——> pom.xml ——> open as project
    
    然后让IDEA下载相关的依赖，等下载完成即可。

4. mvn命令导入

    ```
        mvn clean install -Dmaven.test.skip=true 
    ```
   
    然后执行下面的命令转换成 IDEA 项目：
    
    ```
        mvn idea:idea
    ```

    如果执行报错了，则执行：
    ``` 
        mvn idea:workspace 
    ```

### 2. 分支切换

本人Fork了官方Dubbo源码到本地仓库，并且新建了一个分支名为：SourceCode-of-Translation

该分支主要用于进行源码注释，每个核心功能代码都有详细注释，欢迎大家Fork到本地，然后进行注释查看。

> 地址为：[SourceCode-of-Translation](https://github.com/coderbruis/dubbo)    下载到本地后，只需要切换到SourceCode-of-Translation分支即可。
