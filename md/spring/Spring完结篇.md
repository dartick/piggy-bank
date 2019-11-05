# IOC

- 控制反转（IOC） 和 依赖注入（DI）的概念

- Bean的生命周期的知识点分为 **实例化，属性注入，拓展点**，**作用域**

  - **触发时机**：IOC容器初始化时，手动执行getBean，依赖关系触发
  - **实例化手段**： InstantiationAwareBeanPostProcessor拓展点 -> 工厂方法 -> 有参/无参构造函数反射（如果存在 *lookup*-*method* 属性会对目标对象进行Cglib代理，这种情况下调用的则是代理的构造方法） -> BeanPostProcessor的拓展点
  - **属性注入**：构造函数/Setter注入两种基础的注入方式，@Autowired等注解的字段或方法注入（反射），lookup-method 方法级注入
  - **作用域**：默认支持SingleTon和Prototy两种作用域，在Web容器中拓展了Request和Session
  - **拓展点**：
    - InstantiationAwareBeanPostProcessor：postProcessPropertyValues方法在实例化后，属性注入前的最后一个拓展点，常用于**依赖注入注解的拓展支持**，如@Autowired，@Resource以及Dubbo的@Reference
    - SmartInstantiationAwareBeanPostProcessor：getEarlyBeanReference可以对提前暴露Bean进行自定义，常用于**AOP代理Bean为了解决循环依赖的拓展点**
    - BeanPostProcessor：Bean初始化完成后的后置处理，**常用于初始化Bean的支持以及代理Bean**，如@PostConstruct
    - Aware：用于感知IOC容器，如 BeanNameAware, BeanClassLoaderAware, BeanFactoryAware，ApplicationContextAware
    - InitializingBean/init-method：Bean初始化
    - DisposableBean/destroy-method：Bean销毁前的处理

  > 循环依赖问题：Spring仅在使用Setter注入的单例Bean会解决循环依赖问题。
  >
  > 1. 在实例化前对BeanName进行缓存以便检测循环依赖
  > 2. 在实例化后，初始化前提前暴露对象以便解决循环依赖
  >
  > 构造函数注入不能解决是因为实例化依赖对象在实例化当前对象之前，Prototype不能解决是因此解决循环依赖问题需要缓存对象

- IOC容器初始化流程：创建容器 -> 实例化BeanDefinitionRegistryPostProcessor -> 实例化BeanFactoryPostProcessor -> 实例化BeanPostProcessor -> 初始化国际化资源MessageResource -> 实例化事件广播器ApplicationEventMulticaster -> 注册事件监听器 -> 实例化所有非延迟加载的单例

  > 1. BeanFactory和Application区别？BeanFactory提供了IOC的基本功能，ApplicationContext继承了BeanFactory的基本功能， 同时也继承了容器的高级功能，如：MessageSource（国际化资源接口）、ResourceLoader（资源加载接口）、ApplicationEventPublisher（应用事件发布接口）；实例化Bean的时机不同；BeanPostProcessor优先级
  > 2. Spring的事件发布和监听？分为事件（ApplicationEvent），事件监听者（ApplicationListener）和事件发布者（ApplicationEventMulticaster）。在IOC容器初始化时实例化和事件发布者，然后将所有事件监听者注册到事件发布者（只存beanName但未实例化） 。在getBean时通过MergedBeanDefinitionPostProcessor拓展点进行注册监听者实例；否则在发布事件时实例化相应监听者

- 注解支持原理：使用 `***BeanPostProcessor` 来检测并处理各类注解，触发时机根据不同的拓展点会不相同。主要分为两类注解：

  - @Configuration，@Import，@Bean，@ComponentScan，@Component等**注册Bean类**的注解
  - @PostConstruct，@Autowired等对**已经在Spring容器注册过的Bean**

  这时需要考虑两点：

  1. 何时注册各类注解的后置处理器BeanPostProcessor？标签（如`<context:annotation-config>`） 
  2. 由于BeanPostProcessor针对的是Spring管理的Bean，如何确保BeanPostProcessor对注册类注解起作用？手动注册（`<bean>`） 或者 扫描（如`<context:component-scan>`）

  上述两点在AnnotationConfigApplicationContext容器中已经默认支持，因此AnnotationConfigApplicationContext可以支持纯注解

  > 上述注解解析大体流程：
  >
  > 1. 解析配置文件`<context:component-scan>`，注册各类 `***BeanPostProcessor` ，扫描元注解为@Component的类，因此同样注册了@Configuration
  > 2. IOC容器的BeanDefinitionRegistryPostProcessor拓展点执行ConfigurationClassPostProcessor的方法，注册了@Bean，@Import，@ComponentScan等指定的Bean（BeanDefinition）
  > 3. Bean生命周期的BeanPostProcessor处理注解，如@Autowired，@PostConstruct

- 第三方框架整合。拓展位置主要为：自定义标签，BeanDefinitionRegistryPostProcessor，@Import

  - Mybatis：@MapperScan使用@Import结合@Configuration完成对扫描@Annotation注解所在类的注册，然后将该BeanDefinition的BeanClass改为MapperFactoryBean，因此在实际上使用时会调用getObject交由Mybatis自己通过jdk代理完成实例化

  - Dubbo：@DubboComponentScan使用@Import结合@Configuration完成对注册了两个BeanPostProcessor拓展类

    - ServiceAnnotationBeanPostProcessor：实现BeanDefinitionRegistryPostProcessor，因此在IOC初始化时进行扫描@Service，然后分别注册 原始类型 和 `ServiceBean` 类型（用于暴露服务）两个Bean

    - ReferenceAnnotationBeanPostProcessor：实现InstantiationAwareBeanPostProcessorAdapter的postProcessPropertyValues用于注入依赖类。Dubbo自己通过动态代理生成了ReferenceBean，并且自己维护（非Spring Bean），然后调用ReferenceBean.get返回代理类用于注入

      > ReferenceBean既然不是Spring管理的，为何实现FactoryBean？因此在我们使用`<dubbo:reference>`时解析标签时无法维护ReferenceBean，因此在这时是交由Spring管理的

![Spring IOC基础结构图](http://qiniu.zzcoder.cn/Spring%20IOC%E5%9F%BA%E7%A1%80%E7%BB%93%E6%9E%84%E5%9B%BE.png)

![Bean实例化-01](http://qiniu.zzcoder.cn/Bean%E5%AE%9E%E4%BE%8B%E5%8C%96-01.png)

![Bean实例化-02](http://qiniu.zzcoder.cn/Bean%E5%AE%9E%E4%BE%8B%E5%8C%96-02.png)

# AOP

- AOP名词：

  - 连接点（joinPoint）：可以被拦截的目标函数
  - 切入点（pointcut）：对指定的连接点（joinpoint）进行切入，比如 `@Pointcut`，`<aop:pointcut>`
  - 增强（advice）：在切入点（pointcut）需要执行的逻辑，比如 @Before，@Around，`<aop:before>`
  - 切面（aspect）：由切入点和增强组成，比如 `<aop:aspect>` ，`@Aspect`
  - 织入（weaving）：把切面应用到目标函数的过程

- Spring AOP的编程式和声明式使用和原理（ProxyFactory和ProxyFactoryBean）

  > 涉及嵌套代理失效问题

- Spring AOP的基于XML（`<aop:config>`）和注解（@EnableAspectJAutoProxy/`<aop:aspectj-autoproxy/>` + @AspectJ）的自动代理配置

- 自动代理AbstractAutoProxyCreator原理

  1. InstantiationAwareBeanPostProcessor/SmartInstantiationAwareBeanPostProcessor/BeanPostProcessor会生成代理Bean（原因是代理必须等待目标对象初始化完成）
  2. 根据getAdvicesAndAdvisorsForBean决定最终的增强类（包括通用 + 指定）
  3. 选择JDK代理或Cglib代理

  在代理类中的实现通过责任链模式（MethodInvocation + MethodInterceptor）完成拦截器调用链的执行

- Spring内置的自动代理实现类原理：

  - BeanNameAutoProxyCreator：基于Bean配置名规则的自动代理创建器，继承于AbstractAutoProxyCreator，实现getAdvicesAndAdvisorsForBean方法，仅利用通用拦截器（PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS）和无代理（DO_NOT_PROXY）实现
  - DefaultAdvisorAutoProxyCreator/AspectJAwareAdvisorAutoProxyCreator：基于Advisor匹配机制的自动代理创建器。额外增加从IOC容器获取所有的Advisor，然后根据Pointcut来获取可以应用的Advisor
  - AnnotationAwareAspectJAutoProxyCreator：额外这对@Aspect等注解的支持。会在getAdvicesAndAdvisorsForBean中扫描所有@Aspect的类并解析为多个`Advisor`对象并缓存，然后应用符合条件的`Advisor`返回

- 二次代理遇到的问题。当存在两个自动代理器的时候容易发生二次代理，使用时注意：

  - 第二个代理器强制使用cglib。因为当目标函数的类没有实现接口时，第一次代理会使用cglib，因此依赖注入期望注入的bean是CGLIB子类，但第二次代理会重新生成jdk代理，因此会报错类型错误
  - 获取注解时注意jdk代理是不会维护注解的，需要从目标类获取。而cglib由于继承关系（如果注解允许继承@Inherited），是可以直接获取到注解的

# 事务

- 五个事务属性

  - 事务隔离级别（5）：

    - **TransactionDefinition.ISOLATION_READ_UNCOMMITTED**：读未提交，会存在脏读，更新丢失
    - **TransactionDefinition.ISOLATION_READ_COMMITTED**：读已提交，会存在不可重复读
    - **TransactionDefinition.ISOLATION_REPEATABLE_READ**：可重复读，会存在幻读
    - **TransactionDefinition.ISOLATION_SERIALIZABLE**：串形化读
    - **TransactionDefinition.ISOLATION_DEFAULT:** 使用后端数据库默认的隔离级别，Mysql 默认采用的 REPEATABLE_READ隔离级别 Oracle 默认采用的 READ_COMMITTED隔离级别

  - 事务传播（7）：

    - **PROPAGATION_REQUIRED**：**共同使用同个事务**。`@Transactional`注解默认的传播行为
    - **RROPAGATION_REQUIRES_NEW**：**将当前事务挂起，然后新开启一个事务。**
    - **PROPAGATION_NESTED**：创建一个嵌套事务（实际上是创建一个保存点）
    - **PROPAGATION_SUPPORTS**：如果没有，就以**非事务方式**执行；如果有，就加入当前事务
    - **PROPAGATION_NOT_SUPPORTED**
    - **PROPAGATION_NEVER**
    - **PROPAGATION_MANDATORY**

    > **RROPAGATION_REQUIRES_NEW**和**PROPAGATION_NESTED**的区别？
    >
    > - **RROPAGATION_REQUIRES_NEW**：外部事务和内部事务没有关系，一旦内部事务提交以后，即使外部事务抛出异常，内部事务也不会回滚
    > - **PROPAGATION_NESTED**：内层事务依赖于外层事务。外层事务失败时，会回滚内层事务所做的动作。而内层事务操作失败并不会引起外层事务的回滚

  - 事务超时属性：一个事务所允许执行的最长时间，超时自动回滚

  - 事务只读属性：设置后事务中仅允许存在读操作（否则抛出异常）。设置后jdbc，数据库会有一定的优化

  - 回滚规则：定义了哪些异常会导致事务回滚而哪些不会。默认情况下，事务只有遇到运行期异常时才会回滚，而在遇到检查型异常时不会回滚

- 编程式事务管理的两种方式：最基本就是三个接口的组合调用，TransactionTemplate是通过回调，参考：[全面分析 Spring 的编程式事务管理及声明式事务管理](https://www.ibm.com/developerworks/cn/education/opensource/os-cn-spring-trans/index.html)
- 声明式事务管理的四种方式：通过AOP，参考：[全面分析 Spring 的编程式事务管理及声明式事务管理](https://www.ibm.com/developerworks/cn/education/opensource/os-cn-spring-trans/index.html)。实际上声明式事务是很好的AOP的应用场景，因此需要好好理解这四种配置方式的原理

# SpringMVC

<img src="http://qiniu.zzcoder.cn/SpringMVC%E6%9E%B6%E6%9E%84.png" style="zoom: 33%;" />

1. Servlet，Servlet容器，Http服务器，应用服务器的概念

2. Servlet进入前的大致流程：Http服务器接收请求 -> Servlet容器 -> Servlet，更多的应该是Tomcat的一些流程

3. SpringMVC整合Servlet容器

   - ContextConfigLocation实现ServletContextListener接口，因此会在Tomcat容器初始化后被触发，然后进行IOC容器初始化
   - DispatcherServlet实现了Servlet接口，`/`代表拦截诸如`/user`等路径，对于`.jsp`等路径不会拦截，交由Tomcat默认的JspServlet处理，因此在请求到达时初始化DispatcherServlet

4. DispatcherServlet初始化流程

   - 会建立Springmvc容器，Springmvc还会通过ServletContext拿到Spring IOC容器，并把Spring IOC容器设置为Springmvc容器的父容器
   - 初始化各类组件，如HandlerMapping，HandlerAdapter等

5. 请求处理流程，如上图

   1. DispatcherServlet根据从HandlerMapping获取请求获取对应的Handler，并将Handler和多个HandlerInterceptor拦截器封装在HandlerExecutionChain中返回
   2. 将Handler封装为HandlerAdapter（通过适配器模式来屏蔽底层多种处理器接口）
   3. 调用HandlerInterceptor.preHandle
   4. 通过HandlerAdapter调用目标Handler的方法，Handler执行完成后，返回一个ModelAndView对象
   5. 调用HandlerInterceptor.postHandle
   6. 通过ViewResolver将ModelAndView的逻辑视图名解析为具体的View
   7. View会根据传进来的Model模型数据进行渲染
   8. 调用HandlerInterceptor.afterCompletion（无论成功还是失败都会执行）
   9. 响应客户端

6. SpringMVC注解：@Controller，@RequestMapping，@PathVariable，@ResponseBody，@RequestBody及原理

   > @Controller + @RequestMapping对应的方法作为一个Handler，因此通过`<mvc:annotation-driven>`注册了RequestMappingHandlerMapping用于维护@Controller+@RequestMapping对应的方法和请求的映射关系，以便在处理中寻找到对应的处理器

7. `<mvc:resources>` 和 `<mvc:default-servlet-handler/>` 作用，主要用于处理静态资源，大概原理就是自定义HandlerMapping + Handler

8. 为什么存在两个Spring容器？Springmvc存在Spring IOC以及Springmvc两个容器，主要原因是方便Spring拓展，比如Spring想要结合Struts

9. Springmvc中拦截器和过滤器的区别？Filter依赖于Servlet容器，而Interceptor依赖于SpringMVC容器，因此Filter在进入Servlet前触发，而Interceptor在进入Servlet之后触发

# Springboot

- 自定义Starter方法：
  1. 使用@Configuration，@ConditionalOnClass，@EnableConfigurationProperties等注解定义配置类
  2. 在 META-INF/spring.factories 中记录该配置类全路径，如`org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.xxx.HelloworldAutoConfiguration`
  3. 打包 xxx-spring-boot-autoconfigure
  4. 打包starter jar包，其不包含任何代码，加入xxx-spring-boot-autoconfigure依赖，命名规则为 xxx-spring-boot-starter
- 自定义Starter原理：主要是@EnableAutoConfiguration注解，基于@Import+@Configuration从各个jar包的`META-INF/spring.factories`文件中读取需要导入的自动配置类，@SpringBootApplication注解中使用了@EnableAutoConfiguration注解
- SpringMVC零配置原理：
  1. Springboot启动时创建tomcat容器，容器初始化时会扫描当前应用每一个jar包里面META-INF/services/javax.servlet.ServletContainerInitializer指定的实现类
  2. spring-web的文件中指定了实现类SpringServletContainerInitializer，因此调用其onStartup方法，其方法内部做和普通的springmvc解析web.xml类似的事情
