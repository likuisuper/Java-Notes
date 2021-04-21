#### 查看cpu核数

~~~shell
cat /proc/cpuinfo | grep processor
~~~

#### 上传文件

使用 rz 命令打开文件管理器上传到Linux中

#### 下载文件

使用 sz 命令将文件下载到windows中

~~~shell
sz 文件名
~~~

#### 查看文件尾

~~~shell
tail -f 文件名
~~~

#### jar包运行结果追加到nohup.out

~~~shell
nohup java -jar jar包名 &
~~~

然后可以使用tail -f  nohup.out查看是否成功

#### sysctl -p

使修改文件立即生效

#### watch

没隔2s执行命watch后面的命令，便于监控一些网络信息。比如keepalived信息

~~~shell
watch ipvsadm -L -n -c
~~~

监控当前机器上相关的请求连接

#### rpm

查找安装的软件。

比如查找本机安装的jdk

~~~shell
rpm -qa | grep java
~~~

