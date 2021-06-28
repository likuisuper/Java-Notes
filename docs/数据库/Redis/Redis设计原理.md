redis的key都是String类型的，redis自己实现了一个叫sds（简单动态字符串）的数据结构。

扩容：渐进式扩容

used:size>=1扩容，而且是2倍扩容



redis实际的数据类型

源码：server.h

~~~c
/* The actual Redis Object */
#define OBJ_STRING 0    /* String object. */
#define OBJ_LIST 1      /* List object. */
#define OBJ_SET 2       /* Set object. */
#define OBJ_ZSET 3      /* Sorted set object. */
#define OBJ_HASH 4      /* Hash object. */
~~~

同一种类型的key，时间的编目encoding也是不一样的：

~~~c
127.0.0.1:6379> set aa qwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww
OK
127.0.0.1:6379> object encoding aa
"embstr"
127.0.0.1:6379> set aaa qwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww
OK
127.0.0.1:6379> object encoding aaa
"raw"
127.0.0.1:6379> set b 1
OK
127.0.0.1:6379> object encoding b
"int"
127.0.0.1:6379>
~~~

**一个Redis实例对应一个RedisDB(db0)，一个RedisDB对应一个Dict（哈希表），一个Dict对应两个Dicht（哈希桶），正常情况下只用到ht[0]，ht[1]在Rehash时使用**。

#### redisDb

redisDb数据结构，源码：server.h

~~~c
/* Redis database representation. There are multiple databases identified
 * by integers from 0 (the default database) up to the max configured
 * database. The database number is the 'id' field in the structure. */
typedef struct redisDb {
    dict *dict;                 /* The keyspace for this DB */
    dict *expires;              /* Timeout of keys with a timeout set */
    dict *blocking_keys;        /* Keys with clients waiting for data (BLPOP)*/
    dict *ready_keys;           /* Blocked keys that received a PUSH */
    dict *watched_keys;         /* WATCHED keys for MULTI/EXEC CAS */
    int id;                     /* Database ID */
    long long avg_ttl;          /* Average TTL, just for stats */
    unsigned long expires_cursor; /* Cursor of the active expire cycle. */
    list *defrag_later;         /* List of key names to attempt to defrag one by one, gradually. */
} redisDb;
~~~

#### Dict字典

在redis中，键值对存储方式是由字典（Dict）保存的，源码：dict.h

~~~c
typedef struct dict {
    dictType *type;	//字典类型
    void *privdata;	//私有数据
    dictht ht[2]; // hash table ht[0]  ht[1]
    long rehashidx; //记录rehash进度的标志（渐进式hash），值为-1表示rehash未进行
    int16_t pauserehash; /* If >0 rehashing is paused (<0 indicates coding error) 当前正在迭代的迭代器数*/
} dict;
~~~

![](https://z3.ax1x.com/2021/06/28/RNNUgK.png)

#### Dictht哈希表

字典底层是通过哈希表实现的，源码：dict.h

~~~c
typedef struct dictht {
    dictEntry **table; //哈希表数组，初始大小为4
    unsigned long size; // hashtable size
    unsigned long sizemask;  //哈希表掩码，size -1
    unsigned long used; // hashtable 现有节点的数量
} dictht;
~~~

![](https://z3.ax1x.com/2021/06/28/RNtHXD.png)

#### 哈希桶

哈希表中的table数组存放着哈希桶结构dicEntry，里面就是键值对，当发生哈希冲突时使用拉链法解决冲突，数据结构源码：dict.h

~~~c
typedef struct dictEntry {
    void *key; //键定义
    // 值定义
    union {
        void *val;  //自定义类型
        uint64_t u64; //无符号整形
        int64_t s64; //有符号整形
        double d; //浮点型
    } v;
    struct dictEntry *next; //指向下一个哈希表节点
} dictEntry;
~~~

![](https://z3.ax1x.com/2021/06/28/RNNFBQ.png)

key是string类型，value是一个redisObject，源码：server.h

~~~c
typedef struct redisObject {
    unsigned type:4;  // 4 bit   string,list , set ,zset,hash ...
    unsigned encoding:4; // 4 bit
    unsigned lru:LRU_BITS; /* LRU time (relative to global lru_clock) or
                            * LFU data (least significant 8 bits frequency
                            * and most significant 16 bits access time).
                            *  3 byte
                            * */
    int refcount;  // 4 byte   int 4 ,   long 8
    void *ptr;  // -> [10000000]    8 byte  : 64 bit   num: long : 64 bit =  8 byte
} robj;
~~~

其中，ptr指针存放或指向真正的对象，通过下图可以清晰的了解几个数据结构之间的关系：

![](https://z3.ax1x.com/2021/06/28/RNadTH.png)

关于SDS可以阅读这篇文章：https://doocs.gitee.io/source-code-hunter/#/docs/Redis/redis-sds