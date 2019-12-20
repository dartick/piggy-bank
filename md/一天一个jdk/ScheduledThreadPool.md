# ScheduledThreadPoolExecutor 

## Timer

Timer是JDK1.3中提供的一个可以执行周期性任务的API

### Timer核心内部类

1.  TimerQueue. 优先级队列，保存要执行的任务，通过二叉堆来实现。通过syschronized来保证并发安全。
2.  TimerThread。执行任务的线程，一个Timer只开启一个TimerThread, 即与Timer是一对一关系。重载了run()方法，主要作用是通过while循环不断地拉取TimerQueue中的任务来执行
3.  TimerTask。对任务的封装，主要封装了任务的下一次执行时间，相当于任务的权重

### Timer调度流程

![](https://img-blog.csdnimg.cn/20181123104610889.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2ppemh1NDg3Mw==,size_16,color_FFFFFF,t_70)

### 缺点

1.  单线程运行，一个任务运行时间过长，会影响到其他的任务的执行，可能导致任务堆积
2.  单个任务抛异常后，线程便挂了，整个Timer停止，导致后续的任务得不到执行，同时异常也不会被捕捉，异常信息丢失

## ScheduledThreadPoolExecutor 原理

ScheduledThreadPoolExecutor继承于ThreadPoolExecutor， 所以是基于ThreadPoolExecutor来实现任务调度的，那么在了解ThreadPoolExecutor的前提下， 比较它们的不同点便能知道的它的实现原理，同时在看的同时，对比Timer, 思考其是如何解决Timer的缺陷的（都说ScheduledThreadPoolExecutor是Timer的替代品，所以肯定需要解决Timer上的问题）

与ThreadPoolExecutor的主要不同在于两点，一个提交任务时会把任务封装成**ScheduledFutureTask**，另外一点是使用了**DelayedWorkQueue**作为阻塞队列

### ScheduledFutureTask

ScheduledThreadPoolExecutor的实现原理与Timer差不多，都是都过优先队列来实现的，这个类应该跟TimerTask类似，维护了任务的优先级别（即下一次执行时间）和周期时间。

ScheduledFutureTask做了更细腻的接口划分，比TimerTask更加利于使用和拓展，以下为ScheduledFutureTask的接口继承图：

![](https://github.com/wujiazhen2/pict/blob/master/thread/ScheduledFutureTask.png?raw=true)

主要的方法有以下两点：

1.  通过 **Delayed#getDelayed()** 方法来获取该任务距离下一次执行还剩多少时间
2.  重载**Comparable#compareTo()**方便优先级队列作优先级比较

### DelayedWorkQueue

实际上优先级队列原理与TimerQueue类似，但是继承**BlockingQueue**，所以在实现需要考虑更多的情况，所以会更加复杂，其需要考虑的地方在于以下几点：

1.  poll超时获取一个可执行任务（已到达调度时间即为可执行任务）时，当没获取到可执行时，如何选取一个合理的等待时间，当firstTask为null, 调用**Condition#awaitNanos**(超时的原理)，若非null, 则需要考虑其是否为可执行，若可执行则获取任务成功，若不可执行，则对比超时时间和任务的剩余等待时间，取两者中最小的作为等待时间便可。

2.  在第一点的基础上，若多个线程同时获取到可执行任务，那么他们则会等待相差无几的时间，然后唤醒再去竞争，然后只有一个任务，所以此处多个线程的唤醒是低效的，所以此处采用了leader-follower模式。

3.  当插入完成后（put, offer, add）, 如果插入的任务为当前优先级最高，则会取消当前的leader, 唤醒一个线程，随机选举一个线程作为leader

```
if (queue[0] == e) {
    leader = null;
    available.signal();
}
```
### Timer的问题

1.  单线程。很明显继承ThreadPoolExecutor, 就是利用了其多线程工作的模式
2.  异常不捕捉。ScheduledFutureTask继承于FutureTask，ThreadPoolExecutor在执行FutureTask时发生异常，则会将异常信息保存于FutureTask中。同时，并不会一个任务抛异常而导致整个线程池不可用

###  为什么不用DelayQueue的二叉堆实现

DelayedWorkQueue类似于DelayQueue和PriorityQueue，是基于“堆”的一种数据结构。
区别就在于ScheduledFutureTask记录了它在堆数组中的索引，这个索引的好处就在于：
取消任务时不再需要从数组中查找任务，极大的加速了remove操作，时间复杂度从O(n)降低到了O(log n)，
同时不用等到元素上升至堆顶再清除从而降低了垃圾残留时间。
但是由于DelayedWorkQueue持有的是RunnableScheduledFuture接口引用而不是ScheduledFutureTask的引用，
所以不能保证索引可用，不可用时将会降级到线性查找算法(我们预测大多数任务不会被包装修饰，因此速度更快的情况更为常见)。

所有的堆操作必须记录索引的变化 ————主要集中在siftUp和siftDown两个方法中。一个任务删除后他的headIndex会被置为-1。
注意每个ScheduledFutureTask在队列中最多出现一次(对于其他类型的任务或者队列不一定只出现一次)，
所以可以通过heapIndex进行唯一标识。

## leader-follower 模式

1.  在Leader-follower线程模型中每个线程有三种模式，leader,follower, processing。
2.  在Leader-follower线程模型一开始会创建一个线程池，并且会选取一个线程作为leader线程，leader线程负责监听网络请求，其它线程为follower处于waiting状态，当leader线程接受到一个请求后，会释放自己作为leader的权利，然后从follower线程中选择一个线程进行激活，然后激活的线程被选择为新的leader线程作为服务监听，然后老的leader则负责处理自己接受到的请求（现在老的leader线程状态变为了processing），处理完成后，状态从processing转换为。follower
3.  可知这种模式下接受请求和进行处理使用的是同一个线程，这避免了线程上下文切换和线程通讯数据拷贝。

## 二叉堆

可见优先级队列通过二叉堆来实现优先级的排序，那为什么不使用其他的排序算呢？

因为其他的排序算法是全局有序的，全局有序开销明显更大，而这种场景（top k）下, 并需要全局有序，所以选择二叉堆更优

## 资料

1.  [并发编程 —— Timer 源码分析](https://juejin.im/post/5ae755e7518825671c0e56e9)
2.  [Timer & TimerTask 源码分析](https://chenjiayang.me/2018/07/18/TimerAndTimerTask/)
3.  [任务调度线程池ScheduledThreadPoolExecutor](https://blog.hufeifei.cn/2018/02/22/Java/Java%E5%A4%9A%E7%BA%BF%E7%A8%8B%E5%A4%8D%E4%B9%A0%E4%B8%8E%E5%B7%A9%E5%9B%BA%EF%BC%88%E4%B8%83%EF%BC%89--%E4%BB%BB%E5%8A%A1%E8%B0%83%E5%BA%A6%E7%BA%BF%E7%A8%8B%E6%B1%A0ScheduledThreadPoolExecutor/)
4.  [二叉堆](https://www.itcodemonkey.com/article/8660.html)