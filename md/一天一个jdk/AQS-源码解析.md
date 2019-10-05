# AQS 源码

## 需求点

理论上任一款同步器都可以实现其他类型的同步器, 比如说可以通过信号量来实现重入锁, 也可以通过重入锁来实现锁, 但是这样子的实现, 必定是复杂的, 难用的, 所以在JSR166建立了一个同步框架AQS. 基于通过AQS一个同步框架, 可以实现任意类型的同步器.

1.  AQS的背景
2.  同步器的简单实现
3.  AQS的需求点
4.  状态的开放
5.  阻塞唤醒
6.  队列的管理
7.  CLH
8.  head
9.  pre显式
10. next指针
11. 节点状态-取消

## acquire()方法

1.  尝试获取资源, 成功获取锁成功, 否则执行2.
2.  把当前线程封装成节点挂到队尾
3.  自旋获取锁

```
在队列里面获取到锁的条件是, 前驱节点为head(保证了队列上的公平性), 且竞争到资源的情况下, 才可获取到锁.
```

## release()方法

1.  尝试释放资源, 失败则返回false, 否则执行2
2.  判断head是否不为空且head.waitStatus != 0, 是则说明可能有后继节点需要唤醒, 执行3, 否则返回true

```
head.waitStatus != 0 为什么?

因为当有队列里还有其他节点, 那么必然head.waitStatus != 0, 所以需要当前线程去执行唤醒操作或者是剔除cannel节点
```

```
若当前线程A此处head若刚判断为空, 这时其他线程B把head实例化了, 那线程A不是唤醒不了线程B吗?

不会的, 线程B实例化head后还会去竞争一次资源才会挂起的.
```

3.  设置node的状态为0

4.  若存在后继节点则唤醒后继节点

5.  否则从尾部开始遍历, 距离head非cannel节点并唤醒

```
为什么从尾部遍历呢?

因为next刚好置null, 获取后继节点已取消
```

## Condition

### 作用

Condition就是条件, 它的一个作用就是线程需要等待某个条件才能继续运行下去, 所以会把自己挂起, 当条件达成后则会唤醒自己继续运行.

### await

1.  条件等待队列入队
2.  释放锁
3.  挂起
4.  尝试获取锁

### singal

1.  将条件等待队列中第一个节点入队AQS等待队列
2.  如果前驱节点取消了或者尝试设置sinal失败了, 则唤醒其线程让其自己来设置

## 同步器

研究同步器, 还是从其功能点来切入, 看它如何利用AQS来进行实现这些功能的,
然后思考其对state资源的一个定义, 怎么来维护

### ReentrantLock

#### 排他性

ReentrantLock首先是一个排他锁, 在这点上直接利用AQS的独占模式便可.

#### 公平和非公平

ReentrantLock 可以采用公平机制和非公平机制, 而公平和非公平可以从这点上来考虑, 当有线程已经在等待锁了, 那么新的线程来获取锁, 是否可以竞争到, 这点便可判断是否公平, 而在AQS上, 预留**tryAcquire**方法, 可通过这个实现. 

如果是自己来实现的话, 那么可以想象得到, 公平的一个实现就是, 首先判断是否有线程在等待, 没有才去竞争锁, 而在AQS提供了**hasQueuedPredecessors()**和**compareAndSetState()**便可满足这个实现

非公平的实现更简单了, 直接竞争资源便可.

#### 可重入性

ReentrantLock是通过state来对重入次数进行计数的, 那如果判断其是否重入, AQS提供了**getExclusiveOwnerThread()**方法来判断

#### state资源的定义

1.  0: 代表锁未被占有
2.  大于0: 说明锁已经被占用了, 且代表了重入次数

### CountDownLatch

这个类有什么用呢, 简单来说, 它可以实现一个计数器的功能, 需要等待计数器计完事后才可以做后面的事. 那么完成这个功能, 则需要实现**计数完成前的等待**, **计数**, 同时, 可以多个线程等待这个计数结果, 可以判断其需要使用共享模式完成

#### 计数

很简单, 利用state来计数, 完成一次计数则减一, 重写**tryReleaseShared()**, 对state进行原子性更新

#### 等待计数完成

也很简单, 判断该state是否为0便可, 没有计数完成, 则挂起, 那挂起后, 谁唤醒呢? 由计数线程. 判断state等于0便返回false, 否则返回true

```
这里有一点得注意, tryReleaseShared()返回false才会唤醒排队的线程
```

#### state资源的定义

1.  0: 计数完成
2.  大于0: 剩余未完成计数

#### Semaphore

信号量, 可以控制多个线程进入同步块. 从其功能上来看, 跟ReentrantLock类似, 不同的是进入同步块的线程可以有多个, 何不妨在ReentrantLock基础上考虑它的实现, 其实很简单, 就是共享模式下的ReentrantLock. 

```
注意, Semaphore是不可以重入的, 以下调用会发生死锁

semaphore.acquire()
semaphore.acquire()
semaphore.release()
```

### CyclicBarrier

循环栅栏, 允许一组线程一个公共屏障点才可继续运行, 可以认为这一组线程都在等待一个条件, 而当该组最后一个线程到达屏障点则条件达成.

#### 条件

可以想到AQS的条件队列, 等待一个条件则可使用**Condition#await()**

#### 最后一个线程

通过计数的方式来得知最后一个线程, 相当于可以定义state为该组线程的数量. 每当一个线程获取到锁则state减一, 当最后一个线程获取锁, 则state = 0, 这时条件达成, 调用**Condition#signalAll()**唤醒阻塞的线程, 否则, 调用**Condition#await()**阻塞当前线程.

以上的实现思路是不是非常完美? 然后并不是, 当线程判断 state != 0, 则会调用**Condition#await()**, 问题就在这个方法上, 它会释放当前线程所占有的资源, 即state, 也就是state又重置了, 所以始终判断不了state=0. 

那怎么办呢? 可以自行维护一个计数器了. 通过互斥的方式来控制这个计数器, 再利用AQS来实现互斥. 但是转念一想? 互斥访问一个变量, 不是已有这种同步器了吗? ReentrantLock, Semaphore. 何必再实现一套, 重新造轮子呢? 直接利用上不就得了.

于是最终的答案是ReentrantLock加Condition来实现.

```
突然想到, 重写tryRelease时, 不是释放掉资源不就行了?

并不行, condition并没有调用tryRelease方法, 默认会把占用的资源给释放掉了, 当唤醒时, 重新去竞争这些资源
```

#### 可循环

既然在已经获取到锁了, 在里面做什么事都行, 当最后一个线程到达时, 将计数器重置即可

#### 唤醒与中断(重点)

唤醒与中断的时机判断才是该同步器实现的难点, 其实时序关系其实很重要, 我们知道, 当线程醒过来后, 想要判断自己是中断唤醒地还是调用unpark唤醒的, 是通过**Thread.interrupted()**判断的, 但若是通过Condition的话, 就得判断是否抛出InterruptedException异常来判断了. 

当抛出中断异常时, 若条件达成的话, 即**generation**(这里引入代的概念来判断是否计数完成)不等于当前阻塞前的**generation**, 且栅栏未被破坏, 这时条件达成在前 ,则不能破坏栅栏, 重新设置中断标记便可


### ReentrantReadWriteLock

[【死磕 Java 并发】—– J.U.C 之读写锁：ReentrantReadWriteLock](http://www.iocoder.cn/JUC/sike/ReentrantReadWriteLock/)

### 线程池的Worker

worker实现的是只有互斥, 那么它到底对什么操作需要互斥呢?

```
protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
}
```
互斥的调用初如下:

```
final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
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
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }
```

但是转念一想, worker不是对线程的封装吗? 按道理说worker只有这个线程才会去调用啊, 单线程调用何必加锁呢?

这个出现必然有意义的, 其意义就在线程池需要shutdown, 调用方法 **shutdown()** 时, 会去中断idle(空闲)线程, idle线程调用 **getTask()** 阻塞调用, 当被中断唤醒后, 判断线程池状态为stop后, 然后就可以退出执行了, 但首先得找出idle线程, 又但是实际上并没有地方保存idle线程列表, 于是对所有的worker调用中断方法便可. 但是这会产生一个问题, 有可能线程是因为执行task而阻塞掉的, 并不是idle线程, 那这里怎么区分呢? 

就是通过lock, 通过这个lock锁住task, 然后shutdown通过tryLock()便可知道是否为idle线程了