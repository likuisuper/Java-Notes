## 总体步骤

搜、拉、看

## 安装Mysql

1.首先使用docker search --filter=stars=10 mysql查看前10个镜像

~~~shell
docker search --filter=stars=10 mysql
~~~

2.docker pull mysql:5.7拉取5.7版本的mysql

~~~shell
docker pull mysql:8.0
~~~

3.docker images查看是否拉取成功

~~~shell
docker images
~~~

4.启动容器

~~~shell
docker run -p 3306:3306 --name mysql #暴露端口为3306，指定名字为mysql
-v /lkuse/mysql/conf:/etc/mysql/conf.d #将主机的conf目录挂载到容器的/etc/mysql/conf.d目录
-v /lkuse/mysql/log:/var/log/mysql #将主机的logs目录挂载到容器的/var/log/mysql(也就是将日志文件映射到主机)
-v /lkuse/mysql/data:/var/lib/mysql #将主机的data挂载到容器的/var/lib/mysql
-e MYSQL_ROOT_PASSWORD=root #配置密码
-d mysql:8.0 #后台运行
~~~

执行命令后会在主机根目录下自动创建-v参数后左边的目录，可以阅读容器数据卷相关内容。并且-v参数后面右边的容器内路径一定要是绝对路径

5.docker ps查看

6.进入容器

~~~shell
docker exec -it 容器id /bin/bash
~~~

7.进入该容器的mysql

~~~shell
mysql -uroot -proot (可选)--default-character-set=utf8
~~~

8.修改root账户的权限，使得任何ip都能够访问(可选)

~~~shell
grant all privileges on *.* to 'root'@'%'
~~~



9.数据库中新建数据库，然后建表插入数据，外部使用navcat连接该数据库看是否能查询成功

10.数据备份(可不做)

~~~shell
docker exec 容器id sh -c 'exec mysqldump --all-databases -uroot -p"root"'>/mysql/all_databases.sql
~~~

就是将数据库中的数据导出到主机中的/mysql/all_databases.sql

**说明：sh -c命令可以让 bash 将一个字串作为完整的命令来执行（linux命令，不是docker命令)**

## 安装Redis

1.查找镜像。。。。

2.docker pull redis:3.2

3.。。。

4.运行

~~~shell
docker run -p 6379:6379 
-v /lkuse/myredis/data:/data 
-v /lkuse/myredis/conf/redis.conf:/usr/local/etc/redis/redis.conf 
-d redis:3.2 redis-server /usr/local/etc/redis/redis.conf 
--appendonly yes #开启持久化(aof)
~~~

redis.conf文件中不能将daemon即后台启动设置为yes，否则会使redis-server不能用配置文件启动。

-v参数后面左边的路径都是目录，并不是文件

5.在主机的/lkuse/myredis/conf/redis.conf目录下新建一个redis.conf配置文件，然后找个redis.conf文件将其中内容复制到里面，修改自己想修改的内容，比如端口等

6.启动

~~~shell
docker exec -it 容器id redis-cli(redis客户端)
~~~

7.随便设置几个值，然后shutdown后在/lkuse/myredis/data下看是否有appendonly.aof文件

## 安装rabbitmq

1.下载最新版本

~~~shell
docker pull rabbitmq
~~~

2.启动

~~~shell
docker run -p 5672:5672 -p 15672:15672 --name rabbitmq -d rabbitmq
~~~

**注意**：5672是应用访问端口，15672是控制台web访问端口

3.进入容器开启管理功能

~~~shell
docker exec -it rabbitmq /bin/bash
rabbitmq-plugins enable rabbitmq_management

The following plugins have been configured:
  rabbitmq_management
  rabbitmq_management_agent
  rabbitmq_prometheus
  rabbitmq_web_dispatch
Applying plugin configuration to rabbit@49cc858010bf...
The following plugins have been enabled:
  rabbitmq_management

started 1 plugins.
~~~

4.可在容器外开启防火墙

~~~shell
firewall-cmd --zone=public --add-port=15672/tcp --permanent
firewall-cmd --reload
~~~

5.浏览器访问15672查看是否能访问成功

6.guest账号默认只能访问localhost，所以如果要访问远程服务器，可以配置一个用户。

* 创建账号并设置角色为管理员：cxylk cxylk

  ![](https://s3.ax1x.com/2021/02/09/yaoPII.png)

* 创建一个新的虚拟host为/cxylk

  ![](https://s3.ax1x.com/2021/02/09/yaokJP.png)

* 点击cxylk用户进入配置界面

  ![](https://s3.ax1x.com/2021/02/09/yaoesg.png)

* 给cxylk用户配置该虚拟host权限

  ![](https://s3.ax1x.com/2021/02/09/yaouZj.png)

配置好之后，在yml或properties中修改为如下配置

~~~properties
spring.rabbitmq.username=cxylk
spring.rabbitmq.password=cxylk
spring.rabbitmq.virtual-host=/cxylk
~~~

## Portainer

这是一款轻量级的应用，它提供了图形化界面，用于方便的管理docker环境，包括单击环境和集群环境。使用它可以方便对我们的容器进行管理。

* 官网地址：https://github.com/portainer/portainer

* 获取镜像文件

  ~~~shell
  docker pull portainer/portainer
  ~~~

* 运行容器

  ~~~shell
  docker run -p 9000:9000 -p 8000:8000 --name portainer --restart=always -v /var/run/docker.sock:/var/run/docker.sock -v /lkuse/portainer/data:/data -d portainer/portainer
  ~~~

* 访问9000端口，第一次访问默认会创建账号，这里用户名输入cxylk，密码cxylikui

* 在仪表盘中可以查看所有的镜像信息，容器信息，以及启动日志，并且可以进入容器操作等。。。