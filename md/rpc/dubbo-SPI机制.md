# [SPI机制](http://dubbo.apache.org/zh-cn/docs/source_code_guide/dubbo-spi.html)

## 与JavaSPI机制异同

### 同

 扫描特定路径下的以**接口类的全限定符**命名的文件, 读取其中的实现类并加载

### 异

#### 1.	延迟初始化

Java: 一次性实例化扩展点所有实现

Dubbo: 延迟初始化

#### 2.	扫描路径

Java: META-INF/services

Dubbo: META-INF/dubbo, META-INF/dubbo/internal/, META-INF/services

#### 3.	文件内容格式

Java: 实现类的全限定符

```properties
org.apache.spi.OptimusPrime
org.apache.spi.Bumblebee
```

Dubbo: 实现类的name + 实现类的全限定符

```properties
optimusPrime = org.apache.spi.OptimusPrime
bumblebee = org.apache.spi.Bumblebee
```

> Dubbo对Java的配置格式做了兼容, 即支持只有 实现类的全限定符, 此时name的值, 若实现类添加了**Extension**注解, 则取注解value作为name值, 否则取实现类的simpleName.

> 扩展实现支持多个name, 以逗号隔开

## 特性

### DI

只支持setter的方式进行注入

通过反射获取setter, 再通过**ExtensionFactory**获取实例. 注入的实例来源有两种:

1. 需要注入的类类型为扩展点, 则注入该扩展点的**自适应扩展类**
2. 注入的实例为SpringBean, 则通过以类型的驼峰名称为BeanName从ApplicationContext中获取该Bean

### AOP

构造方法参数为扩展类接口则为代理类Wrapper, Wrapper的实现例子:

```java
package com.alibaba.xxx;
 
import org.apache.dubbo.rpc.Protocol;
 
public class XxxProtocolWrapper implements Protocol {
    Protocol impl;
 
    public XxxProtocolWrapper(Protocol protocol) { impl = protocol; }
 
    // 接口方法做一个操作后，再调用extension的方法
    public void refer() {
        //... 一些操作
        impl.refer();
        // ... 一些操作
    }
 
    // ...
}
```

通过实例化扩展实现类的时候, 以Wrapper包装实现类实例的方式实现:

```java
Set<Class<?>> wrapperClasses = cachedWrapperClasses;
if (wrapperClasses != null && wrapperClasses.size() > 0) {
    for (Class<?> wrapperClass : wrapperClasses) {
         instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
    }
}
```



### [自适应机制](http://dubbo.apache.org/zh-cn/docs/source_code_guide/adaptive-extension.html)

在 Dubbo 中，很多拓展都是通过 SPI 机制进行加载的，比如 Protocol、Cluster、LoadBalance 等。有时，有些拓展并不想在框架启动阶段被加载，而是希望在拓展方法被调用时，根据运行时参数进行加载。这听起来有些矛盾。拓展未被加载，那么拓展方法就无法被调用（静态方法除外）。拓展方法未被调用，拓展就无法被加载。对于这个矛盾的问题，Dubbo 通过自适应拓展机制很好的解决了。自适应拓展机制的实现逻辑比较复杂，首先 Dubbo 会为拓展接口生成具有代理功能的代码。然后通过 javassist 或 jdk 编译这段代码，得到 Class 类。

#### 1.	检查Adaptive注解

对于要生成自适应拓展的接口，Dubbo 要求该接口至少有一个方法被 Adaptive 注解修饰。若不满足此条件，就会抛出运行时异常。

#### 2.	生成类

生成的类示例如下: 

```java
public class Protocol$Adaptive implements com.alibaba.dubbo.rpc.Protocol {
    // 省略方法代码
}
```

#### 3.	生成方法

对于未被 Adaptive 注解修饰, 则生成的方法会抛出*UnsupportedOperationException*异常

对于被 Adaptive 注解修饰的, 则通过以下步骤生成方法:

1. 获取 URL 数据. 通过参数列表来获取URL对象, 从而获取到要执行的扩展的name. 从参数列表获取URL的方式有两种, 一是参数为URL类型, 二是通过反射判断参数类型是否含有URL的getter方法. 以Protocol为例子, 最终生成的方法为:

```
refer:
if (arg1 == null) 
    throw new IllegalArgumentException("url == null");
com.alibaba.dubbo.common.URL url = arg1;

export:
if (arg0 == null) 
    throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
if (arg0.getUrl() == null) 
    throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");
com.alibaba.dubbo.common.URL url = arg0.getUrl();
```

2. 从URL上获取扩展点name. 通过Adaptive的注解值来获取, 其注解值类型为value[]. 没设置值则使用扩展点接口名的点分隔(LoadBalance  => load.balance ). 有三种情况, 例子如下: 

   a. value[i]为protocol

   Protocol#export:

   ```java
   @Adaptive
   <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;
   ```

   生成的代码:

   ```java
   String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
   ```

   b. 方法参数有Invocation类型. 

    LoadBalance#select:

   ```java
   @Adaptive("loadbalance")
   <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;
   ```

   生成的代码:

   ```java
   String extName = url.getMethodParameter(invocation.getMethodName(), "random");
   ```

   c. 方法参数没有Invocation类型. 

   Transporter#connect:

   ```java
   @Adaptive({"client", "transporter"})
   Client connect(URL url, ChannelHandler handler) throws RemotingException;
   ```

   生成的代码:

   ```java
   String extName = url.getParameter("client", url.getParameter("transporter", "netty"));
   ```

3. 通过ExtensionLoader根据name来获取对应的扩展点实现, 并执行方法返回.

Protocol生成自适应扩展类的代码示例:

```java
/**
 * 协议自适应类
 */
public class Protocol$Adaptive implements com.alibaba.dubbo.rpc.Protocol {
    public void destroy() {
        throw new UnsupportedOperationException("method public abstract void com.alibaba.dubbo.rpc.Protocol.destroy() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");
    }

    public int getDefaultPort() {
        throw new UnsupportedOperationException("method public abstract int com.alibaba.dubbo.rpc.Protocol.getDefaultPort() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");
    }

    /**
     * 服务引用
     */
    public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0, com.alibaba.dubbo.common.URL arg1) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg1 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg1;
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
        // 根据extName找到具体适应类，然后调用方法
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.refer(arg0, arg1);
    }

    /**
     * 服务暴露
     */
    public com.alibaba.dubbo.rpc.Exporter export(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg0 == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
        if (arg0.getUrl() == null)
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");
        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
        // 根据extName找到具体适应类，然后调用方法
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.export(arg0);
    }
}
```

## 基于SPI机制的Filter拦截器实现

SPI机制提供了AOP的功能, 从而可以对服务导出及引用进行代理, 而拦截链则是在这里通过代理来生成的:

```java
public class ProtocolFilterWrapper implements Protocol {
     public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            return protocol.export(invoker);
        }
        return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
    }
    
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        Invoker<T> last = invoker;
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);
        if (filters.size() > 0) {
            for (int i = filters.size() - 1; i >= 0; i --) {
                final Filter filter = filters.get(i);
                final Invoker<T> next = last;
                last = new Invoker<T>() {

                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }
                    // 代理invoker的invoke方法来实现拦截链
                    public Result invoke(Invocation invocation) throws RpcException {
                        return filter.invoke(next, invocation);
                    }

                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }
}
```



## 引用

1. [SPI官方源码解析](http://dubbo.apache.org/zh-cn/docs/source_code_guide/dubbo-spi.html)
2. [自适应扩展机制源码解析](http://dubbo.apache.org/zh-cn/docs/source_code_guide/adaptive-extension.html)
3. 

## 