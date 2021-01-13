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

-v参数后面左边的路径都是目录，并不是文件

5.在主机的/lkuse/myredis/conf/redis.conf目录下新建一个redis.conf配置文件，然后找个redis.conf文件将其中内容复制到里面，修改自己想修改的内容，比如端口等

6.启动

~~~shell
docker exec -it 容器id redis-cli(redis客户端)
~~~

7.随便设置几个值，然后shutdown后在/lkuse/myredis/data下看是否有appendonly.aof文件