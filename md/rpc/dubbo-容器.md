# dubbo-容器

## Spring

作用: 加载运行服务并管理

##  Jetty

作用: 启动内嵌的jetty, 用于服务的监控

## Log4j

作用: 自动配置log4j的配置，在多进程启动时，自动给日志文件按进程分目录。

## 容器启动和停止

通过 Main#main 来启动容器, 通过 Runtime#addShutdownHook 来关闭容器, 在关闭容器的时候, 会调用 Spring 的 ClassPathXmlApplicationContext#stop 来关闭服务, 取消服务的暴露

## 优雅停机

 Dubbo 优雅停机需要满足两点基本诉求：

1. 服务消费者不应该请求到已经下线的服务提供者

2. 在途请求需要处理完毕，不能被停机指令中断

优雅停机的意义：应用的重启、停机等操作，不影响业务的连续性。

## 引用

1. [一文聊透 Dubbo 优雅停机](https://www.cnkirito.moe/dubbo-gracefully-shutdown/)