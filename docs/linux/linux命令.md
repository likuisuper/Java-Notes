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

#### 后台运行

可以使用nohup命令，即no hang up（不挂起），即使命令退出程序还要在。当前程序不能霸占交互命令行，而是应该在后台运行，所以要在最后加一个&，表示后台运行。

比如后台运行jar包，jar包输出内容会追加到nohup.out中。

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

rpm是centos系统下的软件安装包，在Ubuntu下是deb。

比如安装jdk可以使用rpm -i jdk-xxx.rpm进行安装，-i就是install的意思

可以使用rpm -qa查看安装的软件列表，-q就是query，a就是all。但是搜索出来的结果很长，这时候可以使用管道命令grep。

~~~shell
rpm -qa | grep java
~~~

这个命令是将列出来的所有软件形成一个输出，|是管道，用于连接两个程序，前面rpm -qa的输出就放进管道里面，然后作为grep的输入，grep将在里面进行搜索带关键字jdk的行，并且输出出来。如果不知道要搜索的关键字，可以使用rpm -qa | more和rpm -qa | less这两个命令，它**可以将很长的结果分页展示出来**。more是分页后只能往后翻页，翻到最后一页自动结束返回命令，less是往前往后都能翻页，需要输入q（quit）返回命令行。

如果要删除，可以用`rpm -e`，-e就是erase。

#### yum

yum类似Windows上的软件管家，Ubuntu下面是apt-get。

可以根据关键词进行搜索，例如搜索jdk，`yum search jdk`，可以搜索到很多可以安装的jdk版本，数目太多可以使用grep、more、less过滤。

选中一个后，就可以使用yum install jdk来进行安装，使用`yum erase jdk`卸载。

#### wget

wget后面加上链接就能从网上下载了。

#### 关机

现在就关机

~~~shell
shutdown -h now
~~~

#### 重启

使用`reboot`重启。

#### strace

用来跟踪进程执行时系统调用和所接收的信号，比如

~~~shell
strace ls -la
~~~

会输出具体的系统调用

#### 查看某个端口连接

~~~shell
netstat -ntulp|grep 端口号
~~~

#### 查看某个端口是否被占用

~~~shell
netstat -apn|grep 端口号
~~~

#### Ubuntu卸载软件

~~~shell
apt-get --purge remove 软件名
~~~

#### Ubuntu查看某个软件

~~~shell
dpkg -l|grep 软件
~~~

#### firewall限制或开放IP及端口

##### 开放端口

（1）比如开放xshell连接时使用的22端口

~~~shell
firewall-cmd --zone=public --add-port=22/tcp --permanent
~~~

其中–permanent的作用是使设置永久生效，不加的话机器重启之后失效

（2）重新载入以下防火墙设置，时设置立即生效，执行下命令，设置才会生效：

~~~shell
firewall-cmd --reload
~~~

（3）可通过如下命令查看是否生效

~~~shell
firewall-cmd --zone=public --query-port=22/tcp
~~~

（4）如下命令可查看当前系统打开的所有端口

~~~shell
firewall-cmd --zone=public --list-ports
~~~

##### 限制端口

比如我们现在需要关掉刚刚打开的22端口

~~~shell
firewall-cmd --zone=public --remove-port=22/tcp --permanent
~~~

##### 批量开放或限制端口

批量开放端口，如从100到500这之间的端口我们全部要打开

~~~shell
firewall-cmd --zone=public --add-port=100-500/tcp --permanent
~~~

