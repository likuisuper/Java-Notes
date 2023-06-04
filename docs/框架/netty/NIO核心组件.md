## IO回顾

IO指 Input(输入流)、OutPut(输出流) 以支持JAVA应用对外传输交换数据。传输入目标可以是文件、网络、内存等。

![](https://s1.ax1x.com/2023/06/04/pC9TPG4.png)

当然，随着对传输性能的要求越来越高，后面也逐渐演变除了BIO、NIO、AIO。

### BIO

即同步阻塞I/O。在java1.4之前 这种传输的实现 只能通过inputStream和OutputStream 实现。这是一种阻塞式IO模型，应对连接数不多的文件系统还好，如果应对的是成千上万的网络服务，这种阻塞式模型就会造成大量的线程占用，造成服务器无法承载更高的并发。

在IO模型中，这里的阻塞指的是：内核数据准备好和数据从内核拷贝到用户态这两个过程。

### NIO

即同步非阻塞I/O。java1.4之后引入了NIO ，它通过双向管道进行通信，并且支持以非阻塞式的方式进行，就解决了网络传输导致线程占用的问题。Netty其在底层就是采用这种通信模型。

在IO模型中，非阻塞指的是：数据从内核拷贝到用户态这个过程会阻塞。

### AIO

同步非阻塞I/O。NIO的非阻塞的实现是依赖选择器 对管道状态进行轮循实现，如果同时进行的管道较多，性能必会受影响，所以java1.7引入了 异步非阻塞式IO，通过异步回调的方式代替选择器。这种改变在windows下是很明显（因为windows实现了ICMP），在linux系统中不明显。现大部分JAVA系统都是通过linux部署，所以AIO直正被应用的并不广泛。所以我们接下学的学习重点更关注到BIO与NIO的对比

## BIO与NIO区别

两组模型最大的别区在于阻塞与非阻塞，而所谓的阻塞是什么呢？而非阻塞又是如何解决的呢？

在阻塞模型中客户端与服务端建立连接后，就会按顺序发生三个事件

1.客户端把数据写入流中。（阻塞）

2.通过网络通道(TCP/IP)传输到服务端。（阻塞）

3.服务端读取。

这个过程中服务端的线程会一直处阻塞等待，直到数据发送过来后执行第3步。

如果在第1步客户端迟迟不写入数据，或者第2步网络传输延迟太高，都会导致服务端线程阻塞时间更长。所以更多的并发，就意味着需要更多的线程来支撑。

![](https://s1.ax1x.com/2023/06/04/pC9HbGD.png)

下图是Tomcat中的BIO模型图：

![](https://s1.ax1x.com/2023/06/04/pC97lkT.png)

BIO模型里是通1对1线程来等待 第1、2步的完成。而在NIO里是指派了选择器（Selector）来检查，是否满足执行第3步的条件，满足了就会通知线程来执行第3步，并处理业务。这样1、2步的延迟就与 用于处理业务线程无关。

![](https://s1.ax1x.com/2023/06/04/pC9HjsA.png)

Tomcat中的非阻塞模型图：

![](https://s1.ax1x.com/2023/06/04/pC97DhD.png)

## NIO基础组件

在BIO API中是通过InputStream 与OutPutStream 两个流进行输入输出。而NIO使用一个双向通信的管道代替了它俩。管道(Channel)必须依赖缓冲区（Buffer）实现通信

![](https://s1.ax1x.com/2023/06/04/pC9bPJS.png)

管道对比流多了一些操作，如：非阻塞、堆外内存映射、零拷贝等。当然，并非所有的管道都支持这些特性。

### buffer

所有管道都依赖了缓冲区。所谓缓冲区就是一个数据容器内部维护了一个数组来存储。**Buffer缓冲区并不支持存储任何数据，只能存储一些基本类型，就连字符串也是不能直接存储的**，理解这点对我们掌握buffer内部结构非常重要。

![](https://s1.ax1x.com/2023/06/04/pC9bz6J.png)

#### buffer内部结构

在Buffer内部维护了一个数组，同时有三个属性我们需要关注：

**capacity：容量，** 即内部数组的大小，这个值一但声明就不允许改变

**position：位置，**当前读写位置，默认是0每读或写个一个位就会加1

**limit：限制**，即能够进行读写的最大值，它必须小于或等于capacity

有了capacity做容量限制为什么还要有limit，原因往Buffer中写数据的时候 不一定会写满，而limit就是用来标记写到了哪个位置，读取的时候就不会超标。

如果读取超标就会报：BufferUnderflowException

同样写入超标也会报：BufferOverflowException

如果不用limit来标识，那我们写入和读取下一个数据时，就需要判断数据是不是null，但是前面说过，buffer中不能存储对象类型的数据，也就没有null的说法。那我们可以用-1或者其他数字标识吗？也不可以，因为在网络传输过程中，-1或其他数字也是正常传输的数据。

**注意：不管是读还是写，都是往前的，不能往后**

#### buffer核心使用

下面这些方法源码都在`Buffer`类中，示例代码在`IntBufferTest`类中

* allocate：声明一个指定大小的buffer，position为0，limit为容量值capacity

* wrap：基于数组包装一个buffer，position为0，limit为容量值

* flip：为读取做好准备，将position置为0，limit为原position的值

  这是一个很重要的方法，进行写入操作后一定要调用flip，不然接着读取就会报错

  ![](https://s1.ax1x.com/2023/06/04/pC9qlAP.png)

  flip能不能连续执行两次呢？不能，因为这样是没有意义的，limit也变为了0，那么此时既不能读也不能写。

* clear：为写入做好准备，将position置为0，limit为capacity，即回到初始状态。clear是不会清除数据的

  ![](https://s1.ax1x.com/2023/06/04/pC9qJ1g.png)

  需要注意，clear后再执行flip也是没有意义的，和上面一样。

* mark：添加标记，以便后续调用reset将position回到标记，比如替换某一段内容。reset不能单独使用，必须配合mark操作

  ![](https://s1.ax1x.com/2023/06/04/pC9qd7q.png)

  之所以需要mark，就是因为在buffer中，读和写都只能往前。

* remaining：为重新读取做好准备，只是将position置为0。

* hasRemaining：用来判断是否还能继续往下读

目前我们知道，总共有4个值，分别是 mark、position、limit、capacity它们等于以下规则：

**0 <= 标记 <= 位置 <= 限制 <= 容量**

### channel

管道用于连接文件、网络Socket等。它可同时同时执行读取和写入两个I/O 操作，固称双向管道，它有连接和关闭两个状态，在创建管道时处于打开状态，一但关闭 在调用I/O操作就会报`ClosedChannelException` 。通过管道的`isOpen` 方法可判断其是否处于打开状态。

#### FileChannel 文件管道

固名思议它就是用于操作文件的，除常规操作外它还支持以下特性：

- 支持对文件的指定区域进行读写

- 堆外内存映射，进行大文件读写时，可直接映射到JVM声明内存之外，从面提升读写效率。

- 零拷贝技术，通过 `transferFrom`  或`transferTo`  直接将数据传输到某个通道，极大提高性能。

- 锁定文件指定区域，以阻止其它程序员进行访问

打开FileChannel目前只能通过流进行打开，如inputStream.getChannel() 和outputStream.getChannel() ,通过输入流打开的管道只能进行读，而outputStream打开的只能写。否则会分别抛出NonWritableChannelException与NonReadableChannelException异常。

如果想要管道同时支持读写，必须用`RandomAccessFile` 读写模式才可以。

FileChannel示例：

~~~java
FileChannel channel = new RandomAccessFile(file_name,"rw").getChannel();
ByteBuffer buffer=ByteBuffer.allocate(1024); 
int count = channel.read(buffer);
~~~

read方法会将数据写入到buffer 直到Buffer写满或者数据已经读取完毕，上面我们声明的大小是1024，如果大于这个值，需要使用while循环读取。count 返回读取的数量，-1表示已读取完毕。

#### DatagramChannel UDP套接字管道

udp 是一个无连接协议，DatagramChannel就是为这个协议提供服务，以接收客户端发来的消息。

udp实现步骤如下：

~~~java
// 1.打开管道DatagramChannel 
channel = DatagramChannel.open();
// 2.绑定端口
channel.bind(new InetSocketAddress(8080));
// 3.接收消息，如果客户端没有消息，则当前会阻塞等待channel.receive(buffer); 
ByteBuffer buffer = ByteBuffer.allocate(8192);
~~~

在mac中，可使用`nc -vu 127.0.0.1 8080`这个命令向udp发送消息

#### TCP套接字管道

TCP是一个有连接协议，须建立连接后才能通信。这就需要下面两个管道：

- **ServerSocketChannel ：**用于与客户端建立连接

- **SocketChannel ：**用于和客户端进行消息读写

基本步骤：

下面这段代码是TCP管道的实现步骤，并且只能在一个连接中发送一个消息：

~~~java
public void  test() throws IOException {
        //ServerSocketChannel用于与客服端建立连接
        //1、打开TCP服务管道
        ServerSocketChannel channel=ServerSocketChannel.open();
        //2、绑定端口
        channel.bind(new InetSocketAddress(8080));
        //3、接收客服端发送的连接请求，如果没有则阻塞
        SocketChannel socketChannel = channel.accept();
    	//因为只声明了1024的字节大小，如果发送消息超过了设定的大小，就需要循环读取
        ByteBuffer buffer=ByteBuffer.allocate(1024);
        //4、读取客服端发来的消息，如果没有则阻塞，idea运行后，可以发现线程一直没有结束，知道收到客户端消息
        socketChannel.read(buffer);
        //read就相当于往buffer中put数据，所以一定要flip，让position重新从0开始，flip常用于put之后，至于为什么？想想就明白了
        buffer.flip();
    	//remaining方法返回的是limit - position的值，也就是剩余数量，但是我们先调用了filp，所以得到的就是已使用的空间大小
        byte[] bytes=new byte[buffer.remaining()];
        //将缓冲区的数据读取到字节数组
        buffer.get(bytes);
        System.out.println(new String(bytes));

        //5、回写消息
        //上面调用了buffer的get方法，所以在调用write之前要先调用rewind，rewind常用于write或get之前
        buffer.rewind();
        //write就相当于从buffer中get数据
        socketChannel.write(buffer);
        //6、关闭管道
        socketChannel.close();
        channel.close();
    }
~~~

使用`telnet ip port`进行连接测试，也可以使用`NetAssist`网络调试助手进行测试，下载地址：http://www.cmsoft.cn

#### 一个连接中发送多个消息

将上面的代码改造成while循环即可：

~~~java
public void  test2() throws IOException {
        //ServerSocketChannel用于与客服端建立连接
        //1、打开TCP服务管道
        ServerSocketChannel channel=ServerSocketChannel.open();
        //2、绑定端口
        channel.bind(new InetSocketAddress(8080));
        //3、接收客服端发送的连接请求，如果没有则阻塞
        SocketChannel socketChannel = channel.accept();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        while (true) {
            //缓冲区重用之前需要clear
            buffer.clear();
            //4、读取客服端发来的消息，如果没有则阻塞
            socketChannel.read(buffer);
            //read就相当于往buffer中put数据，所以一定要flip，让position重新从0开始，flip常用于put之后，至于为什么？想想就明白了
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            //将缓冲区的数据读取到字节数组
            buffer.get(bytes);
            String message = new String(bytes);
            System.out.print(message);
            //5、回写消息
            //上面调用了buffer的get方法，所以在调用write之前要先调用rewind，rewind常用于write或get之前
            buffer.rewind();
            //write就相当于从buffer中get数据
            socketChannel.write(buffer);
            if(message.trim().equals("q")){
                break;
            }
        }
        //6、关闭管道
        socketChannel.close();
        channel.close();
    }
~~~

但是这种方式不能建立多个连接，可以打开两个端口验证。如果想要建立多个连接发送消息，可以使用下面这种方式。

#### 建立多个连接

~~~java
 /**
     * 可以建立多个连接，BIO的简易模型
     * @throws IOException
     */
    @Test
    public void test3() throws IOException {
        //ServerSocketChannel用于与客服端建立连接
        //1、打开TCP服务管道
        ServerSocketChannel channel=ServerSocketChannel.open();
        //2、绑定端口
        channel.bind(new InetSocketAddress(8080));
        //3、接收客服端发送的连接请求，如果没有则阻塞
        while (true){
            //这里将相当于tomcat的BIO模型中的Acceptor
            handle(channel.accept());
        }
    }

    /**
     * 每来一个连接便分配一个线程处理，这个就是BIO的模型
     * @param socketChannel
     */
    public void handle(SocketChannel socketChannel) throws IOException {
        //tomcat的BIO模型采用线程池分配线程，这里直接new线程
        Thread thread=new Thread(()->{
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (true) {
                try {
                    //缓冲区重用之前需要clear
                    buffer.clear();
                    //4、读取客服端发来的消息，如果没有则阻塞
                    socketChannel.read(buffer);
                    //read就相当于往buffer中put数据，所以一定要flip，让position重新从0开始，flip常用于put之后，至于为什么？想想就明白了
                    buffer.flip();
                    byte[] bytes = new byte[buffer.remaining()];
                    //将缓冲区的数据读取到字节数组
                    buffer.get(bytes);
                    String message = new String(bytes);
                    System.out.print(message);
                    //5、回写消息
                    //上面调用了buffer的get方法，所以在调用write之前要先调用rewind，rewind常用于write或get之前
                    buffer.rewind();
                    //write就相当于从buffer中get数据
                    socketChannel.write(buffer);
                    if(message.trim().equals("q")){
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                //6、关闭管道，连接结束后才关闭
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
~~~



开启三个终端，分别使用`telnet`建立连接，这时候使用`jstack`命令可以看到建立了3个线程来处理请求

![](https://s1.ax1x.com/2023/06/04/pC9LfMQ.png)

上面就是一个简易的BIO模型，不管是read还是write都是阻塞的，可以采用下面的方法变为非阻塞：

~~~java
        //构建一个选择器来托管管道
        Selector selector=Selector.open();
        //选择器会监听当前的read操作，当数据读取好了之后，再通知管道
        //只有继承了SelectableChannel的类才能register
        socketChannel.register(selector, SelectionKey.OP_READ);//非阻塞
~~~

