长连接保持机制：1、心跳保活；2、断开重连

netty：**在netty中一切都是异步**

数据都是发送到管道里的。也就是channel

而channelHandler就是用来接收channel传输的数据并进行处理的

netty是不能发送java对象和接收java对象，因为没有配置编码器。它只认request（发送）和response（返回）。但是字符串和Json是可以的