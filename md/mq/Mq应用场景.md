# Mq应用场景

## 应用场景

### 异步解耦

场景: 上游系统A产生了一份数据, 通过接口调用的方式来通知下游系统B, 而当其他的下游系统CDEFG都需要这份数据的时候, 则会导致调用多次接口

![img](https://mmbiz.qpic.cn/mmbiz_png/1J6IbIcPCLbyIUHR4kKkucnHS8KDLgG3cIOhjvEN2MNPTpKF3IwBnGIULUZaNzBrqnLfzicRsw6sE7T419tl2HQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

### 异步调用

场景: 在调用链中, A ->(20ms) B -> (200ms) C -> (2000ms) D, 整个调用的总时长为约为2s, 但实际上, C调用D不一定需要D的实时返回结果, 此时可以通过mq来异步处理:

![img](https://mmbiz.qpic.cn/mmbiz_png/1J6IbIcPCLbyIUHR4kKkucnHS8KDLgG3IHtRribp1Y3smib1Rv0AIXD6epXZeE0EalXulRCGicQp7PpmHHP4OdgUQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1) 

### 流量削峰

场景: 为了抗住高流量, 需要部署多台机器, 但是高流量不是时长有的, 为了瞬时的高峰而部署多台, 导致资源浪费, 此时选择Mq可以进行削峰:

![img](https://mmbiz.qpic.cn/mmbiz_png/1J6IbIcPCLbyIUHR4kKkucnHS8KDLgG32A0RKjSjjYicNmYZvgK2Mgx3ECQcLfSGkdNexBGdX6te9kxmPGyZRAw/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

​																							(高峰期)

![img](https://mmbiz.qpic.cn/mmbiz_png/1J6IbIcPCLbyIUHR4kKkucnHS8KDLgG3sq7JfUcQ4YESavybFKGUTr0m1WMDYoYqLWcxMoxSLxGQ6I1luqEvWg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

​																				 (平时, 浪费)

![img](https://mmbiz.qpic.cn/mmbiz_png/1J6IbIcPCLbyIUHR4kKkucnHS8KDLgG3AeO4LZLibGE15jyz5USY8EvXfBqje8g4qsNTFxGmOSlMu2ZNSqrsUFA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

​																	(引入Mq, 只需要一台便可)



## 引用

1. [你们的系统架构中为什么要引入消息中间件？](https://mp.weixin.qq.com/s?__biz=MzU0OTk3ODQ3Ng==&mid=2247484149&idx=1&sn=98186297335e13ec7222b3fd43cfae5a&chksm=fba6eaf6ccd163e0c2c3086daa725de224a97814d31e7b3f62dd3ec763b4abbb0689cc7565b0&scene=21#wechat_redirect)
2. 