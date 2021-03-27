## 功能架构

首先通过一张图来了解整个功能架构

![](https://z3.ax1x.com/2021/03/26/6vXOjP.png)

下面是rpc的核心流程

![](https://z3.ax1x.com/2021/03/26/6vXxHS.png)

## 服务暴露

首先分析服务暴露的过程，通过源码来理解

通过协议的方式来暴露服务：

~~~java
    String urlText="dubbo://127.0.0.1:20880/cxylk.dubbo.UserService?timeout=6000";
    // 暴露服务测试
    @Test
    public void exportTest() throws IOException, IOException {
        DubboProtocol protocol=new DubboProtocol();
        ProxyFactory proxyFactory=new JdkProxyFactory();// 反射调用
        UserServiceImpl refImpl = new UserServiceImpl();// 实现类
        Invoker<UserService> invoker =
                proxyFactory.getInvoker(refImpl, UserService.class, URL.valueOf(urlText));
        Exporter<UserService> export = protocol.export(invoker);//
        ApplicationModel.getServiceRepository().registerService(UserService.class);

        System.out.println("服务暴露成功");
        System.in.read();
    }
~~~

#### export暴露服务

进入DubboProtocol类的export方法，也就是**暴露服务**。参数是invoker，这里必须要说明invoker和invocation的区别：

invoker是一个调用器，它通过方法invoke和传入的参数invocation来发起调用，而invocation是具体的一次调用，它包含了接口名，方法名，参数等等。可以把invoker理解成一个大炮，而invocation就是炮弹，大炮发射需要炮弹，而炮弹中包含了这个大炮的信息，这样它才知道需要被哪个大炮发射，invocation也是通过内部的invoker来找到具体的调用者。

下面是export的源码：

~~~java
 @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        //url就是测试代码中的Url
        URL url = invoker.getUrl();

        // export service.
        //key就是接口名+端口
        String key = serviceKey(url);
        //初始化dubboExporter中的invoker，key,exporterMap
        //exporterMap是一个concurrentHashMap,
        DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
        exporterMap.put(key, exporter);

        //export an stub service for dispatching event
        Boolean isStubSupportEvent = url.getParameter(STUB_EVENT_KEY, DEFAULT_STUB_EVENT);
        Boolean isCallbackservice = url.getParameter(IS_CALLBACK_SERVICE, false);
        if (isStubSupportEvent && !isCallbackservice) {
            String stubServiceMethods = url.getParameter(STUB_EVENT_METHODS_KEY);
            if (stubServiceMethods == null || stubServiceMethods.length() == 0) {
                if (logger.isWarnEnabled()) {
                    logger.warn(new IllegalStateException("consumer [" + url.getParameter(INTERFACE_KEY) +
                            "], has set stubproxy support event ,but no stub methods founded."));
                }

            }
        }

        //开启服务
        openServer(url);
        optimizeSerialization(url);

        return exporter;
    }
~~~

#### openServer开启服务

进入该类的openServer开启服务：

~~~java
private void openServer(URL url) {
        // find server.
    	//key=127.0.0.1:20880
        String key = url.getAddress();
        //client can export a service which's only for server to invoke
        boolean isServer = url.getParameter(IS_SERVER_KEY, true);
    	//判断是否是服务
        if (isServer) {
            ProtocolServer server = serverMap.get(key);
            if (server == null) {
                synchronized (this) {
                    server = serverMap.get(key);
                    if (server == null) {
                        //没有就创建服务，并且放入serverMap中
                        serverMap.put(key, createServer(url));
                    }
                }
            } else {
                // server supports reset, use together with override
                //如果服务已经被创建，那么重置
                server.reset(url);
            }
        }
    }
~~~

上面代码就是使用一个双重检测实现的，也就是单例模式。

#### 创建服务

```
private ProtocolServer createServer(URL url) {
    url = URLBuilder.from(url)
            // send readonly event when server closes, it's enabled by default
            .addParameterIfAbsent(CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString())
            // enable heartbeat by default
            .addParameterIfAbsent(HEARTBEAT_KEY, String.valueOf(DEFAULT_HEARTBEAT))
            .addParameter(CODEC_KEY, DubboCodec.NAME)
            .build();
    //获取运行服务，str=netty
    String str = url.getParameter(SERVER_KEY, DEFAULT_REMOTING_SERVER);

    if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
        throw new RpcException("Unsupported server type: " + str + ", url: " + url);
    }

    ExchangeServer server;
    try {
    	//最重要的地方，特别是requestHandler这个参数
        server = Exchangers.bind(url, requestHandler);
    } catch (RemotingException e) {
        throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
    }

    str = url.getParameter(CLIENT_KEY);
    if (str != null && str.length() > 0) {
        Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
        if (!supportedTypes.contains(str)) {
            throw new RpcException("Unsupported client type: " + str);
        }
    }

    return new DubboProtocolServer(server);
}
```

#### 开启NettyServer服务

下面的这行代码是最重要的，特别是其中的参数requestHandler，这个参数是当接收到请求后进行处理的。

~~~java
server = Exchangers.bind(url, requestHandler);
~~~

这个bind方法最后会开启netty服务，但是中间会经过很多层封装，我们直接跳到最终实现。

在Transportes中的bind方法会跳转到Transporter接口中的bind方法

~~~java
    public static RemotingServer bind(URL url, ChannelHandler... handlers) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handlers == null || handlers.length == 0) {
            throw new IllegalArgumentException("handlers == null");
        }
        ChannelHandler handler;
        if (handlers.length == 1) {
            handler = handlers[0];
        } else {
            handler = new ChannelHandlerDispatcher(handlers);
        }
        return getTransporter().bind(url, handler);
    }

~~~

bind方法其中的一个实现就是nettyTransporter(基于netty4)

~~~java
    @Override
    public RemotingServer bind(URL url, ChannelHandler handler) throws RemotingException {
        return new NettyServer(url, handler);
    }

    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        return new NettyClient(url, handler);
    }
~~~

**bind方法用于服务端调用的，而connect方法用于客户端进行连接**。当我们在bind方法中打个断点时，最终就会调用到这里，再往下就是关于netty的具体操作了，这里不再深入。

接下来看requestHandler这个参数：

```java
private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {

    @Override
    public CompletableFuture<Object> reply(ExchangeChannel channel, Object message) throws RemotingException {

        if (!(message instanceof Invocation)) {
            throw new RemotingException(channel, "Unsupported request: "
                    + (message == null ? null : (message.getClass().getName() + ": " + message))
                    + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
        }

        Invocation inv = (Invocation) message;
        Invoker<?> invoker = getInvoker(channel, inv);
        // need to consider backward-compatibility if it's a callback
        if (Boolean.TRUE.toString().equals(inv.getObjectAttachments().get(IS_CALLBACK_SERVICE_INVOKE))) {
            String methodsStr = invoker.getUrl().getParameters().get("methods");
            boolean hasMethod = false;
            if (methodsStr == null || !methodsStr.contains(",")) {
                hasMethod = inv.getMethodName().equals(methodsStr);
            } else {
                String[] methods = methodsStr.split(",");
                for (String method : methods) {
                    if (inv.getMethodName().equals(method)) {
                        hasMethod = true;
                        break;
                    }
                }
            }
            if (!hasMethod) {
                logger.warn(new IllegalStateException("The methodName " + inv.getMethodName()
                        + " not found in callback service interface ,invoke will be ignored."
                        + " please update the api interface. url is:"
                        + invoker.getUrl()) + " ,invocation is :" + inv);
                return null;
            }
        }
        RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
        //发起调用，调用reflmpl
        Result result = invoker.invoke(inv);
        return result.thenApply(Function.identity());
    }

    .....
};
```

**requestHandler就是一个回调，什么时候会回调呢？就是当nettyServer接收到客户端发送的请求时**。当执行到requestHandler时，编解码，IO这些操作已经实现了，所以我们不用去关心。当消息过来后，会传入一个invocation，有了这个invocation，就可以进行调用了

当我们发起一个请求调用：

~~~java
@Test
    public void invokeTest() {
        ReferenceConfig<UserService> referenceConfig=new ReferenceConfig();
        //  应用名称
        ApplicationConfig app=new ApplicationConfig("lk-client");
        referenceConfig.setApplication(app);
        // URL地址
        referenceConfig.setUrl(urlText);
        // 指定接口
        referenceConfig.setInterface(UserService.class);
        // 获取接口服务 （动态代理）
        UserService userService = referenceConfig.get();
        System.out.println(userService.getUser(1111));
    }
~~~

就是走到reply方法。

总结：

![](https://z3.ax1x.com/2021/03/26/6xiQ8U.png)

上面应该是创建并添加至serverMap

## 服务引用

服务引用需要做两件事情：

1.创建连接；2.创建代理对象

下面通过代码+源码分析这个过程

~~~java
    /**
     * 服务引用
     */
    @Test
    public void referTest(){
        DubboProtocol protocol=new DubboProtocol();
        ProxyFactory proxyFactory=new JdkProxyFactory();// 反射调用
        //对服务进行引用
        Invoker<UserService> invoker = protocol.refer(UserService.class, URL.valueOf(urlText));
        UserService proxy = proxyFactory.getProxy(invoker);
        System.out.println(proxy.getUser(111));
    }
~~~

#### 引用服务

通过protocol.refer来引用服务

~~~java
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return new AsyncToSyncInvoker<>(protocolBindingRefer(type, url));
    }
~~~

在该方法中会创建两个invoker:

1.AsyncToSyncInvoker：异步转同步

2.dubboInvoker,通过protocolBindingRefer得到

#### 创建invoker

在DubboProtocol类中的如下方法创建dubboInvoker

~~~java
    @Override
    public <T> Invoker<T> protocolBindingRefer(Class<T> serviceType, URL url) throws RpcException {
        optimizeSerialization(url);

        // create rpc invoker.
        DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
        invokers.add(invoker);

        return invoker;
    }
~~~

#### 获取连接

通过getClients来获取连接，还是在该类中：

~~~java
private ExchangeClient[] getClients(URL url) {
        // whether to share connection

        boolean useShareConnect = false;

    	//连接数，这里默认是0
        int connections = url.getParameter(CONNECTIONS_KEY, 0);
        List<ReferenceCountExchangeClient> shareClients = null;
        // if not configured, connection is shared, otherwise, one connection for one service
        if (connections == 0) {
            //使用共享连接
            useShareConnect = true;

            /*
             * The xml configuration should have a higher priority than properties.
             */
            String shareConnectionsStr = url.getParameter(SHARE_CONNECTIONS_KEY, (String) null);
            //此时这里变为1，默认值
            connections = Integer.parseInt(StringUtils.isBlank(shareConnectionsStr) ? ConfigUtils.getProperty(SHARE_CONNECTIONS_KEY,
                    DEFAULT_SHARE_CONNECTIONS) : shareConnectionsStr);
            //获取共享连接
            shareClients = getSharedClient(url, connections);
        }

    	//遍历连接
        ExchangeClient[] clients = new ExchangeClient[connections];
        for (int i = 0; i < clients.length; i++) {
            //如果是共享连接，那么上面的if语句会执行，useSharConnect=true,所以
            //此时这里就直接获取
            if (useShareConnect) {
                clients[i] = shareClients.get(i);

            } else {//否则初始连接
                clients[i] = initClient(url);
            }
        }

        return clients;
    }
~~~

上面提到了共享连接，这里解释下是什么意思：当有多个服务的时候，dubbo默认是让这多个服务使用一个连接的，**除非我们进行了配置，比如让connections=2**。这也是为什么dubbo协议不推荐传输大文件的原因，就是快进快出。如果一个服务传输的数据很大的时候，那么它就会长时间占用这个连接。

#### 获取共享连接

通过getSharedClient获取共享连接，方法很长，贴出最重要的部分：

~~~java
     List<ReferenceCountExchangeClient> clients = referenceClientMap.get(key);
	 ...
			if (CollectionUtils.isEmpty(clients)) {
                clients = buildReferenceCountExchangeClientList(url, connectNum);
                referenceClientMap.put(key, clients);

            } else {
                for (int i = 0; i < clients.size(); i++) {
                    ReferenceCountExchangeClient referenceCountExchangeClient = clients.get(i);
                    // If there is a client in the list that is no longer available, create a new one to replace him.
                    if (referenceCountExchangeClient == null || referenceCountExchangeClient.isClosed()) {
                        clients.set(i, buildReferenceCountExchangeClient(url));
                        continue;
                    }

                    referenceCountExchangeClient.incrementAndGetCount();
                }
            }
	...
~~~

上面代码就是判断是否存在共享连接，如果没存在，那么通过

~~~java
clients = buildReferenceCountExchangeClientList(url, connectNum);
~~~

来初始连接。然后放入**referenceClientMap**中。

#### 初始连接

无论是使用共享连接，还是没有使用，都需要初始化连接。

通过buildReferenceCountExchangeClientList方法，然后调用buildReferenceCountExchangeClient方法：

~~~java
    private ReferenceCountExchangeClient buildReferenceCountExchangeClient(URL url) {
        ExchangeClient exchangeClient = initClient(url);

        return new ReferenceCountExchangeClient(exchangeClient);
    }
~~~

其中**initClient**就是用来初始连接。

在这个方法中，会通过Exchangers.connect来开启NettyClient，我们看下堆栈信息

![](https://z3.ax1x.com/2021/03/27/6z12NT.png)

可以看到，从initClient到开启NettyClient，这期间经过了几层封装。

最后看下NettyTransporter中的方法：

~~~java
    @Override
    public RemotingServer bind(URL url, ChannelHandler handler) throws RemotingException {
        return new NettyServer(url, handler);
    }

    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        return new NettyClient(url, handler);
    }
~~~



bind方法我们在服务暴露的时候分析过来，而connect就是进行服务引用。一样的，再往下就是具体的netty操作，这里不再分析。

经过上面一系列的操作后，最终在getClients方法中就得到了一个client，并且返回出去被DubboInvoker所接收，**DubboInvoker就根据这个clients来进行调用**

~~~java
//dubboInvoker类中存放了clinets
private final ExchangeClient[] clients;
~~~

总结：将上面的步骤总结成如下图：

![](https://z3.ax1x.com/2021/03/27/6z3wa6.png)

## 总结

分析了服务暴露和服务引用后，通过下面这张图来加深理解：

![](https://z3.ax1x.com/2021/03/27/6z3hIf.png)