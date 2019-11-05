# dubbo-安全机制

## 面试题

### Token令牌

![/user-guide/images/dubbo-token.jpg](https://dubbo.gitbooks.io/dubbo-user-book/sources/images/dubbo-token.jpg)

作用： 

1. 防止消费者绕过注册中心访问提供者, 比如提供者对外网开放.
2. 在注册中心控制权限，以决定要不要下发令牌给消费者。 
3. 注册中心可灵活改变授权方式，而不需要修改或升级提供者。

## 黑白名单

通过服务路由可实现黑白名单

