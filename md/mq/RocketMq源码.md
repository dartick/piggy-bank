# RocketMq源码

## Producer

### DefaultMQProducer

该类封装了 DefaultMQProducerImpl (实际的生产客户端), ProducerGroup (生产者组概念) , 及一些默认的参数

### DefaultMQProducerImpl

#### 启动方法 start()

主要做了如下事情:

1. 实例化 Client  

2. 启动 Client (MQClientInstance#start()方法, 使用synchronize并发处理) : 

   a.	启动定时任务:

   * 未配置Namesrv地址, 则定时从 WebService 获取
   * 从NameSrv更新Topic路由信息
   * 清理已下线 broker, 发送心跳包到broker
   * 持久化所有 consumer offset (分本地文件存储 和 broker存储)
   * 调整consumer线程池 (负载均衡优化的其中一种)

   b.   启动拉取消息后台线程

   c.   启动负责均衡后台线程

   d.   启动推送服务, 即 DefaultMQProducerImpl#start() (此处递归调用, 但是通过 startFactory 参数来防止重复启动 Client)

> 实例化 Client  (从 MQClientManager 获取, 默认情况下同一个JVM返回同一个 Client, 返回不同的 Client 通过 **ClientID=localIP + instanceName + unitName** 来配置 )

#### 定时任务

##### 从NameSrv更新Topic路由信息

```java
public class TopicRouteData extends RemotingSerializable {
    private String orderTopicConf;
    private List<QueueData> queueDatas;
    private List<BrokerData> brokerDatas;
    private HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable;
}
```

```java
public class QueueData implements Comparable<QueueData> {
    private String brokerName;
    private int readQueueNums;
    private int writeQueueNums;
    private int perm;
    private int topicSynFlag;
}
```

``` java
public class BrokerData implements Comparable<BrokerData> {
    private String cluster;
    private String brokerName;
   rivate HashMap<Long/* brokerId */, String/* broker address */> brokerAddrs;
}
```

从上可以知道 Topic路由信息 主要包含了两块信息:

1. broker列表: 剔除已下线broker
2. queue列表:

##### 清理已下线 broker, 发送心跳包到broker

心跳包的数据如下:

```java
public class HeartbeatData extends RemotingSerializable {
    private String clientID;
    private Set<ProducerData> producerDataSet = new HashSet<ProducerData>();
    private Set<ConsumerData> consumerDataSet = new HashSet<ConsumerData>();
}
```

```java

public class ProducerData {
    private String groupName;
}
```

```java
public class ConsumerData {
    private String groupName;
    // 消费类型: pull push
    private ConsumeType consumeType;
    // 消息模式: 集群 广播
    private MessageModel messageModel;
    // 消费地址
    private ConsumeFromWhere consumeFromWhere;
    // 订阅信息: topic tags expressionType
    private Set<SubscriptionData> subscriptionDataSet = new HashSet<SubscriptionData>();
    private boolean unitMode;
}
```

**只有producer的话, 只会对master broker发送心跳包**

##### 持久化所有 consumer offset

``` java
public void persistConsumerOffset() {
        try {
            this.makeSureStateOK();
            Set<MessageQueue> mqs = new HashSet<MessageQueue>();
            // 持久化负责均衡过的消息队列offset
            Set<MessageQueue> allocateMq = this.rebalanceImpl.getProcessQueueTable().keySet();
            mqs.addAll(allocateMq);
            this.offsetStore.persistAll(mqs);
        } catch (Exception e) {
            log.error("group: " + this.defaultMQPullConsumer.getConsumerGroup() + " persistConsumerOffset exception", e);
        }
    }
```

##### 调整consumer线程池

动态地调整 consumer 线程池, 代码注释掉了, 看来废弃掉了

#### 后台线程



#### 发送消息 send()

1. 选择 MessageQueue (负责均衡, 策略为 轮询)

2. 计算超时时间

3. 内部发送:

   a.	找到 broker 地址 (本地缓存没有, 会从 NameSrv 拉取)

   b.    设置Msg唯一ID

   c.	实例化 SendMessageContext (存储此次发送的相关信息)

   d.    实例化 SendMessageRequestHeader (发送额外的信息到 broker)

   e.	根据不同的发送方式来调用 MQClientAPIImpl 不同的方法 (异步的方式通过 : ID + Future)

在此过程中, 每次调用都会记录相关的时间到 FaultItem, 用于负责均衡

## Broker

### NettyRemotingServer

基于Netty来实现服务端, 通过 NettyServerHandler 来接收 Client 发送过来的消息, 再通过 NettyRequestProcessor 来处理 Client 发过来的 请求, 其中 SendMessageProcessor 处理 Producer 发过来的消息

### SendMessageProcessor

主要对请求头的解析以及相关权限的处理, 然后调用 DefaultMessageStore 进行消息存储

### DefaultMessageStore

该类主要对消息进行合法性检查, 然后调用 CommitLog 进行消息写入

## NameSrv



## Producer



## 引用

1. [[汪先生：RocketMQ源码分析之服务发现](https://zhuanlan.zhihu.com/p/57703136)](https://zhuanlan.zhihu.com/p/57703136)
2. [汪先生：RocketMQ源码分析之消息发送](https://zhuanlan.zhihu.com/p/58026650)
3. [汪先生：RocketMQ源码分析之消息存储](https://zhuanlan.zhihu.com/p/58728454)
4. [汪先生：RocketMQ源码分析之消息刷盘](https://zhuanlan.zhihu.com/p/58755005)
5. [汪先生：RocketMQ源码分析之ConsumeQueue](https://zhuanlan.zhihu.com/p/59516998)