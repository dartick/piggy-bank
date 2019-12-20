# JMM

## 编程要义

在并发编程中, 肯定会涉及线程之间的通讯, 而在Java中, 其通讯方式有多种多样,消息传递, 共享变量, 管道. 其中共享变量是我们常用的通讯方式. 所以如何正确地通过共享变量来进行并发编程非常重要.

## 一个诡异的现象

假设我们有两个线程（线程1和线程2）分别运行在两个CPU上，有两个初始值为0的全局共享变量x和y，两个线程分别执行下面两条指令：

初始条件： x = y = 0;

| 线程1 | 线程2 |
| ----- | ----- |
| x=1;|y=1;|
| r1=y;|r2=x;|

因为多线程程序, 谁先执行的次序不定, 所以会导致一下不同的结果:

| Execution 1 | Execution 2 | Execution 3 |
| ----------- | -----------| ----------- |
|x = 1;|y = 1;|x = 1;|
|r1 = y;|r2 = x;|y = 1;|
|y = 1;|x = 1;|r1 = y;|
|r2 = x;|r1 = y;|r2 = x;|
|结果:r1==0 and r2 == 1|结果: r1 == 1 and r2 == 0|结果: r1 == 1 and r2 == 1|

当然上面三种情况并没包括所有可能的执行顺序，但是它们已经包括所有可能出现的结果了，所以我们只举上面三个例子。我们注意到这个程序只可能出现上面三种结果，但是不可能出现r1==0 and r2==0的情况。

然后, 在程序实际运行中就会出现这种问题: 

```
public class UnexpectedExample {

    public int x = 0;
    public int y = 0;
    public int r1 = 0;
    public int r2 = 0;

    public void fun1() {
        x = 1;
        r1 = y;
    }

    public void fun2() {
        y = 1;
        r2 = x;
    }

    public static void main(String[] args) throws InterruptedException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
        for (int i = 0; i < 2000; i++) {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            UnexpectedExample unexpectedExample = new UnexpectedExample();
            new Thread(() -> {
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException ignore) {
                }
                unexpectedExample.fun1();
                countDownLatch.countDown();
            }).start();
            new Thread(() -> {
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException ignore) {
                }
                unexpectedExample.fun2();
                countDownLatch.countDown();
            }).start();
            countDownLatch.await();
            if (unexpectedExample.r1 == 0 && unexpectedExample.r2 == 0) {
                throw new AssertionError();
            }
        }
    }
}
```

### 问题原因剖析

 #### 编译重排序

编译器在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序，尽可能减少寄存器的读取、存储次数，充分复用寄存器的存储值。举个例子:

```
int a = 1;
int b = 1;
a = a + 1;
b = b + 1;
```
这种情况下编译器会优化成以下顺序:
```
int a = 1;
a = a + 1;
int b = 1;
b = b + 1;
```
被重排序后, 仍然不会改变程序的执行结果, 但是在性能会有所提升, 因为在对变量a或b操作的话, 已经存在寄存器上了.
```
如果可以重排序的话, 那不是会出问题吗? 比如说:
int a, b = 0;
a = 1;
b = a + 1;

a = 1, b = 2

重排序为:

int a, b = 0;
b = a + 1;
a = 1;

a = 1, b = 1
```

#### as-if-serial

不管怎么重排序（编译器和处理器为了提高并行度），（单线程）程序的执行结果不能被改变。为了遵守 as-if-serial 语义，编译器和处理器不会对存在数据依赖关系的操作做重排序，因为这种重排序会改变执行结果。

| 名称 | 代码示例 | 说明 |
| ----- | ----- | ----- |
| 写后读|a = 1;b = a;|写一个变量之后，再读这个位置。|
| 写后写|a = 1;a = 2;|写一个变量之后，再写这个变量。|
| 读后写|a = b;b = 1;|读一个变量之后，再写这个变量。|

 #### 指令重排序

现在的CPU一般采用流水线来执行指令。一个指令的执行被分成：取指、译码、访存、执行、写回、等若干个阶段。然后，多条指令可以同时存在于流水线中，同时被执行。指令流水线并不是串行的，并不会因为一个耗时很长的指令在“执行”阶段呆很长时间，而导致后续的指令都卡在“执行”之前的阶段上。相反，流水线是并行的，多个指令可以同时处于同一个阶段，只要CPU内部相应的处理部件未被占满即可。
比如：CPU有一个加法器和一个除法器，那么一条加法指令和一条除法指令就可能同时处于“执行”阶段，而两条加法指令在“执行”阶段就只能串行工作。然而，这样一来，乱序可能就产生了。比如：一条加法指令原本出现在一条除法指令的后面，但是由于除法的执行时间很长，在它执行完之前，加法可能先执行完了。再比如两条访存指令，可能由于第二条指令命中了cache而导致它先于第一条指令完成。

#### 内存重排序

##### cpu缓存

CPU的频率太快了，快到主存跟不上，这样在处理器时钟周期内，CPU常常需要等待主存，这样就会浪费资源。所以cache的出现，是为了环节CPU和内存之间速度的不匹配问题（结构：CPU -> cache -> memory）

缓存的容量远远小于主存，因此出现缓存不命中的情况在所难免，既然缓存不能包含CPU所需要的所有数据，那么缓存的存在真的有意义吗？

CPU cache是肯定有它存在的意义的，至于CPU cache有什么意义，那就要看一下它的局部性原理了：
1.  时间局部性：如果某个数据被访问，那么在不久的将来它很可能再次被访问
2.  空间局部性：如果某个数据被访问，那么与它相邻的数据很快也可能被访问

```
直写（write-through）。直写更简单一点：我们透过本级缓存，直接把数据写到下一级缓存（或直接到内存）中，如果对应的段被缓存了，我们同时更新缓存中的内容（甚至直接丢弃）

回写（write-back）. 缓存不会立即把写操作传递到下一级，而是仅修改本级缓存中的数据，并且把对应的缓存段标记为“脏”段。
```

##### MESI协议

如果有多个核，每个核又都有自己的缓存，那么我们就遇到问题了：如果某个 CPU 缓存段中对应的内存内容被另外一个 CPU 偷偷改了，会发生什么？

MESI协议的提出便是解决这一问题:

1.  失效（Invalid）缓存段，要么已经不在缓存中，要么它的内容已经过时。为了达到缓存的目的，这种状态的段将会被忽略。一旦缓存段被标记为失效，那效果就等同于它从来没被加载到缓存中。
2.  共享（Shared）缓存段，它是和主内存内容保持一致的一份拷贝，在这种状态下的缓存段只能被读取，不能被写入。多组缓存可以同时拥有针对同一内存地址的共享缓存段，这就是名称的由来。
3.  独占（Exclusive）缓存段，和 S 状态一样，也是和主内存内容保持一致的一份拷贝。区别在于，如果一个处理器持有了某个 E 状态的缓存段，那其他处理器就不能同时持有它，所以叫“独占”。这意味着，如果其他处理器原本也持有同一缓存段，那么它会马上变成“失效”状态。
4.  已修改（Modified）缓存段，属于脏段，它们已经被所属的处理器修改了。如果一个段处于已修改状态，那么它在其他处理器缓存中的拷贝马上会变成失效状态，这个规律和 E 状态一样。此外，已修改缓存段如果被丢弃或标记为失效，那么先要把它的内容回写到内存中——这和回写模式下常规的脏段处理方式一样。


```
缓存是分“段”（line）的，一个段对应一块存储空间，大小是 32（较早的 ARM、90 年代 /2000 年代早期的 x86 和 PowerPC）、64（较新的 ARM 和 x86）或 128（较新的 Power ISA 机器）字节。每个缓存段知道自己对应什么范围的物理内存地址.
```

在所有的脏缓存段（M 状态）被回写后，任意缓存级别的所有缓存段中的内容，和它们对应的内存中的内容一致。此外，在任意时刻，当某个位置的内存被一个处理器加载入独占缓存段时（E 状态），那它就不会再出现在其他任何处理器的缓存中。只要遵从这个规则, 那么就可以保证缓存能够及时达到一致.

#### 有了MESI协议就完全没问题了吗

如果满足下面的条件，你就可以得到完全的顺序一致性：第一，缓存一收到总线事件，就可以在当前指令周期中迅速做出响应。第二，处理器如实地按程序的顺序，把内存操作指令送到缓存，并且等前一条执行完后才能发送下一条。当然，实际上现代处理器一般都无法满足以上条件：

缓存不会及时响应总线事件。如果总线上发来一条消息，要使某个缓存段失效，但是如果此时缓存正在处理其他事情（比如和 CPU 传输数据），那这个消息可能无法在当前的指令周期中得到处理，而会进入所谓的“失效队列（invalidation queue）”，这个消息等在队列中直到缓存有空为止。
处理器一般不会严格按照程序的顺序向缓存发送内存操作指令。当然，有乱序执行（Out-of-Order execution）功能的处理器肯定是这样的。顺序执行（in-order execution）的处理器有时候也无法完全保证内存操作的顺序（比如想要的内存不在缓存中时，CPU 就不能为了载入缓存而停止工作）。
写操作尤其特殊，因为它分为两阶段操作：在写之前我们先要得到缓存段的独占权。如果我们当前没有独占权，我们先要和其他处理器协商，这也需要一些时间。同理，在这种场景下让处理器闲着无所事事是一种资源浪费。实际上，写操作首先发起获得独占权的请求，然后就进入所谓的由“写缓冲（store buffer）”组成的队列（有些地方使用“写缓冲”指代整个队列，我这里使用它指代队列的一条入口）。写操作在队列中等待，直到缓存准备好处理它，此时写缓冲就被“清空（drained）”了，缓冲区被回收用于处理新的写操作。
这些特性意味着，默认情况下，读操作有可能会读到过时的数据（如果对应失效请求还等在队列中没执行），写操作真正完成的时间有可能比它们在代码中的位置晚，一旦牵涉到乱序执行，一切都变得模棱两可。 

### 解决方案: 内存屏障

1. 编译重排序. 通过内存屏障告诉编译器, 这个地方强制不能重排序.
2. 指令重排序. 同理, 强制不能并行执行指令.
3. 内存重排序. 同步强刷写缓冲和失效队列.

#### 屏障种类

1.  LoadLoad 屏障. 防止LoadLoad屏障前后的读指令的指令重排序处理器以阻塞的方式先处理失效队列的消息，防止读取到老数据
2.  StoreStore 屏障. 防止StoreStore屏障前后的写指令的指令重排序处理器以阻塞的方式将当前存储缓存（store buffer）的值写回主存
3.  LoadStore 屏障. 防止LoadStore屏障前的读指令和屏障后的写指令的指令重排序处理器以阻塞的方式先处理失效队列的消息，防止读取到老数据在JVM中，实际上它和LoadLoad屏障作用是相同的，底层都是调用 acquire() 方法
4.  StoreLoad 屏障. 防止StoreLoad屏障前后的所有读写指令的指令重排序处理器以阻塞的方式将当前存储缓存（store buffer）的值写回主存处理器以阻塞的方式先处理失效队列的消息，防止读取到老数据

该屏障同时具备另三种屏障的作用，因此开销也最大。

实际上, 上面四种屏障是jvm层级定义的, 实际在cpu层级的话, 不同的CPU架构对内存屏障的实现方式与实现程度不一样, 这就可以点题了,jmm的概念是屏蔽底层硬件的实现差异, 是访问内存达到一个一致的效果, 所以这个差异就是指不同CPU对屏障的实现不同, jvm在实现的过程中, 针对不同的cpu, 对内障加与不加, 加什么指令, 都需要有所考虑.

以x86架构来看, 实际的内存屏障有三种:

1.  Store Barrier. sfence指令实现了Store Barrier
2.  Load Barrier. lfence指令实现了Load Barrier
3.  Full Barrier. mfence指令实现了Full Barrier

#### 回到例子上

问题和原因都找到了, 那怎么解决呢, java提供volatile关键字来对相关变量的读写添加内存屏障, 那问题来, 有四种屏障, 怎么加法才会解决这个问题呢? 

从JMM给volatile定义的内存语义上来看:

1.  当写一个volatile变量时，JMM会把该线程对应的本地内存中的共享变量值立即刷新到主内存中。
2.  当读一个volatile变量时，JMM会把该线程对应的本地内存设置为无效，直接从主内存中读取共享变量

首先解决voletile读写与普通读写的重排序问题:

1.  对于volatile写, 首先得保证volatile写不能与前后的普通写重排序, 所以可以在volatile写前后加上StoreStore屏障.
```
StoreStore
volatile写
StoreStore
```
2.  对于volatile读, 首先得保证volatile读前后的普通读不可以重排序, 所以可以在volatile读前后加上LoadLoad屏障.
```
StoreStore
volatile写
StoreStore

....

LoadLoad
volatile读
LoadLoad
```
然后解决voletile读写之间重排序的问题:

1.  voletile读读与voletile写写已经可以通过上面的加法来解决了
2.  voletile读写之间可以在voletile读之后添加LoadStore屏障.
```
StoreStore
volatile写
StoreStore

....

LoadLoad
volatile读
LoadLoad
LoadStore
```
3.  voletile写读之间可以在voletile写之后添加StoreLoad屏障.
```
StoreStore
volatile写
StoreStore
StoreLoad

....

LoadLoad
volatile读
LoadLoad
LoadStore
```

重排序的问题解决之后, 然后需要解决的是voletile读写可见性问题:

1.  根据内存语义, 要使得voletile写之前的数据对该volatile读之后可见.
```
通过在voletile写之后加StoreStore, 在volatile读之后加LoadLoad便行了, 其实在上面已经加上了.
```
最后呈现出来的是一下的样子:

```
StoreStore
volatile写
StoreStore
StoreLoad

....

LoadLoad
volatile读
LoadLoad
LoadStore
```

事实上, 上面加的部分屏障功能已经重叠掉了, 需要再拿掉:

1.  volatile写后的StoreLoad包含StoreStore, 去掉StoreStore

```
StoreStore
volatile写
StoreLoad

....

LoadLoad
volatile读
LoadLoad
LoadStore
```


又事实上, 这里还有可以优化的地方, 是不是每个屏障都不可或缺, 把每个都单独拿掉试试看可以发现:

1.  volatile读前的LoadLoad屏障不是必须的, 根据volitale的语义, 当volatile读之后, 之后的普通读能读到比该volatile的版本一致或者要新就行, 所以可以去掉

```
StoreStore
volatile写
StoreLoad

....

volatile读
LoadLoad
LoadStore
```

所以以上就是volatile为防止重排序和保证可见性进行加屏障的最保守策略.



```
实际上, 内存屏障并没有实际的字节码指令, 能关联的只有class文件上字段表的volalite标记位, 当执行到该字段相对应字节码的时候, 根据volalite标记位来解释成不同的机器码指令. 
```

### 对于long和double变量的规则

保证对long和double的读写是原子性的.

## jmm对其他关键字的语义

### final

在旧的Java模型中, 线程可能看到 final 域的值会改变.

```
1.  在构造函数内对一个 final 域的写入，与随后把这个被构造对象的引用赋值给一个引用变量，这两个操作之间不能重排序。
2.  初次读一个包含 final 域的对象的引用，与随后初次读这个 final 域，这两个操作之间不能重排序。
```

关于jmm对final的语义的实现也是通过内存屏障来实现的.

#### 写 final 域的重排序规则

写 final 域的重排序规则禁止把 final 域的写重排序到构造函数之外。这个规则的实现包含下面 2 个方面：

1.  JMM 禁止编译器把 final 域的写重排序到构造函数之外。
2.  编译器会在 final 域的写之后，构造函数 return 之前，插入一个 StoreStore 屏障。这个屏障禁止处理器把 final 域的写重排序到构造函数之外。

#### 读 final 域的重排序规则

读 final 域的重排序规则如下：

1.  在一个线程中，初次读对象引用与初次读该对象包含的 final 域，JMM 禁止处理器重排序这两个操作（注意，这个规则仅仅针对处理器）。编译器会在读 final 域操作的前面插入一个 LoadLoad 屏障。

初次读对象引用与初次读该对象包含的 final 域，这两个操作之间存在间接依赖关系。由于编译器遵守间接依赖关系，因此编译器不会重排序这两个操作。大多数处理器也会遵守间接依赖，大多数处理器也不会重排序这两个操作。但有少数处理器允许对存在间接依赖关系的操作做重排序（比如 alpha 处理器），这个规则就是专门用来针对这种处理器。

### synchronized

即当读写两个线程同时访问同一个变量时，synchronized用于确保写线程更新变量后，读线程再访问该 变量时可以读取到该变量最新的值。

## happens-before

当我们编写程序时, 如何判断我们写的代码会在多线程环境下是否安全呢, 那就是通过happens-before规则来判断, 在JMM中，如果一个操作执行的结果需要对另一个操作可见，那么这两个操作之间必须存在happens-before关系.

1.  程序次序规则：一个线程内，按照代码顺序，书写在前面的操作先行发生于书写在后面的操作；
2.  锁定规则：一个unLock操作先行发生于后面对同一个锁额lock操作；
3.  volatile变量规则：对一个变量的写操作先行发生于后面对这个变量的读操作；
4.  传递规则：如果操作A先行发生于操作B，而操作B又先行发生于操作C，则可以得出操作A先行发生于操作C；
5.  线程启动规则：Thread对象的start()方法先行发生于此线程的每个一个动作；
6.  线程中断规则：对线程interrupt()方法的调用先行发生于被中断线程的代码检测到中断事件的发生；
7.  线程终结规则：线程中所有的操作都先行发生于线程的终止检测，我们可以通过Thread.join()方法结束、Thread.isAlive()的返回值手段检测到线程已经终止执行；
8.  对象终结规则：一个对象的初始化完成先行发生于他的finalize()方法的开始；

## 资料引用

1. [指令重排序详细例子](https://www.jianshu.com/p/c6f190018db1)
2. [缓存一致性](https://www.infoq.cn/article/cache-coherency-primer)
3.  [一文解决内存屏障](https://monkeysayhi.github.io/2017/12/28/%E4%B8%80%E6%96%87%E8%A7%A3%E5%86%B3%E5%86%85%E5%AD%98%E5%B1%8F%E9%9A%9C/)
4.  [顺序一致性与缓存一致性](http://www.parallellabs.com/2010/03/06/why-should-programmer-care-about-sequential-consistency-rather-than-cache-coherence/)
5.  [内存屏障的实现](https://www.jianshu.com/p/c6f190018db1)
6.  https://docs.oracle.com/javase/specs/jls/se11/html/jls-17.html#jls-17.5
7.  [JSR133中文版](http://ifeve.com/wp-content/uploads/2014/03/JSR133%E4%B8%AD%E6%96%87%E7%89%881.pdf)
8.  [jmm-cookbokk翻译](http://ifeve.com/jmm-cookbook/)
9.  [happens-before例子](http://www.dengshenyu.com/%E5%90%8E%E7%AB%AF%E6%8A%80%E6%9C%AF/2016/05/01/jmm-happens-before.html)



