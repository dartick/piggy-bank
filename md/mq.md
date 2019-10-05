

多Master多Slave的好处在于，即便集群中某个broker挂了，也可以继续消费，保证了实时性的高可用，但是并不是说某个master挂了，slave就可以升级master，
开源版本的rocketmq是不可以的。也就是说，在这种情况下，slave只能提供读的功能，将失去消息负载的能力。

RocketMQ本身对分布式系统有着很好的支持，它的前身使用ZK做分布式已协调，现在自己实现了一个更加适合自己，轻量的NameServer。启动RocketMQ的时候要先启动NameServer。NameServer 10秒一次检测，两分钟没连接回来就给T了。

开源版本的RocketMQ是没有提供切换程序，来自动恢复故障的，因此在实际开发中，我们一般提供一个监听程序，用于监控Master的状态。这个还不明白是什么意思以及如何实现



RocketMQ提供了三种模式的Producer NormalProducer（普通）、OrderProducer（顺序）、TransactionProducer（事务）


	

发布订阅
优先级消息（不严格）
顺序消息（严格）如果需要保证消息的顺序消费，那么很简单，首先需要做到一组需要有序消费的消息发往同一个broker的同一个队列上！其次消费者端采用有序Listener即可（注册监听器的时候注册这个：MessageListenerOrderly）。
消息过滤，尽量放在客户端
消息持久化，很多种方式
消息可靠性，RocketMQ保证只要机子没炸就不会丢失消息
消息实时
每个消息至少会被投递一次
不保证消息重复消费。分布式环境下系统几乎做不到保证。解决方法是幂等性和本地数据库
消息过多，RocketMQ可以无限接受消息
回溯消费   
消息堆积。这里有个不明白的地方。前面提到RMQ没有内存Buffer的概念，队列中的消息都是持久化磁盘中的。这样的话谈消息堆积就没有意义了，讨论消息堆积能力的指标也没有意义了。但是PDF中确实讨论了这方面的。
定时消息
消息重试，消息消费失败也是要分情况处理的。例如：be充话费的手机号不存在，这种消息再消费也是失败，要跳过（也就是说告诉Broker这边已经消费成功了）。如果是下游服务比如DB挂了，这个时候最好sleep一定的时间而不是反复重试。



producer与Master建立长连接
consumer可以订阅Master也可以订阅Slave，采用轮询pull方式。


RMQ使用ext4


RMQ的底层太复杂了，完全弄懂需要很深的操作系统，计算机组成原理功底。
  
单队列也支持并行消费，采用滑动窗口机制，不知道这个需不需要开启

消费失败定时重试，这个肯定需要设置时间的。具体设置方式参考上面提到的消息重试。

Slave异步复制机制：Slave启动一个线程，不断从Master拉起Commit Log，然后根据这个Log生成Consume Queue数据结构。类似MySQL主从同步。

消息过滤
	简单消息过滤：消息有个Tag（本质是个字符串，message.setTags("TagA")），服务端对比的是hashcode，如果匹配就发送给Consumer否则跳过这条消息，因为可能hash冲突，所以客户端还会再对比一次，这次对比的是字符串。
	高级消息过滤：Broker会启动多个FilterServer进程。Consumer启动之后会上传一个Java类。这样可以用CPU换取网卡流量，网络中传的都是该传的。但是这个过滤类一定不要乱写，别把服务器搞坏了。

消息发送失败：
	内部本身有一个重试机制。Message有个sendMsgTimeout() 默认10S。内部的消息重试机制最多重试三次，且不会超过Timeout。所以这机制并不能保证一定重试成功。一般的解决方案是将失败的消息保持到DB，然后开个后台线程重试




	服务调用时候的负载均衡可以让客户端来完成：