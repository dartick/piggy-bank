# FutureTask

## 应用场景

异步执行任务且依赖执行结果, 则可以使用FutureTask

## 原理

### 任务执行

只允许一个线程来执行任务

```
public void run() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        ...
    }
```

#### 执行未发生异常

正常执行完成则设置结果, 保存在**outcome**字段, 同时唤醒等待结果的线程:

```
 protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            finishCompletion();
        }
    }
```

#### 执行发生异常

执行发生异常则 **outcomt** 保存为异常, 同时唤醒等待结果的线程:

```
protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }
```

以上属于任务完成的两种情况, 区别就在于任务状态不同, 获取结果的线程可根据状态得知相应的结果.

### 结果获取

#### 未执行完

未执行完可以选择等待和超时等待

```
 public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        return report(s);
    }
```

```
public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        return report(s);
    }
```

#### 执行完

1.  正常执行完成. 返回执行结果
2.  任务被取消. 抛出任务被取消异常
3.  任务发生异常. 抛出执行异常

```
private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL)
            return (V)x;
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable)x);
    }
```

## 缺点

Future虽然可以实现获取异步执行结果的需求，但是它没有提供通知的机制，我们无法得知Future什么时候完成。要么使用阻塞，在future.get()的地方等待future返回的结果，这时又变成同步操作。要么使用isDone()轮询地判断Future是否完成，这样会耗费CPU的资源。

CompletableFuture是Java 8新增的类, 其扩展了Future, 吸收了所有Google Guava中ListenableFuture和SettableFuture的特征，还提供了其它强大的功能，让Java拥有了完整的非阻塞编程模型：Future、Promise 和 Callback(在Java8之前，只有无Callback 的Future)。

使用例子参考:

1.  [20个使用 Java CompletableFuture的例子](https://colobu.com/2018/03/12/20-Examples-of-Using-Java%E2%80%99s-CompletableFuture/)