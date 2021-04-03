## 远程网络通信

首先来看下通信方案：

![](https://z3.ax1x.com/2021/04/03/cnw1eO.png)

长连接保持机制：1、心跳保活；2、断开重连

netty：**在netty中一切都是异步**

数据都是发送到管道里的。也就是channel

而channelHandler就是用来接收channel传输的数据并进行处理的

netty是不能发送java对象和接收java对象，因为没有配置编码器。它只认request（发送）和response（返回）。但是字符串和Json是可以的

下面通过使用netty4的方式来实现一个最基本的通信方式：

~~~java
public class RemotingTest {
    String url="dubbo://127.0.0.1:20880/cxylk.dubbo.UserService?timeout=3000";

    @Test
    public void openServer() throws IOException, RemotingException {
        NettyServer server=new NettyServer(URL.valueOf(url),new ChannelHandlerAdapter(){
            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                System.out.println("接收到数据："+message);
                //通过channel发送返回结果
                channel.send("收到,over");
            }
        });
        System.in.read();
    }

    @Test
    public void invoke() throws RemotingException, IOException {
        NettyClient client=new NettyClient(URL.valueOf(url),new ChannelHandlerAdapter(){
            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                System.out.println("返回结果:"+message);
            }
        });
        //异步
        client.send("hello netty4");
        //不让线程结束
        System.in.read();
    }
}
~~~

通过new一个NettyServer并绑定一个端口号就开启了一个服务。客户端则是通过new一个NettyClient，然后调用send方法，服务端便能接收到这条消息。上面说过数据都是发送到管道channel中的，所以服务端通过调用channel的send方法发送消息，客户端便能接收到返回结果。需要注意的是，netty中一切皆异步，所以需要使用System.in.read让线程不结束。

dubbo的通信过程就是基于上面这种方式：

![](https://z3.ax1x.com/2021/04/03/cnwtfA.png)

## 线程协作

在上面的通信过程中，涉及到了多个线程，通过debug的方式来了解其中都有哪些线程

1、首先来看客户端：

![](https://z3.ax1x.com/2021/04/03/cnBPVU.png)

其中的DubboClientHandle这个线程它是在一个无限的缓存线程池中，60s过后就会消失，到下面调试源码的时候会看到。

2、服务端：

![](https://z3.ax1x.com/2021/04/03/cnBZx1.png)

NettyServerBoss就是Netty中的主线程，它是用来建立连接的，I/O线程则是负责传输数据的，真正干活的还是DubboServerHandler这些业务线程。而这些业务线程在前面讲线程池的时候提到过，是可以配置的，默认是固定线程池。另外，这里的main线程只是用来启动的，仅此而已。

这些线程是怎么协作的呢？通过源码的方式来一探究竟

首先启动服务端，调试客户端，因为网络通信里面都是异步的，而且有时还有共用一个方法，比如上面的receive，如果两个都调试的话，就分不清了。

![](https://z3.ax1x.com/2021/04/03/cnD7tg.png)

上面是nettyClient的堆栈信息，可以看到，通过采用责任链的方式，最终的handle就跑到了我们自己实现的代码中。其中MultiMessageHandler意思就是发送多个消息，而HearbeatHandler是用来处理心跳的，AllChannelHandler是我们重点关注的对象。

NettyClient的构造函数中会调用到doOpen方法，这里面都是netty的概念，比如像bootstrap，pipeline等。这里面有这么一段代码:

~~~java
                ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                        .addLast("decoder", adapter.getDecoder())
                        .addLast("encoder", adapter.getEncoder())
                        .addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS))
                        .addLast("handler", nettyClientHandler);
~~~

可以看到，将编目，解码，handle这些写入到了管道。然后会经过一些列的网络传输到服务端，服务端又会进行一系列的处理，然后经过网络传输响应给客户端。这些过程很复杂，需要有netty的知识，这里就跳过了。

服务端将消息响应过来后，我们直接跳到责任链的最后AllChannelHandler这个类中的received方法。我们测试代码中实现的是ChannelHandlerAdapter的received方法，而它是实现了ChannelHandler接口的，AllChannelHandler也实现了ChannelHandler接口。

~~~java
    @Override
	//此时的message就是“收到，over”这条消息
    public void received(Channel channel, Object message) throws RemotingException {
        ExecutorService executor = getPreferredExecutorService(message);
        try {
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.RECEIVED, message));
        } catch (Throwable t) {
        	if(message instanceof Request && t instanceof RejectedExecutionException){
                sendFeedback(channel, (Request) message, t);
                return;
        	}
            throw new ExecutionException(message, channel, getClass() + " error when process received event .", t);
        }
    }
~~~

然后进入getPreferredExecutorService方法

~~~java
public ExecutorService getPreferredExecutorService(Object msg) {
        if (msg instanceof Response) {
            Response response = (Response) msg;
            DefaultFuture responseFuture = DefaultFuture.getFuture(response.getId());
            // a typical scenario is the response returned after timeout, the timeout response may has completed the future
            if (responseFuture == null) {
                return getSharedExecutorService();
            } else {
                ExecutorService executor = responseFuture.getExecutor();
                if (executor == null || executor.isShutdown()) {
                    executor = getSharedExecutorService();
                }
                return executor;
            }
        } else {
            return getSharedExecutorService();
        }
    }
~~~

这里的msg明显不是response，所以会调用getSharedExecutorService去获取共享线程池。该方法返回一个executor:

![](https://z3.ax1x.com/2021/04/03/cnc2LR.png)

可以看到，核心线程corePoolSize=0，最大线程maximumPoosize=Integer.MAX_VALUE，keepAliveTime=60s，workQueue是同步队列SynchronousQueue，通过这些参数可以知道这就是一个缓存线程池，而线程名字就是前面说的DubboClientHandler这个业务线程，这也是为什么说它60s后就消失了。

然后回到received方法，会调用executor来处理服务端的返回结果。

通过一张图来总结下上面的流程：

![](https://z3.ax1x.com/2021/04/03/cnRgyQ.png)

## 编解码

过程：

![](https://z3.ax1x.com/2021/04/03/cnWZkt.png)

在前面讲dubbo协议的时候提到过协议的组成，关于它的报文以及各部分的概念这里就不再赘述。而其中的报文体，客户端和服务端是不一样的，如下：

![](https://z3.ax1x.com/2021/04/03/cnWBnJ.png)

#### 编解码Request

![](https://z3.ax1x.com/2021/04/03/cnhNyF.png)

上面除了IO线程还有业务线程，这是因为反序列化是需要时间的，所以交给业务线程来处理。

#### 编解码Response

![](https://z3.ax1x.com/2021/04/03/cnfF3T.png)