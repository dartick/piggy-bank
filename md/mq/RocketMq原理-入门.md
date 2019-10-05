# RocketMq原理-入门

##	RocketMq的简介

1. 一代推push模式, 二代pull模式, 三代pull模式为主, 兼有push模式
2. 应用场景
   * 削峰填谷
   * 削峰填谷
   * 异步解耦
   * 事务消息
3. 缺点

## 消息中间件

消息中间件需要具备的功能点, 是否支持, 支持的程度, 

消息中间件需要解决的问题, 如何解决

对RocketMq深入学习, 同时对比其他产品

1. RocketMq
2. Kafka
3. RabbitMq
4. ActiveMq
5. ZeroMq

### 对比总览

![img_5a295d3e7094050428c129136e33fafa.png](https://yqfile.alicdn.com/img_5a295d3e7094050428c129136e33fafa.png)

### 集群方式

1. 是否支持集群部署
2. 支持的部署方式,  主从, 多主, 一主多从, 多主多从
3. 集群管理机制, 选主方式, 主从切换
4. 如何保证一致性, 双写, 异步复制
5. 横向扩展能力
6. 怎么设置消息是从Master消费还是从Slaver消费。
7. Master和Slaver同时在线，消息是否会从Master消费一遍，然后再从Slaver消费一遍？

### 消息吞吐量

1. 发送端是否支持批量发送, 消费端是否支持批量消费

### 消息投递实时性

从消息开始发送到发送到消费者的延迟

### 消息重复

1. at least once
2. at most once
3. exactly only once

### 消息丢失

保证消息发生成功

### 顺序消息

1. 普通顺序消息
2. 严格顺序消息

### 消息优先级

### 事务消息

### 定时消息

### 负责均衡

1. 发送
2. 消费

### 消息过滤

### 消息回溯

### 消息重试

消费失败后的重试机制

### 消息堆积能力

1. 内存堆积, 持久化
2. 如何评估消息堆积的能力
3. 队列满了丢弃策略
4. 消息清理
5. 队列多或堆积多情况的稳定性, 支持的队列数量
6. RocketMq持久化消息的原理, 刷盘策略

## RocketMq专业术语

1. 核心节点
   - producer
   - nameserver
   - broker
   - consumer(pull, push)
2. 消费模式
   - group(producer, consumer)
   - 广播消费
   - 集群消费
3. 消息领域模型
   - topic (tag, keys)
   - queue
   - message
   - offet

## RocketMq细节原理

1. 消息发送和消息消费的整体流程
2. Topic路由注册与剔除原理
3. nameser的实现
4. 通讯原理

## RocketMq最佳实践

1. 消息隔离
2. 消息的幂等性解决思路 ;重复消费问题
3. 如何处理消息丢失的问题
4. 分布式事务

### Producer

1. 消息结构的设计, 如topic怎么定义, tag, keys怎么用
2. 发送失败处理
3. oneway运用场景
4. 发送顺序消息注意事项

### Consumer

1. 消费失败处理
2. 消费慢的处理(消息堆积)
3. 消费打印日志

