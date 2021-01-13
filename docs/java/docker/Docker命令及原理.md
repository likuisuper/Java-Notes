## Docker的基本组成

鲸鱼背上有集装箱

蓝色的大海里面----宿主机系统比如Windows

鲸鱼-----docker

集装箱------容器实例  from 来自我们的镜像模板

1.镜像(image)

2.容器(container)

3.仓库(repository)

Docker镜像就是一个只读的模板。镜像可以用来创建Docker容器，一个镜像可以创建很多容器。

容器可以类比面向对象中的对象，镜像可以类比面向对象中的类。**容器是用镜像创建的运行实例**，可以把容器看作一个简易版的linux环境和运行在其中的运行程序。

仓库是**集中存放镜像文件**的场所，是一个运行时环境，就是鲸鱼上的集装箱

## Docker安装

* 安装yum-utils:

  ~~~shell
  yum install -y yum-utils device-mapper-persistent-data lvm2
  ~~~

* 为yum源添加docker仓库位置；

  ~~~shell
  #官方源
  yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
  #阿里云
  yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
  ~~~

* 安装docker服务

  ~~~shell
  yum install docker-ce
  ~~~

* 启动docker服务

  ~~~shell
  systemctl start docker
  ~~~

  

## 配置阿里云镜像加速

打开https:dev.aliyun.com，进入镜像加速器，按照步骤配置即可

如果无法启动docker,出现Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?错误：

1、先使用su root切换到root账户

2、systemctl enable docker #开机自动启动docker

3、systemctl start docker #启动docker

4、systemctl restart docker#重启docker

## 基本命令

一、镜像命令

* docker run:从镜像文件中运行容器实例

* docker info:查看docker信息，比如多少容器，多少镜像等

* docker --help:类似linux上的man命令(有问题找男人)

* docker images:列出本地主机上的所有镜像

  ![](https://s3.ax1x.com/2021/01/07/sehLmd.png)

  其中：REPOSITORY表示镜像的仓库源，TAG表示镜像的标签，代表这个仓库源的不同版本，IMAGE ID表示镜像ID,CREATED表示镜像创建时间，SIZE表示镜像大小

  * docker images -a:查出所有镜像(包含中间镜像),镜像是一层套一层的，类似千层饼
  * docker images -q:查出当前所有镜像的id
  * docker images -qa:两者结合，确定一个镜像的唯一id,比如删除一个镜像

* docker search 镜像名字:查找镜像

  * 其中stars类似github上面的star，可以用docker search -s 30 tomcat搜索star>30的镜像(现在-s已经被弃用了，使用--filter替代)，docker search --filter=stars=30 tomcat
  * OFFICIAL表示是否是官方的，ok代表是

* docker pull 镜像名称:从仓库里拉取镜像

  docker pull 镜像名称 不加版本号=docker pull 镜像名称:latest,默认下载最新版本

* docker rmi 镜像名称(镜像id)：删除镜像，如果不加版本号，默认删除版本为latest的镜像，所以**一定要加版本**避免误删

  * 使用-f参数强制删除:docker rmi -f 镜像名称

* docker rmi 镜像名称 镜像名称:加空格删除多个镜像(有i是删除镜像，没i是删除容器)

* docker rmi -f $(docker images -q或者-qa):删除全部镜像

二、容器命令

--name="容器新名字":为容器指定一个名称

-d:后台运行容器，并返回容器ID，也即启动守护式容器

**-i: 以交互模式运行容器，通常与-t同时使用**

**-t: 为容器重写分配一个伪输入终端，通常与-i同时使用**

-P: 随机端口映射；

-p: 指定端口映射

* docker ps:列出当前正在运行的所有容器
  * -a:作用正在运行的+历史上运行过的
  * -l:显示最近创建的容器
  * -n:显示最近n个创建的容器
  * **-q:静默模式，只显示容器编号**
* **docker run -it 镜像id(或者使用--name指定名称):新建并登录进入容器**
  
  * 后面加入/bin/bash表示可以使用shell脚本，比如docker run -it 容器id(name) /bin/bash
  
    加不加影响不大，因为很多容器的dockerfile默认最后一行都加了这句
* **docker run -d 启动守护式容器（后台运行）**
* docker start 容器id或者容器名:启动容器
* docker attach 容器id:重新进入容器
* **docker exec 容器id:在容器外面对容器进行操作**，当使用-d后台启动时，可以使用该命令类似于隔空取物操作
  
  * 后面加入/bin/bash表示可以使用shell脚本，比如docker exec -it 容器id /bin/bash
* **退出容器，两种方式**
  * (1)docker exit:容器停止退出
  * (2)ctrl+P+Q:容器不停止退出(使用attach重新进入)
* docker restart 容器id或者名称:重启容器
* 停止容器:docker stop 容器id或者容器名
* 强制停止容器:docker kill 容器id或者容器名
* 删除容器:docker rm 容器id或者容器名
* **删除全部容器和删除全部镜像一样，不过rmi后面没有i，docker rm -f $ (docker ps -q)**
* 查看容器日志:docker logs -f -t --tail 容器id,-t是加入时间戳，-f跟随最新的日志打印，-tail 数字:显示最后多少条
* 查看容器内的进程:docker top 容器id
* 查看容器内部细节:docker inspect 容器id，内容是json字符串
* **docker容器内部文件拷贝到宿主机:docker cp 容器id:要拷贝文件的地址 目标地址(宿主机下)**(反之亦然)

## 镜像原理

![](https://s3.ax1x.com/2021/01/08/suIqZF.png)

## 使用

1.启动tomcat

docker run -it -p 8888:8080 tomcat

其中-it表示交互式终端，-p表示指定端口，8888:8080表示将tomcat默认的8080端口指定成8888通过docker暴露给外界，否则外界是访问不到的。

如果这里的-p换成-P即大P，则表示一个随机端口

新版本的tomcat启动后但是不能访问，因为webapps里面什么内容都没有，欢迎页在webapps.dist中，解决方法：将原来的webapps重命名为webapps2,将webapps.dist重命名为webapps。首先使用

docker exec -it tomcat容器id /bin/bash进入该容器，使用mv命令重命名，并且exit退出容器

![](https://s3.ax1x.com/2021/01/08/sKkPIJ.png)

刷新浏览器即可成功访问tomcat首页。但是这只是修改了当前容器的配置，当再启动一个容器时，依然会报404，因为容器是根据镜像生成（可以类比类和实例），只有将镜像修改了，再生成的容器才不会出现这个问题。解决办法就是使用docker commit命令将生成的容器生成镜像（镜像可以生成容器，反过来，根据容器也可以得到镜像）。

在上一步中（还没有使用exit退出容器时）：（1）按ctrl+p+q(不退出容器的方式返回到宿主机)；（2）使用docker ps -l查看容器id；（3）使用git commit -a='lk' -m='ssssss' 容器id lk/tomcat提交，其中-a是作者名，-m是注释，lk/tomcat是新生成的镜像名

![](https://s3.ax1x.com/2021/01/11/s8A7uj.png)

## 容器数据卷

作用：数据持久化（相当于redis中的rdb和aof的作用）

特点:

* 1.数据卷可在容器之间共享或重用数据
* 2.卷中的更改可以直接生效
* 3.数据卷中的更改不会包含在镜像的更新中
* 4.数据卷的生命周期一直持续到没有容器使用它为止

使用docker run -it -v /宿主机绝对路径目录:/容器内目录 镜像名，让宿主机和容器进行数据共享和对接：

1.首先在宿主机根目录下和容器根目录下查看是否有volumeContainer目录，这是要演示的目录，现在都没有

![](https://s3.ax1x.com/2021/01/11/s8QlZD.png)

2.执行命令docker run -it -v /volumeContainer:/volumeContainer centos

![](https://s3.ax1x.com/2021/01/11/s8ll60.png)

可以看到，在宿主机和容器内都生成了一个volumeContainer目录

3.使用docker inspect 容器id查看内部细节

![](https://s3.ax1x.com/2021/01/11/s81ox1.png)

可以看到两者进行了绑定

4.演示两者间的数据同步

![](https://s3.ax1x.com/2021/01/11/s8JViD.png)

5.容器退出后数据是否还存在

先将容器退出:exit，然后start启动，使用docker attach 刚才退出的容器id

![](https://s3.ax1x.com/2021/01/11/s8tvi8.png)

可以看到，即使容器退出，但是主机在该目录下的数据写入还是能被容器共享

6.带权限命令

docker run -it -v /volumeContainer:/volumeContainer:ro centos,加了ro,就是read only只读的意思，容器只能对主机的数据而并不能写入

6.使用dckfile完成容器数据卷的管理

说明：由于可移植性和分享的考虑，**用-v 主机目录:容器目录这种方法不能够直接在Dockerfile中实现**

使用VOLUME指令来给镜像添加一个或多个数据卷

首先编写dockfile文件

宿主机下根目录新建一个mydocker文件夹，vim Dockerfile

~~~bash
FROM centos
VOLUME ["/volumeContainer1","/volumeContainer2"]
CMD echo "finshed,-----success1"
CMD /bin/bash
~~~

然后使用build命令构建：docker build -f /mydcker/Dockerfile -t lk/centos .

-f表示文件,指明文件在哪，后面跟路径，当前目录下可不加，-t是命名空间，后面加镜像名。最后还有一个点.

结果如下

![](https://s3.ax1x.com/2021/01/11/s8yw3q.png)

使用dokcer inspect 容器id查看

~~~json
"Volumes": {
    "/volumeContainer1": {},
    "/volumeContainer2": {}
},
~~~

第一个路径是容器内的，第二个路径是宿主机的,上图显示为{},路径是/var/lib/docker/volumes/一大串文件名/_data

进入该目录下，新建一个文件，容器内将能看到该文件

## 容器间的数据共享

1.先使用docker run -it --name dc01 lk/centos(以该镜像作为模板)启动容器作为父容器

进入上一步创建的volumeContainer2

使用touch dc01_add.txt创建文件，然后ctrl+P+Q不停止容器退出

2.docker run -it --name dc02 --volumes-from dc01 lk/centos启动容器，将dc01作为父容器

进入volumeContainer2,使用ls -l命令可以看上dc01容器创建的dc01_add.txt文件，然后使用

touch dc02_add.txt创建一个文件，ctrl+P+Q退出

3.docker run -it --name dc03 --volumes-from dc01 lk/centos启动容器，还是将dc01作为父容器，

然后进入volumeContainer2,可以看到dc01_add.txt和dc02_add.txt文件，然后touch dc03_add.txt

4.使用docker attach dc01重新进入容器dc01,可以看到子容器新建的文件，达到父子之间数据共享

~~~shell
[root@local /]# docker ps
CONTAINER ID   IMAGE       COMMAND                  CREATED              STATUS              PORTS     NAMES
f70e5f23eca7   lk/centos   "/bin/sh -c /bin/bash"   About a minute ago   Up About a minute             dc03
e22bf82083dd   lk/centos   "/bin/sh -c /bin/bash"   7 minutes ago        Up 7 minutes                  dc02
602f2da12a61   lk/centos   "/bin/sh -c /bin/bash"   14 minutes ago       Up 14 minutes                 dc01
[root@local /]# docker attach dc01
[root@602f2da12a61 volumeContainer2]# pwd
/volumeContainer2
[root@602f2da12a61 volumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jan 12 13:40 dc01_add.txt
-rw-r--r--. 1 root root 0 Jan 12 13:50 dc02_add.txt
-rw-r--r--. 1 root root 0 Jan 12 13:54 dc03_add.txt
[root@602f2da12a61 volumeContainer2]# 
~~~

进入容器dc02和容器dc03也是一样的

下面做这样一件事，删除dc01(即父容器),然后进入dc02容器创建一个文件，看dc03是否能共享这个文件

~~~shell
[root@local /]# docker ps
CONTAINER ID   IMAGE       COMMAND                  CREATED          STATUS          PORTS     NAMES
f70e5f23eca7   lk/centos   "/bin/sh -c /bin/bash"   13 minutes ago   Up 13 minutes             dc03
e22bf82083dd   lk/centos   "/bin/sh -c /bin/bash"   19 minutes ago   Up 19 minutes             dc02
602f2da12a61   lk/centos   "/bin/sh -c /bin/bash"   26 minutes ago   Up 25 minutes             dc01
[root@local /]# docker rm -f dc01 #删除容器dc01
dc01
[root@local /]# docker ps
CONTAINER ID   IMAGE       COMMAND                  CREATED          STATUS          PORTS     NAMES
f70e5f23eca7   lk/centos   "/bin/sh -c /bin/bash"   13 minutes ago   Up 13 minutes             dc03
e22bf82083dd   lk/centos   "/bin/sh -c /bin/bash"   19 minutes ago   Up 19 minutes             dc02
[root@local /]# docker attach dc01
Error: No such container: dc01
[root@local /]# docker attach dc02 #进入容器dc02
[root@e22bf82083dd volumeContainer2]# pwd  
/volumeContainer2
[root@e22bf82083dd volumeContainer2]# touch dc02_update.txt #新建一个文件
[root@e22bf82083dd volumeContainer2]# read escape sequence
[root@local /]# docker attach dc03 #进入容器dc03
[root@f70e5f23eca7 volumeContainer2]# pwd
/volumeContainer2
[root@f70e5f23eca7 volumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jan 12 13:40 dc01_add.txt
-rw-r--r--. 1 root root 0 Jan 12 13:50 dc02_add.txt
-rw-r--r--. 1 root root 0 Jan 12 14:07 dc02_update.txt #可以看到容器dc02新建的文件，在父容器被删掉的情况下
-rw-r--r--. 1 root root 0 Jan 12 13:54 dc03_add.txt
[root@f70e5f23eca7 volumeContainer2]# 
~~~

由此可见，就算父容器挂掉了，容器间还是能进行数据的共享的。可以再建一个dc04,然后删除dc03演示

**结论，容器之间配置信息的传递，数据卷的生命周期一直持续到没有容器使用它为止**