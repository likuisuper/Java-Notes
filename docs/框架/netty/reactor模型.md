## reactor模式

### reactor角色

首先在reactor模型中有三种角色，分别是：

* Acceptor：处理客户端新连接，并分派请求到处理链中
* Reactor：负责监听和分配事件，将I/O事件分派给对应的handler
* Handler：处理事件，如编码、解码等

在Netty中，NioEventLoop就充当了reactor的角色，将事件分派给对应的handler。

### reactor线程模型

#### 单reactor单线程

该模型下所有请求的建立、IO操作、业务处理都在一个线程中完成。如果在业务处理中出现了耗时操作，那么所有的请求都会延时。

![](https://s1.ax1x.com/2023/07/07/pCc0gXt.png)

#### 单reactor多线程

为了防止业务处理中出现阻塞，在多线程模型下会使用一个线程池来异步处理任务，当处理完后在回写给客户端。

![](https://s1.ax1x.com/2023/07/07/pCc0RnP.png)

#### 多reactor多线程（主从reactor）

单React始终无法发挥现代服务器多核CPU的并行处理能力，所以Reactor是可以有个的，并且有一主多从之分。一个主Reactor仅处理连接，而多个子Reactor用于处理IO读写。然后交给线程池处理业务。Tomcat就是采用该模式实现。

![](https://s1.ax1x.com/2023/07/07/pCc0W0f.png)

## Netty线程模型

netty采用了主从reactor线程模型。主reactor对应boss group线程组，多个子reactor对应work group线程组。主reactor负责接收连接，并将建立好的连接注册到work组，当最后io事件触发后由pipeline处理。

![](https://s1.ax1x.com/2023/07/13/pChTDv4.png)

### EventLoop

事件循环器，它干的事情就是以前我们没有使用netty时，在循环中调用select方法监听事件是否触发，然后执行。它充当了reactor的核心。每个eventLoop包含了一个selector用于处理I/O事件，和一个taskQueue用于存储用户提交的任务。eventLoop会有一个独立的线程，默认是不启动的，当有任务触发时就会启动，并一直轮询下去。

下面是一个简单的示例：

~~~java
        //当指定一个线程后，下面两个方法打印的线程id都是相同的
        NioEventLoopGroup group=new NioEventLoopGroup(1);
        //execute方法立即执行
        group.execute(()-> System.out.println("execute:"+Thread.currentThread().getId()));
        group.submit(()-> System.out.println("submit:"+Thread.currentThread().getId()));
        //优雅关闭
        group.shutdownGracefully();
~~~

当使用eventLoop后，channel就必须通过注册到eventLoop才能注册到selector，并且服务端的绑定必须要发生在channel注册到eventLoop之后：

~~~java
        NioEventLoopGroup group=new NioEventLoopGroup(1);
        NioDatagramChannel channel=new NioDatagramChannel();
        //一定要先将channel注册到group中，不然调用bind操作就会报错
        group.register(channel);
        //此时bind操作不再由当前线程执行，而是由eventLoop执行，也就是在reactor中完成
        ChannelFuture channelFuture = channel.bind(new InetSocketAddress(8080));
        channelFuture.addListener(future -> System.out.println("完成绑定"));
~~~

### ChannelPipeline

`channelPipeline`是`channelHandler`的容器，它负责`channelHandler`的管理、事件拦截与调度。

由于`channelHandler`中的事件种类繁多，不同的`channelHandler`可能只需要关注其中的个别事件，所以，自定义的`channelHandler`只需要继承 ChannelInboundHandlerAdapter / ChannelOutboundHandlerAdapter，覆盖自己关心的方法即可。

可以通过`addFirst`将`channelHandler`加入pipeline首部或者通过`addLast`加入到pipeline尾部，channelHandler会按照加入顺序严格执行。

### ChannelHandler

ChannelHandler 负责对 I/O 事件 进行拦截处理，它可以选择性地 拦截处理感兴趣的事件，也可以透传和终止事件的传递。

Netty 提供了 ChannelHandlerAdapter 基类，和 ChannelInboundHandlerAdapter / ChannelOutboundHandlerAdapter 两个实现类，如果 自定义 ChannelHandler 关心某个事件，只需要继承 ChannelInboundHandlerAdapter / ChannelOutboundHandlerAdapter 覆盖对应的方法即可，对于不关心的，可以直接继承使用父类的方法

## Netty基本使用

#### NioServerSocketChannel用法

该类是netty对java.nio中ServerSocketChannel的封装。下面我们演示下如何使用eventLoop和pipeLine完成连接的建立和读取数据：

~~~java
    public void test2() throws IOException {
        NioServerSocketChannel serverSocketChannel = new NioServerSocketChannel();
        //将一个线程完成所有工作进行分工，boss线程只负责建立连接
        NioEventLoopGroup boss=new NioEventLoopGroup(1);
        //work线程负责读写，不指定线程的话，默认是cpu核心(逻辑核心)*2
        NioEventLoopGroup work=new NioEventLoopGroup();
        //将channel注册到group中
        boss.register(serverSocketChannel);
        //注册完成后就可以绑定ip和端口了，提交任务到eventLoop
        serverSocketChannel.bind(new InetSocketAddress(8080));
        //然后我们需要监听连接事件
        serverSocketChannel.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                //此时的msg就是NioSocketChannel
                System.out.println(msg);
                System.out.println("已建立新连接");
                //处理NioSocketChannel,读写操作都让work线程处理，通过调用next方法不断让下一个线程处理任务
                handleAccept(work,msg);
            }
        });
        System.in.read();
    }

    private void handleAccept(NioEventLoopGroup group, Object msg) {
        NioSocketChannel socketChannel= (NioSocketChannel) msg;
        group.register(socketChannel);
        //读取客户端的数据
        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                //此时的msg是netty中的byteBuf，它是对java.nio中ByteBuffer的封装
                ByteBuf byteBuf= (ByteBuf) msg;
                System.out.println(byteBuf.toString(Charset.defaultCharset()));
            }
        });
    }
~~~

在上面的代码中，msg对象为什么是`NioSocketChannel`对象呢？我们启动程序后进行断点调试，可以得到如下链路：

![](https://s1.ax1x.com/2023/07/13/pCh56bt.png)

可以看到，在`doReadMessage`方法中，创建了java.nio中的SocketChannel对象，然后又创建了NioSocketChannel，第一个参数指定了它的父类就是NioServerSocketChannel，第二个参数传递了上一步创建的SocketChannel。

#### ServerBootStrap用法

serverBootStrap是服务端引导器。在上面的代码中，我们需要初始化channel，然后进行注册，又要初始话管道，比较麻烦。而使用serverBootStrap后，我们只需要指定channel，它会自动帮我们注册，并且也不需要自己创建piepline，**netty会为每个channel连接创建一个独立的pipeline，我们只需要将自定义的channelHandler加入到pipeline即可**。

我们可以基于此来实现一个简单的http服务：

1、初始化。通过serverBootStrap设定服务的线程组，其中boss用于处理NioServerSocketChannle中的accept事件，work用于处理I/O读写，以及用户提交的异步任务。通过channel class告诉serverBootStrap要维护一个什么样的管道：

~~~java
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup work = new NioEventLoopGroup(8);
        serverBootstrap.group(boss, work)
                //指定要打开的管道，自动进行注册
                .channel(NioServerSocketChannel.class)
~~~

2、设置子管道的pipeline

然后就可以初始化子管道的Pipeline了，为其绑定对应的处理Handler即可。我们目标是实现一个Http服务，对应的三个基本操作是 解友、业务处理、编码。其中编解码是Htpp协议通用处理Netty已自带处理器，直接添加可。业务处理需要手动编写:

~~~java
        serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        //一个http请求，首先是先解码成一个request，然后进行业务处理，最后编码成一个response返回
                        //输入流
                        ch.pipeline().addLast("decode",new HttpRequestDecoder());
                        ch.pipeline().addLast("servlet",new Myservlet());
                        //输出流
                        ch.pipeline().addFirst("encode",new HttpResponseEncoder());
                    }
                });
~~~

这里有个注意的点，就是编码器要调用addFist放入管道头部，不然数据不会写回去，因为在Myservlet中我们调用了ctx.writeAndFlush将数据写了出去，相当于一个出站事件，而HttpResponseEncoder也是一个出站事件（可以看继承图），而出站事件是从尾部向头部进行处理的，原理会在后面详细讲解pipeline时说道。

3、业务处理

通过实现SimpleChannelInboundHandler 可直接处理读事件用来接收客户端的请求。这里有必要先了解下Http请求链路：

![](https://s1.ax1x.com/2023/07/13/pChod6H.png)

当字节流经过解码后，就需要写入成request对象，但是这里是分成两步来写的，第一个部分是请求头request，第二个部分是body，并且body部分也分为多个content，因为当传输数据很大的时候不可能一次就写完，我们可以通过lastHttpContent来判断是否写完：

~~~java
 private static class Myservlet extends SimpleChannelInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            //此时这个msg是什么对象呢？
            //我们在pipeline中添加了decode HttpRequestDecoder,该类的文档中说明了它会将字节流解码为
            //httpRequest和httpResponse

            //这样转换直接报异常：io.netty.handler.codec.http.LastHttpContent$1 cannot be cast to io.netty.handler.codec.http.HttpRequest
            //为什么呢？因为会发两次，一次是请求头request,一次是内容body,第一次发过来，转换没问题，但是第二次进来的body，也就就是content，就转换失败了
            //所以需要分情况处理
            if(msg instanceof HttpRequest){
                HttpRequest httpRequest= (HttpRequest) msg;
                System.out.println("当前请求："+httpRequest.uri());
            }
            if(msg instanceof HttpContent){
                ByteBuf content = ((HttpContent) msg).content();
                OutputStream outputStream= new FileOutputStream("D:\\WorkSpace\\github\\learn-netty\\target\\test.mp4",true);
                content.readBytes(outputStream,content.readableBytes());
                outputStream.close();
            }
            //如果是最后一个content
            if(msg instanceof LastHttpContent){
                FullHttpResponse response=new DefaultFullHttpResponse(HttpVersion.HTTP_1_0,HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
                response.content().writeBytes("上传完毕".getBytes());
                ChannelFuture channelFuture = ctx.writeAndFlush(response);
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
~~~

但是这样就有点麻烦了，netty也提供了一个handler给我们，可以将同一个http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象，这个handler就是`HttpObjectAggregator`：

~~~java
                        ch.pipeline().addLast("decode",new HttpRequestDecoder());
                        ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65535));
~~~

将它加入管道尾部，并且指定一个大小，防止内存溢出，然后我们的消息读取就简化成这样：

~~~java
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof FullHttpRequest) {
                FullHttpRequest request= (FullHttpRequest) msg;
                System.out.println("uri:"+request.uri());
                System.out.println(request.content().toString(Charset.defaultCharset()));
                FullHttpResponse response=new DefaultFullHttpResponse(HttpVersion.HTTP_1_0,HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
                response.content().writeBytes("上传完毕".getBytes());
                ChannelFuture channelFuture = ctx.writeAndFlush(response);
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
~~~

4、绑定端口

最后一个步骤是由ServerBoottrap 来绑定端口：

~~~java
        ChannelFuture channelFuture = serverBootstrap.bind(port);
        channelFuture.addListener(future-> System.out.println("完成绑定"));
~~~

这样就实现了一个简单的http服务。