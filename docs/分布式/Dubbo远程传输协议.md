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

| 名称    | 实现描述                               | 连接描述 | 适用场景 |
| ------- | -------------------------------------- | -------- | -------- |
| dubbo   | 传输服务：netty(默认)，mina            |          |          |
| rmi     | 序列化：hessian2(默认)，java，fastJson |          |          |
| hessian |                                        |          |          |
| http    |                                        |          |          |
| thrift  |                                        |          |          |



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