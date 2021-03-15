## rmi协议

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

## http协议

传输是基于jsonrpc4j框架实现的，它是Google的轻量级rpc框架。所以需要找到对应的依赖。

到github上找到对应版本，因为我的项目使用的是2.7.8的，所以找到该版本对应的项目，在当前项目下搜索jsonrpc4j。对应版本是1.2.0，不支持很新的版本，依赖如下

~~~xml
        <dependency>
            <groupId>com.github.briandilley.jsonrpc4j</groupId>
            <artifactId>jsonrpc4j</artifactId>
            <version>1.2.0</version>
        </dependency>
~~~

