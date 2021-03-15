## redis注册中心

#### 服务发布

/dubbo/{服务名}/providers {提供者url1}: {有效期} {提供者url2}: {有效期}

#### 服务订阅

/dubbo/{服务名}/consumers {订阅者url1}: {有效期} {订阅者url2}: {有效期}

⼀个注册中⼼存储了多个服务、每个服务对应多个提供者和订阅者。每个服务的提供者的URL 作为接⼝map中的⼀个Key，与之对应的value 就是该提供者的有效期。有效期通常只有30 秒，dubbo通过⼀个维护线程，每隔30更新该时间。

![](https://s3.ax1x.com/2021/03/07/6MVePK.png)

#### 发布/订阅流程

Dubbo使⽤Redis的发布订阅特性实现 提供者与消费者之前数据实时同步其原理如下： Redis 发布订阅(pub/sub)是一种消息通信模式：发送者(pub)发送消息，订阅者(sub)接收消息。 Redis 客户端可以订阅任意数量的频道。 下图展示了频道 channel1 ， 以及订阅这个频道的三个客户端 —— client2 、 client5 和 client1 之间的关 系：

![](https://s3.ax1x.com/2021/03/07/6MVbRO.png)

下面演示下这个过程

~~~shell
#创建一个channel,就是key
127.0.0.1:6379> set name lk
OK
#订阅该channel，此时客户端处于阻塞状态，等待接收消息
127.0.0.1:6379> subscribe name
Reading messages... (press Ctrl-C to quit)
1) "subscribe"
2) "name"
3) (integer) 1

#打开一个新的客户端
127.0.0.1:6379> publish name cxylk
(integer) 1

#此时第一个客户端就会收到该信息
1) "message"
2) "name"
3) "cxylk"

~~~

#### 发布与订阅流程 

在提供者和消费者上线上时，分别会进⾏发布与订阅事件，其主体流程如下: 

1.消费端 

​	a. 启动：注册消费者信息 

​	b. 启动：启动⼀个阻塞线程，订阅(subscribe)该接⼝事件 

​	c. 停⽌：删除接⼝消费信息 

​	d. 停⽌：停⽌订阅线程 

2.提供端 

​	a. 启动：注册提供者信息 

​	b. 启动：推送(publish) 接⼝注册事件 

​	c. 停⽌：删除接⼝提供者信息 

​	d. 停⽌：推送(publish) 接⼝注销事件 

相关源码： org.apache.dubbo.registry.redis.RedisRegistry#doSubscribe// 订阅 org.apache.dubbo.registry.redis.RedisRegistry.Notifier#run// 订阅线程，阻塞读取 org.apache.dubbo.registry.redis.RedisRegistry.NotifySub#onMessage// 变更通知 org.apache.dubbo.registry.redis.RedisRegistry#doRegister // 注册 org.apache.dubbo.registry.redis.RedisRegistry#destroy // 消费



## zookeeper注册中心

Zookeper是⼀个树型的⽬录服务，本身⽀持变更推送相⽐redis的实现Publish/Subscribe 功能更稳定。

#### 存储结构

zookeeper 存储结构是采⽤树级⽬录形式，其层级分别是 dubbo\接⼝名\提供者、消费者\URL. 都是以节点名称的形式存储。其中的URL作为临时节点 存在，当应⽤与Zokkepper会话断开后， 其会被⾃动删除。

![](https://s3.ax1x.com/2021/03/07/6MZKYV.png)

启动两个服务，通过prettyzoo可视化界面可以看到当前zk的节点信息

第一个提供者：

![](https://s3.ax1x.com/2021/03/07/6Kv6XT.png)

第二个提供者：

![](https://s3.ax1x.com/2021/03/07/6KvR74.png)

通过ephermeralOwner可以看出这两个节点是**临时节点**，key就是该服务的url，key就是url，通过解码可以得到该服务的各种信息，而value就是ip。

另外，临时节点下面是不能有节点的。

#### 会话断开

* 主动断开：当正常停止服务后，当前临时节点会被立即删除。
* 宕机断开：比如使用kill命令杀死当前进程，**那么当前临时节点并不会被立即删除，而是在超时40s（可设置）后才会被删除**。

#### zk节点监听

进入zk客户端zkCli.sh。

查看当前节点状态

~~~shell
[zk: localhost:2181(CONNECTED) 1] ls /
[dubbo, zookeeper]
[zk: localhost:2181(CONNECTED) 2] 
~~~

创建一个非临时节点

~~~shell
[zk: localhost:2181(CONNECTED) 2] create /names 1
Created /names
[zk: localhost:2181(CONNECTED) 3] ls /
[dubbo, names, zookeeper]
#查询并开启监听模式
[zk: localhost:2181(CONNECTED) 4] ls /names watch
'ls path [watch]' has been deprecated. Please use 'ls [-w] path' instead.
[]
[zk: localhost:2181(CONNECTED) 5] 

~~~

打开一个新客户端，创建临时节点

~~~shell
#-e表示临时节点
[zk: localhost:2181(CONNECTED) 2] create -e /names/lk 11
Created /names/lk
~~~

此时第一个客户端状态

~~~shell
[zk: localhost:2181(CONNECTED) 5] 
WATCHER::

WatchedEvent state:SyncConnected type:NodeChildrenChanged path:/names

#按下enter键，并不会阻塞
[zk: localhost:2181(CONNECTED) 5] 
~~~

此时在创建一个临时节点

~~~shell
[zk: localhost:2181(CONNECTED) 3] create -e /names/lk1 111
Created /names/lk1
~~~

就不会再监听了，需要重新watch

#### 订阅与发布

dubbo正是利用zk的监听机制，去监听/dubbo/{接口名}/providers的子节点信息，从而达到订阅发布的目的。流程如下

1.消费端

​	a.启动：注册消费者信息(创建临时节点)

​	b.启动：订阅提供者信息(添加providers子节点监听事件)

​	c.触发订阅：更新提供者列表，**重新订阅**

​	d.停止：注销消费者信息（删除临时节点），取消订阅

2.提供端

​	a.启动：注册提供者信息(创建临时节点)

​	b.停止：注销提供者信息(删除临时节点)

#### zk比较redis的优势

1.不会产生脏数据

2.redis需要每隔30s进行一次大查询

3.redis每个服务都需要一个线程来监听数据(阻塞)

相关源码 

org.apache.dubbo.registry.zookeeper.ZookeeperRegistry#doRegister// 注册 org.apache.dubbo.registry.zookeeper.ZookeeperRegistry#doUnregister// 注销 org.apache.dubbo.registry.zookeeper.ZookeeperRegistry#doSubscribe// 订阅 org.apache.dubbo.registry.zookeeper.ZookeeperRegistry#doUnregister // 取消订阅 注：doRegister注册包括消费者和提供者的信息。

#### spring一直报无法连接zk的错误解法方法

以前报这个错误，将服务端的超时时间调大就可以了，但有一次怎么调都没用，

debug进入CuratorZookeeperClient类

~~~java
public CuratorZookeeperClient(URL url) {
        super(url);
        try {
            //这里的超时时间默认是3000ms
            int timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_CONNECTION_TIMEOUT_MS);
            int sessionExpireMs = url.getParameter(ZK_SESSION_EXPIRE_KEY, DEFAULT_SESSION_TIMEOUT_MS);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString(url.getBackupAddress())
                    .retryPolicy(new RetryNTimes(1, 1000))
                    .connectionTimeoutMs(timeout)
                    .sessionTimeoutMs(sessionExpireMs);
            String authority = url.getAuthority();
            if (authority != null && authority.length() > 0) {
                builder = builder.authorization("digest", authority.getBytes());
            }
            client = builder.build();
            client.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(url));
            client.start();
            boolean connected = client.blockUntilConnected(timeout, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new IllegalStateException("zookeeper not connected");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
~~~

因为默认超时时间是3000ms，如果这期间连接不上就会报无法连接，所以要想办法将这个时间调大。

通过这个配置来调大超时时间

~~~properties
dubbo.config-center.timeout=10000
~~~

时间确实调大了，但还是没用，后来发现还有个注册中心时间，继续配置该时间

~~~properties
dubbo.registry.timeout=10000
~~~

但发现还是没用，想砸键盘的冲动都有了。。。

没办法，再配置个服务端的超时时间

~~~properties
dubbo.provider.timeout=10000
~~~

这下终于tm能连接成功了！真tm离谱。所以以后如果一直遇到无法连接zk的情况，三个时间全给它配置上。