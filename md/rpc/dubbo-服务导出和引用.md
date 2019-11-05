# dubbo-服务导出和引用

## 服务导出

### 相关资料

1. [dubbo如何一步一步拿到bean](https://blog.kazaff.me/2015/01/26/dubbo%E5%A6%82%E4%BD%95%E4%B8%80%E6%AD%A5%E4%B8%80%E6%AD%A5%E6%8B%BF%E5%88%B0bean/)

2. [官网服务导出简要流程](https://dubbo.apache.org/zh-cn/docs/dev/implementation.html)

3. [官网服务导出源码解析](https://dubbo.apache.org/zh-cn/docs/source_code_guide/export-service.html)

4. 相关领域模型的理解可查看源码上的注释

   a. URL

   主要作为信息载体, 可以为配置信息, 异常信息, 运行时信息等等.

   ```java
   /**
    * URL - Uniform Resource Locator (Immutable, ThreadSafe)
    * <p>
    * url example:
    * <ul>
    * <li>http://www.facebook.com/friends?param1=value1&amp;param2=value2
    * <li>http://username:password@10.20.130.230:8080/list?version=1.0.0
    * <li>ftp://username:password@192.168.1.7:21/1/read.txt
    * <li>registry://192.168.1.7:9090/com.alibaba.service1?param1=value1&amp;param2=value2
    * </ul>
    * <p>
    * Some strange example below:
    * <ul>
    * <li>192.168.1.3:20880<br>
    * for this case, url protocol = null, url host = 192.168.1.3, port = 20880, url path = null
    * <li>file:///home/user1/router.js?type=script<br>
    * for this case, url protocol = null, url host = null, url path = home/user1/router.js
    * <li>file://home/user1/router.js?type=script<br>
    * for this case, url protocol = file, url host = home, url path = user1/router.js
    * <li>file:///D:/1/router.js?type=script<br>
    * for this case, url protocol = file, url host = null, url path = D:/1/router.js
    * <li>file:/D:/1/router.js?type=script<br>
    * same as above file:///D:/1/router.js?type=script 
    * <li>/home/user1/router.js?type=script <br>
    * for this case, url protocol = null, url host = null, url path = home/user1/router.js
    * <li>home/user1/router.js?type=script <br>
    * for this case, url protocol = null, url host = home, url path = user1/router.js
    * </ul>
    * 
    * @author william.liangf
    * @author ding.lid
    * @see java.net.URL
    * @see java.net.URI
    */
   public final class URL implements Serializable {
   
       private static final long serialVersionUID = -1985165475234910535L;
   
       private final String protocol;
   
   	private final String username;
   
   	private final String password;
   
   	private final String host;
   
   	private final int port;
   
   	private final String path;
   
       private final Map<String, String> parameters;
       
       //... 省略其他代码
   }
   ```

   

   b. Invoker

   将所有的调用抽象到invoke方法, 调用可以是远程调用, 本地调用, 集群调用等等.

   ```java
   public interface Invoker<T> extends Node {
   
       /**
        * get service interface.
        * 
        * @return service interface.
        */
       Class<T> getInterface();
   
       /**
        * invoke.
        * 
        * @param invocation
        * @return result
        * @throws RpcException
        */
       Result invoke(Invocation invocation) throws RpcException;
   
   }
   ```

   c. Protocol

   协议, 主要作用是暴露服务及引用服务, 具体的协议来定义如何导出服务及如何引用服务.

   ```java
   /**
    * Protocol. (API/SPI, Singleton, ThreadSafe)
    * 
    * @author william.liangf
    */
   @SPI("dubbo")
   public interface Protocol {
       
       /**
        * 获取缺省端口，当用户没有配置端口时使用。
        * 
        * @return 缺省端口
        */
       int getDefaultPort();
   
       /**
        * 暴露远程服务：<br>
        * 1. 协议在接收请求时，应记录请求来源方地址信息：RpcContext.getContext().setRemoteAddress();<br>
        * 2. export()必须是幂等的，也就是暴露同一个URL的Invoker两次，和暴露一次没有区别。<br>
        * 3. export()传入的Invoker由框架实现并传入，协议不需要关心。<br>
        * 
        * @param <T> 服务的类型
        * @param invoker 服务的执行体
        * @return exporter 暴露服务的引用，用于取消暴露
        * @throws RpcException 当暴露服务出错时抛出，比如端口已占用
        */
       @Adaptive
       <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;
   
       /**
        * 引用远程服务：<br>
        * 1. 当用户调用refer()所返回的Invoker对象的invoke()方法时，协议需相应执行同URL远端export()传入的Invoker对象的invoke()方法。<br>
        * 2. refer()返回的Invoker由协议实现，协议通常需要在此Invoker中发送远程请求。<br>
        * 3. 当url中有设置check=false时，连接失败不能抛出异常，并内部自动恢复。<br>
        * 
        * @param <T> 服务的类型
        * @param type 服务的类型
        * @param url 远程服务的URL地址
        * @return invoker 服务的本地代理
        * @throws RpcException 当连接服务提供方失败时抛出
        */
       @Adaptive
       <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;
   
       /**
        * 释放协议：<br>
        * 1. 取消该协议所有已经暴露和引用的服务。<br>
        * 2. 释放协议所占用的所有资源，比如连接和端口。<br>
        * 3. 协议在释放后，依然能暴露和引用新的服务。<br>
        */
       void destroy();
   
   }
   ```

   d. Exporter

   主要作用还是取消服务的导出, **getInvoker()**方法也是为了服务于**unexport()**, 将导出服务生成的Exporter进行维护一个exporterMap, 用于服务取消暴露.

   ```java
   public interface Exporter<T> {
       
       /**
        * get invoker.
        * 
        * @return invoker
        */
       Invoker<T> getInvoker();
       
       /**
        * unexport.
        * 
        * <code>
        *     getInvoker().destroy();
        * </code>
        */
       void unexport();
   
   }
   ```

   

### 流程图

1. 服务导出大体流程

![1571275805239](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571275805239.png)

2. 协议导出服务大体流程, 对应源码

   ```java
   Exporter<?> exporter = protocol.export(proxyFactory.getInvoker(ref, (Class) interfaceClass, url));
   ```

   

   ![1571277017792](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277017792.png)

3. 本地服务导出, 对应类**InjvmProtocol**

   ![1571277137612](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277137612.png)

4. 远程服务导出, 对应类**DubboProtocol**

   ![1571277190828](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277190828.png)

5. 导出到注册中心, 对应类 **RegistryProtocol**

   ![1571277236301](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277236301.png)

## 服务引用

### 相关资料

1. [官网服务引用简要流程](https://dubbo.apache.org/zh-cn/docs/dev/implementation.html)
2. [官网服务引用源码解析](https://dubbo.apache.org/zh-cn/docs/source_code_guide/refer-service.html)
3. 涉及的领域模型服务导出基本有讲到

### 流程图

1. 服务引用大体流程图

   ![1571277636867](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277636867.png)

2. 本地服务引用

   ![1571277709879](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277709879.png)

3. 远程服务引用

   ![1571277729176](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277729176.png)

4. 注册中心服务引用

   ![1571277750980](C:\Users\WD\AppData\Roaming\Typora\typora-user-images\1571277750980.png)