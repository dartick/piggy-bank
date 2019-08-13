# 事务

## 分布式事务

### Open/X XA

1.  [初识Open/X XA](https://www.jianshu.com/p/6c1fd2420274)

### CAP理论

#### P的理解

P指的是分区容忍性，其定义为机器故障、网络故障、机器停电等异常情况下仍然能够满足一致性和可用性。从字面上进行理解，分区就是由于某些原因导致集群分裂为多个不连通的子集群，由于不连通导致部分数据无法访问，从而不能保证一致性。