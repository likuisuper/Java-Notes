## 远程调用机制

基本过程：客户端向服务端发送参数，并等待获取结果。如果调用过程出错则需要对异常进行处理。

![](https://s3.ax1x.com/2021/03/17/66xLA1.png)

dubbo默认是使用同步调用的，还支持异步调用、并行调用、广播调用。

### 同步调用

对远程接口方法调用就属于同步调用。

原理：向远程服务端发送参数后，整个线程将会阻塞，直到服务端将结果返回。

dubbo远程调用传输是由专门的IO线程(非阻塞)完成的，调用线程把结果传递给IO线程后，会构建一个CompletableFuture，并通过它阻塞当前线程去等待结果返回，当服务端返回结果后就会为CompletableFuture填充结果，并释放阻塞的调用线程。如果在设定的时间内服务端没有返回，就会触发超时异常。

![](https://s3.ax1x.com/2021/03/17/66zLrQ.png)

相关源码：

org.apache.dubbo.remoting.exchange.support.DefaultFuture// 结果回执 

org.apache.dubbo.rpc.protocol.AsyncToSyncInvoker // 异步转同步

### 异步调用

客户端配置

~~~java
    @DubboReference(group = "${server.member.group}",methods = {@Method(name = "getUser",async=true,timeout = 5000)})
    private UserService userService;
~~~

即加上async=true开启异步调用

~~~java
public User getUser(Integer id){
        long start = System.currentTimeMillis();
        userService.getUser(id);
        //调用方法后，会将结果填充到future(setFuture)，所以要立即获取。不能再调用一个方法然后拿回执，是拿不到最开始调用方法的回执的
        //是根据requestId来获取defaultFuture的
        Future<User> future1 = RpcContext.getContext().getFuture();
        userService.getUser(id);
        Future<User> future2 = RpcContext.getContext().getFuture();
        userService.getUser(id);
        Future<User> future3 = RpcContext.getContext().getFuture();
        User user=null;
        try {
            user=future1.get();
            System.out.println("future1:"+user);
            user=future2.get();
            System.out.println("future2:"+user);
            user=future3.get();
            System.out.println("future3:"+user);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时："+(end-start));
        return user;
    }
~~~

异步调用通过RpcContext来获取当前调用的结果回执，然后阻塞获取结果。

客户端发起调用

~~~java
future1:User{id=1, name='lk', age=22, birthday='1998', desc='当前服务:李四'}
future2:User{id=1, name='lk', age=22, birthday='1998', desc='当前服务:张三'}
future3:User{id=1, name='lk', age=22, birthday='1998', desc='当前服务:张三'}
耗时：2003
~~~

服务端配置当前服务睡眠2s，三个服务就是6s，但是异步调用会在2s的时间调用完3个服务并返回结果。

如果将async改为false,那么返回结果就是：

~~~java
future1:User{id=1, name='lk', age=22, birthday='1998', desc='当前服务:张三'}
future2:User{id=1, name='lk', age=22, birthday='1998', desc='当前服务:张三'}
future3:User{id=1, name='lk', age=22, birthday='1998', desc='当前服务:李四'}
耗时：6075
~~~

可以看到耗时为调用每个服务所花费的时间之和。

异步调用和同步调用对比：

![](https://s3.ax1x.com/2021/03/17/6cSrZj.png)

**实现原理**：其实dubbo的调用本身就是异步的，其常规的调用是通过AsyncToSyncInvoker组件，由异步转成了同步。所以异步的实现就是让该组件不去执行阻塞逻辑即可。此外为了顺利拿到结果回执(Future)，在调用发起之后其回执会被填充到RpcContext中。

![](https://s3.ax1x.com/2021/03/17/6cpfht.png)

### 并行调用

为了尽可能获得更高的性能，以及最高级别的保证服务的可用性。面对多个服务，并不知道哪个处理更快。这时客户端可并行发起多个调用，只要其中一个成功返回，其他出现异常的将会被忽略，**只有所有服务出现异常情况才会判定调用出错。**。

配置

~~~properties
dubbo.consumer.cluster=forking
~~~

客户端配置

~~~java
    @DubboReference(group = "${server.member.group}",methods = {@Method(name = "getUser",timeout = 5000)})
    private UserService userService;
~~~

客户端调用服务，莫名其妙会报空指针异常，异常出现在AbstractCluster.java类中。通过debug来看下具体原因。

1.具体报错方法：

![](https://s3.ax1x.com/2021/03/14/60RV8H.png)

invocation里面是调用方法的一些信息，比如方法名，参数，返回结果等。

报错是因为asyncResult为null，所以在下面用它来调用方法就会出现空指针异常，而asyncResult是通过interceptor.intercept方法返回的，所以进入该方法：

2.AbstractClusterInvoker.java的invoke方法：

~~~java
    @Override
    public Result invoke(final Invocation invocation) throws RpcException {
        checkWhetherDestroyed();

        // binding attachments into invocation.
        Map<String, Object> contextAttachments = RpcContext.getContext().getObjectAttachments();
        if (contextAttachments != null && contextAttachments.size() != 0) {
            ((RpcInvocation) invocation).addObjectAttachments(contextAttachments);
        }

        List<Invoker<T>> invokers = list(invocation);
        LoadBalance loadbalance = initLoadBalance(invokers, invocation);
        RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
        return doInvoke(invocation, invokers, loadbalance);
    }
~~~

locadBalance默认是随机算法。

3.进入ForkingClusterInvoker.java的doInvoke方法，这是核心的调用实现。

~~~java
 public Result doInvoke(final Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        try {
            checkInvokers(invokers, invocation);
            final List<Invoker<T>> selected;//选择要调用的服务
            //并行数，默认是DEFAULT_FORKS=2
            final int forks = getUrl().getParameter(FORKS_KEY, DEFAULT_FORKS);
            //超时时间，这里是默认值DEFAULT_TIMEOUT=1000ms
            final int timeout = getUrl().getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
            //如果并行数<=0或者>=要调用的服务数，就从调用服务里面选择，很显然不走这里
            if (forks <= 0 || forks >= invokers.size()) {
                selected = invokers;
            } else {
                selected = new ArrayList<>(forks);
                while (selected.size() < forks) {
                    //根据负载均衡算法选择并行数个服务
                    Invoker<T> invoker = select(loadbalance, invocation, invokers, selected);
                    if (!selected.contains(invoker)) {
                        //Avoid add the same invoker several times.
                        selected.add(invoker);
                    }
                }
            }
            RpcContext.getContext().setInvokers((List) selected);
            final AtomicInteger count = new AtomicInteger();
            final BlockingQueue<Object> ref = new LinkedBlockingQueue<>();
            for (final Invoker<T> invoker : selected) {
                //这里为了实现并行调用，采用线程池
                executor.execute(() -> {
                    try {
                        //注意这里是同步调用而不是异步
                        Result result = invoker.invoke(invocation);
                        ref.offer(result);
                    } catch (Throwable e) {//只有当异常数量>=当前调用服务的数量，异常才会被填充到阻塞队列中，否则异常将会被忽略掉。
                        int value = count.incrementAndGet();
                        if (value >= selected.size()) {
                            ref.offer(e);
                        }
                    }
                });
            }
            try {
                Object ret = ref.poll(timeout, TimeUnit.MILLISECONDS);
                if (ret instanceof Throwable) {
                    Throwable e = (Throwable) ret;
                    throw new RpcException(e instanceof RpcException ? ((RpcException) e).getCode() : 0, "Failed to forking invoke provider " + selected + ", but no luck to perform the invocation. Last error is: " + e.getMessage(), e.getCause() != null ? e.getCause() : e);
                }
                return (Result) ret;
            } catch (InterruptedException e) {
                throw new RpcException("Failed to forking invoke provider " + selected + ", but no luck to perform the invocation. Last error is: " + e.getMessage(), e);
            }
        } finally {
            // clear attachments which is binding to current thread.
            RpcContext.getContext().clearAttachments();
        }
    }
~~~

需要注意下这行代码：

~~~java
Result result = invoker.invoke(invocation);
~~~

这里不是异步调用，如果是异步调用的话这里需要使用Future来接收返回值。**这也是为什么并行调用不能和异步调用同时使用，使用异步的话这里是获取不到返回结果的**

然后看一些下面这张图：

![](https://s3.ax1x.com/2021/03/14/605LUx.png)

上面这部分是不会出错的，因为我们配置了5000ms的超时时间，那么在这5000ms内，就是将调用的返回结果填充到阻塞队列中。

而下面这部分从阻塞队列获取结果即ref.poll的代码使用的参数却是上面的默认超时时间1000ms，也就是说上面代码还没有将调用结果放入阻塞队列，下面代码就从队列中获取，那么结果当然为空。可以看到ret是null。

此时就不会走下面的if语句，直接返回null。然后在AbstractCluster中就会抛出空指针异常。

这应该算是dubbo的一个bug，这里需要抛出的是超时异常而不是空指针异常。并且超时时间也不应该是默认的1000ms。

解决办法：

~~~java
    @DubboReference(group = "${server.member.group}",timeout=5000,methods = {@Method(name = "getUser",timeout = 5000)})
    private UserService userService;
~~~

在外层配置超时时间。按dubbo的设计来说，Method注解的配置应该是最高级别的，但是这里却要在外面配置超时时间，很奇怪。

这个时候客户端再发起调用，就会返回调用最先成功的结果，**它并不会像异步调用一样返回多个结果，而是谁最先成功就返回谁。但是它调用所花费的时间和异步调用是一样的**

**原理**：通过上面的源代码可以看出并行调用的实现原理，它是通过线程池异步发送远程请求，流程如下：

1.根据forks(并行数量)挑选出服务节点；

2.基于线程池(ExecutorService)并行发起远程调用

3.基于阻塞队列(BlockingQueue)等待结果返回

4.第一个结果返回，填充阻塞队列，并释放线程

### 广播调用

广播调用一次调用，会遍历所有服务提供者并发起调用，任意一台报错就算失败。**确保所有节点都被调用到**。

配置：

~~~properties
dubbo.consumer.cluster=broadcast
~~~

源码：org.apache.dubbo.rpc.cluster.support.BroadcastClusterInvoker 

~~~java
    public Result doInvoke(final Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        checkInvokers(invokers, invocation);
        RpcContext.getContext().setInvokers((List) invokers);
        RpcException exception = null;
        Result result = null;
        //循环调用服务提供者
        for (Invoker<T> invoker : invokers) {
            try {
                //返回结果，同步调用
                result = invoker.invoke(invocation);
            } catch (RpcException e) {
                exception = e;
                logger.warn(e.getMessage(), e);
            } catch (Throwable e) {
                exception = new RpcException(e.getMessage(), e);
                logger.warn(e.getMessage(), e);
            }
        }
        if (exception != null) {
            throw exception;
        }
        return result;
    }
~~~

**原理**：用一个循环遍历所有提供者，然后顺序**同步**发起调用

## 集群容错

在调用过程中，如果出现错误，框架会对其进行补救措施称为容错。这里的容错是指**除业务异常外的所有异常**。

![](https://s3.ax1x.com/2021/03/17/6cFWS1.png)

异常类型定义在RpcException中

~~~java
    public static final int UNKNOWN_EXCEPTION = 0;
    public static final int NETWORK_EXCEPTION = 1;
    public static final int TIMEOUT_EXCEPTION = 2;
    public static final int FORBIDDEN_EXCEPTION = 4;
    public static final int SERIALIZATION_EXCEPTION = 5;
    public static final int NO_INVOKER_AVAILABLE_AFTER_FILTER = 6;
    public static final int LIMIT_EXCEEDED_EXCEPTION = 7;
    public static final int TIMEOUT_TERMINATE = 8;
~~~

其中

~~~java
    public static final int BIZ_EXCEPTION = 3;
~~~

是业务异常。

### 容错策略

dubbo支持4中容错策略

**1.失败自动切换：**调用失败会基于`retries`属性重试其他服务器，这是**默认的容错机制**，重试默认次数为2。加上最开始调用的一次，相当于一共调用3次

**2.快速失败：**快速失败，只发起一次调用，失败立即报错。通常用于非幂等写入

**3.忽略失败：**失败后忽略，不抛出异常给客户端，并且返回一个空，常用于不重要的接口调用，比如记录日志。

**4.失败重试：**失败时记录失败请求并安排定期重发。通常用于消息通知操作

设置使用：

~~~properties
<!--
2 Failover 失败自动切换 retries="2" 切换次数
3 Failfast 快速失败
4 Failsafe 勿略失败,返回一个null
5 Failback 失败重试，5秒后仅重试一次
6 -->
7 #设置方式支持如下两种方式设置，优先级由低至高
8 <dubbo:service interface="..." cluster="broadcast" />
9 <dubbo:reference interface="..." cluster="broadcast"/ >
~~~

相关源码：

org.apache.dubbo.rpc.cluster.support.FailoverClusterInvoker// 失败自动切换org.apache.dubbo.rpc.cluster.support.FailfastClusterInvoker // 快速失败 org.apache.dubbo.rpc.cluster.support.FailsafeClusterInvoker // 勿略失败org.apache.dubbo.rpc.cluster.support.FailbackClusterInvoker //失败重试