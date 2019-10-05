# JDK8线程池

## 线程池执行任务流程

执行流程图如下:

![](https://user-gold-cdn.xitu.io/2019/8/29/16cddd942a2fe81a?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

```
添加到阻塞队列后, 考虑到当前的工作线程数为0, 会调用addWorker(null, false)来执行阻塞队列的任务
```

## 工作线程的原理

while循环不断地通过getTask()方法获取任务； getTask()方法从阻塞队列中取任务

### 非核心线程超时销毁

间接通过阻塞队列的超时拉取任务.  **workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS)**

### 维持核心线程数

判断工作线程总数是否大于核心线程数且任务队列为空

```
为什么还要判断核心线程数呢, 前面提交任务的时候不是控制好了吗?  

有可能是调用**setCorePoolSize**导致核心线程数变少
```

### 任务队列不为空时保证至少有一个工作线程

## 线程池的关闭

### 线程池的状态

![](https://user-gold-cdn.xitu.io/2019/8/29/16cddd942a186ef0?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

#### 拒绝新任务

设置线程池状态, 通过判断线程状态来拒绝:

1. 添加工作线程**addWorker**, 若是shutdown调用则需要考虑workQueue是否为空
2. 添加任务前判断状态

#### 结束线程

##### shutdown

在调用shutdown的情况下, 非空闲线程的结束是在其跑完任务后, 判断任务队列不为空, 则会通过判断线程池状态来结束. 而空闲线程处于阻塞状态, 需要通过调用**Thread#interrupt()**方法唤醒, 唤醒后再判断状态来结束

```
1. 如何判断空闲线程呢? 并没有状态标记当前线程处于空闲状态的.

工作线程在执行任务的时候会调用 Woker#lock() , 所以可以通过Woker#tryLock()来判断其是否为空闲线程

2. 为什么使用锁的方式呢, 而不适用状态?

状态也是一个int值的维护, 考虑到需要维护的状态不多, 通过锁的形式可以方便地达到这个效果, 非常地巧妙. 从源码中可以看到只有执行任务的时候才会调用 Woker#lock() 方法, 说明完全不会阻塞其他线程

3. 若任务队列只有一个任务, 线程池shutdown了, 这时候两个非空闲线程完成任务后(逃过了shutdown的中断), 去获取任务时同时断线程池状态且任务队列只有一个, 那么只有一个线程会获取到任务, 导致任务队列空了, 另外一个线程则会阻塞, 这时已经逃过了shutdown的中断, 那不是会一直阻塞吗?

可以通过其他工作线程来中断. 在线程结束前会调用processWorkerExit方法, 里面会调用一次tryTerminate方法来设置TIDYING状态, 若工作线程数不为0, 则会尝试去中断一个线程, 以后保证TIDYING状态设置的传播, 同时解决以上的问题.
```

##### shutdownNow

shutdownNow则会中断所有线程, 非常粗暴地调用所有线程的**Thread#interrupt()**方法, 对于空闲线程醒过来后判断状态便可结束, 但是对于正在执行任务的线程, 则不一定能结束掉, **所以在设计一个任务的时候, 建议响应中断**

#### TIDYING状态的设置

通过调用 **tryTerminate()** 来设置, 判断工作线程数为0时, 便可设置. 

在 **tryTerminate()** 中会判断工作线程数不为空的时候, 会去中断一个空闲线程, 以保证TIDYING状态设置的传播. 

```
在shutdown和shutdownNow主动调用, 如果工作线程全部结束了, 便会设置成功,但是没有全部结束呢? 需要等待所有的线程结束吗? 但是这两个方法并不是阻塞的, 等待不合理啊?

在工作线程结束的时候, 执行 processWorkerExit() 的时候会也调用一次 tryTerminate() 方法.
```

## 线程数的设置

这实际是非常困难的问题, 因为涉及的因素很多:

1.  硬件环境(CPU、内存、硬盘读写速度、网络状况等)
2.  应用数量
3.  线程池数量
4.  执行任务类型(IO密集/CPU密集/混合型)

### 天真法

假设要求系统TPS至少为20, 而单线程处理一个Transaction需要4s, 即单线程的TPS为0.25TPS, 那么需要线程数为 20/0.25 = 80

很显然, 一般的服务器cpu核心至多为32, 必然导致过多的线程上下文切换开销

### 简单法

1.  CPU密集型任务 = N + 1

    > +1的原因是操作系统由于缺失故障或者其他原因而暂停时，这个额外的线程也能确保CPU的时钟周期不会被浪费。

2.  IO密集型任务 = 2N + 1

### 公式法

单线程单核心下, CPU利用率 = 计算时间 / (计算时间 + 等待时间)

多线程单核心下, CPU利用率 / 线程数 = 计算时间 / (计算时间 + 等待时间)

多线程多核心下, 核心数 * CPU利用率 / 线程数 = 计算时间 / (计算时间 + 等待时间)

最后变换可得: 线程数 = (1 + 等待时间/计算时间) * 核心数 * CPU利用率

* 1 + 等待时间/计算时间” 只与任务本身有关。
* 核心数可通过cat /proc/cpuinfo | grep -c processor得到。
* 标准的CPU利用率要通过实际监控得到，但在估算线程池大小时，应看做“期望得到的CPU利用率”，即可分配给该任务的CPU比例。如果只打算分配一半CPU给任务的话，就是0.5。

一个系统最快的部分是CPU，所以决定一个系统吞吐量上限的是CPU。增强CPU处理能力，可以提高系统吞吐量上限。但根据短板效应，真实的系统吞吐量并不能单纯根据CPU来计算。那要提高系统吞吐量，就需要从“系统短板”（比如网络延迟、IO）着手：

*   尽量提高短板操作的并行化比率，比如多线程下载技术
*   增强短板能力，比如用NIO替代IO

## BlockingQueue

要明白阻塞队列的原理, 首先得了解其接口的定义:

1. boolean add(E e); 元素入队, 队列未满则返回true, 否则返回抛异常; 非阻塞
2. boolean offer(E e); 元素入队, 队列未满则返回true, 否则返回false; 非阻塞
3. void put(E e) throws InterruptedException; 元素入队, 队列未满则直接返回, 否则会阻塞直到队列未满或者线程被中断
4. boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;  元素入队, 队列未满则直接返回, 否则会阻塞直到队列未满或者线程被中断或者超时
5. E peek(); 获取队首元素, 但不出队;
6. E take() throws InterruptedException; 元素出队, 有元素则返回, 否则阻塞直到有元素或者线程被中断
7. E poll(); 元素出队, 有元素则返回, 否则返回null
8. E poll(long timeout, TimeUnit unit) throws InterruptedException; 元素出队, 有元素则返回, 否则阻塞直到有元素或者线程被中断或者超时

### LinkedBlockingQueue

与ArrayBlockingQueue不同的是, 分别使用了takeLock 和 putLock 对并发进行控制，也就是说，添加和删除操作并不是互斥操作，可以同时进行，这样也就可以大大提高吞吐量

```
因为使用两个锁, 所以在通知notFull和notEmpty需要去获取对应的锁
```

### SynchronousQueue

它是一个非常特殊的阻塞队列，他的模式是：在 offer的时候，如果没有另一个线程在 take 或者 poll 的话，就会失败，反之，如果在 take或者 poll的时候，没有线程在offer ，则也会失败，而这种特性，则非常适合用来做高响应并且线程不固定的线程池的Queue。所以，在很多高性能服务器中，如果并发很高，这时候，普通的 LinkedQueue就会成为瓶颈，性能就会出现毛刺，当换上 SynchronousQueue后，性能就会好很多。

### LinkedTransferQueue

从名字看其实跟LinkedBlockingQueue貌似有点关联, 那到底有什么区别呢.

#### 原理

实际上其是LinkedBlockingQueue和SynchronousQueue的结合体:
1.  LinkedBlockingQueue内部使用了锁, 并发度不高
2.  SynchronousQueue在put的时候, 如果没有匹配上, 则会阻塞

LinkedTransferQueue解决了以上的问题, 那么是怎么解决的呢?

1.  LinkedTransferQueue内部使用了大量的自旋来替代锁, 是无锁的
2.  LinkedTransferQueue是有容量的, 在put操作成功的时候, 不会阻塞掉

## Executors

### FixedThreadPool

#### 原理

1. 核心线程数=最大线程数
2. LinkedBlockingQueue

#### 应用场景

适用于执行长期任务

### ForkJoinPool

关于ForkJoinPool是什么?

1.  ForkJoinPool 不是为了替代 ExecutorService，而是它的补充，在某些应用场景下性能比 ExecutorService 更好。
2.  ForkJoinPool 主要用于实现“分而治之”的算法，特别是分治之后递归调用的函数，例如 quick sort 等。
3.  ForkJoinPool 最适合的是计算密集型的任务，如果存在 I/O，线程间同步，sleep() 等会造成线程长时间阻塞的情况时，最好配合使用 ManagedBlocker。

#### 原理

1.  ForkJoinPool 的每个工作线程都维护着一个工作队列（WorkQueue），这是一个双端队列（Deque），里面存放的对象是任务（ForkJoinTask）。
2.  每个工作线程在运行中产生新的任务（通常是因为调用了 fork()）时，会放入工作队列的队尾，并且工作线程在处理自己的工作队列时，使用的是 LIFO 方式，也就是说每次从队尾取出任务来执行。
3.  每个工作线程在处理自己的工作队列同时，会尝试窃取一个任务（或是来自于刚刚提交到 pool 的任务，或是来自于其他工作线程的工作队列），窃取的任务位于其他线程的工作队列的队首，也就是说工作线程在窃取其他工作线程的任务时，使用的是 FIFO 方式。
4.  在遇到 join() 时，如果需要 join 的任务尚未完成，则会先处理其他任务，并等待其完成。
5.  在既没有自己的任务，也没有可以窃取的任务时，进入休眠。

![](https://img-blog.csdnimg.cn/20181111222837182.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTA4NDEyOTY=,size_16,color_FFFFFF,t_70)

#### 应用场景

其最适合的是计算密集型任务

```
IO密集型任务就不行了吗? 线程数是可控的(构造函数的parallelism来控制), 那么按照IO密集型来设置便可

实现上确实可以, 但是在JDK8 Stream提供了并行流的API--parallelStream, 其底层使用的就是ForkJoinPool, 用的是一个公共的ForkJoinPool, 其线程数就是当前机器的核心数, 也就是所有用到parallelStream的任务都在同一个ForkJoinPool里执行, 一旦出现阻塞时间过长的任务, 使用并行流的效率就变低了, 因为线程阻塞了, 部分CPU核心就处于空闲状态, 所以其不适合IO密集型任务的由来就是这里

那么怎么办呢?

在ForkJoinPool中提供了这么一个API--ManagedBlocker, IO型任务实现这一套便可, 原理就是执行任务阻塞的时候, ForkJoinPool会 额外创建线程 来执行其他任务, 这样一来便能提高CPU的利用率
```

### SingleThreadExecutor

#### 原理

1. 核心线程数=最大线程数=1
2. LinkedBlockingQueue

#### 应用场景

适用于串行任务执行的场景

### CachedThreadPool

#### 原理

1. 核心线程数=0
2. SynchronousQueue

```
为什么使用 SynchronousQueue ?


```

#### 应用场景

执行很多短期异步的小程序或者负载较轻的服务器

### ScheduledThreadPoolExecutor

1. 最大线程数=Integer.MAX_VALUE
2. DelayedWorkQueue

```
DelayedWorkQueue 是什么队列?


```

## 引用

1.  [JDK8线程池ThreadPoolExecutor的实现原理一](https://juejin.im/post/5d67e5b4e51d4561f64a0849)
2.  [JDK8线程池ThreadPoolExecutor的实现原理二](https://juejin.im/post/5d688686e51d4561ce5a1c8b)
3.  [如何合理地估算线程池大小？](http://ifeve.com/how-to-calculate-threadpool-size/)
4. [聊聊并发（七）——Java 中的阻塞队列](https://www.infoq.cn/article/java-blocking-queue)
5. [死磕 Java 并发精品合集](http://cmsblogs.com/?p=2611)
6. [Java 并发编程笔记：如何使用 ForkJoinPool 以及原理](http://blog.dyngr.com/blog/2016/09/15/java-forkjoinpool-internals/)
7.  [ForkJoinPool实现原理和源码解析](https://blog.csdn.net/u010841296/article/details/83963637)
8.  [并发编程之 SynchronousQueue 核心源码分析](https://juejin.im/post/5ae754c7f265da0ba76f8534)

