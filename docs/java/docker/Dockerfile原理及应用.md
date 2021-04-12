## Dockerfile

对镜像的描述文件。

#### 构建三步骤：

* 手动编写一个dockerfile文件,当然，必须要符合file的规范
* 有这个文件后，直接docker build命令执行，获得一个自定义的镜像
* run

首先熟悉几个关键词

1.scratch:基础镜像，所有镜像的祖先类，类似于Java中的object类

#### 基础知识

* 每条保留字指令必须大写，且后面跟随至少一个参数
* 指令按照从上到下顺序执行
* #表示注释
* 每条指令都会创建一个新的镜像层，并对镜像进行提交

#### 执行流程

* docker从基础镜像运行一个容器
* 执行一条指令并对容器做出修改
* 执行类似docker commit的操作提交一个镜像
* docker再基于刚提交的镜像运行一个新容器
* 执行dockerfile中的下一条指令直到所有指令都执行完成

从应用软件的角度来看：

Dockerfile面向开发，Docker镜像成为交付标准，Docker容器则设计部署与运维，三者缺一不可

#### 体系结构(保留字)

FROM:基础镜像，即当前镜像是基于哪个镜像的

MAINTAINER:镜像维护者的姓名和邮箱地址

RUN:容器构建时需要运行的命令

EXPOSE:当前容器对外暴露的端口

WORKDIR:指定在创建容器后，终端默认登录进来的工作目录，一个落脚点

ENV:用来在构建镜像过程中设置环境变量

ADD:将宿主机目录下的文件拷贝进镜像且ADD命令会自动处理URL和解压tar压缩包。就是拷贝+解压缩，功能比copy强大

COPY:拷贝文件和目录到镜像中，将从构建上下文目录中<源路径>的文件/目录复制到新一层的镜像内的<目标路径>位置。两种写法：1.COPY src dest 2:COPY ["src","dest"]

VOLUME:容器数据卷，用于数据保存和持久化工作

CMD:指定一个容器启动时要运行的命令。可以用多个CMD指令，但只有最后一个会生效，CMD会被docker run 之后的参数替换

ENTRYPOINT:指定一个容器运行时要运行的命令。目的和CMD一样，都是在指定容器启动程序及参数。

​						**两者不一样的地方：ENTRYPOINT不会被docker run之后的参数替换，而是追加**

ONBUILD:当构建一个被继承的Dockerfile时运行命令，父镜像在被子继承后父镜像的onbuild被触发

## 应用

一、现在有这样一个需求：

1.进入centos容器后落脚点不让它为/，而是tmp路径

2.可以使用vim命令,因为拉下来的centos镜像相当于一个精简版的,只有内核

3.可以使用ifconfig命令

~~~shell
FROM centos #继承本地镜像的centos
ENV MYPATH /tmp #配置一个环境变量方便下面引用
WORKDIR $MYPATH #进入容器的落脚点，没有上一步就是WORKDIR /tmp
RUN yum -y install vim #安装vim
RUN yum -y install net-tools #安装net工具包
EXPOSE 80 #暴露端口
CMD echo $MYPATH #打印
CMD echo "success--------ok" #打印
CMD /bin/bash
~~~

构建镜像：使用命令:docker build -f /mydocker/Dockerfile2 -t mycentos:1.3 . 

-f:文件路径，-t:镜像名，1.3表示版本，最后一个点.表示当前路径

运行：

![](https://s3.ax1x.com/2021/01/12/sJ2yyn.png)

如上图表示构建成功。这时候使用docker run -it mycentos:1.3启动容器进入后，可以看到当前目录是/tmp,并且

可以使用vim和ifconfig命令。

使用docker history 5072f163e6bb查看过程

二、CMD命令：当我们启动一个tomcat的时候，docker run -it -p 8888:8080 tomcat,之所以能够启动，是因为在tomcat的Dockerfile文件中最后一行有

~~~shell
CMD ["catalina.sh", "run"]
~~~

现在在 docker run -it -p 8888:8080 tomcat 后面加上ls -l,那么这个ls -l就是覆盖上面的CMD，也就是不会启动成功,相当于

~~~shell
CMD ["catalina.sh", "run"]
ls -l
~~~

而ENTRYPOINT不会被替换，只会被追加。比如Dockerfile中有CMD ["curl","-s","http://www.baidu.com"]，现在要查出http标头，如果dockerfile最后是CMD的话，docker run启动后加一个-i参数会报错，但是如果Dockerfile最后是ENTRYPOINT的话，那么此时加一个-i，就相当于原来-s参数后面追加了一个-i

~~~shell
ENTRYPOINT ["curl","-s","-i","http://www.baidu.com"]
~~~

三、现在有一个Dockerfile1

~~~shell
FROM centos
RUN yum install -y curl
ENTRYPOINT ["catalina.sh", "run"]
ONBUILD RUN echo "father images onbuild----886"
~~~

然后使用build构建,加入构建后的镜像名叫father

新建一个Dockerfile2

~~~shell
From father
RUN yum install -y curl
CMD ["catalina.sh", "run"]
~~~

然后使用build构建当前文件成镜像，就会触发Dockerfile1父类中的ONBUILD,输出上面那句话

四、做一个tomcat

1.使用mkdir -p /mydockerfile/tomcat9创建目录，-p是递归创建，即使上级目录不存在

2.将tomcat和jdk的压缩包放到该目录下

3.编写dockerfile文件

~~~shell
FROM centos
MAINTAINERzzyy<cxylk@163.com>
#把宿主机当前上下文的c.txt拷贝到容器/usr/local/路径下并且重命名为cincontainer.txt
COPY c.txt /usr/local/cincontainer.txt
#把java与tomcat添加到容器中
ADD jdk-8u171-linux-x64.tar.gz /usr/local/
ADDapache-tomcat-9.0.8.tar.gz /usr/local/
#安装vim编辑器
RUN yum -y install vim
#设置工作访问时候的WORKDIR路径，登录落脚点(默认是根目录)
ENV MYPATH /usr/local
WORKDIR $MYPATH
#配置java与tomcat环境变量
ENV JAVA_HOME /usr/local/jdk1.8.0_171
ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar201
ENV CATALINA_HOME /usr/local/apache-tomcat-9.0.8
ENV CATALINA_BASE /usr/local/apache-tomcat-9.0.8
ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/lib:$CATALINA_HOME/bin
#容器运行时监听的端口
EXPOSE 8080
#启动时运行tomcat
# ENTRYPOINT ["/usr/local/apache-tomcat-9.0.8/bin/startup.sh”] 标准写法
# CMD["/usr/local/apache-tomcat-9.0.8/bin/catalina.sh" , "run"] json写法
CMD /usr/local/apache-tomcat-9.0.8/bin/startup.sh && tail -F /usr/local/apache-tomcat-9.0.8/bin/logs/catalina.out
~~~

tail是追加的意思

## 部署SpringBoot项目

#### 编写Dokerfile文件

例如：

~~~dockerfile
# 该镜像依赖的基础镜像
FROM java:8
# 将当前目录下的jar包复制到docker容器的/目录下
ADD redis-0.0.1-SNAPSHOT.jar /redis-0.0.1-SNAPSHOT.jar
# 运行过程中创建一个seckill.jar文件
RUN bash -c 'touch /seckill.jar'
# 声明服务运行在8080端口
EXPOSE 8080
# 指定docker容器启动时运行jar包
ENTRYPOINT ["java","-jar","/redis-0.0.1-SNAPSHOT.jar"]
# 指定维护者的名字
MAINTAINER cxylk
~~~

#### 使用maven打包项目

将Dockerfile和jar包上传到linux

~~~shell
[root@local mydocker]# ll
total 46152
-rw-rw-r--. 1 cxylk cxylk      492 Jan 13 22:08 Dockerfile
-rw-rw-r--. 1 cxylk cxylk 47252870 Jan 13 22:08 redis-0.0.1-SNAPSHOT.jar
~~~

#### 在linux上构建docker镜像

在Dockerfile所在目录下执行命令

~~~shell
# -t表示指定 镜像仓库名称/镜像名称：镜像标签 . 表示当前目录下的Dockerfile,所以-f参数可以省略
docker build -t redis-test/redis-docker-file:0.0.1-SNAPSHOT .
~~~

输出如下信息：

![](https://s3.ax1x.com/2021/01/13/sNeNHU.png)

因为该项目依赖mysql和redis，所以需要启动mysql和redis容器，如果一开始这两者就已经是用docker部署的话，那直接执行run就行了。

#### 运行mysql服务

1.启动

~~~shell
docker run -p 3306:3306 --name mysql #暴露端口为3306，指定名字为mysql
-v /lkuse/mysql/conf:/etc/mysql/conf.d #将主机的conf目录挂载到容器的/etc/mysql/conf.d目录
-v /lkuse/mysql/log:/var/log/mysql #将主机的logs目录挂载到容器的/var/log/mysql(也就是将日志文件映射到主机)
-v /lkuse/mysql/data:/var/lib/mysql #将主机的data挂载到容器的/var/lib/mysql
-e MYSQL_ROOT_PASSWORD=root #配置密码
-d mysql:8.0 #后台运行
~~~

2.进入容器

~~~shell
docker exec -it /bin/bash
~~~

3.打开客户端

~~~shell
mysql -uroot -proot --default-character-set=utf8
~~~

4.修改root账户权限，使得任何IP都能访问

~~~shell
grant all privileges on *.* to 'root'@'%'
~~~

5.创建数据库

~~~sql
create database mall character set utf8
~~~

6.将mall.sql文件上传到Linux，然后拷贝到容器的/目录下

~~~shell
docker cp /lkuse/mysql/mall.sql 运行容器id:/
~~~

7.进入数据库，导入sql文件

~~~sql
use mall;
source mall.sql;
~~~

#### 运行Redis服务

首先得将项目中的redis地址改为服务器地址

启动：

~~~shell
docker run -p 6379:6379 -v /lkuse/myredis/conf/redis.conf:/usr/local/etc/redis/redis.conf
-v /lkuse/myredis/data:/data -d redis:5.0.5 redis-server /usr/local/etc/redis/redis.conf
~~~

如果redis.conf需要配置密码的话，提前在文件中修改然后挂载到容器目录中。如果这种方式不行的话，先启动容器，然后去宿主机生成的conf文件中将conf文件删除，拷贝一份已有的conf文件，在里面配置密码，然后需要停止容器在启动。当用密码连接redis的时候，如果设置了密码但是提示没有密码，那么先停止容器再启动就好了。为了能让所有客服端能访问，将bind 127.0.0.1该为0.0.0.0(根据需求)

8.运行redis-test/redis-docker-file:0.0.1-SNAPSHOT应用

~~~shell
docker run -p 8080:8080 --name redis-docker-file 
-v /etc/localtime:/etc/localtime 
-v /lkuse/app/seckill-docker-file/logs:/var/logs
-d redis-test/redis-docker-file:0.0.1-SNAPSHOT
~~~

9.访问接口文档地址：http:193.168.136.15:8080/swagger-ui.html#!。由于该项目是redis项目，所以还要配置redis,否则会报无法连接redis