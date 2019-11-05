# dubbo-通讯机制

![/dev-guide/images/dubbo-framework.jpg](https://dubbo.apache.org/docs/zh-cn/dev/sources/images/dubbo-framework.jpg)

dubbo的通讯机制在 Remoting 模块, 而Remoting 实现是 Dubbo 协议的实现，如果你选择 RMI 协议，整个 Remoting 都不会用上，Remoting 内部再划为 Transport 传输层和 Exchange 信息交换层，Transport 层只负责单向消息传输，是对 Mina, Netty, Grizzly 的抽象，它也可以扩展 UDP 传输，而 Exchange 层是在传输层之上封装了 Request-Response 语义。

## 传输层

官方文档中有说, 传输层为通讯框架Mina, Netty, Grizzly等的抽象, 所以在理解该层次的领域模型时, 通过其实现来了解其具体的作用, 此处通过Netty的实现来看.

### Transporter    ->   NettyTransporter

```java
public class NettyTransporter implements Transporter {

    public static final String NAME = "netty";
    /**
     * 实例化Server
     */
    public Server bind(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyServer(url, listener);
    }

    /**
     * 实例化Client
     */
    public Client connect(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyClient(url, listener);
    }

}
```

首先看bind方法, 实例了Server, 那实例化的过程发生了什么呢? 

### Server -> AbstractServer  -> NettyServer

```java
public AbstractServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    //....
        try {
            // 抽象方法, Server实例化做了什么事情主要看这里
            doOpen();
            // ...
        } catch (Throwable t) {
		//..
        }
    // ..
}
```

```java
public class NettyServer extends AbstractServer implements Server {
    // ...省略其他方法
    
    @Override
    protected void doOpen() throws Throwable {
        // 省略其他代码
        bootstrap = new ServerBootstrap(channelFactory);
        // bind
        // ServerBootstrap 为Netty的类, bind 方法主要对端口进行了监听
        channel = bootstrap.bind(getBindAddress());
    }
    
    // ..省略其他方法
}
```

所以得出结论: **Server在实例化中对配置的端口进行了监听**, 而Server则是对ServerBootstrap的一个封装

那么回过来, Transporter实例化了Server并返回, 所以 Transporter#bind 的作用为开启端口监听

接下来看connect方法, 从方法名上几乎能猜到其做了什么事情.

### Client -> AbstractClient -> NettyClient

AbstractClient 在实例化过程中主要调用 doOpen , connect 这两个抽象方法, 从子类的实现便能知道其做了什么事情

```java
public abstract class AbstractClient extends AbstractEndpoint implements Client {
	public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        //..
        try {
            // 抽象方法
            doOpen();
        } catch (Throwable t) {
            // ...
        }
        try {
            // 抽象方法
            connect();
            // ...
        } catch (RemotingException t) {
            // ...
        } catch (Throwable t){
            // ...
        }
        // ...
    }
    
    // ...
}
```

```java
public class NettyClient extends AbstractClient {
    
    // ...
    /**
     * 仅仅实例化ClientBootstrap
     */
	@Override
    protected void doOpen() throws Throwable {
     	// ...
        bootstrap = new ClientBootstrap(channelFactory);
        // ...
    }
    
    //...
}
```

``` java
public class NettyClient extends AbstractClient {
    
    // ...
    /**
     * 调用connect方法进行远程连接
     */
	protected void doConnect() throws Throwable {
        // ...
        // 建立连接
        ChannelFuture future = bootstrap.connect(getConnectAddress());
        // ...
        // 获取连接后的通道
        Channel newChannel = future.getChannel();
    }
    
    //...
}
```

所以Client的实例化为**建立远程连接**, 而 Client 则为对 ClientBootstrap 的封装

那么回过来 Trasporter#connect 真如其名, 建立远程连接并返回 Client

### Channel

客户端与服务端通过Channel来进行通讯, 作用为主动发送消息

Client 与 Channel的关系: 1 : 1

Server 与 Channel的关系: 1 : n

### ChannelHandler

处理Channel发生的操作, 如连接, 连接断开, 接收消息, 异常等等, 通过此, 在接收消息的时候, 可实现不同的线程派发模型

与Channel的关系是: 1: 1

| 策略       | 用途                                                         |
| ---------- | ------------------------------------------------------------ |
| all        | 所有消息都派发到线程池，包括请求，响应，连接事件，断开事件等 |
| direct     | 所有消息都不派发到线程池，全部在 IO 线程上直接执行           |
| message    | 只有**请求**和**响应**消息派发到线程池，其它消息均在 IO 线程上执行 |
| execution  | 只有**请求**消息派发到线程池，不含响应。其它消息均在 IO 线程上执行 |
| connection | 在 IO 线程上，将连接断开事件放入队列，有序逐个执行，其它消息派发到线程池 |



总结一下:

Server: 对通讯框架的服务端API进行了抽象

Client: 对通讯框架的客户端API进行了抽象

Transporter: Server及Client的工厂

Channel: 发送消息, 关闭连接, 主动

ChannelHandler: 处理Channel发生的操作, 如连接, 连接断开,发送消息, 接收消息, 异常等等, 被动

## 信息交换层

Exchange 层是在传输层之上封装了 Request-Response 语义.

> 消息交换模式（Message Exchange Pattern：MEP）在SOA中是一个重要的概念。MEP定义了参与者进行消息交换的模板，这是一个很抽象的定义。实际上我们可以这样理解MEP：消息交换模式（MEP）代表一系列的模板，它们定义了消息的发送者和接收者相互进行消息传输的次序。消息交换模式包括：数据报模式（Datagram）、请求/回复模式（Request/Reply）和双工模式（Duplex）, 也就是 单工, 半双工, 全双工

> Q: 为什么要抽象出此层?
>
> A: 如同方法调用, 需要返回结果. 当使用通讯框架时, 是全双工模式, 无法对请求和结果进行一一对应.

### Exchanger   ->  HeaderExchanger

同样的, Exchager也是一个工厂, 分别返回 HeaderExchangeServer 和 HeaderExchangeClient, 这两个类的构造方法分别需要传入 Server 和 Client, 所以通过这两个类来对传输层进行了封装.

### ExchangeServer

主要把Channel封装成ExchangeChannel

### ExchangeClient

同上

### ExchangeChannel

信息交换层中最重要的类, 主要作用对 Request 和 Response 进行映射, 原理很简单: ID + Future









