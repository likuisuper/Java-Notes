### TCP套接字管道

#### 基本步骤

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
        ByteBuffer buffer=ByteBuffer.allocate(1024);
        //4、读取客服端发来的消息，如果没有则阻塞
        socketChannel.read(buffer);
        //read就相当于往buffer中put数据，所以一定要flip，让position重新从0开始，flip常用于put之后，至于为什么？想想就明白了
        buffer.flip();
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
        //这里将相当于tomcat的BIO模型中的Acceptor
        while (true){
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

![](https://z3.ax1x.com/2021/10/01/47EeAK.png)