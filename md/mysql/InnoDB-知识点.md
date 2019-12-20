# InnoDB-知识点



## Myisam与InnoDB的区别

1. 锁的粒度不同，Myisam支持表级锁，而InnoDB支持行级锁。表级锁粒度大，并发低，但是加锁快，开销小，不会发生死锁，适用于少量数据存储
2. Myisam不支持事务
3. Myisam不支持外键

## 四大特性

### change buffer

### double write

### 自适应哈希

### 预读

## 逻辑存储结构

### 表空间

### 段

### 区

### 页

### 行

## 事务的实现原理

### 原子性

### 隔离性

#### RU

#### RC

#### RR

#### 串行

### 持久性

## MVCC原理

## Buffer Pool

### 缓存管理

1. LRU List
2. Free List
3. Flush List

## 索引的实现

