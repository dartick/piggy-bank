# AQS

## 背景

1.  可以通过任一同步器来实现其他同步器, 但是这样的实现复杂, 不灵活且开销大, 所以制定一款通用的框架AQS,, 基于它可以解决以上问题.

## 需求

### 功能

要保证同步, 最基础的功能需要包含两个方法:

1.  acquire. 阻塞调用的线程，直到或除非同步状态允许其继续执行。
2.  release. 通过某种方式改变同步状态，使得一或多个被acquire阻塞的线程继续执行。

额外的功能包括:

1.  非阻塞同步
2.  等待超时机制
3.  可中断机制
4.  独占模式和共享模式
5.  公平和不公平策略

### 性能

1.  可伸缩性. 不管多少线程竞争, 通过同步点的开销应该是个常量.
2.  同步时间. 主要目标之一, 锁定资源的时间耗费最少.
3.  总CPU时间. 指的也是同步时间.
4.  内存负载. 同步消耗的内存大小
5.  线程调度开销. 阻塞线程会减少线程调度的开销, 但是阻塞了的线程需要未阻塞的线程来唤醒, 会占用用户线程的cpu时间.

## 设计与实现

基本思路, acquire操作如下：

```
while (synchronization state does not allow acquire) {
    enqueue current thread if not already queued;
    possibly block current thread;
}
dequeue current thread if it was queued;
```
release操作如下：
```
update synchronization state;
if (state may permit a blocked thread to acquire)
    unblock one or more queued threads;
```
要实现以上流程, 则需要以下三个组件来协作:

1.  同步状态的原子性管理；
2.  线程的阻塞与解除阻塞；
3.  队列的管理；

分别具体实现三个组件虽然可行, 但是在使用上则会带来一定的复杂性, 想想在使用一个同步器的时候需要new三个实例, 这太麻烦了, 所以选择其中一个作为具体实现, 其余作为补充进行适当的耦合, 虽然适用范围减少了, 但是却提供了足够的效率.

### 同步状态

#### 状态的原子操作

```
/**
 * The synchronization state.
 */
private volatile int state;
```

AQS类使用单个int（32位）来保存同步状态，并暴露出getState、setState以及compareAndSet操作来读取和更新这个状态。

```
protected final int getState() {
    return state;
}
```
```
protected final void setState(int newState) {
    state = newState;
}
```
这些方法都依赖于j.u.c.atomic包的支持，这个包提供了兼容JSR133中volatile在读和写上的语义，并且通过使用本地的compare-and-swap或load-linked/store-conditional指令来实现compareAndSetState，使得仅当同步状态拥有一个期望值的时候，才会被原子地设置成新值。

```
protected final boolean compareAndSetState(int expect, int update {
    // See below for intrinsics setup to support this
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

综上, volatile加CAS可以保证状态的原子操作.

#### 32位还是64位

1.  64位性能问题. 虽然JSR166也提供了64位long字段的原子性操作，但这些操作在很多平台上还是使用内部锁的方式来模拟实现的，这会使同步器的性能可能不会很理想。
2.  适用大部分场景. 目前来说，32位的状态对大多数应用程序都是足够的。在j.u.c包中，只有一个同步器类可能需要多于32位来维持状态，那就是CyclicBarrier类，所以，它用了锁（该包中大多数更高层次的工具亦是如此）。

#### 如何暴露状态相关的方法

让子类提供tryAcquire和tryRelease方法来控制acquire和release操作。
当同步状态满足时，tryAcquire方法必须返回true，而当新的同步状态允许后续acquire时，tryRelease方法也必须返回true。这些方法都接受一个int类型的参数用于传递想要的状态。例如：可重入锁中，当某个线程从条件等待中返回，然后重新获取锁时，为了重新建立循环计数的场景。很多同步器并不需要这样一个参数，因此忽略它即可。

```
tryAcquire和tryRelease方法对子类开放, 以实现不同的需求
```



## 资料引用

1.  [同步框架设计论文](http://gee.cs.oswego.edu/dl/papers/aqs.pdf)
2.  [同步框架设计论文翻译](https://www.cnblogs.com/dennyzhangdd/p/7218510.html)