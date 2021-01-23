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