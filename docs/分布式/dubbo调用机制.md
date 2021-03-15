## 同步调用



## 异步调用

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

## 并行调用

同时调用多个服务，返回一个结果，如果其中一个出现异常，继续往下调用，**直到最先一个成功返回为止**。

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

## 广播调用

和并行调用是相反的