## RPC协议

在一个典型的RPC使用场景中，包含了服务发现、负载、容错、网络传输、序列化等组件，其中RPC协议就指明了程序如何进行**网络传输**和**序列化**。也就是说一个RPC协议的实现就等于一个非透明的远程调用实现。

通过下面一张图来理解：

![](https://s3.ax1x.com/2021/03/17/6c1HzT.png)

**RPC协议组成**

![](https://s3.ax1x.com/2021/03/17/6c3SF1.png)

分别说下每个部分的作用：

1.地址：服务提供者的地址和端口

2.运行服务：用于网络传输实现，常用的服务有：

* netty
* mina
* RMI服务
* servlet容器（jetty、Tomcat、Jboss)

3.报文编码：协议报文编码，分为请求头和请求体两部分

4.序列化方式：将对象序列化成二进制流，反序列化则相反，常用的有：

* Hessian2Serialization
* DubboSerialization
* JavaSerialization
* JsonSerialization

### dubbo支持的协议

| 名称    | 实现描述                                                     | 连接描述                                  | 适用场景                                                     |
| ------- | ------------------------------------------------------------ | ----------------------------------------- | ------------------------------------------------------------ |
| dubbo   | 传输服务：netty(默认)，mina；序列化：hessian2(默认)，java，fastjson；报文：自定义报文 | 单个长连接，NIO异步传输                   | 1.常规RPC调用 ；2.传输数据量小；3.提供者少于消费者           |
| rmi     | 传输：java rmi服务；序列化：java原生二进制序列化             | 多个短连接，BIO同步传输                   | 1.常规RPC调用；2.与原RMI客户端继承；3.可传少量文件；4.不防火墙穿透 |
| hessian | 传输服务：servlet容器 ；序列化：hessian⼆进制序列化          | 基于Http 协 议传输， 依懒servlet容 器配置 | 1、提供者多于消费者 2、可传⼤字段和⽂件                      |
| http    | 传输服务：servlet容器 序列化：java原⽣⼆进制序列化           | 依懒servlet容 器配置                      | 1、数据包⼤⼩混合                                            |
| thrift  | 与thrift RPC 实现集成，并在其基础 上修改了报⽂头             | ⻓连接、NIO 异步传输                      |                                                              |

### dubbo协议

概念 

Dubbo协议是专⻔RPC远程调⽤所设计的协议，也是Dubbo默认协议，它是⼀种⼆进协议，特 性是：体积⼩，编解码速度快

**协议报⽂** 

以下是Dubbo 协议的报⽂

![](https://z3.ax1x.com/2021/03/20/6ho7TI.png)

* magic：类似java字节码⽂件⾥的魔数，⽤来判断是不是dubbo协议的数据包。魔数是常 量0xdabb,⽤于判断报⽂的开始。
*  flag：标志位, ⼀共8个地址位。低四位⽤来表示消息体数据⽤的序列化⼯具的类型（默认 hessian），⾼四位中，第⼀位为1表示是request请求，第⼆位为1表示双向传输（即有返 回response），第三位为1表示是⼼跳ping事件。
*  status：状态位, 设置请求响应状态，dubbo定义了⼀些响应的类型。具体类型⻅ com.alibaba.dubbo.remoting.exchange.Response 
*  invoke id：消息id, long 类型。每⼀个请求的唯⼀识别id（由于采⽤异步通讯的⽅式，⽤ 来把请求request和返回的response对应上） 
*  body length：消息体Body ⻓度, Int 类型，即记录Body Content有多少个字节。

### 运行服务

每个协议的使用都必须有一个运行服务器。dubbo同时支持Netty和minia，默认是Netty

~~~properties
dubbo.ptotocol.server=netty
~~~



### rmi协议

RMI协议是原生的JAVA远程调用协议，其特性如下：

* 运行服务：JAVA基于Socket自身实现
* 连接方式：短连接，BIO同步传输
* 序列化：JAVA原生，不支持其他序列化框架
* 其他特性：**防火墙穿透**

RMI协议是不支持防火墙穿透的：原因在于RMO底层实现中会有两个端口，一个固定的用于服务发现的注册端口，另外会生成一个**随机**端口用于网络传输，因此不容易穿过防火墙，所以存在防火墙穿透问题。

在服务端配置文件加上

~~~properties
#rmi协议
dubbo.protocols.rmi.name=rmi
dubbo.protocols.rmi.id=rmi
dubbo.protocols.rmi.port=9999
~~~

为了让dubbo和rmi区分开来，给dubbo协议配置一个id

~~~properties
dubbo.protocol.id=dubbo
~~~

在@DubboService注解中配置

~~~properties
@DubboService(group = "${server.member.group}",protocol = {"rmi","dubbo"},
        methods = {@Method(name = "findUsersByLabel",loadbalance = "consistenthash")})
~~~

使用protocol同时使用dubbo和rmi协议。

启动服务，查看zk，可以发现多了一个rmi服务

![](https://s3.ax1x.com/2021/03/14/6Bk26g.png)

这时客户端发起调用的话，使用dubbo协议的服务和rmi协议的服务都有。

客户端指定rmi协议

~~~java
    @DubboReference(group = "${server.member.group}",timeout = 5000,protocol = "rmi",methods = {@Method(name = "getUser",timeout = 5000)})
    private UserService userService;
~~~

这时候在AbstractClusterInvoke的负载均衡算法选择方法debug

~~~java
Invoker<T> invoker = loadbalance.select(invokers, getUrl(), invocation);
~~~

因为使用的是默认的随机负载均衡，所以会进入RandomLoadBalance中

![](https://s3.ax1x.com/2021/03/14/6BZVHJ.png)

此时可以看到调用的服务都是采用rmi协议的服务。

### http协议

传输是基于jsonrpc4j框架实现的，它是Google的轻量级rpc框架。所以需要找到对应的依赖。

到github上找到对应版本，因为我的项目使用的是2.7.8的，所以找到该版本对应的项目，在当前项目下搜索jsonrpc4j。对应版本是1.2.0，不支持很新的版本，依赖如下

~~~xml
        <dependency>
            <groupId>com.github.briandilley.jsonrpc4j</groupId>
            <artifactId>jsonrpc4j</artifactId>
            <version>1.2.0</version>
        </dependency>
~~~

jsonrpc4j依赖于jetty实现，所以还要引入jetty依赖，注意是jetty-servlet依赖

~~~xml
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
            <version>9.4.11.v20180605</version>
        </dependency>
~~~

服务端配置和rmi一样，就是换了个名字。然后启动客户端查看zk上面的节点：

![](https://s3.ax1x.com/2021/03/17/6cuZo6.png)

可以看到在提供者下面新增了一个http服务。

## 序列化

dubbo默认使用的是Hessian2序列化。

这里说下java的序列化和hessian2序列化有什么区别：

1.比如java先将一个user对象写进一个文件，再读取出来，这时候是没问题的，但是现在在原来的user对象中加一个属性，那么再读取的时候就会报两次序列化的id不一样。但是使用hessian2读取是没有问题的

2.从文件体积来说，hessian2也比java的序列化体积小

3.原来user对象中有个age属性是Integer类型，现在改为String，是能够正常读取的。但是如果将它改为成Date类型，那么就会报错，也就是说hessian2会最大可能的保持兼容性。

4枚举类型。在枚举中新加属性是不会报错的，但是去掉一个属性就是报错

## 线程池

dubbo支持四种线程池，如下图所示：

![](https://s4.ax1x.com/2021/03/19/6fa1C8.png)

需要注意这个ThreadPool并不是JDK实现的线程池看，而是dubbo自己实现的。

~~~java
@SPI("fixed")
public interface ThreadPool {

    /**
     * Thread pool
     *
     * @param url URL contains thread parameter
     * @return thread pool
     */
    @Adaptive({THREADPOOL_KEY})
    Executor getExecutor(URL url);

}
~~~



#### FixedThreadPool

固定大小的线程池，也是dubbo默认的线程池。核心线程数=线程池大小。源码：

~~~java
public class FixedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(THREAD_NAME_KEY, DEFAULT_THREAD_NAME);
        //线程池大小和核心线程
        int threads = url.getParameter(THREADS_KEY, DEFAULT_THREADS);
        int queues = url.getParameter(QUEUES_KEY, DEFAULT_QUEUES);
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedInternalThreadFactory(name, true), new AbortPolicyWithReport(name, url));
    }

}
~~~

配置。**线程池都是在服务端配置的，因为服务端提供服务**

~~~properties
#默认是固定线程池
dubbo.protocol.threadpool=fixed
#线程池大小
dubbo.protocol.threads=10
~~~

dubug启动服务端，然后再dubug启动客户端，这时候可以查看服务端线程：

![](https://s4.ax1x.com/2021/03/19/6fdoF0.png)

默认开启一个线程。这时候客户端调用服务10次：

![](https://s4.ax1x.com/2021/03/19/6fwilD.png)

可以看到会创建10个线程，当再调用服务的时候，就不会再创建线程。

#### CachedThreadPool

可缓存的线程池，和JDK机制是一样的，当超过核心线程后就不再扩充。

~~~java
public class CachedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(THREAD_NAME_KEY, DEFAULT_THREAD_NAME);
        //核心线程数
        int cores = url.getParameter(CORE_THREADS_KEY, DEFAULT_CORE_THREADS);
        //线程池大小
        int threads = url.getParameter(THREADS_KEY, Integer.MAX_VALUE);
        //队列
        int queues = url.getParameter(QUEUES_KEY, DEFAULT_QUEUES);
        //非核心线程的存活时间，默认6000ms，也就是60s
        int alive = url.getParameter(ALIVE_KEY, DEFAULT_ALIVE);
        return new ThreadPoolExecutor(cores, threads, alive, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedInternalThreadFactory(name, true), new AbortPolicyWithReport(name, url));
    }
}
~~~

配置

~~~properties
#缓存线程池
dubbo.protocol.threadpool=cached
#核心线程数
dubbo.protocol.corethreads=5
#也可以配置队列
dubbo.protocol.queues=10
#最大线程池，超出后采取拒绝策略
dubbo.protocol.threads=100
~~~

客户端发起5次请求，会创建5个线程，再发起请求，不会再创建线程

![](https://s4.ax1x.com/2021/03/19/6f0dUI.png)

扩充顺序和JDK中的缓存线程池是一样的，先是核心线程，然后是队列，最后是线程池，如果超过最大线程池大小就采取拒绝策略。

#### LimitedThreadPool

可伸缩的线程池，和cachedThreadPool唯一不同的是非核心线程的存活时间几乎是永久的

~~~java
/**
 * Creates a thread pool that creates new threads as needed until limits reaches. This thread pool will not shrink
 * automatically.
 */
public class LimitedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(THREAD_NAME_KEY, DEFAULT_THREAD_NAME);
        int cores = url.getParameter(CORE_THREADS_KEY, DEFAULT_CORE_THREADS);
        int threads = url.getParameter(THREADS_KEY, DEFAULT_THREADS);
        int queues = url.getParameter(QUEUES_KEY, DEFAULT_QUEUES);
        //可以看到keepAliveTime相当于永久
        return new ThreadPoolExecutor(cores, threads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedInternalThreadFactory(name, true), new AbortPolicyWithReport(name, url));
    }

}

~~~

#### EagerThreadPool

自定义线程池，和其他线程池不一样的地方在于它的扩充顺序：**当核心线程都处于繁忙状态时，它会去创建新线程，而不是将任务放入阻塞队列**。

假如现在有110个请求，核心线程池是10个，队列大小是100个，如果是其他线程池，那么此时的并发是10个，因为剩余的100个请求会被放入到阻塞队列。但如果是EagerThreadPool，那么此时的并发就是110，它会先去创建100个线程来执行任务，而不是将他们放入阻塞队列。

源码：

~~~java
/**
 * EagerThreadPool
 * When the core threads are all in busy,
 * create new thread instead of putting task into blocking queue.
 */
public class EagerThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(THREAD_NAME_KEY, DEFAULT_THREAD_NAME);
        int cores = url.getParameter(CORE_THREADS_KEY, DEFAULT_CORE_THREADS);
        int threads = url.getParameter(THREADS_KEY, Integer.MAX_VALUE);
        int queues = url.getParameter(QUEUES_KEY, DEFAULT_QUEUES);
        int alive = url.getParameter(ALIVE_KEY, DEFAULT_ALIVE);

        // init queue and executor
        TaskQueue<Runnable> taskQueue = new TaskQueue<Runnable>(queues <= 0 ? 1 : queues);
        EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
                threads,
                alive,
                TimeUnit.MILLISECONDS,
                taskQueue,
                new NamedInternalThreadFactory(name, true),
                new AbortPolicyWithReport(name, url));
        taskQueue.setExecutor(executor);
        return executor;
    }
}
~~~

#### IO线程

它不属于业务线程，一般不去配置，要配置的话将它配置为cpu的个数+1即可。