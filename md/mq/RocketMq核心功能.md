# RocketMq核心功能

## Broker

### Topic管理

核心功能于 TopicConfigManager 实现 :

1. 增删改查 ([Topic创建机制](https://objcoding.com/2019/03/31/rocketmq-topic/))
2. 持久化

定时任务:

1. 向 NameSrv 上报 Broker 的状态, 包括 Topic 信息

### 消息接收

1. 调用方式处理(OneWay, Sync, Async)
2. 消息合法性校验, 比如消息的Topic长度和内容长度是否超出限制

### 消息存储

核心功能于 MessageStore, CommitLog 实现 :

1. 写入 CommitLog 文件组, 顺序写
2. 根据Topic 写入 ConsumeQueue 文件组, 顺序写
3. mmap文件内存映射, 使用 MappedByteBuffer 实现
4. 刷盘方式 (同步刷盘, 异步刷盘)

定时任务:

1. 异步刷盘任务

### 消息投递

### 订阅组管理

### 集群





