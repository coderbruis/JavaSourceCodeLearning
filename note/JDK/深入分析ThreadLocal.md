# 是什么？解决什么问题？
ThreadLocal是什么？ThreadLocal 是Java提供的一种“线程本地变量”机制。它解决的是：让同一个变量在不同线程中有各自独立的副本。不同线程访问**同一个ThreadLocal对象**时，拿到的值互不影响。



# ThreadLocal适用场景
## 2.2 用户上下文场景
这种场景在Web后端很常见：用户登录后，客户端每次请求会带上 token / session / cookie，服务端在过滤器、拦截器或网关层解析出用户信息，然后放到 ThreadLocal 里，后续业务代码就可以直接取当前用户。

```scala
// 过滤器 / 拦截器
try {
    User user = parseToken(request);
    // 用户信息存入ThreadLocal
    UserContext.CURRENT_USER.set(user);
    // 执行拦截器
    chain.doFilter(request, response);
} finally {
    UserContext.CURRENT_USER.remove();
}
```

业务代码里直接通过这个代码取用户信息即可。

```scala
User user = UserContext.CURRENT_USER.get();
```



多个线程访问的是同一个静态变量，这个静态变量指向同一个 ThreadLocal 对象，但是每个线程保存值的位置不同。

```plain
同一个 ThreadLocal 对象：UserContext.CURRENT_USER

Thread-8
 └── ThreadLocalMap
      └── key: UserContext.CURRENT_USER -> value: "userA"

Thread-9
 └── ThreadLocalMap
      └── key: UserContext.CURRENT_USER -> value: "userB"

Thread-10
 └── ThreadLocalMap
      └── key: UserContext.CURRENT_USER -> value: "userC"
```



+ 保存当前用户信息，比如登录用户 userId
+ 保存请求链路 ID，比如 traceId
+ 保存数据库连接、事务上下文
+ 在线程执行过程中传递上下文参数，避免方法层层传参

# ThreadLocal中的核心数据结构
ThreadLocal中核心的数据结构有3个，分别是：Thread、ThreadLocal、ThreadLocalMap。他们之间的关系如下图：

```scala
Thread
 └── ThreadLocal.ThreadLocalMap threadLocals
      └── Entry[]
           ├── Entry(ThreadLocal<?> key, Object value)
           ├── Entry(ThreadLocal<?> key, Object value)
           └── ...
```

在Thread内部，都有一个成员变量threadLocals，它是一个ThreadLocalMap类型。

```plain
ThreadLocal.ThreadLocalMap threadLocals = null;
```

这说明ThreadLocal 的数据实际是挂在 Thread 对象上的。

```java
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

    // ThreadLocalMap中的元素个数
    private int size = 0;
    
    // 扩容阈值，扩容阈值为初始大小值的三分之二。           
    private int threshold; // Default to 0
                                        
    private void setThreshold(int len) {          
        threshold = len * 2 / 3;                  
    }

    // 在table中获取下一个索引位置，如果到达数组末尾，则从头部继续扫描，解决哈希冲突
    private static int nextIndex(int i, int len) {
        return ((i + 1 < len) ? i + 1 : 0);       
    }

    // prevIndex() 获取环形数组中的上一个索引下标；如果到达头部 0，则从尾部 len - 1 继续。
    private static int prevIndex(int i, int len) {
        return ((i - 1 >= 0) ? i - 1 : len - 1);  
    }                                                                                                         
}
```





ThreadLoca中的核心数据结构

```java
// ThreadLoca中的黄金比例常量
private static final int HASH_INCREMENT = 0x61c88647;
private final int threadLocalHashCode = nextHashCode();

private static int nextHashCode() {
    return nextHashCode.getAndAdd(HASH_INCREMENT);
}
```

这几个核心变量在ThreadLocalMap.set()中有重要作用，在ThreadLocalMap.set()中进行了深入的分析。



## ThreadLocalMap.Entry
Entry是ThreadLocalMap数组中的核心元素，它继承了WeakReference。核心原因是：让 ThreadLocal 对象在外部没有强引用时，可以被 GC 回收，避免 ThreadLocal key 被线程长期强引用住。

### 如果Entry继承的强引用
假设Entry强引用ThreadLocal：

```java
Thread -> ThreadLocalMap -> Entry -> ThreadLocal
```

在线程池场景下，线程可能长期存活。只要线程不结束，ThreadLocalMap 就还在，Entry 也还在，那么 ThreadLocal 对象就永远无法被 GC。即使业务代码已经不再持有这个 ThreadLocal 变量了，它也会被 Entry 强行引用住。这会导致：**<font style="color:#DF2A3F;">ThreadLocal 对象无法回收，对应的 value 也无法回收。</font>**

这个后果就是线程长期持有已经没用的 ThreadLocal 和 value，导致内存释放不了，严重时内存泄漏、数据串用、甚至 OOM。



### 如果Entry继承的弱引用
现在Entry继承了弱引用，则：

```java
Thread -> ThreadLocalMap -> Entry --weak--> ThreadLocal
```

当业务代码不再强引用 ThreadLocal 时，threadLocal = null。GC 可以回收这个 ThreadLocal key，然后ThreadLocalMap在set/get/remove等操作时，会顺手清理这种 stale entry。



但是现在继承了弱引用的Entry还是有内存泄漏风险，如果key=null了，但是value还是被业务对象关联，value 仍然可能被 Entry 强引用着。所以在使用ThreadLocal时，都建议：

```java
try {
    threadLocal.set(value);
    // 使用
} finally {
    threadLocal.remove();
}
```

# ThreadLocal中的核心方法
## ThreadLocalMap.set()
ThreadLocalMap.set()的核心注释逻辑如下：

```java
private void set(ThreadLocal<?> key, Object value) {

    // 获取当前线程 ThreadLocalMap 底层的 Entry 数组
    Entry[] tab = table;

    // 获取数组长度，ThreadLocalMap 的长度始终是 2 的幂
    int len = tab.length;

    // 根据 ThreadLocal 的 hash 值计算它在数组中的初始下标
    // len - 1 用作位运算取模，等价于 hash % len
    int i = key.threadLocalHashCode & (len-1);

    // 三个作用
    // 1. 找到相同 key 就覆盖 value
    // 2. 遇到 key 为 null 的过期 Entry 就走替换和清理逻辑
    // 3. 不是1、2，就继续往后找，直到遇到 null 槽位退出循环
    for (Entry e = tab[i];
         e != null;
         // 3. 当前槽位不匹配时，移动到下一个槽位继续查找
         e = tab[i = nextIndex(i, len)]) {

        // Entry 继承 WeakReference，这里获取弱引用中的 ThreadLocal key
        // 如果 ThreadLocal 已经被 GC 回收，k 会是 null
        ThreadLocal<?> k = e.get();

        // 1. 如果当前 Entry 的 key 就是要设置的 ThreadLocal
        // 说明这个 ThreadLocal 在当前线程中已经有值了
        if (k == key) {
            // 直接覆盖旧 value
            e.value = value;

            // 设置完成，直接返回
            return;
        }

        // 2. 如果 key 为 null，说明这是一个 stale entry
        // 即 ThreadLocal 对象已经被 GC 回收，但 value 还残留在数组里
        if (k == null) {
            // 用当前新 key/value 替换这个过期 Entry
            // 同时会顺带清理一段连续区域里的过期 Entry
            replaceStaleEntry(key, value, i);

            // 替换和清理完成，直接返回
            return;
        }
    }

    // 执行到这里，说明从初始位置向后探测时遇到了 null 槽位
    // 当前 ThreadLocal 在 map 中还不存在，且当前位置可以插入
    tab[i] = new Entry(key, value);

    // 元素数量加 1
    int sz = ++size;

    // 尝试清理一些过期 Entry
    // 如果没有清理掉任何 Entry，并且当前元素数量达到扩容阈值，则执行 rehash
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        rehash();
}
```

几个需要注意的地方，下面重点讲下。

ThreadLoca中的黄金比例常量：

```java
private static final int HASH_INCREMENT = 0x61c88647;
private final int threadLocalHashCode = nextHashCode();

private static int nextHashCode() {
    return nextHashCode.getAndAdd(HASH_INCREMENT);
}
```

它的作用就是让连续创建的 ThreadLocal 的 hash 值，在经过 hash & (len - 1) 后，落到数组下标时更均匀、更分散。

```java
int i = key.threadLocalHashCode & (len-1);
```

此处进行的是二进制运算，len初始长度是16，16-1=15，二进制也就是 1111。0x61c88647 的低四位是最后一个十六进制位：0x7，换算成二进制也就是：0111。所以

```java
int i = 0111 && 1111;
//    i = 0111;
//    i = 7;
```

由于nextHashCode()是累加HASH_INCREMENT，所以：int i = key.threadLocalHashCode & (len-1); 最终得到的效果就是：

```plain
0, 7, 14, 5, 12, 3, 10, 1, 8, 15, 6, 13, 4, 11, 2, 9
```

这 16 个位置刚好都走一遍，而且不是连续的0，1，2，3，4，5挨着走。



## ThreadLocalMap.replaceStaleEntry()
ThreadLocalMap.replaceStaleEntry()的核心代码注释如下：

```java
// 在发现过期槽位 staleSlot 时，插入或更新 key/value，并清理过期 Entry
private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                               int staleSlot) {
    // 获取 ThreadLocalMap 的底层数组
    Entry[] tab = table;

    // 获取数组长度
    int len = tab.length;

    // 临时保存当前扫描到的 Entry
    Entry e;

    // 记录需要开始清理过期 Entry 的位置，默认从 staleSlot 开始
    int slotToExpunge = staleSlot;

    // 从 staleSlot 向前扫描，寻找同一连续区间里更靠前的过期槽位
    for (int i = prevIndex(staleSlot, len);
         // 遇到 null 说明连续探测区间结束
         (e = tab[i]) != null;
         // 继续向前扫描
         i = prevIndex(i, len))

        // 如果 key 已被 GC，说明这是过期 Entry
        if (e.get() == null)
            // 更新清理起点为更靠前的过期槽位
            slotToExpunge = i;

    // 从 staleSlot 向后扫描，查找 key 是否已经存在
    for (int i = nextIndex(staleSlot, len);
         // 遇到 null 说明连续探测区间结束
         (e = tab[i]) != null;
         // 继续向后扫描
         i = nextIndex(i, len)) {

        // 取出 Entry 中弱引用的 ThreadLocal key
        ThreadLocal<?> k = e.get();

        // 如果找到了当前要设置的 key
        if (k == key) {
            // 更新已有 Entry 的 value
            e.value = value;

            // 将 staleSlot 上的过期 Entry 移到当前位置
            tab[i] = tab[staleSlot];

            // 将更新后的有效 Entry 放到 staleSlot，尽量靠近原始 hash 位置
            tab[staleSlot] = e;

            // 如果向前没有找到过期槽位，则从当前位置开始清理
            if (slotToExpunge == staleSlot)
                slotToExpunge = i;

            // 清理过期 Entry，并继续做启发式清理
            cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);

            // 更新完成，结束方法
            return;
        }

        // 如果当前 Entry 过期，且之前还没找到清理起点
        if (k == null && slotToExpunge == staleSlot)

            // 记录第一个向后扫描到的过期槽位
            slotToExpunge = i;
    }

    // 没找到已有 key，先断开 staleSlot 中旧 value 的强引用
    tab[staleSlot].value = null;

    // 复用 staleSlot，放入新的 key/value
    tab[staleSlot] = new Entry(key, value);

    // 如果 staleSlot 附近还有其他过期 Entry
    if (slotToExpunge != staleSlot)
        // 从记录的位置开始清理过期 Entry，并继续启发式清理
        cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
}
```

这个方法的核心作用就是：当 set() 过程中遇到过期 Entry 时，复用这个过期槽位保存新的 key/value，并顺带清理当前连续探测区间内的其他过期 Entry，最后维护开放地址法的查找连续性。



## ThreadLocalMap.expungeStaleEntry()
ThreadLocalMap.expungeStaleEntry()核心逻辑注释如下：

```java
// 清理 staleSlot 位置的过期 Entry，并重新整理后续冲突链
private int expungeStaleEntry(int staleSlot) {
    // 获取底层 Entry 数组
    Entry[] tab = table;

    // 获取数组长度
    int len = tab.length;

    // 断开 staleSlot 中 value 的强引用
    tab[staleSlot].value = null;

    // 清空 staleSlot 槽位
    tab[staleSlot] = null;

    // 有效元素数量减 1
    size--;

    // 临时保存当前扫描到的 Entry
    Entry e;

    // 当前扫描下标
    int i;

    // 从 staleSlot 的下一个位置开始向后扫描
    for (i = nextIndex(staleSlot, len);
         // 遇到 null，说明当前连续探测区间结束
         (e = tab[i]) != null;
         // 继续扫描下一个槽位
         i = nextIndex(i, len)) {

        // 获取 Entry 弱引用的 ThreadLocal key
        ThreadLocal<?> k = e.get();

        // 如果 key 已被 GC，说明当前 Entry 也是过期 Entry
        if (k == null) {
            // 断开 value 强引用
            e.value = null;

            // 清空当前槽位
            tab[i] = null;

            // 有效元素数量减 1
            size--;

        // 如果当前 Entry 仍然有效
        } else {
            // 重新计算该 key 理论上应该落到的初始槽位
            int h = k.threadLocalHashCode & (len - 1);

            // 如果当前位置不是它理论上的初始槽位
            if (h != i) {
                // 先清空当前位置，准备重新放置
                tab[i] = null;

                // 从理论槽位 h 开始，向后找第一个空槽位
                while (tab[h] != null)

                    // 遇到占用则继续向后线性探测
                    h = nextIndex(h, len);

                // 将 Entry 移动到更合适的位置
                tab[h] = e;
            }
        }
    }

    // 返回遇到的 null 槽位下标，也就是清理结束位置
    return i;
}
```

这个方法核心作用就是：清理一个连续探测区间里的过期 Entry，并对还有效的 Entry 重新哈希摆放，避免清理产生的空洞破坏后续查找。



## ThreadLocalMap.cleanSomeSlots()
ThreadLocalMap.cleanSomeSlots()核心逻辑注释如下：

```java
// 启发式清理一部分过期 Entry，避免每次都全表扫描
private boolean cleanSomeSlots(int i, int n) {
    // 标记本次是否清理过过期 Entry
    boolean removed = false;

    // 获取底层 Entry 数组
    Entry[] tab = table;

    // 获取数组长度
    int len = tab.length;

    // 至少执行一次扫描
    do {
        // 从传入位置的下一个槽位开始检查
        i = nextIndex(i, len);

        // 获取当前槽位的 Entry
        Entry e = tab[i];

        // 如果槽位不为空，并且 key 已被 GC，说明是过期 Entry
        if (e != null && e.get() == null) {
            // 发现过期 Entry 后，把扫描预算扩大到整张表长度
            n = len;

            // 标记已经发生清理
            removed = true;

            // 从当前位置开始清理过期 Entry，并返回清理结束后的 null 槽位位置
            i = expungeStaleEntry(i);
        }

    // 每轮把扫描预算减半；n 变成 0 时停止
    // 每次把 n 无符号右移一位，也就是约等于除以 2。
    } while ( (n >>>= 1) != 0);

    // 返回本次是否清理过过期 Entry
    return removed;
}
```

这个方法的核心作用是：cleanSomeSlots 是 ThreadLocalMap 的“低成本局部清理机制”：平时少扫一点，发现过期 Entry 后多扫一点，用来控制内存泄漏风险和哈希表性能退化。



# InheritableThreadLocal
InheritableThreadLocal的作用场景是，父线程里设置了线程本地变量，希望新创建的子线程能自动拿到一份初始值。

场景业务场景包括：

1）传递请求上下文：比如 traceId、requestId、租户 ID、用户信息等。

2）父子线程任务链路追踪：日志 MDC、分布式链路追踪中可能需要把父线程上下文传给子线程。

普通 ThreadLocal 是线程隔离的，子线程拿不到父线程的值：

```java
ThreadLocal<String> local = new ThreadLocal<>();

local.set("parent");

new Thread(() -> {
    System.out.println(local.get()); // null
}).start();
```

	而 InheritableThreadLocal 可以让子线程继承父线程的值：

```java
InheritableThreadLocal<String> local = new InheritableThreadLocal<>();

local.set("parent");

new Thread(() -> {
    System.out.println(local.get()); // parent
}).start();
```



核心传值逻辑在Thread.init()方法中。

```java
private void init(ThreadGroup g, Runnable target, String name,
                  long stackSize, AccessControlContext acc,
                  boolean inheritThreadLocals) {
    ...   
    // 创建子线程时复制
    if (inheritThreadLocals && parent.inheritableThreadLocals != null)
        this.inheritableThreadLocals =
        ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
    ...
}

static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
return new ThreadLocalMap(parentMap);
}

private ThreadLocalMap(ThreadLocalMap parentMap) {
    Entry[] parentTable = parentMap.table;
    int len = parentTable.length;
    setThreshold(len);
    table = new Entry[len];

    // 拷贝父线程的entry
    for (int j = 0; j < len; j++) {
        Entry e = parentTable[j];
        if (e != null) {
            @SuppressWarnings("unchecked")
            ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
            if (key != null) {
                Object value = key.childValue(e.value);
                Entry c = new Entry(key, value);
                int h = key.threadLocalHashCode & (len - 1);
                while (table[h] != null)
                h = nextIndex(h, len);
                table[h] = c;
                size++;
            }
        }
    }
}
```



但是现在基本都没有直接通过new Thread()的方式创建线程了，基本都是通过线程池来管理线程。而在常规业务线程池里，InheritableThreadLocal 基本不适合作为上下文传递方案。它的继承时机是**<font style="color:#DF2A3F;">“创建线程时”</font>**，而线程池的线程通常早就创建好了，任务提交时不会重新继承父线程上下文。

线程池上下文传递方案，用的最多的就是阿里的TransmittableThreadLocal，简称 TTL。



# TransmittableThreadLocal
TransmittableThreadLocal是怎么实现的线程池上下文传递方案？它核心原理就是：在提交任务时捕获父线程上下文，在任务执行时恢复到工作线程，执行完再还原工作线程原来的上下文。



InheritableThreadLocal在线程池场景最核心的原因，就是当提交任务到线程池，工作线程已经存在，不会重新new Thread()，所以不会重新复制inheritableThreadLocals。

TransmittableThreadLocal底层核心伪代码如下。

```java
class TtlRunnable implements Runnable {
    private final Runnable runnable;
    private final Object captured;

    TtlRunnable(Runnable runnable) {
        this.runnable = runnable;

        // 1. 在提交任务的线程中捕获上下文
        this.captured = Transmitter.capture();
    }

    @Override
    public void run() {
        // 2. 在工作线程执行前，把捕获的上下文恢复到当前线程
        // 也就是将父线程的值传到当前线程池要执行的线程
        Object backup = Transmitter.replay(captured);

        try {
            // 3. 执行业务逻辑
            runnable.run();
        } finally {
            // 4. 执行完成后，恢复工作线程原来的上下文，避免下次线程池使用的线程时出现值污染
            Transmitter.restore(backup);
        }
    }
}
```

	

总结：TransmittableThreadLocal在线程池里传值，是通过包装任务，在任务提交时捕获父线程的 TTL 快照，在工作线程执行前恢复这份快照，执行结束后再还原工作线程原上下文来实现的。

