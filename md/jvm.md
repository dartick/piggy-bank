# jvm

## 疑问点

### 自动内存管理机制

1.  returnAddress类型

在jdk1.6之前, finally的实现是通过jsr和ret指令来实现的, 在catch块return或者抛异常的时候都得先执行finally块, 在执行完finally块之后如何返回原来的代码继续执行呢? 就是通过jsr和ret. 执行jsr [finally address offset] 在跳转之前会把当前指令(jsr)的下一条执行地址进行压入操作数栈, 然后跳转finally并执行, 最后在finally执行ret指令, 把栈中的地址对应指令取出来继续执行. 

2.  java内存的分布

### 垃圾收集器与内存分配策略

1.  4种引用的实现


2.  保守式GC和准确式GC

https://www.cnblogs.com/strinkbug/p/6376525.html

3.  oopmap收集引用的原理

同上

4.  Safe Region没法解决sleep或blocked的线程

实际上safe region把正在sleep或blocked这种状态的线程认为处于一个safe point, 当gc发生时不需要等待这些线程, 而当这些线程唤醒后, 则会判断当前是否完成gc root tracking, 若没完成则会自行进入到下一个safe point等待.

5.  新生代晋升到老年代的条件



## 垃圾收集器

### 垃圾收集器总结

#### serial (young gen)

1.  单线程(减少线程切换开销), 专注, 高效
2.  适用于client模式

#### ParNew (young gen)

1. serial的多线程版本
2. server模式首选

#### parallel scavenge (young gen)

1.  吞吐量可控
2.  控制垃圾收集停顿时间
3.  控制吞吐量大小



### 垃圾收集时机

Partial GC：并不收集整个GC堆的模式
1.  Young GC：只收集young gen的
2.  GCOld GC：只收集old gen的GC。只有CMS的concurrent collection是这个模式
3.  Mixed GC：收集整个young gen以及部分old gen的GC。只有G1有这个模式

Full GC：收集整个堆，包括young gen、old gen、perm gen（如果存在的话）等所有部分的模式。

#### young gc

1.  eden区分配满
2.  Parallel Scavenge（-XX:+UseParallelGC）框架下，默认是在要触发full GC前先执行一次young GC，并且两次GC之间能让应用程序稍微运行一小下，以期降低full GC的暂停时间（因为young GC会尽量清理了young gen的死对象，减少了full GC的工作量）

#### full gc

1.  触发young gc前, 分配担保失败(cms 除外)
2.  perm gen分配满
3.  System.gc()
4.  heap dump

https://www.zhihu.com/question/41922036