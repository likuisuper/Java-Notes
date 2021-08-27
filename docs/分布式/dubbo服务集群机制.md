## 负载均衡

dubbo支持的几种负载均衡算法

![](https://s3.ax1x.com/2021/03/07/6Mr6De.png)

一共有5种：

| 算法名称   | 配置值             |
| ---------- | ------------------ |
| 随机+权重  | random             |
| 轮询+权重  | roundrobin         |
| 最短响应   | short est response |
| 最少连接   | least active       |
| 一致性哈希 | consistent hash    |

权重默认为100

~~~java
public interface Constants {
	...
    String WEIGHT_KEY = "weight";

    int DEFAULT_WEIGHT = 100;
}
~~~

#### 随机+权重

配置

~~~properties
dubbo.provide.loadbalance=random
~~~

然后服务配置相应权重即可

通过下面一张图来讲解该算法

![](https://s3.ax1x.com/2021/03/08/6lH236.png)

(1)首先计算总的权重

(2)在0和总权重之间得到一个随机数，假设这里为180

(3)随机权重与权重列表中的值遍历相减，如果小于0就返回，否则继续。比如180-100=80不小于0，那么下次就是80-200小于0，那么第一次请求就落到权重为200的服务上。

看一下源码实现：

~~~java
public class RandomLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "random";

    /**
     * Select one invoker between a list using a random criteria
     * @param invokers List of possible invokers
     * @param url URL
     * @param invocation Invocation
     * @param <T>
     * @return The selected invoker
     */
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // Number of invokers
        int length = invokers.size();
        // Every invoker has the same weight?
        boolean sameWeight = true;
        // the weight of every invokers(权重数组)
        int[] weights = new int[length];
        // the first invoker's weight
        int firstWeight = getWeight(invokers.get(0), invocation);
        weights[0] = firstWeight;
        // The sum of weights
        int totalWeight = firstWeight;
        for (int i = 1; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation);
            // save for later use
            weights[i] = weight;
            // Sum
            totalWeight += weight;
            if (sameWeight && weight != firstWeight) {
                sameWeight = false;
            }
        }
        //总权重不小于零并且权重不相同
        if (totalWeight > 0 && !sameWeight) {
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // Return a invoker based on the random value.
            for (int i = 0; i < length; i++) {
                //随机数减去列表中的权重值，然后设置随机数为减去后的值
                offset -= weights[i];
                //如果结果<0,直接返回
                if (offset < 0) {
                    return invokers.get(i);
                }
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }

}
~~~

**优势**

算法的优点是其简洁性，和实⽤性。它⽆需记录当前所有连接的状态，所以它是⼀种⽆状态调 度

**劣势**

不适⽤于请求服务时间变化⽐较⼤，或者每个请求所消耗的时间不⼀致的情况，此时随机权重 算法容易导致服务器间的负载不平衡

#### 轮询+权重

**这里的轮询并不是nginx中的顺序轮询，而是交叉轮询**

假设有三个服务，weight分别为100,200,300，如果是顺序轮询的话，当请求都落到weight为300的时候，其他两个服务就会处于闲置状态

下面演示一下这个过程，有很多需要注意的细节

首先开启三个服务：

第一个服务是张三，weight为100

![](https://s3.ax1x.com/2021/03/07/6My5nS.png)

第二个服务是李四，weight为200

![](https://s3.ax1x.com/2021/03/07/6MyjXT.png)

第三个服务是王五，weight为300

![](https://s3.ax1x.com/2021/03/07/6M6EjK.png)

在服务端配置文件中添加配置：

~~~properties
#轮询模式
dubbo.provider.loadbalance=roundrobin
~~~

可在启动界面中检查loadbalance是否为roundrobin

服务代码

~~~java
    @Value("${server.name}")
    private String name;


    public User getUser(Integer id) {
        User user = createUser(id);
        user.setDesc("当前服务:" + name);
        if(id==1){
            System.out.println("调用服务...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return user;
    }
~~~

在controller编写接口，调用该服务，此时访问服务的话，应该是以下面加粗字体顺序来调用

|       | 张三             | 李四             | 王五             |
| ----- | ---------------- | ---------------- | ---------------- |
| 第1次 | 100              | 200              | **300-600=-300** |
| 第2次 | 100+100=200      | **400-600=-200** | -300+300=0       |
| 第3次 | **300-600=-300** | 0                | 300              |
| 第4次 | -200             | 200              | **600-600=0**    |
| 第5次 | -100             | **400-600=-200** | 300              |
| 第6次 | 0                | 0                | **600-600=0**    |
| 第7次 | 100              | 200              | **300-600=-300** |

说明：总的权重为100+200+300=600

1.第一次的时候，王五的权重为300，此时请求王五，然后它的权重要减去600变成-300；

2.第二次的时候，张三的权重要加上设置的初始权重：100+100=200，李四的也一样变成400，王五变为0，这时候明显是李四的权重最大，所以请求落到李四，然后它要减去600，变为-200

3.第三次的时候，张三权重为200+100=300，李四为-200+200=0，王五为0+300=300，明显是张三权重最大，此时请求落到张三上，然后张三权重减去总权重600变为-300.

4.按上述方式递推。。。

但是实际调用的却不是这个顺序，因为有个东西没有设置，就是**预热时间**

**dubbo引入了预热的概念，其权重值会缓慢上升。直到预热结束，才会变成所设定的权重值**

在服务端配置文件添加

~~~properties
#默认是10分钟
dubbo.provider.warmup=0
~~~

此时重新启动服务，调用结果如下

~~~java
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:李四"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:张三"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:李四"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>
~~~

可以发现和表格中的顺序一致。

**注意下第三次请求出现的情况，此时张三和王五都是300，但是张三的启动顺序在王五前面，所以此时请求落到的是张三上面，而不是王五上面**

如果将启动顺序换一下，变成王五，李四，张三，此时表格的请求顺序如下

|       | 张三             | 李四             | 王五             |
| ----- | ---------------- | ---------------- | ---------------- |
| 第1次 | 100              | 200              | **300-600=-300** |
| 第2次 | 100+100=200      | **400-600=-200** | -300+300=0       |
| 第3次 | 300              | 0                | **300-600=-300** |
| 第4次 | **400-600=-200** | 200              | 0                |
| 第5次 | -100             | **400-600=-200** | 300              |
| 第6次 | 0                | 0                | **600-600=0**    |
| 第7次 | 100              | 200              | **300-600=-300** |

输出结果如下

~~~java
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:李四"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:张三"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:李四"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>curl 127.0.0.1:8080/getUser?id=2
{"id":2,"name":"lk","age":22,"birthday":"1998","desc":"当前服务:王五"}
D:\workspace\learn-dubbo>
~~~

**也就是说，当请求的服务权重一样的时候，将会按照启动顺序来决定请求应该落在哪个服务上面**。

下面剖析一下RoundRobinLoadBalance的源码

~~~java
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        ConcurrentMap<String, WeightedRoundRobin> map = methodWeightMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        //总的权重
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        Invoker<T> selectedInvoker = null;
        WeightedRoundRobin selectedWRR = null;
        //此时的invokers为3，因为启动了3个服务
        for (Invoker<T> invoker : invokers) {
            String identifyString = invoker.getUrl().toIdentityString();
            //当前权重
            int weight = getWeight(invoker, invocation);
            WeightedRoundRobin weightedRoundRobin = map.computeIfAbsent(identifyString, k -> {
                WeightedRoundRobin wrr = new WeightedRoundRobin();
                wrr.setWeight(weight);
                return wrr;
            });

            if (weight != weightedRoundRobin.getWeight()) {
                //weight changed
                weightedRoundRobin.setWeight(weight);
            }
            //increasecurrent是自增操作
            long cur = weightedRoundRobin.increaseCurrent();
            weightedRoundRobin.setLastUpdate(now);
            //该循环就是选取当前服务中最大的权重值，所以第一次权重就是300
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedInvoker = invoker;
                //选择权重
                selectedWRR = weightedRoundRobin;
            }
            //每循环一次，将加上当前权重，循环结束后该权重为100+200+300=600
            totalWeight += weight;
        }
        if (invokers.size() != map.size()) {
            map.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > RECYCLE_PERIOD);
        }
        if (selectedInvoker != null) {
            //当前权重-总的权重
            selectedWRR.sel(totalWeight);
            return selectedInvoker;
        }
        // should not happen here
        return invokers.get(0);
    }
~~~

看下计算权重的getWeight(invoker, invocation);方法

~~~java
int getWeight(Invoker<?> invoker, Invocation invocation) {
        int weight;
        URL url = invoker.getUrl();
        // Multiple registry scenario, load balance among multiple registries.
        if (REGISTRY_SERVICE_REFERENCE_PATH.equals(url.getServiceInterface())) {
            weight = url.getParameter(REGISTRY_KEY + "." + WEIGHT_KEY, DEFAULT_WEIGHT);
        } else {
            weight = url.getMethodParameter(invocation.getMethodName(), WEIGHT_KEY, DEFAULT_WEIGHT);
            if (weight > 0) {
                long timestamp = invoker.getUrl().getParameter(TIMESTAMP_KEY, 0L);
                if (timestamp > 0L) {
                    long uptime = System.currentTimeMillis() - timestamp;
                    if (uptime < 0) {
                        return 1;
                    }
                    int warmup = invoker.getUrl().getParameter(WARMUP_KEY, DEFAULT_WARMUP);
                    if (uptime > 0 && uptime < warmup) {
                        weight = calculateWarmupWeight((int)uptime, warmup, weight);
                    }
                }
            }
        }
        return Math.max(weight, 0);
    }

//其中的calculateWarmupWeight((int)uptime, warmup, weight);方法会根据预热时间和当前时间来计算权重，它是一个缓慢上升的过程，并不是直接上升的
    /**
     * Calculate the weight according to the uptime proportion of warmup time
     * the new weight will be within 1(inclusive) to weight(inclusive)
     *
     * @param uptime the uptime in milliseconds
     * @param warmup the warmup time in milliseconds
     * @param weight the weight of an invoker
     * @return weight which takes warmup into account
     */
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        //当前时间/(预热时间/权重)=(当前时间/预热时间)*权重
        //为什么要不直接写成后面这种乘法形式呢？就是为了防止预热时间为0，即分母为0
        //比如预热时间是10分钟，现在已经启动了5分钟，那就是5/10在来乘以权重
        int ww = (int) ( uptime / ((float) warmup / weight));
        return ww < 1 ? 1 : (Math.min(ww, weight));
    }
~~~



increaseCurrnet方法

~~~java
//当前权重+预设权重
public long increaseCurrent() {
    return current.addAndGet(weight);
}
~~~

sel方法源码

~~~java
public void sel(int total) {
    //原子操作，虽然调用的是addAndGet方法，但参数是-1*参数值，即做减法
    current.addAndGet(-1 * total);
}
~~~

当进行到第三次请求的时候，通过debug看一下存放当前服务状态的map

![](https://s3.ax1x.com/2021/03/07/6MohjJ.png)

可以看到，此时王五(weight=300)和张三(weight=100)的值(current)都为300，

继续dubug，此时map的状态变为

![](https://s3.ax1x.com/2021/03/07/6MTRat.png)

可以看到，当两个服务的权重值相同时，它是**根据map中存放服务的顺序来决定到底将请求落到哪个服务**。

**优势**

实现简单，请求分布⽐例明确客观

**劣势**

需要记录访问状态，缺少灵活性，当服务负载出现压⼒时，依然会固定发送处理请求

#### 最少并发

并发**是指客服端当前未完成的请求总数，并非服务端所承载的同时响应的请求**

具体规则如下：

1.只有一个最少并发的服务，就使用该服务

2.如果有多个最少并发，则基于权重随机（前提是总权重不等于零，并且权重不相同）

3.否则直接在多个最少并发中随机找一个

![](https://s3.ax1x.com/2021/03/08/6194rn.png)

并发统计方式：

在RpcStatus中请求在执行前将请求数加1，请求结束后在减1

RpcStatus部分源码：

~~~java
public static boolean beginCount(URL url, String methodName, int max) {
        max = (max <= 0) ? Integer.MAX_VALUE : max;
        RpcStatus appStatus = getStatus(url);
        RpcStatus methodStatus = getStatus(url, methodName);
        if (methodStatus.active.get() == Integer.MAX_VALUE) {
            return false;
        }
        for (int i; ; ) {
            i = methodStatus.active.get();
            if (i + 1 > max) {
                return false;
            }
            if (methodStatus.active.compareAndSet(i, i + 1)) {
                break;
            }
        }
    	//加1
        appStatus.active.incrementAndGet();
        return true;
    }

    /**
     * @param url
     * @param elapsed
     * @param succeeded
     */
    public static void endCount(URL url, String methodName, long elapsed, boolean succeeded) {
        endCount(getStatus(url), elapsed, succeeded);
        endCount(getStatus(url, methodName), elapsed, succeeded);
    }

    private static void endCount(RpcStatus status, long elapsed, boolean succeeded) {
        //减1
        status.active.decrementAndGet();
        status.total.incrementAndGet();
        ......
 }
~~~

但是active值默认为0是不会变的，我们要通过调用统计过滤器来实现加1减1的效果。

过滤器部分源码

~~~java
@Activate(group = CONSUMER, value = ACTIVES_KEY)
public class ActiveLimitFilter implements Filter, Filter.Listener {

    private static final String ACTIVELIMIT_FILTER_START_TIME = "activelimit_filter_start_time";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String methodName = invocation.getMethodName();
        int max = invoker.getUrl().getMethodParameter(methodName, ACTIVES_KEY, 0);
        final RpcStatus rpcStatus = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
        if (!RpcStatus.beginCount(url, methodName, max)) {
            ....
        }
        .....
    }
~~~

我们可以看到beginCount在invoke方法中被调用了，ActiveLimit有一个value值，我们通过配置该值来实现效果。

在左侧maven依赖中搜索dubbo，找到jar包，然后在meta-inf下面搜索filter找到rpc.filter文件，其中有这个配置

~~~properties
activelimit=org.apache.dubbo.rpc.filter.ActiveLimitFilter
~~~

然后在我们的配置文件配置即可

~~~properties
#负载均衡之最少并发
dubbo.consumer.loadbalance=leastactive
#在消费端设置统计调用统计过滤器
dubbo.consumer.filter=activelimit
~~~

**优势**

时刻让服务端处理处于更均匀的状态，当服务端压⼒⼤时，处理时间将会变⻓，积累的未完成 请求越多，得到的分配就越少。从⽽到达背压反馈的效果

**劣势**

实现复杂，需要实时统计并发

#### 最短响应

在最少并发的基础上，加上一个平均执行时间作为一个度量。基于历史平均响应 时间乘以当前并发数量，选出最⼩值。当结果出现多个时，其选择算法如下： 

1. 如果只有一个调用程序，则直接使用该调用程序；
2. 如果有多个调用者并且权重不相同，则根据总权重随机；
3. 如果有多个调用者且权重相同，则将其随机调用。 优势： 在最短并发基础之上，获得了服务的历史表现，对服务处理性能判断更加精准。 劣势： 实现复杂，需要实时统计并发，并实时计算平均响应时间

**配置**

~~~properties
#配置最少并发负载均衡器
dubbo.consumer.loadbalance=shortestresponse
#在消费端设置统计调用统计过滤器
dubbo.consumer.filter=activelimit
~~~

**优势**

在最短并发基础之上，获得了服务的历史表现，对服务处理性能判断更加精准

**劣势**

实现复杂，需要实时统计并发，并实时计算平均响应时间

#### 一致性哈希

用于大规模缓存系统的负载均衡。与取模哈希相比，优势在于，当服务端节点变更时，其影响范围将会缩小。算法机制如下：

1.使用0到2^32-1之间的数字，构成一个首尾相连的圆。

2.计算多个服务地址的哈希值，并使用虚拟节点将其均匀的分布在圆上

3.当执行请求时，先计算参数的哈希值，然后**顺时针**找到离他最近的节点访问。

![](https://s3.ax1x.com/2021/03/08/61KS6x.png)

**使用：**

~~~properties
#配置最少并发负载均衡器
hash.arguments #要hash的参数，默认第一个，
hash.nodes #虚拟节点数量
loadbalance = "consistenthash" #一至性哈希
~~~

上面是在配置文件中配置的，但是需要注意：

**一致性哈希算法比较特殊，通常用于缓存场景，所以要单独设置，不然所有服务都会使用该算法。建议统一设置在服务端的方法处**

如下：

![](https://s3.ax1x.com/2021/03/08/61KZ9A.png)

然后客户端每次访问的时候，可以看到，王五服务这个节点就会被缓存起来，下次访问的时候都会访问该节点。

**优势**

⽤于将相同的参数映射到固定的服务上，通常⽤于分布式缓存的场景。节点变更时缓存影响范 围降⾄最低

**劣势**

实时对参数进⾏md5 以及hash取值，参数值不建议太⼤

源码见ConsistentHashLoadBalance

#### 负载均衡算法选择

1.客户端并发⼤，服务端并发也⼤，照顾客户端性能 ===》 随机+权重 或 轮循加权重 

2.服务端数量⼩，客户端数量极⼤，照顾服务端的性能 ==》 最短连接或最短响应 

3.缓存应⽤，保证缓存命中率 ==》⼀⾄性啥希