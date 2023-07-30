## Netty核心组件

### Netty Channel

Netty的channel是在java.nio中的channel基础上进行封装的，同时引入了pipeline与异步执行机制。异步机制是指在原生NIO Channel中异步进行IO是一种不安全行为，它会触发阻塞甚至导致死锁。所以一般情况下**在非IO线程中操作channel都会以任务的形式进行封装，并提交到IO线程执行，而这些在Netty中都已经内部封装实现，即使异步调用Netty Channel都是安全的**。

channel中有三个核心的部分：

* EventLoop：提供异步执行机制
* Pipeline：提供操作pipeline的能力，比如将消息写入到pipeline
* unsafe：提供对原生管道的安全操作

不同的子类会包含对应原生NIO中的channel，如下图：

![](https://s1.ax1x.com/2023/07/17/pCoegUJ.png)

最终还是会调用到原生管道，`doXXX`方法即是直接对原生管道的IO调用。在非IO线程调用这是一种不安全的行为。所以所有do开头的方法都是不开放的(protected)。不过netty在channel中还提供了一个Unsafe可以直接调用这些方法。

### Unsafe

unsafe是channel中的一个内部接口，可以用于直接操作channel中的IO方法，而不必经过异步和管道。**实际的IO读写操作都是由unsafe接口负责完成的**。但正如它的名字一样，它是不安全的，它不应该被用户代码直接调用，需要调用者自己确保当前对unsafe的调用是在IO线程下，否则就会报异常。以下方法除外：

~~~java
localAddress()
remoteAddress()
closeForcibly()
//这是一个异步方法不会立马返回，而是完成后通知ChannelPromise
register(EventLoop, ChannelPromise)deregister(ChannelPromise)
voidPromise()
~~~

unsafe是channel的内部类，不同的channel会对应不用的unsafe，所提供的功能也不一样。其结构与继承关系如下图：

![](https://s1.ax1x.com/2023/07/24/pCL5nr6.png)

unsafe并不只是作为中介把调用转发到channel，它还提供了如下作用：

* 线程检测：当前调用是否是IO线程
* 状态检测：写入前判断是否已注册
* 写入缓存：write时把数据写入临时缓冲中，当调用flush时才真正提交
* 触发读取：eventLoop会基于读取事件通知unsafe，在由unsafe读取后发送到pipeline

也就是说，它在channel、eventLoop、pipeline这三者之间起到了一个桥梁的作用。

**在一个读取场景中流程是这样的**：

1、EventLoop触发读取并通知unsafe ==>unsafe.read();

2、unsafe调用channel读取消息 ==>channel.doReadBytes(ByteBuf)

3、unsafe将消息传入pipeline ==>pipeline.fireChannelRead(byteBuf)

见`AbstractNioByteChannel`类

**写入场景**：

1、业务开发调用channel写入消息 ==>channel.write(msg)

2、channel将消息写入pipeline ==>pipeline.wirte(msg)

3、pipeline中的handler异步处理消息 ==>ChannelOutboundHandler.write()

4、pipeline调用unsafe写入消息 ==>unsafe.write(msg)

5、unsafe调用channel完成写入 ==>channel.doWrite(msg)

在下面这个例子中，绑定有三种方式：

~~~java
unsafe.register(loopGroup.next(), channel.newPromise());
//1、直接调用unsafe绑定
unsafe.bind(new InetSocketAddress(8080), channel.newPromise());
//2、提交到IO线程中异步绑定
channel.eventLoop().submit(()->unsafe.bind(new InetSocketAddress(8080), channel.newPromise()));
//3、通过channel完成绑定，推荐这种方式
channel.bind(new InetSocketAddress(8080));
~~~

### channelPipeline

ChannelPipeline 是 ChannelHandler 的容器，它负责 ChannelHandler 的管理、事件拦截与调度。每个channel中都会有一条唯一的pipeline用于流转的方式处理channel中发生的事件比如注册、绑定端口、读写消息等。这些时间会在pipeline流中的各个节点轮转并依次处理，而每个节点可以处理对应的功能，这是一种责任链的设计模型，目的是为了让各个节点处理聚焦的业务。

关于pipeline的详细介绍，可以看类文档，里面说明了在pipeline中的事件如何流转，以及哪些操作会触发什么事件等。

#### 结构

要知道事件如何在pipeline中流转，就需要了解pipeline的结构。pipeline内部采用了双向链表结构，通过ChannelHandlerContext包装唯一的channel，并通过prev和next属性链接节点前后的context，从而组成链条。pipeline中有hear与tail两个context对应链条的首尾。

![](https://s1.ax1x.com/2023/07/17/pCIv9fJ.png)

#### channelHandler

如上图所示，pipeline中的节点，共有三种类型：

* 入站处理器：即`ChannelInboundHandler`的实现，可用于处理如消息读取等入站事件
* 出站处理器：即`ChannelOutboundHandler`的实现，可用于处理消息写入、端口绑定等出站事件
* 出入站处理器：`ChannelDuplexHandler`的实现，可以处理所有出入站事件。某些协议的编解码操作想写在一个类里面，即可使用该处理器实现。

图中间的handler表示出站处理器，两边的handler表示入站处理器。

#### 出入站事件

netty中的事件分为Inbound入站事件和Outbound出站事件。

**入站事件**：

是指站内发生的事件，通常由I/O线程触发，例如TCP链路建立事件、管道注册、读事件，这些都是由Eventloop基于IO事件被动开始发起的。注意所有入站事件触发必须由ChannelInBoundInvoker的子类执行。

**出站事件**：

指向channel的另一端发起请求或写入消息。通常是由用户主动发起的网络IO操作，例如用户发起的连接操作、绑定操作、消息发送等操作。其均由ChannelOutboundInvoker触发并由ChannelOutboundHandler处理。与入站事件不同其都由开发者自己发起。

**事件触发**

查看pipeline源码，明确的写了哪些方法会触发事件，比如如下方法就是入站事件：

~~~java
Inbound event propagation methods:
ChannelHandlerContext.fireChannelRegistered()
ChannelHandlerContext.fireChannelActive()
ChannelHandlerContext.fireChannelRead(Object)
ChannelHandlerContext.fireChannelReadComplete()
ChannelHandlerContext.fireExceptionCaught(Throwable)
ChannelHandlerContext.fireUserEventTriggered(Object)
ChannelHandlerContext.fireChannelWritabilityChanged()
ChannelHandlerContext.fireChannelInactive()
ChannelHandlerContext.fireChannelUnregistered()
~~~

如下方法则是出站事件：

~~~java
Outbound event propagation methods:
ChannelHandlerContext.bind(SocketAddress, ChannelPromise)
ChannelHandlerContext.connect(SocketAddress, SocketAddress, ChannelPromise)
ChannelHandlerContext.write(Object, ChannelPromise)
ChannelHandlerContext.flush()
ChannelHandlerContext.read()
ChannelHandlerContext.disconnect(ChannelPromise)
ChannelHandlerContext.close(ChannelPromise)
ChannelHandlerContext.deregister(ChannelPromise)
~~~

可以发现，以`fireXXX`命名的方法都是从I/O线程流向用户业务Handler的inbound事件，它们的实现因功能而异，但是处理步骤类似，都是调用HeadHandler对应的fireXXX方法，然后执行事件相关的逻辑操作。

下图可以看出pipeline与context分别实现了出入站接口，说明其可以触发所有出入站事件，而channel只继承 出站口，只能触发出站事件。

![](https://s1.ax1x.com/2023/07/17/pCoA1wd.png)

#### ChannelHandlerContext

context的作用如下：

1、结构上链接上下节点

2、传递出入站事件，所有的事件都可以由context进行上下传递

3、保证处理在IO线程上，前面说过所有的IO操作都需要异步提交到IO线程处理，这个逻辑就是由Context实现的。如下面的绑定操作就保证了IO线程执行：

~~~java
    @Override
    public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
        ObjectUtil.checkNotNull(localAddress, "localAddress");
        if (isNotValidPromise(promise, false)) {
            // cancelled
            return promise;
        }

        final AbstractChannelHandlerContext next = findContextOutbound(MASK_BIND);
        EventExecutor executor = next.executor();
        //是否是IO线程
        if (executor.inEventLoop()) {
            next.invokeBind(localAddress, promise);
        } else {
            safeExecute(executor, new Runnable() {
                @Override
                public void run() {
                    next.invokeBind(localAddress, promise);
                }
            }, promise, null, false);
        }
        return promise;
    }
~~~

ChannelHandlerContext是在哪里构造的呢？

当我们调用pipeline的addLast或者addFirst的时候，就会将channel包装成一个context，代码见`DefaultChannelPipeline`：

~~~java
public final ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
        final AbstractChannelHandlerContext newCtx;
        synchronized (this) {
            checkMultiplicity(handler);

            //构造context
            newCtx = newContext(group, filterName(name, handler), handler);

            addLast0(newCtx);
            ....
        }
}

private AbstractChannelHandlerContext newContext(EventExecutorGroup group, String name, ChannelHandler handler) {
        return new DefaultChannelHandlerContext(this, childExecutor(group), name, handler);
}
~~~

#### pipeline处理流程（事件传递）

如果是入站事件将会从头部向下传递到尾部并跳过OutboundHandler，而出站则相反，从尾部往上传递，并跳过InboundHandler处理器。

下面通过一个例子来说明pipeline的处理过程，UDP实现较为简单，所以这里就以UDP为例：

1、初始化UDP管道

~~~java
NioDatagramChannel channel = new NioDatagramChannel();
ChannelFuture channelFuture = new NioEventLoopGroup(1).register(channel);
channelFuture.addListener(future -> System.out.println("注册完成"));
~~~

2、为管道添加入站处理节点1

~~~java
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                System.out.println("入站事件1:" + msg);
                String message = (String) msg;
                //传递到下一个节点
                ctx.fireChannelRead(message += " hahaha");
                //当然，我们也可以调用父类的channelRead方法，它默认就会调用ctx.fireChannelRead(msg);
                //super.channelRead(ctx,message+" hahaha");
            }
        });
~~~

3、添加入站处理节点2

~~~java
		//第二个入站事件，但是会发现消息没有打印
        //原因是上一个channelHandler处理完后就不再先下传递了，需要上一个handler调用fireChannelRead方法手动进行触发
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                System.out.println("入站事件2:" + msg);
            }
        });
~~~

4、手动触发入站事件

~~~java
        //触发入站消息
        channel.pipeline().fireChannelRead("hello netty");
~~~

5、执行后得到如下结果

~~~java
入站事件1：hello netty
入站事件2：hello netty hahaha
~~~

流程说明：

1、基于pipeline触发入站处理，由头部开始处理，并向下传递

2、节点1接收消息，并改写消息后通过ctx.fireChannelRead继续向下传递

3、节点2接收消息，并打印。此时节点2并没有调用fireChannelRead，所以处理流程不会传到tail节点

下面再添加两个出站事件，用于发送消息到指定端口：

~~~java
        //第一个出站事件
        channel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                System.out.println("出站事件:" + msg.toString());
                ctx.write(msg.toString() + " love netty");
            }
        });
        //第二个出站事件，要添加到头部，因为出站事件是从尾部开始往头部处理的
        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                DatagramPacket packet = new DatagramPacket(Unpooled.wrappedBuffer(msg.toString().getBytes()), new InetSocketAddress("127.0.0.1", 8080));
                ctx.write(packet);
            }
        });
		//触发出站消息,写入是写入缓冲区，必须刷新
        channel.pipeline().writeAndFlush("lk");
~~~

这里要注意，这里要提前绑定一个端口，这个端口和要发送的端口没有关系，但是不绑定的话就会报错：

~~~java
channel.bind(new InetSocketAddress(8081));
~~~

然后启动一个UDP服务，指定端口号：

~~~java
    public void open() throws IOException {
        NioDatagramChannel datagramChannel = new NioDatagramChannel();
        datagramChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                System.out.println(msg);
                DatagramPacket packet = (DatagramPacket) msg;
                System.out.println(packet.content().toString(Charset.defaultCharset()));
            }
        });
        NioEventLoopGroup group = new NioEventLoopGroup();
        group.register(datagramChannel);
        datagramChannel.bind(new InetSocketAddress(8080));
        System.in.read();
    }
~~~

### ByteBuf

#### 结构

我们在java.nio中使用过ByteBuffer，但是ByteBuffer只维护了一个索引，所以我们需要使用flip()方法来切换读写模式。netty为了满足字节流传输的高效、方便、易用，所以自己实现了一个数据容器ByteBuf。

ByteBuf类似于一个字节数组，最大的区别是读和写的索引可以用来控制对缓冲区数据的访问：

![](https://s1.ax1x.com/2023/07/20/pCHMpKs.png)

读操作使用readerIndex，写操作使用writeIndex，并且一定符合：

`0<=readerIndex<=writeIndex<=capacity`。

调用byteBuf的read或write开头的任何方法都会增加对应的索引。而使用set或者get操作字节则不会更改索引位置。

#### 分配

可以通过下面几种方式获取ByteBuf

##### ByteBufAllocator

为了减少分配和释放内存的开销。可以通过Channel或者绑定到的ChannelHandler的ChannelHandlerContext：

~~~java
ByteBufAllocator alloc();
~~~

##### Unpooled（非池化）缓存

当未使用ByteBufAllocator时，netty提供了一个使用工具类Unpooled，它提供了静态辅助方法来创建非池化的ByteBuf实例。

##### ByteBufUtil

ByteBufUtil 通过静态辅助方法来操作 ByteBuf，因为这个 API 是通用的，与使用池无关。最常用的是 hexDump() 方法，这个方法返回指定 ByteBuf 中可读字节的十六进制字符串，可以用于调试程序时打印 ByteBuf 的内容。

#### 简单使用

下面通过几个例子简单演示下ByteBuf的功能

1、读写

~~~java
    public void rwTest(){
        //声明一个初始容量为5，最大容量为10的字节数组
        ByteBuf buffer = Unpooled.buffer(5,10);
        //不指定最大容量，默认是Integer最大值
        int maxCapacity = buffer.maxCapacity();
        //readIndex:0,writeIndex:1
        buffer.writeByte((byte)1);
        //readIndex:0,writeIndex:2
        buffer.writeByte((byte)2);
        //readIndex:0,writeIndex:3
        buffer.writeByte((byte)3);
        //readIndex:0,writeIndex:4
        buffer.writeByte((byte)4);
        //readIndex:0,writeIndex:5
        buffer.writeByte((byte)5);
        //当超过初始容量后，会进行扩容-->io.netty.buffer.AbstractByteBuf.ensureWritable0-->io.netty.buffer.UnpooledHeapByteBuf.capacity(int)
        //readIndex:0,writeIndex:6
        buffer.writeByte((byte)6);
        //readIndex:1,writeIndex:6
        buffer.readByte();
        //readIndex:2,writeIndex:6
        buffer.readByte();
        buffer.readByte();
        buffer.readByte();
        buffer.readByte();
        //readIndex:6,writeIndex:6
        buffer.readByte();
        //readIndex:7,writeIndex:6
        //报错：IndexOutOfBoundsException
        buffer.readByte();
        //丢弃读索引之前的字节
        //buffer.discardReadBytes();
    }
~~~

2、复制

~~~java
    public void copyTest(){
        ByteBuf byteBuf = Unpooled.wrappedBuffer(new byte[]{1, 2, 3, 4, 5});
        //复制一个视图，内部的byteBuf指向被复制的byteBuf，修改数据会影响原来的byteBuf，但两者具有独立的读写索引
        ByteBuf duplicate = byteBuf.duplicate();
        byteBuf.readByte();
        System.out.println(byteBuf.readerIndex());
        System.out.println(duplicate.readerIndex());
        //复制全部可读视图区域，和duplicate一样，修改数据会影响原来的byteBuf，读写索引独立
        ByteBuf slice = byteBuf.slice();
        //因为byteBuf已经调用了readByte，所以slice此时的可读区域为4，此时读索引变为0，写索引变为4
        System.out.println(slice.readableBytes());
        //读索引加2
        slice.readSlice(2);
        slice.writerIndex(3);
        //此时字节数字变为1,2,3,4,6  ,byteBuf和duplicate中的字节数组都会改变
        slice.writeByte(6);
        //完全复制一个新的缓冲区，彼此不会影响
        ByteBuf copy = byteBuf.copy();
    }
~~~

3、其他

~~~java
    public void releaseTest(){
        ByteBuf byteBuf = Unpooled.wrappedBuffer(new byte[]{1, 2, 3, 4, 5});
        //从索引1开始，保留2个长度，此时retainedSlice 读索引变为0，写索引变为2，容量变为2
        ByteBuf retainedSlice = byteBuf.retainedSlice(1, 2);
        //读写索引变为0
        byteBuf.clear();
    }
~~~

更多功能，使用的时候可参考具体的API文档。

#### 总结

* 读写双索引维护，不需要flip
* 可手动释放回收
* 复制视图
* 自动扩容
* 可使用堆内存或者直接内存、复合内存