# dubbo 序列化

## 领域模型设计

主要分为三个维度进行组织：Serialization(序列化策略)、DataInput(反序列化，二进制->对象)、DataOutput（序列化，对象->二进制流）。上述类图的设计及其优雅，遵循的设计原则大体为：

1. 单一职责，抽象出Input、Output、Serialization，从而抽象出3个维度的类继承体系。
2. 优先使用类聚合，类在多个维度分别衍生出3个继承体系，然后采用聚合，Serialization聚合input,output，典型的桥接模式。

## 面试题

### 为什么默认序列化框架是hession2?

在dubbo RPC中，同时支持多种序列化方式，例如：

1. dubbo序列化：阿里尚未开发成熟的高效java序列化实现，阿里不建议在生产环境使用它
2. hessian2序列化：hessian是一种跨语言的高效二进制序列化方式。但这里实际不是原生的hessian2序列化，而是阿里修改过的hessian lite，它是dubbo RPC默认启用的序列化方式
3. json序列化：目前有两种实现，一种是采用的阿里的fastjson库，另一种是采用dubbo中自己实现的简单json库，但其实现都不是特别成熟，而且json这种文本序列化性能一般不如上面两种二进制序列化。
4. java序列化：主要是采用JDK自带的Java序列化实现，性能很不理想。

在通常情况下，这四种主要序列化方式的性能从上到下依次递减。对于dubbo RPC这种追求高性能的远程调用方式来说，实际上只有1、2两种高效序列化方式比较般配，而第1个dubbo序列化由于还不成熟，所以实际只剩下2可用，所以dubbo RPC默认采用hessian2序列化。

### 其他的序列化框架?

最近几年，各种新的高效序列化方式层出不穷，不断刷新序列化性能的上限，最典型的包括：

- 专门针对Java语言的：Kryo，FST等等
- 跨语言的：Protostuff，ProtoBuf，Thrift，Avro，MsgPack等等

这些序列化方式的性能多数都显著优于hessian2（甚至包括尚未成熟的dubbo序列化）。

## 引用

1. [dubbo多种序列化方式对比](https://dangdangdotcom.github.io/dubbox/serialization.html)