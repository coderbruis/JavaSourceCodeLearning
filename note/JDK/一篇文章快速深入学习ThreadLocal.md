<!-- TOC -->
- [前言](#前言)
- [正文](#正文)
    - [1. 适用（使用）场景](#1-适用使用场景)
        - [1.1 线程资源持有（线程隔离）](#11-线程资源持有线程隔离)
        - [1.2 线程资源一致性](#12-线程资源一致性)
        - [1.3 线程安全](#13-线程安全)        
        - [1.4 分布式计算](#14-分布式计算)        
        - [1.5 在SqlSessionManager中的应用](#15-在sqlsessionmanager中的应用)        
        - [1.6 在Spring框架中的TransactionContextHolder中的应用](#16-在spring框架中的transactioncontextholder中的应用)    
    - [2. 源码学习](#2-源码学习)        
        - [2.1 ThreadLocal内部使用了哪些数据结构？](#21-threadlocal内部使用了哪些数据结构)        
        - [2.2 源码分析](#22-源码分析)
    - [3. 总结](#3-总结)
        - [3.1 ThreadLocal的适用场景](#31-threadlocal的适用场景)
        - [3.2 哪些开源框架、源码使用到了ThreadLocal](#32-哪些开源框架源码使用到了threadlocal)
        - [3.3 关于内存泄漏](#33-关于内存泄漏)    
        - [3.4 其他](#34-其他)
- [引用](#引用)<!-- /TOC -->

## 前言
最近开始空闲起来了，深入学习系列以及自我提升系列都得提上日程了。本次学习的ThreadLocal，是由JDK提供的一个用于存储每个线程本地副本信息的类，它的编写者就是著名的并发包大神Doug Lea。要想深入学习一个类，首先得先阅读它的官方类注释：

```
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
```

翻译过来的意思就是：
```
这个类用于提供线程本地变量，这些变量和普通的变量不同，因为每个线程通过访问ThreadLocal的get或者
是set方法都会有其独立的、初始化的变量副本。ThreadLocal实例通常是希望将线程独有的状态（例如用户ID、交易ID）
线程中的私有静态字段进行关联，即将线程独有的状态存储到线程中。
```

```
* <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist)
```
这段的意思是：
```
每个线程都会持有一个指向ThreadLocal变量的隐式引用，只要线程还没有结束，该引用就不会被GC。
但当线程结束后并且其他地方没有对这些副本进行引用，则线程本地实例的所有副本都会被GC。
```

## 正文

### 1. 适用（使用）场景

源码学习总是非常枯燥的，所以得先了解要学习的类能够做什么，适用和使用场景有哪些，这样学起来就更有目的性。经过前文对于ThreadLocal源码类注释的翻译过后，我们大致知道了ThreadLocal的作用，可以概括为两点：

1. 用于存储线程本地的副本变量，说白了就是为了做到线程隔离。
2. 用于确保线程安全。

但ThreadLocal的作用不止这两点，带着疑惑我们先看下ThreadLocal有哪些使用场景。

#### 1.1 线程资源持有（线程隔离）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516100901122.png)
在WEB程序中，每个线程就是一个session，不同用户访问程序会通过不同的线程来访问，通过ThreadLocal来确保同一个线程的访问获得的用户信息都是相同的，同时也不会影响其他线程的用户信息。所以ThreadLocal可以很好的确保线程之间的隔离性。

#### 1.2 线程资源一致性

![在这里插入图片描述](https://img-blog.csdnimg.cn/2020051610161534.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020051610243492.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
嗯.... 图片有些模糊。

  这个场景呢，在JDBC内部都有使用到。在JDBC内部，会通过ThreadLocal来实现 **线程资源的一致性**。我们都知道，每个HTTP请求都会在WEB程序内部生成一个线程，而每个线程去访问DB的时候，都会从连接池中获取一个Connection连接用于进行数据库交互。那么当一个HTTP请求进来，该请求在程序内部调用了不同的服务，包括搜索服务、下单服务、付款服务等，在这个调用链中每次请求一个服务都需要进行一次数据库交互，那么有一个问题就是如何确保请求过程中和数据库交互的 **事务状态一致** 的问题，如果同一个请求的调用链中connection都不同，则事务就没法控制了，因此在JDBC中通过了ThreadLocal来确保每次的请求都会和同一个connection进行一一对应，确保一次请求链中都用的同一个connection，这就是 **线程资源的一致性**。

#### 1.3 线程安全
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516102700304.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
基于ThreadLocal存储在Thread中作为本地副本变量的机制，保证每个线程都可以拥有自己的上下文，确保了线程安全。相比于加锁（Synchronize、Lock），ThreadLocal的效率更高。

#### 1.4 分布式计算
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516102924241.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
对于分布式计算场景中，即每个线程都计算出结果后，最终通过将ThreadLocal存储的结果取出，并收集。

#### 1.5 在SqlSessionManager中的应用
在SqlSessionManager中，对于SqlSession的存储，就是通过ThreadLocal来进行的。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516103223860.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
可以看到，在getConnection()的时候，实际上就是去从ThreadLocal中去获取连接—SqlSession。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516103238445.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516103246506.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

#### 1.6 在Spring框架中的TransactionContextHolder中的应用
在Spring框架中的TransactionContextHolder中，也同样使用了ThreadLocal，以一个分布式事务的业务场景来进行分析：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516103340255.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
在淘宝APP中，需要购买某个商品，会涉及交易中台，履约中台。购买一个商品后，会在交易中台去更新订单，同时需要去履约中台进行合约签订。但如果淘宝APP回滚了，则履约中台和交易中台也需要进行业务回滚。对于分布式事务，需要有一个context，即资源上下文，用于存储用户的信息、订单的信息以及来源等，因此在Spring的TransactionContextHolder中，就通过ThreadLocal来存储context。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516103356344.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
以上都是在学习慕课网“求老仙奶我不到P10”老师的《ThreadLocal讲解》的视频中总结的内容，如有侵权请联系删除。PS：老师讲解的非常好，建议小伙伴们都去观看学习一波。

### 2. 源码学习
上面已经了解到ThreadLocal的适用（使用）场景了，下面就开始枯燥的源码学习了，在学习之前，我们先提出几个疑问：

1. ThreadLocal是怎么保证了线程隔离的？
2. ThreadLocal注释中提到的隐式引用是什么？有什么作用？
3. ThreadLocal为什么要用到隐式引用？而不用强引用？
4. 据说ThreadLocal会发生内存泄漏？什么情况下会发生内存泄漏？如何避免内存泄漏？
5. 使用ThreadLocal有什么需要注意的点？

#### 2.1 ThreadLocal内部使用了哪些数据结构？
首先，我们来看下ThreadLocal中几个比较重要的数据结构。

```Java
/**
 * 用于ThreadLocal内部ThreadLocalMap数据结构的哈希值，用于降低哈希冲突。
 */
private final int threadLocalHashCode = nextHashCode();

/**
 * 原子操作生成哈希值，初始值为0.
 */
private static AtomicInteger nextHashCode =  new AtomicInteger();

/*
 * 用于进行计算出threadLocalHashCode的哈希值。
 */
private static final int HASH_INCREMENT = 0x61c88647;

/**
 * 返回下一个哈希值，让哈希值散列更均匀。
 */
private static int nextHashCode() {
    return nextHashCode.getAndAdd(HASH_INCREMENT);
}
```


下面将是ThreadLocal最终要的一个数据结构：ThreadLocalMap

```Java
/**
 * ThreadLocalMap其实就是一个用于ThreadLocal的自定义HashMap，它和HashMap很像。在其内部有一个自定义的Entry类，
 * 并且有一个Entry数组来存储这个类的实例对象。类似于HashMap，ThreadLocalMap同样的拥有初始大小，拥有扩容阈值。
 */
static class ThreadLocalMap {
	/*
	 *  可以看到，Entry类继承了WeakReference类，它的含义是弱引用，即JVM进行GC时，无论当前内存是否够用，
	 *  都会把被WeakReference指向的对象回收掉。
	 */
	static class Entry extends WeakReference<ThreadLocal<?>> {   
	    /** The value associated with this ThreadLocal. */       
	    Object value;
	
	    Entry(ThreadLocal<?> k, Object v) {                      
	        super(k);                                            
	        value = v;                                           
	    }                                                        
	}
	// ThreadLocalMap的初始大小
	private static final int INITIAL_CAPACITY = 16
	                                                
	// 用于存储Entry的数组                                                                               
	private Entry[] table;
	                                      
	private int size = 0;
	
	// 扩容阈值，扩容阈值为初始大小值的三分之二。           
	private int threshold; // Default to 0
	                                    
	private void setThreshold(int len) {          
	    threshold = len * 2 / 3;                  
	}
	                                       
	private static int nextIndex(int i, int len) {
	    return ((i + 1 < len) ? i + 1 : 0);       
	}
	                                     
	private static int prevIndex(int i, int len) {
	    return ((i - 1 >= 0) ? i - 1 : len - 1);  
	}                                                                                                         
}
```

那么对于ThreadLocalMap中，Entry为什么要继承WeakReference，而不是其他的Reference？这里由于篇幅原因，就不加以介绍，并且网上已经有很多优秀的分析博文，可以看下末文的引用，这里就不继续深入了，简单总结以下几点原因：
1. 是为了再Thread线程在执行过程中，key能够被GC掉，从而在需要彻底GC掉ThreadLocalMap时，只需要调用ThreadLocal的remove方法即可。
2. 如果是用的强引用，虽然Entry到Thread不可达，但是和Value还有强引用的关系，是可达的，所以无法被GC掉。

虽然Entry使用的是WeakReference虚引用，但JVM只是回收掉了ThreadLocalMap中的key，但是value和key是强引用的（value也会引用null），所以value是无法被回收的，所以如果线程执行时间非常长，value持续不GC，就有内存溢出的风险。所以最好的做法就是调用ThreadLocal的remove方法，把ThreadLocal.ThreadLocalMap给清除掉。



#### 2.2 源码分析

先看下Thread类的源码，在Thread类中，定义了两个ThreadLocalMap变量
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200516131832553.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
这里就可以发现，ThreadLocalMap变量定义在Thread中，因而每个Thread都拥有自己的ThreadLocalMap变量，互不影响，因而实现了线程隔离性。

这里有一个inheritableThreadLocals，作用是用于父子线程间ThreadLocal变量的传递。详细的关于inheritableThreadLocals的分析可以学习下博文[InheritableThreadLocal详解](https://www.jianshu.com/p/94ba4a918ff5)。

下面回到关于ThreadLocal源码的介绍，先看看set()和get()方法源码：

```Java
	// ThreadLocal中的set()方法
	public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
        	// 将当前线程传入，作为ThreadLocalMap的引用，创建出ThreadLocalMap
            createMap(t, value);
    }
    
    // ThreadLocalMap中的set()方法
	private void set(ThreadLocal<?> key, Object value) {
			// 初始化Entry数组
            Entry[] tab = table;
            int len = tab.length;
            // 通过取模计算出索引值
            int i = key.threadLocalHashCode & (len-1);

			// 如果ThreadLocalMap中tab的槽位已经被使用了，则寻找下一个索引位，i=nextIndex(i, len)
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }
				// 如果key引用被回收了，则用新的key-value来替换，并且删除无用的Entry
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            tab[i] = new Entry(key, value);
            int sz = ++size;
            // 清楚哪些get()为空的对象，然后进行rehash。
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }
```



```Java
	public T get() {
		// 获取当前线程
        Thread t = Thread.currentThread();
        // 获取线程t中的ThreadLocalMap
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        // 如果没有获取到ThreadLocalMap，则初始化一个ThreadLocalMap
        return setInitialValue();
    }
	ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }
    // 初始化
	private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
        	// 把线程存放到当前线程的ThreadLocalMap中
            createMap(t, value);
        return value;
    }
```

知道怎么存储以及获取ThreadLocal之后，还要知道怎么清除ThreadLocal，防止内存泄漏，下面看下remove()源码：

```Java
	// ThreadLocal的remove()方法
	public void remove() {
		// 获取当前线程中的ThreadLocalMap
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }

	// ThreadLocalMap中的remove()方法
	private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            // 通过取模获取出索引位置，
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
               
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

	/**
	 *  清除没用的槽位以及null插槽，并且对其进行重新散列。
	 */
	private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // 将插槽位置的键和值都设置为null
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // 遇到null的插槽，重新散列计算哈希值。
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }
```


## 3. 总结

整片文章，先介绍了ThreadLocal的适用场景，然后再由此带着疑问深入学习了ThreadLocal源码，不过ThreadLocal源码中，其实还有许多没有挖掘完的细节，这部分接下来会持续的深入分析并学习，然后再过来跟下本篇博文。

### 3.1 ThreadLocal的适用场景
① 线程资源持有（线程隔离）
② 线程资源一致性
③ 线程安全
④ 分布式计算

### 3.2 哪些开源框架、源码使用到了ThreadLocal
① JDBC获取Connection相关源码
② MyBatis中的SqlSessionManager相关源码
③ Spring框架中的TransactionContextHolder相关源码

### 3.3 关于内存泄漏
由于ThreadLocalMap的Entry继承了WeakReference，所以只要JVM发起了GC，就会回收掉Entry的键，导致当线程持续运行时，ThreadLocal中value值增多，并且没法对其进行GC，所以导致内存泄漏，因此需要调用其remove方法，避免内存泄漏。

### 3.4 其他
有哪里分析总结不对的地方，欢迎各位读者及时指出。

## 引用
1.  [求老仙奶我不到P10 老师的ThreadLocal讲解](https://www.imooc.com/video/21060)
2. [ThreadLocal分析其弱引用和可能引起的内存泄漏](https://www.jianshu.com/p/94de80aee1bf)
3. [InheritableThreadLocal详解](https://www.jianshu.com/p/94ba4a918ff5)