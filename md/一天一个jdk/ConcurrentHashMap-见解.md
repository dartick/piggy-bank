# ConcurrentHashMap

## 1.7

网上已经有各种资料详解1.7的ConcurrentHashMap, 这里记录一下自己对该容器的个人见解.

### scanAndLockForPut方法解析

1.7 采用分段锁技术, 降低锁的粒度, 提高并发度. 在实现中, 采用segment来进行分段, 相当于操作segment需要原子地进行. 在put方法的实现中, 会先定位到segment, 然后再调用segment的put方法, 而segment的put方法里面, 并不是一开始调用lock()就完事了, 里面有两个优化点:

1.  调用tryLock()尝试获取锁, 获取锁失败, 则会自旋一定的次数才会阻塞当前线程.
2.  其中, 跟其他自旋不一样的一个地方是, 它并不是空等待一段时间再去tryLock(), 而是查到链表里是否含有与put的key值相等的Node, 如果没有则会进行实例化需要put的Node, 从而节省实例化Node的时间开销


>   Q: 当获取到锁之后, 还会去查找key是否存在, 如果存在的话, 则会替换值就可以了, 那么在自旋实例化的Node就浪费了, 这是一个缺点吗?
>
>   A:  Node确实浪费了, 但是可以加快查询速度, 通过读取HashEntry帮助将对应的数据加载到CPU缓存行中并且帮助触发JIT编译来加热代码，能有效地减少之后加锁的时间, 有点空间换时间的味道。

### 讲解思路

#### HashMap非线程安全

#### HashTable性能低下

#### 分段锁思想

#### Segment与hash桶的映射

#### 扩容机制

#### 1.6与1.7的区别

## 1.8

### 讲解思路

#### 并发扩容

#### 红黑树

#### sizeCtl



#### 


## 资料引用

1.  [juc系列-ConcurrentHashMap](https://www.jianshu.com/p/fadc5bc01e23)
2.  [为什么1.6的ConcurrentHashMap是弱一致的](http://ifeve.com/concurrenthashmap-weakly-consistent/)
3.  