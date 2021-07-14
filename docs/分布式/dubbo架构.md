## 基本架构

官方介绍：

![](https://z3.ax1x.com/2021/07/14/WZbkCT.png)

| 节点        | 角色说明                               |
| ----------- | -------------------------------------- |
| `Provider`  | 暴露服务的服务提供方                   |
| `Consumer`  | 调用远程服务的服务消费方               |
| `Registry`  | 服务注册与发现的注册中心               |
| `Monitor`   | 统计服务的调用次数和调用时间的监控中心 |
| `Container` | 服务运行容器                           |

可以发现，不管是哪个节点，其中都出现了服务这个词，那么在dubbo中，一个服务是什么？怎么开启一个服务？我们通过下面这段代码来了解：

### 单机

#### 提供服务

~~~java
 public void openService() throws IOException {
        //服务设置
        ServiceConfig serviceConfig = new ServiceConfig();

        //1.应用
        //指定服务名称，否则会报错
        ApplicationConfig app = new ApplicationConfig("lk-server");
        serviceConfig.setApplication(app);

        //2.协议
        ProtocolConfig protocolConfig = new ProtocolConfig("dubbo");//dubbo是二进制、全双工的应用协议
        //指定端口
        protocolConfig.setPort(8080);
        serviceConfig.setProtocol(protocolConfig);
        //注册中心(集群时才会用到)这里不是集群，所以设置为空，不然会报错
        RegistryConfig registryConfig = new RegistryConfig(RegistryConfig.NO_AVAILABLE);
        serviceConfig.setRegistry(registryConfig);

        //3.接口
        serviceConfig.setInterface(UserService.class);

        //4.实现
        serviceConfig.setRef(new UserServiceImpl(8080));

        //开启服务
        serviceConfig.export();

        //不让主线程关闭，否则客服端获取不到
        System.in.read();
    }
~~~

#### 消费服务

~~~java
    public void invokeRemote() {
        //引用服务
        ReferenceConfig<UserService> referenceConfig = new ReferenceConfig();

        //1.应用
        ApplicationConfig app = new ApplicationConfig("lk-client");
        referenceConfig.setApplication(app);

        //2.URL地址
        referenceConfig.setUrl("dubbo://127.0.0.1:8080/cxylk.dubbo.UserService");

        //3.指定接口
        referenceConfig.setInterface(UserService.class);

        //4.获取接口服务(动态代理)
        UserService userService = referenceConfig.get();

        //先启动服务端，再启动客服端
        System.out.println(userService.getUser(111));
    }
~~~

需要注意，上面的注册中心是没有配置的，因为这里不是集群，没有必要配置，但又不能为空，否则会报错。

### 集群

#### 提供服务

```
//开启三个服务(三个进程，不要写在一个方法里)
    @Test
    public void openService1() throws IOException {
        openService(12345);
    }

    @Test
    public void openService2() throws IOException {
        openService(12346);
    }

    @Test
    public void openService3() throws IOException {
        openService(12347);
    }

    public void openService(int port) throws IOException {
        ServiceConfig serviceConfig = new ServiceConfig();

        //1.指定应用
        ApplicationConfig app = new ApplicationConfig("lk-server");
        serviceConfig.setApplication(app);

        //2.指定协议
        ProtocolConfig protocolConfig = new ProtocolConfig("dubbo");
        protocolConfig.setPort(port);
        serviceConfig.setProtocol(protocolConfig);

        //注册中心
        //指定为multicast(组网广播协议，不能写成localhost，使用这个就不用搭建zookeeper了),后面地址要在224.0.0.0 - 239.255.255.255之间
//        RegistryConfig registryConfig=new RegistryConfig("multicast://224.1.1.1:2223");
        RegistryConfig registryConfig = new RegistryConfig("zookeeper://192.168.63.128:2181");
//        registryConfig.setTimeout(10000);如果出现zookeeper无法连接，调大超时时间即可
        serviceConfig.setRegistry(registryConfig);

        //3.指定接口
        serviceConfig.setInterface(UserService.class);

        //4.指定实现
        serviceConfig.setRef(new UserServiceImpl(port));

        //开启服务
        serviceConfig.export();
        System.out.println("服务已开启:" + port);

        //不让主线程结束
        System.in.read();
    }
```

#### 消费服务

~~~java
 //客服端调用
    public static void main(String[] args) throws IOException {
        UserService userService = getClient();
        while (System.in.read() != 'q') {
            //不停发起调用
            System.out.println(userService.getUser(111));
        }
    }

    private static UserService getClient() {
        ReferenceConfig<UserService> referenceConfig = new ReferenceConfig();

        //应用
        ApplicationConfig app = new ApplicationConfig("lk-client");
        referenceConfig.setApplication(app);

        //地址
        RegistryConfig registryConfig = new RegistryConfig("zookeeper://192.168.63.128:2181");
        //连接不上，调大注册中心的超时时间即可
        registryConfig.setTimeout(30000);
        referenceConfig.setRegistry(registryConfig);

        //接口
        referenceConfig.setInterface(UserService.class);

        //获取接口服务
        return referenceConfig.get();
    }
~~~

