# 核心数据结构
## ctl
ctl的作用：在ThreadPoolExecutor中，最核心的一个变量。ctl最核心的作用是设置线程池状态（rs）以及活跃线程数（wc）。

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
```



在不同操作系统下，Java 中的 Integer 变量都是32位，ThreadPoolExecutor 使用前3位（31~29）表示线程池状态，用后29位（28~0）表示活跃线程数。

## COUNT_BITS
COUNT_BITS的作用：用来划分 ctl 这个 int 变量的高低位边界。

```java
private static final int COUNT_BITS = Integer.SIZE - 3; // 29
```



int的最大位值是32位，32 - 3 = 29。通过这29来控制高3位存线程池状态**<font style="color:#DF2A3F;">（注意这里不是线程，是线程池）</font>**，低29位表示线程池中worker数量。通过位移来实现高效操作。

## CAPACITY
CAPACITY的作用：用来表示 workerCount 线程数量部分的最大容量，同时也作为低 29 位的**<font style="color:#DF2A3F;">掩码</font>**。

```java
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
```

> CAPACITY = (1 << COUNT_BITS) - 1计算逻辑拆解
>

1）先左移29位

1 << COUNT_BITS：1的32位为：00000000 00000000 00000000 00000001，**<font style="color:#DF2A3F;">左移29</font>**位得到：00100000 00000000 00000000 00000000

2）对左移结果 - 1

(1 << COUNT_BITS) - 1：就是00100000 00000000 00000000 00000000 - 1，对于二进制，如果高位减1，会借位到对应位置-1。举例如下：

+ 01000 - 1 = 01000 - 01111 = 00111 
+ 00100000 00000000 00000000 00000000 - 1 = 00100000 00000000 00000000 00000000 - 00111111 11111111 11111111 11111111 = 00011111 11111111 11111111 11111111
+ 口诀：对于这种数：00100000 00000000 00000000 00000000，它是“某一位是 1，右边全是 0”。

### 【扩展】掩码
掩码 Mask，就是一串二进制位，用来通过位运算“筛选”出你想要的部分。在ThreadPoolExecutor里：

```java
private static final int COUNT_BITS = 29;
private static final int CAPACITY = (1 << COUNT_BITS) - 1;
```

CAPACITY 的二进制：

```java
000 11111 11111111 11111111 11111111
```

其中低29位都是1，所以它叫：低 29 位掩码。作用是：保留低 29 位，清除高 3 位。

举例：workerCount = ctl & CAPACITY;

```java
ctl: 11100000 00000000 00000000 00000101

CAPACITY: 00011111 11111111 11111111 11111111

& 之后: 00000000 00000000 00000000 00000101
```

高 3 位线程池状态被清掉，只剩下低 29 位的线程数量。

总结：掩码就是“过滤器”，CAPACITY 这个掩码专门用来从 ctl 里取出 workerCount。

## 线程池相关状态
线程池完整生命周期图：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2026/png/12890164/1780810463940-3a2fb7db-c019-4d2a-88ed-b395d768d830.png)

### RUNNING
RUNNING的作用：**表示当前线程池状态是运行中，线程池可以接收新任务，也可以处理阻塞队列里的任务。**

```java
private static final int RUNNING = -1 << COUNT_BITS;
```

-1的二进制：对负数而言，补码 = 它对应正数的二进制，按位取反，再加 1。所以-1 = 11111111 11111111 11111111 11111111，左移29位=11100000 00000000 00000000 00000000。因此RUNNING的二进制为：

```java
11100000 00000000 00000000 00000000
```

#### 【扩展】补码规则
根据补码规则：负数的二进制 = 对应正数取反 + 1

1的32位的二进制：

```java
00000000 00000000 00000000 00000001
```

取反得到：

```java
11111111 11111111 11111111 11111110
```

再+1得到：

```java
11111111 11111111 11111111 11111111
```

最终-1得到：

```java
-1 = 11111111 11111111 11111111 11111111
```

因此：在 Java 里，int 是 32 位，负数用补码表示，所以**<font style="color:#DF2A3F;"> -1 的二进制天然就是 32 个 1。</font>**

### SHUTDOWN
SHUTDOWN的作用：不接收新任务，但处理队列任务。

```java
private static final int SHUTDOWN = 0 << COUNT_BITS;
```

0 << 29 还是 0，所以：

```java
SHUTDOWN = 000 00000000000000000000000000000
```

总结：SHUTDOWN 就是线程池“温和关闭”状态，不让新任务进来，但会把队列里的旧任务执行完。

### STOP
STOP的作用： 表示线程池进入强制停止状态。

```java
private static final int STOP = 1 << COUNT_BITS;
```

1 左移 29位，得到：

```java
STOP = 001 00000000000000000000000000000
```

总结：STOP 是线程池的“强制关闭”状态，核心是停止接收任务、丢弃队列任务，并尝试中断正在运行的线程。



### TIDYING
TIDYING的作用：线程池任务都执行完了，工作线程数量 workerCount = 0，准备执行 terminated() 钩子方法。

```java
private static final int TIDYING = 2 << COUNT_BITS;
```

计算得到二进制也就是：

```java
010 00000000000000000000000000000
```

总结：TIDYING就是线程池已经基本结束，正在做最后的收尾。

### TERMINATED
TERMINATED的作用：terminated() 方法执行完毕，线程池彻底终止。到了这个状态，线程池生命周期正式结束。

```java
private static final int TERMINATED = 3 << COUNT_BITS;
```

二进制就是：

```java
011 00000000000000000000000000000
```

## 二进制图总结
<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2026/png/12890164/1780810735409-8b698335-3b31-4cc0-a3cf-d4f57367ac50.png)

# 核心方法
## runStateOf()
runStateOf方法作用：从 ctl 这个变量中提取线程池状态 runState。

```java
private static int runStateOf(int c) {
    return c & ~CAPACITY;
}
```

上文已经分析了ctl一个字段里同时存储了两个关键信息：

+ 高 3 位：runState 线程池状态
+ 低 29 位：workerCount 工作线程数



CAPACITY二进制：

```java
000 11111111111111111111111111111
```

低29位全是1，取反操作：~CAPACITY，得到结果：

```java
111 00000000000000000000000000000
```

因此c&~CAPACITY就是：保留高 3 位状态，清空低 29 位线程数。

总结：runStateOf() 就是用掩码从 ctl 中取出线程池状态，比如 RUNNING、SHUTDOWN、STOP、TIDYING、TERMINATED。



## workerCountOf()
workerCountOf() 的作用：从 ctl 变量中提取当前线程池的工作线程数量 workerCount。

```java
private static int workerCountOf(int c) {
    return c & CAPACITY;
}
```

上文已经分析了ctl一个字段里同时存储了两个关键信息：

+ 高 3 位：runState 线程池状态
+ 低 29 位：workerCount 工作线程数



CAPACITY 是低 29 位全是 1：

```java
000 11111111111111111111111111111
```

因此c & CAPACITY的作用就是：清空高 3 位状态，保留低 29 位线程数。

最后的结果就是：workerCountOf() 就是用掩码从 ctl 中取出当前线程池里的工作线程数量。  


## execute()
execute()的作用：向线程池提交一个 Runnable 任务，并根据当前线程池状态、线程数量、阻塞队列情况，决定任务怎么执行。

经过前面核心数据结构以及方法的分析，下面execute()源码读起来也会容易很多。

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();

    // 获取 ctl：包含线程池运行状态 + worker 数量
    int c = ctl.get();

    // 如果当前 worker 数量小于核心线程数上限，则优先创建核心线程执行当前任务
    if (workerCountOf(c) < corePoolSize) {
        // 尝试创建核心线程，并将当前任务作为 firstTask 交给该线程执行
        if (addWorker(command, true))
            return;
        // 创建失败后，重新读取最新 ctl（状态 + worker 数量）
        c = ctl.get();
    }

    // 如果线程池仍处于 RUNNING 状态，则尝试将任务加入阻塞队列
    if (isRunning(c) && workQueue.offer(command)) {
        // 任务入队成功后，再次检查线程池状态，防止入队后状态发生变化
        int recheck = ctl.get();

        // 如果入队后线程池变为非 RUNNING，则尝试将任务从队列移除；
        // 移除成功说明任务尚未被执行，此时执行拒绝策略
        if (!isRunning(recheck) && remove(command))
            reject(command);

        // 如果队列里已经有任务，但当前没有任何 worker，
        // 则补一个线程去队列中取任务执行
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }

    // 如果任务无法入队，则尝试创建非核心线程直接执行；
    // 如果创建失败（如线程池状态不允许或已达到最大线程数），则执行拒绝策略
    else if (!addWorker(command, false))
        reject(command);
}
```

核心流程为如下四步：

1. workerCount < corePoolSize  
→ 创建核心线程执行任务
2. 核心线程已满  
→ 尝试把任务放入阻塞队列
3. 队列也满了  
→ 尝试创建非核心线程执行任务
4. 线程数也达到 maximumPoolSize  
→ 执行拒绝策略

### 细节点：为什么核心线程满了，尝试往队列中添加任务时，为什么要判断isRunning(c)？
首先回顾下线程池的状态

1. RUNNING ：接收新任务，也处理队列任务
2. SHUTDOWN ：不接收新任务，但处理队列已有任务

线程池状态是随时都会变化的，防止并发情况下，出现：

**<font style="color:#DF2A3F;">防止线程池刚判断是 RUNNING，任务刚入队成功，线程池就变成 SHUTDOWN，导致这个“新提交任务”混进队列被执行。</font>**





## addWorker()
addWorker()作用：尝试创建一个 Worker 工作线程，并让它执行任务。

addWorker源码如下：

```java
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (;;) { // 第一层外循环
            // 获取 ctl：包含线程池状态 + worker 数量
            int c = ctl.get();
            // 当前线程池状态
            int rs = runStateOf(c);

            // 1.线程池如果是SHUTDOWN\STOP\TIDYING\TERMINATED等状态，直接结束当前循环，返回false添加worker失败
            // 2.但是 SHUTDOWN 有例外情况：当线程池状态是SHUTDOWN，且 firstTask == null，且任务队列不为空时，说明这是为了处理队列中的旧任务而补 worker，这种情况下允许继续，不会返回 false
            // 2这种场景，允许场景一个空的worker去消费旧任务
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;
            // 第二层内循环
            for (;;) {
                // 线程池运行线程数
                int wc = workerCountOf(c);
                // 线程池运行线程数大于等于最大容量，以及大于核心线程池数或者最大线程数，直接返回false，添加worker失败
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                // CAS线程数+1，添加成功，结束外循环
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                // 二次校验线程池状态以及线程数，防止并发
                c = ctl.get();  // Re-read ctl
                // 线程池状态变了，重新执行外循环
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
                // 其他情况就是CAS失败了，一般是因为线程池数量变更了，继续执行内循环
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            // 封装任务到worker中，并通过ThreadFactory来创建一个线程
            w = new Worker(firstTask);
            final Thread t = w.thread;
            // 防止ThreadFactory创建的线程为空
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                // 加内存锁
                mainLock.lock();
                try {         
                    int rs = runStateOf(ctl.get());
                    // 重新校验当前线程池状态
                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {
                        // 新建的线程不可能是alive，一定是NEW的状态，否则说明线程状态异常
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        // 线程池Set添加这个新的worker
                        workers.add(w);
                        int s = workers.size();
                        // largestPoolSize更新
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    // 启动新建的线程
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (! workerStarted)
                // 启动线程失败，回滚worker
                addWorkerFailed(w);
        }
        return workerStarted;
    }
```

addWorker核心流程总结为如下四步：

1. 判断线程池状态

→ RUNNING 可以创建；SHUTDOWN 只允许创建空 Worker 处理队列旧任务；其他状态直接失败

2. 判断 workerCount 是否超限

→ core=true 对比 corePoolSize；core=false 对比 maximumPoolSize

3. CAS 增加 workerCount

→ 抢占创建 Worker 的资格；CAS 失败则重试

4. 创建并启动 Worker

→ 创建 Worker 和 Thread，加入 workers 集合，然后 thread.start() 执行任务

### 细节点：当线程池状态为SHUTDOWN时的特殊场景
上述源码中有一个特殊场景需要注意：

```java
 if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;
```

```java
! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty())
```

有例外情况：当线程池状态是SHUTDOWN，且 firstTask == null，且任务队列不为空时，说明这是为了处理队列中的旧任务而补 worker，这种情况下允许继续，不会返回 false。这种场景，允许场景一个空的worker去消费旧任务。



## runWorker()
runWorker()的作用是：Worker 线程启动后，不断执行 firstTask 或从 workQueue 获取任务，执行前后调用钩子方法，处理异常，最后退出Worker。

runWorker()源码如下：

```java
    final void runWorker(Worker w) {
        // 线程池里的工作线程
        Thread wt = Thread.currentThread();
        // firstTask是创建Worker时传进来的第一个任务。
        Runnable task = w.firstTask;
        // 先把 firstTask 置空，避免重复执行，也方便 GC 回收引用。
        w.firstTask = null;
        // 当前Worker已经正式开始运行，后续可以响应shutdownNow()这类中断操作
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            // 如果task为空，则从阻塞队列取任务，getTask()为null，说明worker要退出
            while (task != null || (task = getTask()) != null) {
                // 加锁表明当前worker正在执行任务
                w.lock();
                // 每次 Worker 准备执行任务前，重新校准当前线程的中断状态：
                // 如果线程池已经 STOP，就确保线程被中断；如果还没 STOP，就清掉无效中断，避免影响正常任务。
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        // 真正执行任务
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    // 解锁
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }
```



### 细节点1：Worker 继承了AQS，自己带锁
Worker 继承了 AQS，自己带锁。Worker 初始化时默认是加锁状态，目的是：防止线程刚创建但还没真正开始执行任务时，被提前中断。

调用 w.unlock() 之后，表示：当前 Worker 已经正式开始运行，后续可以响应 shutdownNow() 这类中断操作。

### 细节点2：wt.interrupt()执行时机
```java
if ((runStateAtLeast(ctl.get(), STOP) ||
     (Thread.interrupted() &&
      runStateAtLeast(ctl.get(), STOP))) &&
    !wt.isInterrupted())
    wt.interrupt();
```

这一段代码比较绕，分三层分析。

> **第一层：线程池已经 STOP 了吗？**
>

runStateAtLeast(ctl.get(), STOP)表示线程池状态 >= STOP，也就是：

STOP / TIDYING / TERMINATED



当runStateAtLeast(ctl.get(), STOP)=true，则还需要判断下wt.isInterrupted()是否是中断状态

+ wt.isInterrupted()：只判断状态，不标记中断
+ Thread.interrupted()：判断中断状态，并且标记中断



```java
// 当runStateAtLeast(ctl.get(), STOP) = true，则还需要保证wt不是中断状态，则发起中断
if (runStateAtLeast(ctl.get(), STOP) && !wt.isInterrupted()) {
    wt.interrupt();
}
```

线程池状态变成 STOP 和 worker 被设置中断，不是一个原子动作，所以runStateAtLeast(ctl.get(), STOP)和wt.isInterrupted()需要单独判断。



wt.interrupt() 不是杀线程，而是给工作线程设置**<font style="color:#DF2A3F;">中断标记</font>**，通知它“你应该停止/退出了”。





> **第二层：为什么要调用 Thread.interrupted()之后，还要再次判断runStateAtLeast(ctl.get(), STOP)**
>

首先Thread.interrupted() 会返回当前线程是否被中断，同时会**<font style="color:#DF2A3F;">清除</font>**中断标记，这个第二层判断，是防止极端并发场景下，当第一次runStateAtLeast(ctl.get(), STOP)=false，表明线程池还没到STOP状态，这个瞬间另外一个线程调用了shutdownNow()，线程池状态瞬间变成STOP，并且interrupt当前这个worker，因此当worker 执行 Thread.interrupted()发现自己被中断了，同时把中断标记清掉，所以必须再判断一次runStateAtLeast(ctl.get(), STOP)，如果发现已经 STOP，就重新执行：wt.interrupt()把中断标记补回来。

（中断标记补回来，是为了后续其他地方可以继续用到这个中断标记）



> **第三层：为什么还要判断!wt.isInterrupted()**
>

这个判断是为了避免重复判断，如果当前 worker 线程还没有中断标记，才调用 wt.interrupt()。如果已经被中断了，就不用重复 interrupt。



三层判断总体意思就是：只要线程池已经进入 STOP 状态，就要求 worker 必须是中断状态。如果 worker 还不是中断状态，就补一次 interrupt。



## getTask()
getTask()的作用：是 ThreadPoolExecutor 里 Worker 从阻塞队列获取任务的核心方法。

getTask()底层源码如下：

```java
private Runnable getTask() {
    // 上一次从队列拉取任务时，是否发生了超时
    boolean timedOut = false;

    for (;;) {
        // 获取 ctl：包含线程池状态 + worker 数量
        int c = ctl.get();
        // 提取线程池运行状态
        int rs = runStateOf(c);

        // 如果线程池已经 >= SHUTDOWN：
        // 1. 如果已经 >= STOP，说明不仅不接收新任务，也不再处理队列任务，当前 worker 直接退出
        // 2. 如果是 SHUTDOWN 且队列空了，说明旧任务也处理完了，当前 worker 直接退出
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            // worker 数量减 1
            decrementWorkerCount();
            return null;
        }

        // 当前 worker 总数量
        int wc = workerCountOf(c);

        // 当前 worker 是否需要按 keepAliveTime 做“超时等待”
        // allowCoreThreadTimeOut == true：核心线程也允许超时退出
        // wc > corePoolSize：说明当前属于非核心线程，允许超时退出
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

        // 下面这个 if 处理“当前 worker 是否应该退出”
        //
        // 情况1：wc > maximumPoolSize
        //   说明当前 worker 数超过了最大线程数，需要回收一部分线程
        //
        // 情况2：timed && timedOut
        //   说明当前线程属于允许超时的线程，并且上一轮 poll(keepAliveTime) 已经超时
        //   同时还要满足：
        //   wc > 1 || workQueue.isEmpty()
        // 含义是：
        //   - 如果线程数大于 1，可以安全回收一个
        //   - 或者虽然只剩 1 个线程，但队列已经空了，也可以回收
        //   - 反过来，如果只剩 1 个线程且队列不空，就不能让它退出，
        //     否则队列中的任务可能没人处理
        if ((wc > maximumPoolSize || (timed && timedOut))
            && (wc > 1 || workQueue.isEmpty())) {
            // CAS 尝试将 workerCount 减 1
            if (compareAndDecrementWorkerCount(c))
                // 返回 null，runWorker() 收到 null 后会结束循环，当前线程退出
                return null;
            // CAS 失败，说明并发下 ctl 变了，重试
            continue;
        }

        try {
            // 如果当前线程允许超时退出，则最多等待 keepAliveTime 去拉取任务
            // 超时仍拿不到任务，会返回 null
            // 如果当前线程不允许超时退出（典型是核心线程），则用 take() 一直阻塞等待任务
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                workQueue.take();

            // 成功拿到任务，直接返回给 runWorker() 执行
            if (r != null)
                return r;

            // 走到这里说明 timed == true 且 poll() 超时返回了 null
            // 记录“本轮超时了”，下一轮循环可能会触发线程回收
            timedOut = true;
        } catch (InterruptedException retry) {
            // 如果在等待任务时被中断，不立刻退出
            // 而是把 timedOut 重置为 false，继续下一轮循环重试取任务
            timedOut = false;
        }
    }
}
```



getTask()核心流程如下四步：

1. 判断线程池状态

→ 如果是 STOP/TIDYING/TERMINATED，或者 SHUTDOWN 且队列为空，就减少 workerCount，返回 null

2. 判断当前 Worker 是否需要超时回收

→ 如果 allowCoreThreadTimeOut = true，或者 workerCount > corePoolSize，当前线程就允许超时等待任务

3. 判断是否需要回收线程

→ 如果线程数超过 maximumPoolSize，或者上次取任务超时，并且队列为空/线程数大于 1，就 CAS 减少 workerCount，返回 null

4. 从阻塞队列取任务

→ 如果允许超时，用 poll(keepAliveTime)；否则用 take() 一直阻塞等待任务

## shutdown()
shutdown的作用：不再接收新任务，但会**<font style="color:#DF2A3F;">继续处理已提交任务</font>**，并唤醒**<font style="color:#DF2A3F;">空闲线程</font>**，最终让线程池自然终止。

shutdown源码：

```java
public void shutdown() {
    // 加锁，保护 workers 集合、线程池状态变更过程中的并发安全。
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        // 校验当前调用者是否有权限关闭线程池。如果存在 SecurityManager，会检查调用线程是否有权限操作工作线程。
        checkShutdownAccess();

        // 将线程池运行状态推进到 SHUTDOWN，只允许推进到SHUTDOWN，不允许回归。
        // SHUTDOWN 状态表示：
        // 1. 不再接收新任务
        // 2. 但会继续处理阻塞队列中已经提交的旧任务
        advanceRunState(SHUTDOWN);

        // 中断“空闲”的工作线程。
        // 目的是唤醒那些阻塞在 getTask()、take()、poll() 上等待任务的线程，
        interruptIdleWorkers();

        // 关闭时的钩子方法，留给 ScheduledThreadPoolExecutor 扩展使用。
        // ThreadPoolExecutor 自己这里是空实现。
        // ScheduledThreadPoolExecutor 会在这里处理延迟任务、周期任务等。
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        // 无论上面是否抛异常，最终都要释放主锁。
        mainLock.unlock();
    }

    // 尝试终止线程池。
    // 如果此时：
    // 1. 线程池状态满足可终止条件
    // 2. 工作线程数量已经为 0
    // 就会把状态推进到 TIDYING / TERMINATED。
    // 否则先不终止，等后续工作线程退出时再继续尝试。
    tryTerminate();
}
```

## shutdownNow()
核心源码如下：

```java
public List<Runnable> shutdownNow() {
    // 用来保存还没有执行的任务。
    // shutdownNow() 会把阻塞队列中的任务取出来，然后返回给调用方。
    List<Runnable> tasks;

    // 加锁，保护 workers 集合、线程池状态变更、队列清理过程的并发安全。
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        // 校验当前调用者是否有权限关闭线程池。
        checkShutdownAccess();

        // 将线程池状态推进到 STOP。
        // STOP 状态表示：
        // 1. 不再接收新任务
        // 2. 不再处理阻塞队列中的任务
        // 3. 尝试中断正在执行任务的 worker
        advanceRunState(STOP);

        // 中断所有 worker，包括：
        // 1. 正在执行任务的 worker
        // 2. 空闲等待任务的 worker
        //
        // 和 shutdown() 不一样：
        // shutdown() 只中断空闲 worker；
        // shutdownNow() 会尝试中断所有 worker。
        //
        // 但 interrupt 不是强制杀死线程，只是设置中断标记。
        // 任务能不能停下来，取决于任务代码是否响应中断。
        interruptWorkers();

        // 清空阻塞队列，把还没来得及执行的任务取出来。
        // shutdownNow() 的语义是：
        // 队列里还没执行的任务不再执行，而是返回给调用方。
        tasks = drainQueue();
    } finally {
        // 无论上面是否抛异常，都要释放主锁。
        mainLock.unlock();
    }

    // 尝试终止线程池。
    // 如果此时：
    // 1. 线程池已经是 STOP
    // 2. 队列已经被清空
    // 3. workerCount 已经为 0
    // 就会进入：
    // STOP -> TIDYING -> TERMINATED
    //
    // 如果还有 worker 正在执行任务，则暂时无法 TERMINATED。
    // 等 worker 执行结束退出时，会再次调用 tryTerminate()。
    tryTerminate();

    // 返回还没有执行的任务。
    return tasks;
}
```



### shutdown和shutdownNow的区别
> **<font style="color:#DF2A3F;">shutdown</font>**
>

适合：

系统准备停机、重启、发布、容器销毁时，  
不再接收新任务，但希望已经提交的任务尽量执行完。

典型场景：

1. Spring 容器关闭时，关闭业务线程池
2. 服务发布 / 重启时，优雅下线
3. 定时任务线程池关闭
4. MQ 消费线程池关闭
5. 异步日志、异步通知、异步落库线程池关闭



```java
executor.shutdown();

try {
    // 等30秒还没shutdown，则调用shutdownNow
    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```



> **<font style="color:#DF2A3F;">shutdownNow</font>**
>

适合：

线程池已经无法正常退出，需要尽快停止。

典型场景：

1. 服务停机超时，任务迟迟不结束
2. 任务卡死、死循环、长时间阻塞
3. 应用要快速关闭，不能继续等
4. 测试环境 / 管理后台手动停止任务
5. shutdown() 等待超时后的兜底动作



