## 编写Dockerfile文件

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
# -t表示指定镜像仓库名称/镜像名称：镜像标签 . 表示当前目录下的Dockerfile,所以-f参数可以省略
docker build -t redis-test/redis-docker-file:0.0.1-SNAPSHOT .
~~~

输出如下信息：

![](https://s3.ax1x.com/2021/01/13/sNeNHU.png)

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
docker run -p 8080:8080 --name redis-docker-file -d redis-test/redis-docker-file:0.0.1-SNAPSHOT
~~~

9.访问接口文档地址：http:193.168.136.15:8080/swagger-ui.html#!。由于该项目是redis项目，所以还要配置redis,否则会报无法连接redis