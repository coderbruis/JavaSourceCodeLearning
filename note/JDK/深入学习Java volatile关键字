### 前言
在学习volatile之前，先需要了解并发编程的一些基础概念。
并发编程的目的是为了让程序运行得更快，但是，并不是启动的线程越多就能让程序大幅度的并发执行。因为在实际开发中，并发编程将会面临大量的问题，比如上下文切换问题、死锁问题，以及受限于硬件和软件资源限制问题。

> **上下文切换**

时间片是CPU分给各个线程的时间，因为时间片非常短，所以CPU将会在各个线程之间来回切换从而让用户感觉多个程序是同时执行的。CPU通过时间片分配算法来循环执行任务，因此需要在各个线程之间进行切换。从任务的保存到加载过程就称作“上下文切换”。这里需要知道的是上下文切换是需要系统开销的。

**减少上下文切换的措施：**
- 无锁并发编程
    多线程竞争锁时，会引起上下文切换，所以多线程处理数据时，可以用一些方法来避免使用锁，如将数据的ID按照Hash算法取模分段，不同线程处理不同段的数据。
- CAS算法
    Java的Atomic包使用CAS算法来更新数据，不需要加锁。
- 使用最少的线程来完成任务
    避免不需要的线程。
- 协程
    在单线程里实现多任务的调度，并在单线程里维持多个任务间的切换。

> **死锁**

死锁就是两个或者两个以上的线程在执行过程中，由于竞争资源或者由于彼此通信而造成的一种阻塞的现象。

**死锁产生的四个必要性：**
- 互斥条件
- 不可抢占条件
- 请求和保持条件
- 循环等待条件

**避免死锁的几个常见方法：**
- 避免获取同一个锁。
- 避免一个线程在锁内同时占用多个资源，尽量保证每个锁只保持一个资源。
- 尝试使用定时锁，使用tryLock(timeout)来代替使用内部锁机制。
- 对于数据库锁，加锁和解锁必须在同一个数据库连接中，否则会出现解锁失败的问题。

### volatile

在深入volatile之前，先简单的说一说我之前理解的volatile的作用：
1. 是一个轻量级的synchronized。
2. 在多处理器开发中保证的共享变量的“可见性”。
3. 在硬件底层可以禁止指令的重排序。

volatile在底层是如何保证可见性的？

在volatile变量修饰的共享变量进行写操作的时候回多出Lock前缀指令（硬件操作），这个Lock指令在多核处理器下回引发两件事情（硬件操作）：
1. 当前处理器缓存行内的该变量的数据写回到系统内存中。
2. 这个数据写回操作会是其他CPU内缓存内缓存的该变量的数据无效，当处理器对这个数据进行修改操作的时候，会重新从系统内存中读取该数据到处理器缓存里。

Lock引起的将当前处理器缓存该变量的数据写回到系统内存中这一动作，为什么会触发其他CPU缓存行内该变量的数据无效呢？因为变量被修改了，所以其他CPU缓存行内缓存的数据就会无效，但是对于无效了的数据，CPU是怎么操作其变为新的数据呢？这是因为**“缓存一致性协议”**。在多处理器中，为了保证各个处理器的缓存是一致的，就会实现**“缓存一致性协议”**。

> 缓存一致性协议

每个处理器通过嗅探在总线上传播的数据来检查自己缓存的值是否过期，当处理器发现自己缓存行对于数据的内存地址被修改了，就会将当前缓存行设置为无效。当处理器对这个数据进行修改操作时，会重新从系统内存中读取该数据到处理器缓存中。

[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-bTawJOHf-1596181331575)(https://note.youdao.com/yws/api/personal/file/AA87E3ABBEDB4A37B69D8E75B5ED12C1?method=download&shareKey=f9788b07ab72368f3613b2744614eecf)]

为了实现volatile的内存语义，编译期在生成字节码时会对使用volatile关键字修饰的变量进行处理，在字节码文件里对应位置生成一个Lock前缀指令，Lock前缀指令实际上相当于一个内存屏障（也成内存栅栏），它确保指令重排序时不会把其后面的指令排到内存屏障之前的位置，也不会把前面的指令排到内存屏障的后面；即在执行到内存屏障这句指令时，在它前面的操作已经全部完成。

下面代码来演示一下禁止指令重排序：
```
a = 1;            //语句一
b = 2;            //语句二
flag = true;      //语句三，flag为volatile变量
c = 3;            //语句四
d = 4;            //语句五
```
由于flag变量为volatile变量，那么在进行指令重排序的过程的时候，不会将语句3放到语句1、语句2前面，也不会讲语句3放到语句4、语句5后面。但是要注意语句1和语句2的顺序、语句4和语句5的顺序是不作任何保证的，有可能语句一和语句二发生重排序，语句四和语句五发生重排序。并且volatile关键字能保证，执行到语句3时，语句1和语句2必定是执行完毕了的，且语句1和语句2的执行结果对语句3、语句4、语句5是可见的。

> volatile的内存语义

在了解volatile的内存语义之前，先了解一下happens-before规则。

在JMM规范中，happens-before规则有如下：
1. 程序顺序规则：一个线程内保证语义的串行化
2. volatile规则：volatile变量的写先发生于读，这保证了volatile变量的可见性
3. 锁规则：解锁必定发生于加锁之前
4. 传递性：A先于B，B先于C，A一定先于C

**volatile关键字对于变量的影响**

要知道，一个volatile变量的单个读/写操作，与一个普通变量的读/写操作是使用同一个锁来同步，他们之间的执行效果相同。锁的happens-before规则保证释放锁和获取锁的两个线程之间的内存可见性，这以为着一个volatile变量的读，总是能够（任意线程）对这个volatile变量最后的写入。可见对于单个volatile的读/写就具有原子性，但如果是多个volatile操作类似于volatile++这种复合操作，就不具备原子性，是线程不安全的操作。

总结一下volatile变量的特性：
- 可见性：对一个volatile变量的读，总是能看到（任意线程）对这个volatile变量最后的写
入
- 原子性：对一个volatile变量的读，总是能看到（任意线程）对这个volatile变量最后的写
入

**volatile关键字对于线程内存的影响**

对于程序员来说，volatile对于线程内存的影响更为重要。这里就是我们常说的“内存可见性”

从JDK1.5开始，volatile变量的写/读可以实现线程之间通信。从内存语义来说，volatile的读-写与锁的释放-获取有相同的内存效果。**volatile的写与锁的释放有相同的内存语义；volatile的读与锁的获取有相同的内存语义；**

现在有一个线程A和一个线程B拥有同一个volatile变量。当写这个volatile变量时，JMM会把该A线程对应的本地内存中的共享变量值刷新到主内存，当B线程读这个volatile变量时，JMM会把该线程对应的本地内存置为无效。线程接下来将从主内存中读取共享变量。这一写一读，达到的就相当于线程之间通信的效果。

**volatile内存语义的底层实现原理——内存屏障**

为了实现volatile的内存语义，编译期在生成字节码时，会在指令序列中插入内存屏障来禁止特定类型的处理器重排序。下图看看JMM针对编译期指定的volatile重排序的规则表：
[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-nUC08aj9-1596181331578)(https://note.youdao.com/yws/api/personal/file/2DB4A9DDE8D243E680668BEDA1EA931D?method=download&shareKey=03684bd761521c57dfea00548eadeb15)]
就上面的图标，是什么含义呢？
举例来说，

- 第三行最后一个单元格的意思是：在程序中，当第一个操作为普通变量的读或
写时，如果第二个操作为volatile写，则编译器不能重排序这两个操作。
- 当第二个操作是volatile写时，不管第一个操作是什么，都不能重排序。这个规则确保
volatile写之前的操作不会被编译器重排序到volatile写之后。
- 当第一个操作是volatile读时，不管第二个操作是什么，都不能重排序。这个规则确保
volatile读之后的操作不会被编译器重排序到volatile读之前。
- 当第一个操作是volatile写，第二个操作是volatile读时，不能重排序。

重排序的语义都是通过内存屏障来实现的，那内存屏障是什么呢？硬件层的内存屏障分为两种：Load Barrier 和 Store Barrier即读屏障和写屏障，内存屏障的作用有两个：
- 阻止屏障两侧的的指令重排
- 强制把高速缓存中的数据更新或者写入到主存中。Load Barrier负责更新高速缓存， Store Barrier负责将高速缓冲区的内容写回主存

编译器来说对所有的CPU来说插入屏障数最小的方案几乎不可能，下面是基于保守策略的JMM内存屏障插入策略：
1. 在每个volatile写操作前面插入StoreStore屏障
2. 在每个volatile写操作后插入StoreLoad屏障
3. 在每个volatile读前面插入一个LoadLoad屏障
4. 在每个volatile读后面插入一个LoadStore屏障
 
[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-Z1N3KBZj-1596181331583)(https://note.youdao.com/yws/api/personal/file/E11087F8FD5B4673ABD8C58F6F8DA232?method=download&shareKey=cf78d935c04cb11b039399e1d4825b74)]
- StoreStore屏障可以保证在volatile写之前，所有的普通写操作已经对所有处理器可见，StoreStore屏障保障了在volatile写之前所有的普通写操作已经刷新到主存。
- StoreLoad屏障避免volatile写与下面有可能出现的volatile读/写操作重排。因为编译器无法准确判断一个volatile写后面是否需要插入一个StoreLoad屏障（写之后直接就return了，这时其实没必要加StoreLoad屏障），为了能实现volatile的正确内存语意，JVM采取了保守的策略。在每个volatile写之后或每个volatile读之前加上一个StoreLoad屏障，而大多数场景是一个线程写volatile变量多个线程去读volatile变量，同一时刻读的线程数量其实远大于写的线程数量。选择在volatile写后面加入StoreLoad屏障将大大提升执行效率（上面已经说了StoreLoad屏障的开销是很大的）。

[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-pRcUS5Mm-1596181331589)(https://note.youdao.com/yws/api/personal/file/2A92B2D468A345F6A55C75249A89845A?method=download&shareKey=ac99a6bcd169bf4bcda8b0fbd33e0003)]
- LoadLoad屏障保证了volatile读不会与下面的普通读发生重排
- LoadStore屏障保证了volatile读不回与下面的普通写发生重排。

**组合屏障**
LoadLoad,StoreStore,LoadStore,StoreLoad实际上是Java对上面两种屏障的组合，来完成一系列的屏障和数据同步功能：
- LoadLoad屏障：对于这样的语句Load1; LoadLoad; Load2，在Load2及后续读取操作要读取的数据被访问前，保证Load1要读取的数据被读取完毕。
- StoreStore屏障：对于这样的语句Store1; StoreStore; Store2，在Store2及后续写入操作执行前，保证Store1的写入操作对其它处理器可见。
- LoadStore屏障：对于这样的语句Load1; LoadStore; Store2，在Store2及后续写入操作被刷出前，保证Load1要读取的数据被读取完毕。
- StoreLoad屏障：对于这样的语句Store1; StoreLoad; Load2，在Load2及后续所有读取操作执行前，保证Store1的写入对所有处理器可见。它的开销是四种屏障中最大的。在大多数处理器的实现中，这个屏障是个万能屏障，兼具其它三种内存屏障的功能。

> volatile的应用场景

下面来谈谈volatile的应用场景：
1. 状态标志：多个线程以一个volatile变量作为为状态标志，例如完成**初始化**或者**状态同步**。典型例子AQS的同步状态：
```
/**
* The synchronization state.
*/
private volatile int state;
```
2. 一次性安全发布

最典型的例子就是安全的单例模式：
```
private static Singleton instance;
public static Singleton getInstance() {
    //第一次null检查
    if(instance == null) {
        synchronized(Singleton.class) {
            //第二次null检查
            if(instance == null) {
                instance = new Singleton();
            }
        }
    }
    return instance;
}
```
上面这种写法，仍然会出现问题——多线程调用getInstance方法时，有可能一个线程会获得还**没有初始化的对象**!这都是因为重排序的原因，具体分析这里不展开。

解决办法及时用volatile对instance进行修饰
```
private static volatile Singleton instance;
```
这就是经典的“双重检查锁定与延迟初始化”。
3. JDK1.7版本的ConcurrentHashmap的HashEntry的value值，就是通过volatile来修饰的，就是由于volatile的“内存可见性”使得ConcurrentHashMap的get()过程高效、无需加锁。


