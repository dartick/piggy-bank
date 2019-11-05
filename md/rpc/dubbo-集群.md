# dubbo-集群

## [服务目录](https://dubbo.apache.org/zh-cn/docs/source_code_guide/directory.html)

服务目录中存储了一些和服务提供者有关的信息，通过服务目录，服务消费者可获取到服务提供者的信息，比如 ip、端口、服务协议等。通过这些信息，服务消费者就可通过 Netty 等客户端进行远程调用。在一个服务集群中，服务提供者数量并不是一成不变的，如果集群中新增了一台机器，相应地在服务目录中就要新增一条服务提供者记录。或者，如果服务提供者的配置修改了，服务目录中的记录也要做相应的更新。如果这样说，服务目录和注册中心的功能不就雷同了吗？确实如此，这里这么说是为了方便大家理解。实质为Invoker的容器, 对Invoker的管理, 所以有关注册中心的Invoker的新建和销毁逻辑都在此.

### StaticDirectory

StaticDirectory 即静态服务目录，顾名思义，它内部存放的 Invoker 是不会变动的.

### RegistryDirectory

RegistryDirectory 是一种动态服务目录，实现了 NotifyListener 接口。当注册中心服务配置发生变化后，RegistryDirectory 可收到与当前服务相关的变化。收到变更通知后，RegistryDirectory 可根据配置变更信息刷新 Invoker 列表。

#### 列举 Invoker

根据调用的方法的相关信息, 从localMethodInvokerMap中获取可调用的Invoker列表, 获取的方式如下, 如若获取不到继续往下, 否则返回:

1. 通过 方法名 + 第一个参数名称 查询 Invoker 列表，具体的使用场景暂时没想到
2. 通过方法名获取 Invoker 列表
3. 通过星号 \* 获取 Invoker 列表

#### 监听服务变更

实现NotifyListener接口, 当服务发生变更时, 则会以URL列表的方式来进行通知.

```java
public interface NotifyListener {
    void notify(List<URL> urls);
}
```

根据URL的protocol和category来做以下操作:

1. protocol=route &&  category=route. 使用自适应RouterFactory获取Router并存储在 routers 域
2. protocol=override &&  category=configurators. 使用适应ConfiguratorFactory获取Configurator存储在 configurators 域, 并合并成 overrideDirectoryUrl.
3.  category=providers. 进行Invoker列表刷新.


#### 刷新 Invoker 列表

官方总结如下:

1. 检测入参是否仅包含一个 url，且 url 协议头为 empty
2. 若第一步检测结果为 true，表示禁用所有服务，此时销毁所有的 Invoker
3. 若第一步检测结果为 false，此时将入参转为 Invoker 列表 (通过自适应Protocol引用生成, 已引用过的则不会重新引用)
4. 对将上一步逻辑生成的结果进行进一步处理，得到方法名到 Invoker 的映射关系表
5. 合并多组 Invoker (通过cluster将同一group下的多个Invoker合并成一个Invoker)
6. 销毁无用 Invoker

## [服务路由](https://dubbo.apache.org/zh-cn/docs/source_code_guide/router.html)

服务路由包含一条路由规则，路由规则决定了服务消费者的调用目标，即规定了服务消费者可调用哪些服务提供者。Dubbo 目前提供了三种服务路由实现，分别为条件路由 ConditionRouter、脚本路由 ScriptRouter 和标签路由 TagRouter。

### [服务路由的应用场景](https://dubbo.apache.org/zh-cn/docs/user/demos/routing-rule.html): 

1. 排除预发布机
2. 黑白名单
3. 服务寄宿在应用上，只暴露一部分的机器，防止整个集群挂掉
4. 为重要应用提供额外的机器
5. 读写分离
6. 前后台分离
7. 隔离不同机房网段

### 条件路由

条件路由规则由两个条件组成，分别用于对服务消费者和提供者进行匹配。比如有这样一条规则：

```
host = 10.20.153.10 => host = 10.20.153.11
```

该条规则表示 IP 为 10.20.153.10 的服务消费者**只可**调用 IP 为 10.20.153.11 机器上的服务，不可调用其他机器上的服务。条件路由规则的格式如下：

```
[服务消费者匹配条件] => [服务提供者匹配条件]
```

如果服务消费者匹配条件为空，表示不对服务消费者进行限制。如果服务提供者匹配条件为空，表示对某些服务消费者禁用服务。官方文档中对条件路由进行了比较详细的介绍，大家可以参考下，这里就不过多说明了。

### 标签路由

标签路由通过将某一个或多个服务的提供者划分到同一个分组，约束流量只在指定分组中流转，从而实现流量隔离的目的，可以作为蓝绿发布、灰度发布等场景的能力基础。

## [集群容错](https://dubbo.apache.org/zh-cn/docs/source_code_guide/cluster.html)

集群容错机制解决的问题是, 当服务调用失败时, dubbo应该怎么处理。

### Failover Cluster

失败自动切换，当出现失败，重试其它服务器 (重试时, 会动态获取服务列表)。通常用于读操作，但重试会带来更长延迟。可通过 `retries="2"` 来设置重试次数(不含第一次)。

### Failfast Cluster

快速失败，只发起一次调用，失败立即报错。通常用于非幂等性的写操作，比如新增记录。

### Failsafe Cluster

失败安全，出现异常时，直接忽略。通常用于写入审计日志等操作。

### Failback Cluster

失败自动恢复，后台记录失败请求，定时重发。通常用于消息通知操作。

### Forking Cluster

并行调用多个服务器，只要一个成功即返回。通常用于实时性要求较高的读操作，但需要浪费更多服务资源。可通过 `forks="2"` 来设置最大并行数。

### Broadcast Cluster

广播调用所有提供者，逐个调用，任意一台报错则报错。通常用于通知所有提供者更新缓存或日志等本地资源信息。

## [负载均衡](https://dubbo.apache.org/zh-cn/docs/source_code_guide/loadbalance.html)

### RandomLoadBalance

### LeastActiveLoadBalance

### ConsistentHashLoadBalance

用于有状态的服务上

> ​	通过TreeMap来实现环的下一节点的调用

### RoundRobinLoadBalance

普通加权轮询算法:  权重轮询 + invoker轮询

平滑加权轮询算法: 权重升级 + 权重降级

 