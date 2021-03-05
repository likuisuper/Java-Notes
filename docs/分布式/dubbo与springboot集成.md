## 配置传递

服务端设置超时时间

![](https://s3.ax1x.com/2021/03/05/6m7Rzj.png)

超时时间配置为1000ms，但是睡眠了1110ms，然后启动服务端。客户端不做任何更改，

然后启动客服端后访问服务，比如在命令端输入curl命令来访问接口地址。这时**服务端并不会报超时异常，但是客服端会报超时异常**

~~~java
org.apache.dubbo.remoting.TimeoutException: Waiting server-side response timeout by scan timer
~~~

进入docker启动的zk客户端，进入bin目录，输入

~~~shell
zkCli.sh
~~~

然后输入

~~~shell
ls /dubbo/服务名称/providers
~~~

比如该项目就是

~~~shell
ls /dubbo/cxylk.dubbo.UserService/providers
~~~

如果查询结果不为空，就是有服务

![](https://s3.ax1x.com/2021/03/05/6mHg76.png)

粘贴到url解码网站，可以看到服务详细信息

~~~shell
dubbo://192.168.63.15:20880/cxylk.dubbo.UserService?anyhost=true&application=boot-server&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=cxylk.dubbo.UserService&metadata-type=remote&methods=getUser&pid=22412&release=2.7.8&side=provider&timeout=1000&timestamp=1614952379834
~~~

可以看到超时时间为1000

现在在客户端配置超时时间

![](https://s3.ax1x.com/2021/03/05/6mHzcj.png)

这时候访问服务就不会出现超时异常。

**也就是说，服务端配置的超时时间只是一个参考，一个建议，如果客户端没有配置超时时间，那么就是以服务端为准，否则就是以客户端为准**

## 配置继承

如果服务端或者客户端没有配置超时时间，但是在yml文件或者properties文件配置了超时时间，那么就会去继承文件中的配置。

## 配置优先级

![](https://s3.ax1x.com/2021/03/05/6mboPU.png)

以上面的配置为例，客户端配置超时时间为3000，服务端配置超时时间，并且新加配置如下图

![](https://s3.ax1x.com/2021/03/05/6mLpT0.png)

在DefaultFuter类的newFuter方法第一行打个断点，debug启动客户端，这个时候的超时时间就不再是以客户端的配置为准了，而是以服务端的超时配置为准，因为我们在@DubboService注解中加了@Method注解，**这是最小粒度的配置**。配置后，可以看到当前的超时时间为1500。

