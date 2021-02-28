## 虚拟机网络配置

1.桥接模式配置，若没有net0要添加一个，net0就代表桥接模式

![](https://s3.ax1x.com/2021/02/28/6CXdqe.png)

已桥接那里选择物理机 上面的无线网就好了

2.net模式设置

![](https://s3.ax1x.com/2021/02/28/6CXhZQ.png)

net设置

![](https://s3.ax1x.com/2021/02/28/6CXTGq.png)

3.确认要启动的虚拟机的网络适配器类型是"NAT"模式，选择虚拟机配置选择模式

4.修改文件**vim /etc/sysconfig/network-scripts/ifcfg-ens33*

修改前：

~~~shell
TYPE=Ethernet
PROXY_METHOD=none
BROWSER_ONLY=no
BOOTPROTO=dhcp
DEFROUTE=yes
IPV4_FAILURE_FATAL=no
IPV6INIT=yes
IPV6_AUTOCONF=yes
IPV6_DEFROUTE=yes
IPV6_FAILURE_FATAL=no
IPV6_ADDR_GEN_MODE=stable-privacy
NAME=ens33
UUID=49b33a65-c5e2-4e4a-a281-cbf13a513e0e
DEVICE=ens33
ONBOOT=no
~~~

修改后

~~~shell
TYPE=Ethernet
PROXY_METHOD=none
BROWSER_ONLY=no
BOOTPROTO=static #这里改为static
DEFROUTE=yes
IPV4_FAILURE_FATAL=no
IPV6INIT=yes
IPV6_AUTOCONF=yes
IPV6_DEFROUTE=yes
IPV6_FAILURE_FATAL=no
IPV6_ADDR_GEN_MODE=stable-privacy
NAME=ens33
UUID=49b33a65-c5e2-4e4a-a281-cbf13a513e0e
DEVICE=ens33
ONBOOT=yes #这里要将no改为yes
IPADDR=192.168.136.128 #ip地址随便配置，但是必须同VMnet8的子网IP在同一网段
NETMASK=255.255.255.0  #子网掩码
GATEWAY=192.168.136.2  #网关，这个值与我们在上面NAT设置设置的网关一样
DNS1=192.168.136.2  #DNS的值也跟上面的NET设置中设置的网关一样
~~~

5.重启服务

~~~shell
systemctl restart network.service
~~~

如果ping不通的话重启虚拟机

~~~shell
reboot
~~~

* 查看防火墙

~~~shell
systemctl status firewalld
~~~

* 关闭防火墙，下次重启仍然生效

~~~shell
systemctl stop firewalld
~~~

* 永久关闭

~~~shell
systemctl disable firewalld
~~~

配置好后重启

#### 虚拟机克隆

右键虚拟机->管理，然后选择克隆，创建完整克隆，其他默认，克隆完成后将ifcfg-ens33中的ip地址最后一位随便改个其他的就可以了，然后重启服务或者重启虚拟机，使用xshell连接测试。如果连接不上那么多试几次，实在不行将虚拟机网络配置重置，然后再把被克隆的虚拟机和克隆的虚拟机的ifcfg-ens33重新配置。

## Linux配置本地Java环境

* 先卸载默认的openjdk

  * 查看openjdk的位置

    ~~~shell
    -- linux管道命令grep
    
    [cxylk@localhost ~]$ rpm -qa | grep java
    java-1.8.0-openjdk-headless-1.8.0.131-11.b12.el7.x86_64
    javapackages-tools-3.4.1-11.el7.noarch
    tzdata-java-2017b-1.el7.noarch
    java-1.7.0-openjdk-headless-1.7.0.141-2.6.10.5.el7.x86_64
    java-1.7.0-openjdk-1.7.0.141-2.6.10.5.el7.x86_64
    java-1.8.0-openjdk-1.8.0.131-11.b12.el7.x86_64
    python-javapackages-3.4.1-11.el7.noarch
    ~~~

  * 使用root账户卸载，除了.noarch不需要卸载，其余全部卸载

    ~~~shell
    [root@localhost cxylk]# rpm -e --nodeps java-1.8.0-openjdk-headless-1.8.0.131-11.b12.el7.x86_64
    [root@localhost cxylk]# rpm -e --nodeps java-1.7.0-openjdk-headless-1.7.0.141-2.6.10.5.el7.x86_64
    [root@localhost cxylk]# rpm -e --nodeps java-1.7.0-openjdk-1.7.0.141-2.6.10.5.el7.x86_64
    [root@localhost cxylk]# rpm -e --nodeps java-1.8.0-openjdk-1.8.0.131-11.b12.el7.x86_64
    [root@localhost cxylk]# rpm -qa | grep java
    javapackages-tools-3.4.1-11.el7.noarch
    tzdata-java-2017b-1.el7.noarch
    python-javapackages-3.4.1-11.el7.noarch
    ~~~

* 在/opt/目录下新建soft目录，修改权限为777

  ~~~shell
  [cxylk@localhost opt]$ mkdir soft
  [cxylk@localhost opt]$ sudo chmod 777 soft
  ~~~

* 利用xftp将jdk进行上传，并解压

  ~~~shell
  tar -zxvf jdk....tar.gz
  ~~~

* 给/etc/profile授权,先切换到root

  ~~~shell
  chmod 777 profile
  ~~~

* 编辑该文件，在末尾添加

  ~~~shell
  export JAVA_HOME=/opt/soft/jdk1.8.0_201
  export export PATH=$PATH:${JAVA_HOME}/bin
  ~~~

* 测试是否成功(source FileName作用是在当前环境下读取并执行FileName中的命令，能够立即生效)

  ~~~shell
  source profile
  java -version
  ~~~

  如果不能立即生效，重启虚拟机即可

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

3.使用tar -zxvf redis-5.0.5.tar.gz解压

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

## Linux下nginx的安装配置

#### 1.打开官网

http://nginx.org/，下载2017的nginx-1.21.1版本

#### 需要装的素材(依赖)：

pcre-8.37.tar.gz

openssl-1.0.1t.tar.gz

zlib-1.2.8.tar.gz

nginx-1.11.1.tar.gz

安装方法：

* 1.可以通过wget命令安装，例如安装pcre-8.37.tar.gz

  ~~~shell
  wget http://downloads.sourceforge.net/project/pcre/pcre/8.37/pcre-8.37.tar.gz /usr/src/(安装到该目录下)
  ~~~

  使用tar -xvf解压后进入该文件，然后使用./configure命令进行检查，如果出现You need a C++ compiler for C++ support错误，则安装c++ compiler，否则编译的时候会报错

  ~~~shell
  yum install -y gcc gcc-c++
  ~~~

  然后使用如下命令编译并安装

  ~~~shell
  make && make install
  ~~~

  然后使用pcre-config --version查看版本

* 2.通过yum命令安装其他依赖

  ~~~shell
  yum -y install make zlib zlib-devel gcc-c++ libtool openssl openssl-devel
  ~~~

#### 安装nginx

安装到/usr/src下

将文件拖入该目录，然后解压:tar -xvf ,解压完成后进入该目录，使用./configure命令检查，最后使用

~~~shell
make && make install
~~~

进行编译执行

安装成功后，在usr目录多出来一个文件夹local/nginx，在nginx中有sbin里面有启动脚本

#### 启动

进入上一步的sbin目录，然后使用

~~~shell
./nginx命令启动
~~~

使用ps -ef | grep nginx查看

![](https://s3.ax1x.com/2021/01/16/sDKQ9x.png)

可以看到一个master和worker进程，这个后面再讲

然后浏览器中根据ip和80端口可以访问。该页面不能访问的话关闭防火墙，或者使用以下命令查看开放端口

~~~shell
firewall-cmd --list-all
~~~

设置开放的端口号

~~~shell
sudo firewall-cmd --add-port=80/tcp --permanent
~~~

重启防火墙

~~~shell
firewall-cmd --reload
~~~

## Linux中tomcat安装

上传安装包到/usr/src目录下，解压

解压完成后进入该目录，进入/bin目录，使用./startup.sh启动即可。可以进入tomcat目录下的logs目录，然后使用tail -f catalina.out查看日志文件

## Linux下rabbitmq安装配置

使用docker安装。