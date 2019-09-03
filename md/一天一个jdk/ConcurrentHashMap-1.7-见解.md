# ConcurrentHashMap 1.7

网上已经有各种资料详解1.7的ConcurrentHashMap, 这里记录一下自己对该容器的个人见解.

## scanAndLockForPut方法解析

1.7 采用分段锁技术, 降低锁的粒度, 提高并发度. 在实现中, 采用segment来进行分段, 相当于操作segment需要原子地进行. 在put方法的实现中, 会先定位到segment, 然后再调用segment的put方法, 而segment的put方法里面, 并不是一开始调用lock()就完事了, 里面有两个优化点:

1.  调用tryLock()尝试获取锁, 获取锁失败, 则会自旋一定的次数才会阻塞当前线程.
2.  其中, 跟其他自旋不一样的一个地方是, 它并不是空等待一段时间再去tryLock(), 而是查到链表里是否含有与put的key值相等的Node, 如果没有则会进行实例化需要put的Node, 从而节省实例化Node的时间开销

```
当获取到锁之后, 还会去查找key是否存在, 如果存在的话, 则会替换值就可以了, 那么在自旋实例化的Node就浪费了, 这是一个缺点吗?

并不是, hashmap在大部分场景下, key是不会冲突的.
```

## 资料引用

1.  [juc系列-ConcurrentHashMap](https://www.jianshu.com/p/fadc5bc01e23)