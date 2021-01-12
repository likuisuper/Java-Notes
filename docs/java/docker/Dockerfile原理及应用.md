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