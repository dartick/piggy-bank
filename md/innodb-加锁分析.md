# select for update 加锁分析

一句select for update虽然显示地进行了加锁，但是实际加了什么锁，加在什么身上，并不能单靠这条语句便能知道了，还得依靠以下几个因素：

1.  隔离级别（RC/RR）
2.  执行计划（不走索引，聚簇索引，唯一索引，非唯一索引）
3.  查询条件（等值/范围）

## 读已提交（RC）

### 不走索引

innodb会对表中的所有记录进行加锁，返回给service层过滤出命中的记录，然后会通知innodb把未命中的记录的锁释放掉

### 二级索引

对命中的二级索引添加record lock, 同时对二级索引相应的聚簇索引添加record lock

### 聚簇索引

对命中的聚簇索引添加record lock

## 可重复读（RR）

### 不走索引

会对全表的每行记录添加next-key lock

### 二级索引

无论是等值查询还是范围查询，都会添加next-key lock， 同时对应的聚簇索引会加record lock

### 唯一索引

等值查询对命中的二级索引添加record lock， 范围查询添加next-key lock, 同时对应的聚簇索引会加record lock

### 聚簇索引

等值查询对命中的聚簇索引添加record lock， 范围查询添加next-key lock

## 锁分析工具
Mysql提供了语句来查询当前持有锁的状态和类型等等，是验证我们的判断的利器。语句如下：
> SELECT * FROM performance_schema.data_locks

它提供几个关键信息：

*  LOCK_TYPE：锁类型，RECORD 代表行锁，TABLE 代表表锁
*  LOCK_MODE：锁模式，X,REC_NOT_GAP 代表 Record Lock , X, GAP 代表 Gap Lock , X 代表 Next-key Lock
*  INDEX_NAME：锁定索引的名称
*  LOCK_DATA：与锁相关的数据，比如锁在主键上就是主键值

除此之外，Mysql还提供了查询当前正在执行的每个事务（不包括只读事务）的信息，比如隔离级别，内存中此事务的锁结构占用的总大小等等。语句如下：

> SELECT * FROM INFORMATION_SCHEMA.INNODB_TRX
它提供几个关键信息：

*  TRX_ID：如果是非锁定的只读事务是没有该id的
*  TRX_REQUESTED_LOCK_ID：当前事务正在等待的锁id
*  TRX_TABLES_LOCKED：当前SQL语句具有行锁定的表的数量
*  TRX_LOCK_MEMORY_BYTES：内存中此事务的锁结构占用的总大小。
*  TRX_ISOLATION_LEVEL：当前事务的隔离级别

