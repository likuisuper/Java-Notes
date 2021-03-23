## 控制后台

### 监控中心搭建

dubbo-admin就是后台服务，不过这里使用旧版本，也就是2.6以前的。它包括：控制后台，监控中心，简单注册中心。而新版的话是控制后台，控制后台包含了服务节点和前台，采用了前后端分离，但是不太稳定。

下载：目前是到github上的dubbo-admin模块下载分支0.2.0的代码

下载完后进入dubbo-monitor-simple目录，使用以下命令打包：

~~~xml
mvn clean -Dmaven.test.skip=true package
~~~

打包之后会生成一个target目录，在该目录下有个dubbo-monitor-simple-2.0.0-assembly.tar.gz压缩包，解压后程序目录结构为

~~~java
assembly.bin	conf	lib
~~~

分别代表：启动脚本目录，配置文件，lib程序包

关于配置只需配置三点：

1.注册中心地址

2.dubbo协议端口

3.jetty服务器端口

~~~~properties
dubbo.container=log4j,spring,registry,jetty-monitor
dubbo.application.name=simple-monitor
dubbo.application.owner=dubbo
#dubbo.registry.address=multicast://224.5.6.7:1234
dubbo.registry.address=zookeeper://193.168.63.128:2181 #注册中心
#dubbo.registry.address=redis://127.0.0.1:6379
#dubbo.registry.address=dubbo://127.0.0.1:9090
dubbo.protocol.port=7070 #dubbo协议端口，提供端和消费端上报数据
dubbo.jetty.port=8000 #jetty服务器端口，http访问端口
dubbo.jetty.directory=${user.home}/monitor
dubbo.charts.directory=${user.home}/monitor/charts
~~~~

接下来就可以启动：

~~~properties
#启动 windows下直接双击start.bat
#关闭 双击stop.bat
~~~

但是发现一直报错，没办法，只能把压缩包发送到linux服务器构建服务。但是要注意，当执行./start.sh的时候会报错，原因是当前文件是从Windows发送过来的，所以格式不对，通过vim start.sh进入编辑模式，然后使用**shift+:**进入命令模式，使用

~~~shell
set ff
~~~

查看当前文件格式，会发现是dos，通过

~~~shell
set ff=unix
~~~

设置为unix，再启动就ok了。

接下来对监控进行配置，**这里不在项目的springboot模块配置，因为老版本不支持，会导致找不到服务**，这里是maven工程项目

~~~xml
    <!-- 使用zk注册中心暴露服务地址 -->
    <dubbo:registry address="zookeeper://192.168.63.128:2181" />
    <!-- 用dubbo协议暴露服务 在消费者这里这行代码可以不用-->
    <dubbo:protocol name="dubbo" port="-1" />

    <!-- 声明需要暴露的服务接口 -->
    <dubbo:reference id="userService" interface="cxylk.dubbo.UserService" />
<!--    <dubbo:monitor address="192.168.63.128:7070"/>-->
    <dubbo:monitor protocol="registry"/>
~~~

有两种方式监控，第一种就是直接使用监控地址，但是不灵活，我们使用最下面的，即使用注册中心。

除了客户端需要配置外，服务端也需要同样的配置。

启动服务端和客户端，访问193.168.63.128:8000即可访问监控中心

### 原理

![](https://z3.ax1x.com/2021/03/20/64VHf0.png)

性能日志上报

![](https://z3.ax1x.com/2021/03/20/64VXXF.png)