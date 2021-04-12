## 简介

以前我们使用DockerFile来创建镜像，但这种方式都是手动的，并且是单个容器。而Docker Compose可以帮助我们轻松高效的管理容器，定义运行多个容器。

具体介绍和说明可以查看官方文档：docs.docker.com，produce manuals

#### 三步骤

Using Compose is basically a three-step process:

1. Define your app’s environment with a `Dockerfile` so it can be reproduced anywhere.
2. Define the services that make up your app in `docker-compose.yml` so they can be run together in an isolated environment.
3. Run `docker-compose up` and Compose starts and runs your entire app.

**几个重要概念**

* 服务services，容器，应用。
* 项目project，一组关联的容器

#### 安装

1.下载

官方给的地址，比较慢

~~~shell
sudo curl -L "https://github.com/docker/compose/releases/download/1.3.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
~~~

换成下面这个快点

~~~shell
curl -L https://get.daocloud.io/docker/compose/releases/download/1.24.0/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
~~~

**注意**：试了很多版本都不行，会报Unsupported config option for services service: 'web'这个错，但是上面这个版本即

1.24.0可以不会报错，不知道为啥

2.赋予权限

sudo chmod +x docker-compose

3.docker-compose --verison查看是否安装成功

#### 快速开始

根据官网例子：https://docs.docker.com/compose/gettingstarted/

例子的话都在本机的home目录下面进行操作。

查看镜像和容器

![](https://s3.ax1x.com/2021/01/23/s7Zrxf.png)

查看网络

~~~shell
[root@local cxylk]# docker network ls
NETWORK ID     NAME                  DRIVER    SCOPE
514e29789261   bridge                bridge    local
1dbb21b3ca75   composetest_default   bridge    local
46d6d1418071   host                  host      local
2b0ff8fd8743   none                  null      local
[root@local cxylk]# 
~~~

查看细节

~~~shell
docker network inspect composetest_default
~~~

![](https://s3.ax1x.com/2021/01/23/s7e4TH.png)

我们通过name访问好处就是，就算ip挂掉了，再起一个ip，又可以映射到name上。

#### yaml规则

文档地址：https://docs.docker.com/compose/compose-file/

3层

~~~yaml
version: "" #版本
services: #服务
   服务1：web
     #服务配置
     images
     build
     ...
   服务2: redis
   ...
   服务3：mysql
~~~

举个例子

**depends_on**

~~~yaml
version: "3.9"
services:
  web:
    build: .
    depends_on:
      - db
      - redis
  redis:
    image: redis
  db:
    image: postgres
~~~

该命令是依赖的意思，比如上面的例子，web服务依赖于db和redis，当启动web时会先启动db和redis

## 部署SpringBoot项目

#### 编写docker-compose.yml文件

例：

~~~yml
version: '3'
services:
  # 指定服务名称
  db:
    # 指定服务使用的镜像
    image: mysql:8.0
    # 指定容器名称
    container_name: mysql
    # 执行命令
    command: mysqld --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    # 开机启动
    restart: always
    # 指定服务运行端口
    ports:
    - 3306:3306
    # 指定容器挂载文件
    volumes:
    - /lkuse/mysql/conf:/etc/mysql/conf.d #配置文件挂载
    - /lkuse/mysql/data:/var/lib/mysql #数据文件挂载
    - /lkuse/mysql/log:/var/log/mysql #日志文件挂载
    # 指定容器的环境变量，即设置密码
    environment:
      - MYSQL_ROOT_PASSWORD=root
  redis:
    image: redis:5.0.5
    container_name: redis
    # 指定配置文件和密码
    command: redis-server /usr/local/etc/redis/redis.conf --requirepass 12345 --appendonly yes
    ports:
    - 7369:6379
    volumes:
    - /lkuse/myredis/conf/redis.conf:/usr/local/etc/redis/redis.conf
    - /lkuse/myredis/data:/data
  rabbitmq:
    image: rabbitmq:latest
    container_name: rabbitmq
    ports:
      - 5672:5672
      - 15672:15672
    volumes:
    - /lkuse/rabbitmq/data:/var/lib/rabbitmq 
    - /lkuse/rabbitmq/log:/var/log/rabbitmq 
  seckill-docker-file:
    image: seckill-docker-file:0.0.1-SHAPSHOT
    container_name: seckill-docker-file
    # 下面三个服务启动后再启动该服务
    depends_on:
      - db
      - redis
      - rabbitmq
    ports:
    - 8080:8080
    volumes:
    - /etc/localtime:/etc/localtime
    - /lkuse/app/seckill-docker-file/logs:/var/logs
~~~

#### 打包并上传

将当前项目打成jar包，和yml文件上传到服务器。如果当前jar包没有构建成镜像，需要构建成镜像(可以使用dockerfile)。

#### 启动

在当前目录下执行命令,所以 -f 指定yml文件可以省略

~~~shell
docker-compose up -d
~~~

需要注意的是，yml文件中的中文注释要去掉，否则会被解码错误。