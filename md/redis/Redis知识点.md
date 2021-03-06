## Redis-知识点

## 内存模型

### 内存分布

1. 数据
2. 进程本身运行的内存
3. 缓冲内存
4. 内存碎片

### 数据结构

#### redisObect

redis里的数据首先是个redisObject, 其作用对基础存储对象(字符串, 列表, 哈希, 集合, 有序集合)进行了包装, 类似基础数据结构的接口, 然后基于它, 一种数据数据接口可以有不同的实现方式, 提高灵活性

```c
typedef struct redisObject {
　　unsigned type:4; // 字符串, 列表, 哈希, 集合, 有序集合
　　unsigned encoding:4; // 不用的实现
　　unsigned lru:REDIS_LRU_BITS; // 对象最后一次被命令程序访问的时间
　　int refcount; // 引用次数
　　void *ptr; // 数据具体的地址
} robj;
```

#### SDS

对char[]进行了包装,  类比于 Java 的 String

```c
struct sdshdr {
    int len;
    int free;
    char buf[];
};
```

#### 压缩列表-ziplist

压缩列表是Redis为了节约内存而开发的，是由一系列特殊编码的**连续内存块**组成的顺序型数据结构；进行修改或增删操作时，复杂度较高

#### 双端链表-linkedlist

即普通的双端列表, 表头和表尾都可以操作

#### 哈希表-hashtable

跟其他哈希表比较不同的地方是其扩容方式, 其扩容不是一次性完成的, 而是渐进的, 分批完成的, 在扩容时, 一次性迁移所有数据, 在海量数据的场景下是必然导致性能的下降

#### 整数集合-intset

整数的集合, 不允许重复

#### 跳跃表-skiplist

原本链表的查询需要从头遍历到尾, 这样在链表很长的情况下, 查询性能就低下, 而跳表就跳跃查询的思想来优化链表的查询, 原理也很简单, 将节点拉高便能跳着查询:  [原理简介](https://juejin.im/post/57fa935b0e3dd90057c50fbc)

![img](https://user-gold-cdn.xitu.io/2016/11/29/a9648d8a8d71023a630eee04a57e2116?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

> 为什么不用平衡树? 
>
> 在做范围查找的时候，平衡树比skiplist操作要复杂。
>
> 平衡树的插入和删除操作可能引发子树的调整，逻辑复杂
>
> 从内存占用上来说，skiplist比平衡树更灵活一些

### 对象

#### 字符串

字符串长度不能超过512MB

##### 内部编码

1. int. 字符串值是整型时，这个值使用long整型表示。
2. embstr. <=39字节的字符串。
3. raw：大于39个字节的字符串

> embstr与raw都使用redisObject和sds保存数据，区别在于，embstr的使用只分配一次内存空间（因此redisObject和sds是连续的），而raw需要分配两次内存空间（分别为redisObject和sds分配空间）。因此与raw相比，embstr的好处在于创建时少分配一次空间，删除时少释放一次空间，以及对象的所有数据连在一起，寻找方便。而embstr的坏处也很明显，如果字符串的长度增加需要重新分配内存时，整个redisObject和sds都需要重新分配空间，因此redis中的embstr实现为只读。

##### 编码转换

当int数据不再是整数，或大小超过了long的范围时，自动转化为raw。

#### 列表

列表（list）用来存储多个有序的字符串

##### 内部编码

1. ziplist
2. linkedlist

##### 编码转换

列表中元素数量小于512个；列表中所有字符串对象都不足64字节。如果有一个条件不满足，则使用双端列表；且编码只可能由压缩列表转化为双端链表，反方向则不可能。

#### 哈希

不仅是redis对外提供的5种对象类型的一种（与字符串、列表、集合、有序结合并列），也是Redis作为Key-Value数据库所使用的数据结构。

##### 内部编码

1. ziplist
2. hashtable

##### 编码转换

只有同时满足下面两个条件时，才会使用压缩列表：哈希中元素数量小于512个；哈希中所有键值对的键和值字符串长度都小于64字节。如果有一个条件不满足，则使用哈希表；且编码只可能由压缩列表转化为哈希表，反方向则不可能。

#### 集合

集合（set）与列表类似，都是用来保存多个字符串, 且不能重复

##### 内部编码

1. intset
2. hashtable. 集合在使用哈希表时，值全部被置为null

##### 编码转换

只有同时满足下面两个条件时，集合才会使用整数集合：集合中元素数量小于512个；集合中所有元素都是整数值。如果有一个条件不满足，则使用哈希表；且编码只可能由整数集合转化为哈希表，反方向则不可能。

#### 有序集合

有序集合与集合一样，元素都不能重复, 但元素是有顺序的

##### 内部编码

1. ziplist
2. skiplist

##### 编码转换

只有同时满足下面两个条件时，才会使用压缩列表：有序集合中元素数量小于128个；有序集合中所有成员长度都不足64字节。如果有一个条件不满足，则使用跳跃表；且编码只可能由压缩列表转化为跳跃表，反方向则不可能。

## 持久化

### RDB (Redis DataBase 默认开启)

保存内存数据的快照到磁盘

#### 手动触发

可以**save**和**bgsave**两种指令, **save** 指令执行导致redis服务不可用, 所以线上环境都禁用, **bgsave** 会创建子进程来执行快照的保存, 这样主进程依然能继续提供服务.

#### 自动触发

**save m n** 指令, 在 m 秒 发生 n 次 变化 则会触发执行 **bgsave**

### AOF (Append Only File-主流方案)

以日志的形式来实现持久化

AOF的执行流程包括：

- 命令追加(append)：将Redis的写命令追加到缓冲区**aof_buf**；
- 文件写入(write)和文件同步(sync)：根据不同的同步策略将**aof_buf**中的内容同步到硬盘；
- 文件重写(rewrite)：定期重写AOF文件，达到压缩的目的。

#### 文件重写

AOF重写是把Redis进程内的数据转化为写命令，同步到新的AOF文件；不会对旧的AOF文件进行任何读取、写入操作 !

![img](https://images2018.cnblogs.com/blog/1174710/201806/1174710-20180605092001589-1724580361.png)



RDB和AOF各有优缺点：

**RDB持久化**

优点：RDB文件紧凑，体积小，网络传输快，适合全量复制；恢复速度比AOF快很多。当然，与AOF相比，RDB最重要的优点之一是对性能的影响相对较小。

缺点：RDB文件的致命缺点在于其数据快照的持久化方式决定了必然做不到实时持久化，而在数据越来越重要的今天，数据的大量丢失很多时候是无法接受的，因此AOF持久化成为主流。此外，RDB文件需要满足特定格式，兼容性差（如老版本的Redis不兼容新版本的RDB文件）。

**AOF持久化**

与RDB持久化相对应，AOF的优点在于支持秒级持久化、兼容性好，缺点是文件大、恢复速度慢、对性能影响大。

## 主从复制

### 全量复制

通过全量复制的过程可以看出，全量复制是非常重型的操作：

（1）从节点判断无法进行部分复制，向主节点发送全量复制的请求；或从节点发送部分复制的请求，但主节点判断无法进行全量复制；具体判断过程需要在讲述了部分复制原理后再介绍。

（2）主节点收到全量复制的命令后，执行bgsave，在后台生成RDB文件，并使用一个缓冲区（称为复制缓冲区）记录从现在开始执行的所有写命令

（3）主节点的bgsave执行完成后，将RDB文件发送给从节点；**从节点首先清除自己的旧数据，然后载入接收的****RDB****文件**，将数据库状态更新至主节点执行bgsave时的数据库状态

（4）主节点将前述复制缓冲区中的所有写命令发送给从节点，从节点执行这些写命令，将数据库状态更新至主节点的最新状态

（5）如果从节点开启了AOF，则会触发bgrewriteaof的执行，从而保证AOF文件更新至主节点的最新状态

### 增量复制

用于处理网络中断时的数据同步。

#### 复制偏移量

主节点和从节点分别维护一个复制偏移量（offset），代表的是**主节点向从节点传递的字节数**；主节点每次向从节点传播N个字节数据时，主节点的offset增加N；从节点每次收到主节点传来的N个字节数据时，从节点的offset增加N。

offset用于判断主从节点的数据库状态是否一致：如果二者offset相同，则一致；如果offset不同，则不一致，此时可以根据两个offset找出从节点缺少的那部分数据。例如，如果主节点的offset是1000，而从节点的offset是500，那么部分复制就需要将offset为501-1000的数据传递给从节点。而offset为501-1000的数据存储的位置，就是下面要介绍的复制积压缓冲区。

#### 复制积压缓冲区

复制积压缓冲区是由主节点维护的、固定长度的、先进先出(FIFO)队列，默认大小1MB；当主节点开始有从节点时创建，其作用是备份主节点最近发送给从节点的数据。注意，无论主节点有一个还是多个从节点，都只需要一个复制积压缓冲区。

从节点将offset发送给主节点后，主节点根据offset和缓冲区大小决定能否执行部分复制：

- 如果offset偏移量之后的数据，仍然都在复制积压缓冲区里，则执行部分复制；
- 如果offset偏移量之后的数据已不在复制积压缓冲区中（数据已被挤出），则执行全量复制。

#### 服务器运行ID

每个Redis节点(无论主从)，在启动时都会自动生成一个随机ID(每次启动都不一样)，由40个随机的十六进制字符组成；runid用来唯一识别一个Redis节点。

主从节点初次复制时，主节点将自己的runid发送给从节点，从节点将这个runid保存起来；当断线重连时，从节点会将这个runid发送给主节点；主节点根据runid判断能否进行部分复制：

- 如果从节点保存的runid与主节点现在的runid相同，说明主从节点之前同步过，主节点会继续尝试使用部分复制(到底能不能部分复制还要看offset和复制积压缓冲区的情况)；
- 如果从节点保存的runid与主节点现在的runid不同，说明从节点在断线前同步的Redis节点并不是当前的主节点，只能进行全量复制。

### 主从复制的问题

#### 延迟与不一致问题

优化主从节点之间的网络环境（如在同机房部署）；监控主从节点延迟（通过offset）判断，如果从节点延迟过大，通知应用不再通过该从节点读取数据；使用集群同时扩展写负载和读负载等。

#### 数据过期问题

在主从复制场景下，为了主从节点的数据一致性，从节点不会主动删除数据，而是由主节点控制从节点中过期数据的删除。由于主节点的惰性删除和定期删除策略，都不能保证主节点及时对过期数据执行删除操作，因此，当客户端通过Redis从节点读取数据时，很容易读取到已经过期的数据。

Redis 3.2中，从节点在读取数据时，增加了对数据是否过期的判断：如果该数据已过期，则不返回给客户端；将Redis升级到3.2可以解决数据过期问题。

#### 故障切换

在没有使用哨兵的读写分离场景下，应用针对读和写分别连接不同的Redis节点；当主节点或从节点出现问题而发生更改时，需要及时修改应用程序读写Redis数据的连接；连接的切换可以手动进行，或者自己写监控程序进行切换，但前者响应慢、容易出错，后者实现复杂，成本都不算低。

## 哨兵

哨兵主要作用就是主节点在发生故障的时候进行**故障转移**, 从而需要对节点进行**监控**, 同时, 当故障转移之后, 需要通知客户端主节点的变更, 从而需要**通知**的功能, 同时, 当新的客户端想要连接主节点, 通过哨兵来获取主节点的地址, 所以哨兵也是**配置提供者**

#### 实现

1. **定时任务**：每个哨兵节点维护了3个定时任务。定时任务的功能分别如下：通过向主从节点发送info命令获取最新的主从结构；通过发布订阅功能获取其他哨兵节点的信息；通过向其他节点发送ping命令进行心跳检测，判断是否下线。
2. **主观下线**：在心跳检测的定时任务中，如果其他节点超过一定时间没有回复，哨兵节点就会将其进行主观下线。顾名思义，主观下线的意思是一个哨兵节点“主观地”判断下线；与主观下线相对应的是客观下线。
3. **客观下线**：哨兵节点在对主节点进行主观下线后，会通过sentinel is-master-down-by-addr命令询问其他哨兵节点该主节点的状态；如果判断主节点下线的哨兵数量达到一定数值，则对该主节点进行客观下线。
4. **选举领导者哨兵节点**：当主节点被判断客观下线以后，各个哨兵节点会进行协商，选举出一个**领导者哨兵节点**，并由该领导者节点对其进行故障转移操作。
5. **故障转移**：选举出的领导者哨兵，开始进行故障转移操作，该操作大体可以分为3个步骤：
   - 在从节点中选择新的主节点：选择的原则是，首先过滤掉不健康的从节点；然后选择优先级最高的从节点(由slave-priority指定)；如果优先级无法区分，则选择复制偏移量最大的从节点；如果仍无法区分，则选择runid最小的从节点。
   - 更新主从状态：通过slaveof no one命令，让选出来的从节点成为主节点；并通过slaveof命令让其他节点成为其从节点。
   - 将已经下线的主节点(即6379)设置为新的主节点的从节点，当6379重新上线后，它会成为新的主节点的从节点。

## 集群

在以上的高可用方案中, 仍然无法解决redis单机问题及写负载均衡问题, 在Redis 3.0 后引入**集群**, 这些问题便得以解决.

集群通过**数据分片**来实现写的负载均衡, 然而将数据进行了分片, 必然会带来新的问题:

### 分片方式

分片的方式大体分为三种:

1. **顺序分片**. 即指定分区处理的数据范围. 但不具备随机性, 数据分布不均匀
2. **哈希取余**. 哈希天然支持随机性, 同时均匀程度也取决于哈希函数, 但是当节点数量发生变化, 所有节点的数据都得重新哈希定位.
3. **一致性哈希**. 降低节点数量发生变化带来的影响, 但是有分布不均的问题, 引入虚拟节点便可解决. redis所采取的方案

在redis中, 虚拟节点被称为哈希槽, 引入槽以后，数据的映射关系由数据hash->实际节点，变成了数据hash->槽->实际节点。**在使用了槽的一致性哈希分区中，槽是数据管理和迁移的基本单位。槽解耦了数据和实际节点之间的关系，增加或删除节点对系统的影响很小。**

### 数据定位

当数据分散到各个节点上, 那么就需要解决一个数据定位的问题, Redis在服务端和客户端分别来解决这个问题:

1. 服务端. 请求过来后, 判断该key的数据并不在当前节点上, 那么会返回**moved**错误, 该错误会携带目标key在节点的地址, 客户端接收到该错误, 然后再请求到真正的节点上.
2. 客户端. 在客户端也会维护槽与节点的关系, 那么就可以在请求前, 在本地计算出目标key所在的真正的节点. 当然, 本地维护也不一定会完全正确(节点增加减少, 客户端并没有及时变更), 当请求后收到**moved**错误, 会重新维护本地的映射关系.

集群的引入, 那么Redis解决集群中会遇到的问题:

### 节点发现

Redis没有引入第三者来维护集群, 而是节点自己来维护, 每个节点都知道集群下的所有节点的信息. 当需要加入一个节点, 可选择该集群下任意节点发送请求, 接收该请求的节点由于拥有集群每个节点的信息, 那么它可以告知其他节点, 有新节点来加入, 当集群的每个节点都与新节点完成握手后, 新节点也便完成了集群的加入

### 节点通讯

#### 端口

在哨兵系统中，节点分为数据节点和哨兵节点：前者存储数据，后者实现额外的控制功能。在集群中，没有数据节点与非数据节点之分：所有的节点都存储数据，也都参与集群状态的维护。

为此，集群中的每个节点，都提供了两个TCP端口：

1. 普通端口：即我们在前面指定的端口(7000等)。通端口主要用于为客户端提供服务
2. 集群端口：端口号是普通端口+10000（10000是固定值，无法改变），如7000节点的集群端口为17000。集群端口只用于节点之间的通信，如搭建集群、增减节点、故障转移等操作时节点间的通信

#### 协议

节点间通信，按照通信协议可以分为几种类型：单对单、广播、Gossip协议等。重点是广播和Gossip的对比。

广播是指向集群内所有节点发送消息；优点是集群的收敛速度快(集群收敛是指集群内所有节点获得的集群信息是一致的)，缺点是每条消息都要发送给所有节点，CPU、带宽等消耗较大。

Gossip协议的特点是：在节点数量有限的网络中，每个节点都“随机”的与部分节点通信（并不是真正的随机，而是根据特定的规则选择通信的节点），经过一番杂乱无章的通信，每个节点的状态很快会达到一致。Gossip协议的优点有负载(比广播)低、去中心化、容错性高(因为通信有冗余)等；缺点主要是集群的收敛速度慢。

#### 消息类型

**集群中的节点采用固定频率（每秒10次）的定时任务进行通信相关的工作**：判断是否需要发送消息及消息类型、确定接收节点、发送消息等。如果集群状态发生了变化，如增减节点、槽状态变更，通过节点间的通信，所有节点会很快得知整个集群的状态，使集群收敛。

节点间发送的消息主要分为5种：meet消息、ping消息、pong消息、fail消息、publish消息。不同的消息类型，通信协议、发送的频率和时机、接收节点的选择等是不同的。

- **MEET消息**：在节点握手阶段，当节点收到客户端的CLUSTER MEET命令时，会向新加入的节点发送MEET消息，请求新节点加入到当前集群；新节点收到MEET消息后会回复一个PONG消息。

- **PING消息**：集群里**每个节点每秒钟会选择部分节点发送PING消息**，接收者收到消息后会回复一个PONG消息。PING消息的内容是自身节点和部分其他节点的状态信息；作用是彼此交换信息，以及检测节点是否在线。PING消息使用Gossip协议发送，接收节点的选择兼顾了收敛速度和带宽成本，具体规则如下：

  (1)随机找5个节点，在其中选择最久没有通信的1个节点

  (2)扫描节点列表，选择最近一次收到PONG消息时间大于cluster_node_timeout/2的所有节点，防止这些节点长时间未更新。

- PONG消息：PONG消息封装了自身状态数据。可以分为两种：第一种是在接到MEET/PING消息后回复的PONG消息；**第二种是指节点向集群广播PONG消息**，这样其他节点可以获知该节点的最新信息，例如**故障恢复后新的主节点会广播PONG消息。**

- FAIL消息：当一个主节点判断另一个主节点进入FAIL状态时，会向集群广播这一FAIL消息；接收节点会将这一FAIL消息保存起来，便于后续的判断。

- PUBLISH消息：节点收到PUBLISH命令后，会先执行该命令，然后向集群广播这一消息，接收节点也会执行该PUBLISH命令。

### 节点增减 (集群伸缩)

伸缩的核心是槽迁移：修改槽与节点的对应关系，实现槽(即数据)在节点之间的移动。

#### 迁移过程的请求处理

![img](https://img2018.cnblogs.com/blog/1174710/201810/1174710-20181025213612837-648236990.png)

返回ask错误, 告诉客户端正在进行槽迁移, 这时是不会更新映射关系的.

### 故障转移

故障转移与哨兵类似, 只不过选主的对象由哨兵变成了从节点. 原本哨兵是由所有的哨兵节点来选出主哨兵, 再由主哨兵选出从节点, 而集群是由所有的主节点来选择从节点

与哨兵一样，集群只实现了主节点的故障转移；**从节点故障时只会被下线，不会进行故障转移**。因此，**使用集群时，应谨慎使用读写分离技术**，因为从节点故障会导致读服务不可用，可用性变差。

### 集群的限制

由于集群中的数据分布在不同节点中，导致一些功能受限，包括：

（1）key批量操作受限：例如mget、mset操作，只有当操作的key都位于一个槽时，才能进行。针对该问题，一种思路是在客户端记录槽与key的信息，每次针对特定槽执行mget/mset；另外一种思路是使用Hash Tag，将在下一小节介绍。

（2）keys/flushall等操作：keys/flushall等操作可以在任一节点执行，但是结果只针对当前节点，例如keys操作只返回当前节点的所有键。针对该问题，可以在客户端使用cluster nodes获取所有节点信息，并对其中的所有主节点执行keys/flushall等操作。

（3）事务/Lua脚本：集群支持事务及Lua脚本，但前提条件是所涉及的key必须在同一个节点。Hash Tag可以解决该问题。

（4）数据库：单机Redis节点可以支持16个数据库，集群模式下只支持一个，即db0。

（5）复制结构：只支持一层复制结构，不支持嵌套。

#### hash tag

一个key包含 {} 的时候，不对整个key做hash，而仅对 {} 包括的字符串做hash。Hash Tag可以让不同的key拥有相同的hash值，从而分配在同一个槽里；这样针对不同key的批量操作(mget/mset等)，以及事务、Lua脚本等都可以支持。不过Hash Tag可能会带来数据分配不均的问题，这时需要：

(1)调整不同节点中槽的数量，使数据分布尽量均匀；

(2)避免对热点数据使用Hash Tag，导致请求分布不均。

## 引用

1. [深入学习Redis](https://www.cnblogs.com/kismetv/p/8654978.html)