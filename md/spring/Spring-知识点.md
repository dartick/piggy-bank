# Spring-知识点

## ApplicationContext

ApplicationContext 主要对 BeanFactory 进行了扩展, 扩展的功能点有:

1. 事件机制
2. 国际化
3. 容器后置处理
4. 注册Bean后置处理器
5. Bean后置处理器优先级
6. 触发 non-lazy-init Bean 实例化

## 容器初始化流程

## Bean生命周期

1. 缓存中获取 (若FactoryBean, 则调用getObject)
2. 循环依赖检测 (通过BeanName在map实现, 在实例化前塞进去)
3. 获取BeanDefinition
4. BeanName塞进循环依赖map

### 循环依赖解决

#### 构造方法循环依赖

通过**singletonsCurrentlylnCreation**将BeanName缓存，当发生构造方法循环依赖则会抛出异常**BeanCurrentlylnCreationException**。

#### setter 循环依赖

Bean的实例化过程主要分为三步：

1. Bean对象实例化
2. Bean填充属性
3. Bean的init方法执行

出现循环依赖的地方主要在1和2，Spring通过三级缓存来解决：

```java
/** 一级缓存：用于存放完全初始化好的 bean **/
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);

/** 二级缓存：存放原始的 bean 对象（尚未填充属性），用于解决循环依赖 */
private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

/** 三级级缓存：存放 bean 工厂对象，用于解决循环依赖 */
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

/**
bean 的获取过程：先从一级获取，失败再从二级、三级里面获取

创建中状态：是指对象已经 new 出来了但是所有的属性均为 null 等待被 init
*/
```

检测循环依赖的过程如下：

- A 创建过程中需要 B，于是 **A 将自己放到三级缓里面** ，去实例化 B

- B 实例化的时候发现需要 A，于是 B 先查一级缓存，没有，再查二级缓存，还是没有，再查三级缓存，找到了！

- - **然后把三级缓存里面的这个 A 放到二级缓存里面，并删除三级缓存里面的 A**
  - B 顺利初始化完毕，**将自己放到一级缓存里面**（此时B里面的A依然是创建中状态）

- 然后回来接着创建 A，此时 B 已经创建结束，直接从一级缓存里面拿到 B ，然后完成创建，**并将自己放到一级缓存里面**

- 如此一来便解决了循环依赖的问题

## Spring注解实现原理

Spring的注解大体分两类:

1. 定义Bean的注解: @Configuration，@Import，@Bean，@ComponentScan，@Component
2. 处理Bean的注解: @PostConstruct，@Autowired

### 定义Bean

## Spring 扩展点