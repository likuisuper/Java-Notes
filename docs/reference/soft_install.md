## node.js安装配置

安装略，主要讲下配置：

说明：这里的环境配置主要配置的是npm安装的全局模块所在的路径，以及缓存cache的路径，之所以要配置，是因为以后在执行类似：npm install express [-g] （后面的可选参数-g，g代表global全局安装的意思）的安装语句时，会将安装的模块安装到【C:\Users\用户名\AppData\Roaming\npm】路径中，占C盘空间。
例如：我希望将全模块所在路径和缓存路径放在我node.js安装的文件夹中，则在我安装的文件夹【D:\workspace\nodejs】下创建两个文件夹【node_global】及【node_cache】

创建完两个空文件夹之后，打开cmd命令窗口，输入

```cmd
npm config set prefix "D:\workspace\nodejs\node_global"
npm config set cache "D:\workspace\nodejs\node_cache"
```

接下来设置环境变量，关闭cmd窗口，“我的电脑”-右键-“属性”-“高级系统设置”-“高级”-“环境变量”

进入环境变量对话框，在【系统变量】下新建【NODE_PATH】，选择文件夹【D:\workspace\nodejs\node_global\node_modules】，将【用户变量】下的【Path】中原来在c盘中的npm修改为【D:\workspace\nodejs\node_global】

配置完后，安装个module测试下，我们就安装最常用的express模块，打开cmd窗口，
输入如下命令进行模块的全局安装：

```
npm install express -g     # -g是全局安装的意思
```

## Zookeeper安装配置

官网下载地址：http://www.apache.org/dyn/closer.cgi/zookeeper/，下载bin.tar.gz然后解压。

进入conf目录，有一个zoo_sample.cfg文件，将其复制修改为zoo.cfg，因为zookeeper启动时默认会找这个文件。然后打开该文件，修改 dataDir=D:\\\workspace\\\apache-zookeeper-3.5.8-bin\\\data,复制data路径的时候默认是单斜线，**一定要改为双斜线**。如果没有dataLogDir的话，可以加一个dataLogDir=D:\\\workspace\\\apache-zookeeper-3.5.8-bin\\\log。

## Linux下Redis的安装配置

一、基本配置

1.使用xshell连接到Linux

2.使用xftp将redis的linux版本传输到 /tmp目录下(根目录下的tmp目录)

3.使用tar -zvxf redis-5.0.5.tar.gz解压

4.移动目录：mv redis-5.0.5(即解压后的文件) /usr/local/redis(重命名为redis)  一般安装软件都放在usr/local/目录下,

如果提示权限不够，使用su root切换到root账户

5.使用cd /usr/local/redis进入目录中

6.使用make -j 4命令编译，其中-j加快编译速度，4是cpu个数，该虚拟机为4核，如果出现什么cc未找到命令的错误，就是系统没有gcc环境，因为redis是用c写的。解决办法：(1)yum install gcc-c++,yum安装gcc;(2)make distclean,清空上次编译失败残留文件，一定要执行这步，不然还是报错。（3）再次编译:make -j 4

7.make install:把编译完成的可执行文件添加到启动目录里面

8.配置redis.conf文件，使用vim redis.conf编辑

​	（1）将bind 127.0.0.1(默认只允许本机访问)改为0.0.0.0(允许所有访问，因为后面要做一个分布式的业务)

​	（2）将daemonize改为yes,允许后台执行

9.redis-server ./redis.conf启动redis

10.使用ps -ef | grep redis查看进程

11.redis-cli进入redis

12.为了安全起见，可以在redis.conf中找到requirepass设置密码，然后redis-cli登录后使用shutdown save,exit完成重启

13.重启后，使用redis-cli -a 12345进入

二、将redis做成服务

1.cd utils/

2.找到install_server.sh

3../install_server.sh执行该脚本文件

4.（1）根据提示选择默认端口，enter键即可

​	（2）选择配置文件，输入 /usr/local/redis/redis.conf

​	（3）选择日志文件，输入 /usr/local/redis/redis.log

​	（4）选择数据文件，输入 /usr/local/redis/data

​	（5）executable path，按enter默认即可

​	（6）最后再按一次enter

5.chkconfig --list | grep redis查看服务

![](https://s3.ax1x.com/2021/01/10/slOujg.png)

6.使用systemctl(centos7下) status redis_6379查看状态，systemctl stop redis_6379关闭，systemctl start redis_6379开启，然后使用ps -ef | grep redis看到该服务

7.实际上会在etc/init.d目录下生成一个redis_6379文件，里面可以改成我们想要的配置。其实就是系统帮我们生成的文件

![](https://s3.ax1x.com/2021/01/10/slXjSK.png)

