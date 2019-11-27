# Spring-注解实现原理

当我们使用 @Controller, @Service 等注解的时候, 我们知道, 通过 @ComponetScan 或者 \<context:annotation-config\> 来扫描特定路径下的被该注解标记的类, 注册为Bean. 但是有以下几个问题:

1. 如何获取到被注解到的类 ?
2. 该类是什么时机进