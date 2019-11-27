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
5. 

## Spring注解实现原理

Spring的注解大体分两类:

1. 定义Bean的注解: @Configuration，@Import，@Bean，@ComponentScan，@Component
2. 处理Bean的注解: @PostConstruct，@Autowired

### 定义Bean

## Spring 扩展点