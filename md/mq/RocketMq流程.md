# RocketMq流程

## 总体流程图

![1574318084491](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1574318084491.png)

## Topic创建流程

### Topic

Topic可以理解为在rocketMq体系当中作为一个逻辑消息组织形式，一般情况下一类业务消息会申请一个topic来实现业务之间隔离

name:	topic的唯一标识

writeQueueNums: 写队列数, 默认是 8

readQueueNums: 读队列数, 默认是 8

perm: 控制该topic消息的读写权限, 2是写, 4是读, 6是读写

>  a.假设写队列有8个、读队列有4个，那么producer产生的消息会按轮训的方式写入到8个队列中，但是consumer却只能消费前4个队列，只有把读队列重新设置为8后，consumer可以继续消费后4个队列的历史消息；
>
>   b.假设写队列有4个、读队列有8个，那么producer产生的消息会按轮训的方式写入到4个队列中，但是consumer却能消费8个队列，只是后4个队列没有消息可以消费罢了。

### 手动创建流程

![img](https://upload-images.jianshu.io/upload_images/6302559-cbc5e945be638187.png?imageMogr2/auto-orient/strip|imageView2/2/w/697/format/webp)

手动创建需要通过mqadmin提供的topic相关命令进行创建，执行：

```
./mqadmin updateTopic
```

当用集群模式去创建topic时，集群里面每个broker的queue的数量相同，当用单个broker模式去创建topic时，每个broker的queue数量可以不一致。broker, topic, queue关系如下图所示:

![img](https://upload-images.jianshu.io/upload_images/6302559-5693e4bec15216b5.png?imageMogr2/auto-orient/strip|imageView2/2/w/837/format/webp)

## 发送消息流程

![img](https://user-gold-cdn.xitu.io/2019/6/9/16b3b96b0792f595?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

### 调用方式

OneWay: producer只保证发送消息到broker, 不理会返回结果, 常用于非重要消息的发送.

同步: 同步发送并等待broker返回结果, 内部原理为netty的异步转同步: ID + future 

异步: 发送不会阻塞, 可传入 callback 异步处理结果

### 超时机制

从 send 方法调用到broker返回结果的时间, 如果超过设定值(默认是 3s)则会抛出超时异常

### 重试机制

当使用同步的调用方式情况下, 才会有重试机制, 当调用过程中发生异常(包括超时), 则会重试, 默认是 3 次 (包括第一次).

### 负责均衡

当发送某topic消息时, 支持该topic的broker有多个, 该topic下的队列也有多个, 那么就需要路由的过程, 这里叫负载均衡, 即挑其中一个broker, 一个队列来发送. 默认会轮询所有的message queue发送，以达到让消息平均落在不同的queue上.

![çäº§èè´è½½åè¡¡](http://jaskey.github.io/images/rocketmq/producer-loadbalance.png)

> 当往 broker1 的 queue0 发送消息发生异常时, 那么重试时, 负责均衡策略则不会选择上次发送的 broker 来进行发送, 即 broker1, 所以当消息第N次重试时，就要避开第N-1次时消息发往的 broker.

### 重复发送

RocketMq 并不会处理 produer 端消息的重复发送, 而且由于网络原因导致的重复发送是无法解决的. 当然RocketMq可以降低重复发送的概率, 但是这样做必然会带来性能上的损耗, 而且 producer 端重复发送的概率低下, 没必要为低概率事件而耗费大部分系统性能. 

那当重复发送发生时怎么解决呢? 官方强制要求(并不是建议, 而是要求): 做好消息幂等 !

### 事务消息

传送门: https://juejin.im/post/5b5e7e1c51882561b75a6791

![image-20190726114320498](https://user-gold-cdn.xitu.io/2019/7/27/16c321f9e0ddb952?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

### 自动创建Topic流程

当 broker 允许自动创建 topic 时, 才会自动创建 topic , 其自动创建 topic 的流程如下: 

![img](https://raw.githubusercontent.com/objcoding/objcoding.github.io/master/images/rocketmq_7.png)

#### TWB102 topic的作用

原理很简单, 无非是broker需要标识一下自己可以支持自动创建topic, 那么标识的方式便是通过这个特殊的 **TWB102** topic, 当producer发现自己的topic没有任何broker支持, 则往支持 **TWB102** topic 的broker 发消息即可.

#### 自动创建的缺点

自动创建会破坏负责均衡, 假设场景如下:

broker1, broker2 支持 自动创建 topic

producer1, producer2, .... 等多个 producer 都会发送 broker 不支持的 topic1

producer1 首先发送topic1消息, 由于自动创建机制, 会发送到 broker1, 同时 broker1 会创建 topic1 的信息并上报给 namesrv

当剩余的 producer 发送topic1消息时, 此时 broker1 已经支持 topic1了, 则会导致 所有消息都会发往 broker1, 导致 broker1 负载过重.

所以基于以上场景, 一般线上环境都会禁止自动创建 topic.

## 消息存储流程

### 文件形式

broker 核心的存储相关的领域模型如下:

![](https://pic1.zhimg.com/80/v2-d0d53b05c6f84c250ea4e0afd6cebadc_hd.jpg)

1. MessageStore:  权限校验, 消息校验
2. CommitLog: 消息写入, 异步定时刷盘, 同步刷盘
3. MappedFileQueue: 文件组管理
4. MappedFile: 文件管理, 对应物理上的文件

从图中可知, RocketMq 以文件组的方式来储存文件, 每个文件的名字为 20 位数字组织，以该文件第一条消息的偏移量为文件名，长度不足 20 的在前面补 0。文件默认大小为 1G，可根据 mappedFileSizeCommitLog 属性改变文件大小:

1. 写入消息: 写入最后一个文件, 当文件容量不足时, 则会新建文件, 写入新的文件, 上一个文件则会留出一部分空白.
2. 读取消息: 根据消息偏移量offset来定位到所在文件, 然后再从该文件获取消息

![clipboard.png](https://segmentfault.com/img/bVbpQn6?w=838&h=79)

除了CommitLog以文件组的方式存储, ConsumeQueue也一样. ConsumeQueue是什么呢? 

RocketMQ 基于Topic的订阅模式实现消息消费，由于同一Topic的消息不连续的存储在 CommitLog 文件中，遍历 CommitLog 文件会导致效率非常低下，为了适应消息消费的检索需求，设计了消息消费队列文件。一个 ConsumeQueue 文件可以作为检索指定 topic 的消息索引。其数量与Topic的writeQueueNums对应, ConsumeQueue 文件存储消息的逻辑偏移量，而不存储消息的全部内容，存储格式如下：

![clipboard.png](https://segmentfault.com/img/bVbpQok?w=440&h=53)

### 流程

大体的消息存储流程:

1. 消息检验
2. 写入消息到CommitLog
3. 写入对应Topic的ConsumerQueue

![img](https://user-gold-cdn.xitu.io/2019/6/9/16b3c679bf8c166c?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

### 刷盘方式

刷盘的方式有两种:

1. 同步刷盘. 同步的意思就是说当消息追加到内存后，就立即刷到文件中存储。
2. 异步刷盘: 当消息追加到内存中，并不是理解刷到文件中，而是在后台任务中进行异步操作。默认采用异步

![img](https://user-gold-cdn.xitu.io/2019/6/9/16b3ceaf941f02a5?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

### Mmap

传送门: https://www.jianshu.com/p/719fc4758813

## 消息消费流程

![consumeræ¶è´¹æµç¨å¾](http://zsfblues.oss-cn-hangzhou.aliyuncs.com/blog/006HiYd9ly1g0goksd761j31ly0wedwi.jpg)

### Push or Pull

RocketMq提供了两种客户端来进行消息消费:

1. DefaultMQPullConsumer. Pull模式, 只支持单次消息的主动拉取, 要实现后台自动拉取的功能得由用户来实现, 所以该客户端较为灵活, 有特定需求可基于此客户端进行开发.
2. DefaultMQPushConsumer. Push模式, 但是非真正的Push, 而是通过长轮询的方式来达到push的实时性.

> 普通轮询: 客户端后台定时不断地请求服务端
>
> 长轮询: 客户端发送拉取请求到服务端, 服务端判断若有消息, 则返回, 否则会将该请求挂起, 当消息来了后, 才响应该请求

### 心跳

consumer在启动时会向所有 broker 注册订阅信息，并启动心跳机制，定时更新订阅信息

broker端接收到心跳信息后会以 consumer group 来进行维护

> 当 group 中出现订阅不同 topic 时, 则发生 topic 不存在的异常
>
> 原因就在于 broker 以 consumer group 来进行维护的订阅信息的

### 消息拉取

#### consumer 拉取消息

consumer以生产者-消费者模型来处理拉取请求: 

1. 接收到pullRequest
2. **流控处理** (未消费消息数达到1000个或消息存储容量100MB, 顺序消费则消息数阈值为 2000)
3. 获取topoic相关的信息
4. 构建callback, 在callbakc中处理拉取到的消息
5. 获取commitOffsetValue (在拉取消息的同时提交消费进度)
6. 发送RPC请求

#### broker响应消息拉取

同样的, broker也以生产者-消费者模型来处理拉取请求:

1. 根据ConsumerGroup、Topic、queueId定位到ConsumeQueue
2. 根据offset从ConsumeQueue获取到消息的offset
3. 再从CommitLog获取到消息, 并返回给Consumer
4. 持久化消费进度(commitOffsetValue)

#### broker挂起消息拉取请求

当没有新消息时, broker则会挂起该请求, 不会响应结果给consumer, 那这个请求什么时候会处理呢?有两种时机:

1. 后台轮询. 后台线程不断轮询**待拉取消息的偏移量是否小于消费队列最大偏移量**, 条件达成则会处理请求.
2. 消费队列接收到消息. 这时会主动触发pull请求

### 负载均衡

上面讲到consumer以生产者-消费者来处理拉取请求, 那么谁是生产者呢? 负责均衡.

这里的负责均衡是**指queue与consumer之间的负载均衡, 即决定consumer消费哪几个queue**.

在consumer启动时, 或者queue数量变更时(修改queue数, broker增加或减少), 则会触发负载均衡.

负载均衡首先和**消费模式**有关:

* **广播消费**. consumer分配到所有的queue

* **集群消费**.  默认consumer均分所有的queue, 那怎么做到呢? 在发送**心跳**时, broker会返回该topic的所有queue和订阅该topic的consumer, 基于这些就可以实现均分

当负责均衡导致queue重新分配时, 则需要产生pullRequest, 来通知broker, 我是你的消费者, 有消息就跟我说一下哦.

### 消费方式

从 broker 拉取的**最大消息数量为 32**, 拉取到消息后,  如何处理呢? 通过拉取时构建的callback来处理, 在callback中, 对消息进行分批处理(默认是批次数为 1), 即 MessageListener 传入的 **List\<MessageExt\> msgs**,  然后根据注册不同的消费方式来处理:

#### 并发消费

将拉取到消息投递到线程池进行消费, 默认核心线程数为 20, 默认最大线程数为 64, LinkedBlockingQueue 阻塞队列

消息消费有以下几种情况:

* 正常返回CONSUME_SUCCESS:  保存**消费进度(即队列的offset)**到本地的offsetTable
* 正常返回RECONSUME_LATER:  需要根据情况不同消费模式来处理
  * 集群模式: 重发回 broker 的 **延迟队列**, 延迟一段时间再消费, 同时保存消费进度
  * 广播模式: 保存消费进度

* 发生异常: 同上
* 返回null值: 同上
* 超时: 同上

> 从中可以知道, 广播模式和集群模式在消费失败的情况下, 都会认为消费成功, 但是区别在于, 广播模式的消息不再重新消费, 而集群模式会延迟消费
>
> 还有一点需要注意的是, **集群模式消费发回消息失败的话, 后面的消息依然会被消费, 但是消费进度则会卡住**, 所以当去拉取消息时, 则会以卡住的进度来拉取, 导致消费过的消息重新拉取并**重复消费**

#### 顺序消费

同样的, 顺序消费也是投递到线程池来消费, 但是, 在线程里以队列锁来保证队列上消息的顺序消费

顺序消费有以下几种情况:

* 正常返回 SUCCESS: 保存消费进度
* 正常返回 SUSPEND_CURRENT_QUEUE_A_MOMENT:  重试, 但是后面的消息不会被消费, 从而保证顺序消费, 重试达到阈值(默认是 16次) 则会发给 broker 的**死信队列**, 此时后面的消息则会允许消费
* 发生异常: 同上
* 返回null: 同上
* 超时: 同上

> 顺序消费失败, 消费会重试, 进度则会卡住, 但为了保证后续的消息消费, 重试到达一定次数后也会认为其被消费成功

### 消费进度

消费进度以队列的维度维护在offsetTable, 然后每 5 s (默认) 进行上报给broker, 除此之外, 拉取消息的时候也会上报消费进度.

### 其他队列

除了 ConsumeQuque, 还有一些其他的队列, 来实现RocketMq的一些特性

#### 延迟队列

RocketMq不支持任意时间维度的延迟消息, 原因在于, 支持延迟消息必须实现优先级队列, 但是RocketMq的消息是持久化到硬盘的, 优先级排序必然会导致大量的IO操作, 影响性能.

任意时间维度不支持, 但是特定时间维度是支持的, 比如延迟级别为: 1s, 2s, 5s, ....

原理便是通过各个延迟级别都有相应的延迟队列, 文件名为n (表示延迟级别, 1s 则为 1), 然后通过定时器, 定时n秒来处理队列上的消息, 从而到达延迟消息的特性

投递到延迟队列上消息**首先会修改其 topic 为 SCHEDULE_TOPIC_XXXX, 原 Topic 和 队列id 则会存储在消息的properties中**

使用延迟队列有两种时机:

##### 延迟消息

producer发送延迟消息, 延迟的时间到达后, 则会投递到真正的队列上

##### 消息重试

消息消费失败, 进行延迟重试, 延迟时间到达后, 则投递到**重试队列**以供消费者重试, 延迟时间随着重试次数的增加而增加, 而当重试次数达到上线, 则会投递到**死信队列**

![æµç¨å¾.png](http://tech.dianwoda.com/content/images/2018/02/rocketmq------broker-4.png)

> 类似的, 延迟消息流程图也差不多, 会在CommitLog储存两次消息

#### 重试队列

如果Consumer端因为各种类型异常导致本次消费失败，为防止该消息丢失而需要将其重新回发给Broker端保存，保存这种因为异常无法正常消费而回发给MQ的消息队列称之为重试队列。RocketMQ会为每个消费组都设置一个Topic名称为**“%RETRY%+consumerGroup”的重试队列**. consumer默认订阅了该 Topic. 

#### 死信队列

由于有些原因导致Consumer端长时间的无法正常消费从Broker端Pull过来的业务消息，为了确保消息不会被无故的丢弃，那么超过配置的“最大重试消费次数”后就会移入到这个死信队列中, 移入至死信队列的消息，需要人工干预处理.

#### 未提交半消息队列

半消息, 即事务消息第一个阶段提交的prepare消息, 会存储于该队列, 对consumer不可见

#### 已提交或已回滚半消息队列

在半消息被commit或者rollback处理后，会存储到Topic为RMQ_SYS_TRANS_OP_HALF_TOPIC的队列中，标识半消息已被处理

## 引用

1. [图解RocketMQ消息发送和存储流程](https://juejin.im/post/5cfcd223f265da1bc23f6b06)
2. [源码分析RocketMQ之消息ACK机制](https://blog.csdn.net/prestigeding/article/details/79090848)
3. [RocketMQ为什么要保证订阅关系的一致性？](http://objcoding.com/2019/07/27/rocketmq-consumer-subscription/)
4. [consumer 5.push消费-顺序消费消息](https://www.iteye.com/blog/m635674608-2378794)
5. [RocketMQ消费失败消息深入分析](http://tech.dianwoda.com/2018/02/09/rocketmq-reconsume/)
6. [消息重试](http://zsfblues.github.io/2019/02/17/RocketMQ%E5%AD%A6%E4%B9%A0-%E5%9B%9B-consumer/#%E5%9B%9B-%E6%B6%88%E6%81%AF%E9%87%8D%E8%AF%95)
7. [RocketMQ高性能的原因](https://www.imooc.com/article/252270)
8. [RocketMq源码阅读系列-简书](https://www.jianshu.com/p/8a0e1d2da75f)
9. [RocketMq源码分析系列-知乎](https://zhuanlan.zhihu.com/p/57703136)
10. [RocketMQ高性能之底层存储设计](https://juejin.im/entry/5c014ea1f265da6157053888)
11. [RocketMq为什么不用ZK](https://www.iteye.com/blog/manzhizhen-2317354)