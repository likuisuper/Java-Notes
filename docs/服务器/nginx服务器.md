## 反向代理

反向代理：客户端把代理服务器和真实的服务器看成一个整体，客服端不需要做配置

## 负载均衡

将请求通过反向代理服务器转发到多台服务器

## 动静分离



## nginx操作的常用命令

**使用nginx操作命令前提条件：必须进入nginx的目录**

~~~shell
/usr/local/nginx/sbin
~~~

**常用命令**

*  查看版本

  ~~~shell
  ./nginx -v
  ~~~

* 启动nginx

  ~~~shell
  ./nginx
  ~~~

* 关闭nginx

  ~~~shell
  ./nginx -s stop
  ~~~

* 重新加载nginx，比如修改配置文件后，不需要重启服务器

  ~~~shell
  ./nginx -s reload
  ~~~

## 配置文件

#### 位置:usr/local/nginx/conf/nginx.conf

#### 组成

**三部分**

1.全局块

从配置文件开始到events块之间的内容，主要会设置一些影响nginx服务器整体运行的配置指令。主要包括配置运行Nginx服务器的用户(组)，允许生成的worker process数，进程PID存放路径、日志存放路径和类型以及配置文件的引入等

2.events块

主要影响Nginx服务器与用户的网络连接，常用的设置包括是否开启对多work process下的网络连接进行序列化，是否允许同时接收多个网络连接，选取哪种事件驱动模型来处理连接请求，每个word process可以同时支持的最大连接数等。

这部分的配置对Nginx的性能影响较大，在实际中应该灵活配置

3.http块

Nginx配置最频繁的部分，分为两部分

(1)http全局块

包含文件引入，MIME-TYPE定义、日志自定义、连接超时时间、单链接请求数上限等

(2)server块

这块和虚拟主机有密切关系。**单个http块可以包含多个server块，而每个server块就相当于一个虚拟主机，而每个server块也分为全局server块，以及可以同时包含多个location块**

①、全局server块

最常见的配置是本虚拟机主机的监听配置和本虚拟机主机的名称或IP配置

②、location块

## 配置实例：反向代理1

在window上面的浏览器中输入www.123.com，然后通过nginx反向代理访问到tomcat的页面，不让tomcat服务暴露。

![](https://s3.ax1x.com/2021/01/16/sD6Qw4.png)

因为www.123.com并没有对应的域名，所以可以在windows的host文件中配置一个本地域名。

**windows配置本地域名**

host位置：C:\Windows\System32\drivers\etc\hosts。打开该文件，在末尾加上

~~~xml
193.168.136.15 www.123.com
~~~

其中的193.168.136.15是虚拟机ip地址。可以在浏览器中测试

**在nginx中进行请求转发的配置(反向代理)**

![](https://s3.ax1x.com/2021/01/16/sDBu9K.png)

下面红框那里表示当请求上面的ip时将会代理(转发)到该路径。**注意一定是在usr/local/nginx/conf目录下的配置文件，不是usr/src/nginx/conf中的配置文件，不然无效**。修改完后重启nginx就可以看到该效果

## 配置实例：反向代理2

需求：访问http://193.168.136.15:9001/edu/	直接跳转到127.0.0.1:8080

访问http://193.168.136.15:9001/vod/	直接跳转到127.0.0.1:8081

先准备两个tomcat服务器，一个8080端口，一个8081端口。

**步骤**

在/usr/src目录中新建两个目录，一个为tomcat8080，一个为tomcat8081

然后将tomcat的压缩文件分别上传到这两个文件夹中，解压。在这之前，先将原来的tomcat服务关闭。tomcat8080目录中的端口默认就是8080，不需要改。tomcat8081目录中的tomcat服务通过server.xml文件将shutdown端口从原来的8005改为8015，8080端口修改为8081。

然后分别启动两个tomcat服务。浏览器测试是否成功。

* 在tomcat8080的webapps目录下新建一个edu目录，新建一个html文件，内容写上8080，然后浏览器测试是否能够访问该html文件
* 在tomcat8080的webapps目录下新建一个vod目录，新建一个html文件，内容写上8081，然后测试

打开nginx的conf配置文件,在http块中新建一个server块

~~~shell
    server  {
        listen       9001;
        server_name  193.168.136.15;
		
		# ~ 表示正则表达，当请求中包含/edu/路径则转发到下面这个8080服务
        location ~ /edu/  {
            proxy_pass  http://127.0.0.1:8080;
        }
        #当请求中包含/vod/路径则转发到下面这个8081服务
        location ~/vod/  {
            proxy_pass  http://127.0.0.1:8081;
        }
    }

~~~

测试：

访问193.168.136.15:9001/edu/a.html，页面显示8080

访问193.168.136.15:9001/vod/a.html，页面显示8081

**location指令说明**

~~~xml
location [= | ~ | ~* | ^~] uri{

}
~~~

1、=：用于不含正则表达式的uri前，要求请求字符串与uri严格匹配，如果匹配成功，就停止继续向下搜索并立即处理该请求

2、~：用于表示uri包含的正则表达式，并且**区分大小写**

3、~*：与上面不同的是**不区分大小写**

4、~^：用于不含正则表达式的uri前，要求nginx服务器找到标识uri和请求字符串匹配度最高的location后，立即使用此location处理请求，而不再使用location块中的正则uri和请求字符串做匹配

**注意**：如果uri包含正则表达式，则必须要有~或者~*标识

## 配置实例：负载均衡

1.实现效果：

（1）浏览器地址栏输入http://193.168.136.15/edu/a.html，负载均衡效果，平均8080和8081端口中

（2）分别在上面的tomcat8080和tomcat8081的webapps中新建一个edu文件夹，里面创建一个a.html文件，一个内容是8080，一个内容是8081

（3）配置nginx配置文件

在http块里面加上一个upstream块，起一个名字，里面是tomcat服务器的列表。在server块里面将server_name改为自己的ip，然后在location块里面加上proxy_pass,后面就是upstream块的名字。

upstream指令：

该指令用于设置一组可以在proxy_pass和fastcgi_pass指令中使用的代理服务器，默认的负载均衡方式为轮询

~~~shell
upstream myserver  {
	#server指令用于指定后端服务器的名称和参数
	server 193.168.136.15:8080;
	server 193.168.136.15:8081;
}

#虚拟主机
server {
    listen       80;
    server_name  193.168.136.15;

    #charset koi8-r;

    #access_log  logs/host.access.log  main;

    location / {
        root   html;
        proxy_pass  http://myserver;
        index  index.html index.htm;
}

~~~

然后在浏览器输入网址

http://193.168.136.15/edu/a.html，多刷新几下浏览器，便可以看到不同的内容

#### 分配策略

1、轮询：上面采取的分配策略是默认的，也就是**轮询**

每个请求按照时间顺序逐一分配到不同的后端服务器，如果后端服务器down掉了，能自动剔除

2、weight(权重)

默认为1，权重越高，被分配到的请求就越多

~~~shell
upstream myserver  {
	#server指令用于指定后端服务器的名称和参数
	server 193.168.136.15:8080 weight=5;
	server 193.168.136.15:8081 weight=10;
}
~~~

这时候刷新浏览器，可以看到8081到内容出现两次，8080出现的内容1次，即下面的请求是上面的两倍

3、ip_hash

每个请求按访问的hash结果分配，这样每个访客固定访问一个后端服务器，可以解决session的问题。

比如第一次访问的ip地址是8081，那么后面访问的一直都是8081，不会访问8080

**注意**：使用该指令无法保证后端服务器的负载均衡，可能有些后端服务器接收到的请求多，有些后端服务器接收到的请求少，**而且设置后端服务器的权重等方法将不起作用**。所以，如果后端的动态应用服务器能做到SESSION共享，还是建议采用后端服务器的SESSION共享替代nginx的ip_hash。如果后端服务器有时要从nginx负载均衡(已使用ip_hash)摘除一段时间，必须将其标记为down，而不是注释或删掉，否则会导致服务器重新进行hash，导致session失效

~~~shell
upstream myserver  {
	ip_hash;
	#server指令用于指定后端服务器的名称和参数
	server 193.168.136.15:8080 weight=5;#权重不起作用
	server 193.168.136.15:8081 down;#这时只会访问8080
~~~

4、fair(第三方)

按后端服务器的响应时间来分配请求，响应时间短的优先分配

## 配置实例：动静分离

在根目录下新建dmsttest目录，在里面新建www目录和image目录分别存放html文件和img文件

~~~shell
    server {
        listen       80;
        server_name  193.168.136.15;

        #charset koi8-r;

        #access_log  logs/host.access.log  main;

        #location / {
        #    root   html;
        #    proxy_pass  http://myserver;
        #    index  index.html index.htm;
        #}

        location /www/  {
            root /dmsttest/;
            index index.html index.htm;
        }
        location /image/  {
            root /dmsttest/;#表示根目录为/dmsttest
            autoindex  on;#列出该目录中的文件，可以不加
        }
~~~

## Nginx优化

~~~she
1.工作线程数和并发连接数
worker_rlimit_nofile 20480; #每个进程打开的最大的文件数=worker_connections*2是安全的，受限于操作系统/etc/security/limits.conf
vi /etc/security/limits.conf
* hard nofile 204800
* soft nofile 204800
* soft core unlimited
* soft stack 204800

worker_processes 4; #cpu，如果nginx单独在一台机器上
worker_processes auto;
events {
    worker_connections 10240;#每一个进程打开的最大连接数，包含了nginx与客户端和nginx与upstream之间的连接。出错后可以调大
    multi_accept on; #可以一次建立多个连接
    use epoll;
}

2.操作系统优化
配置文件/etc/sysctl.conf（注意，真正写入conf的是sysctl -w 后面的命令）
sysctl -w net.ipv4.tcp_syncookies=1 #防止一个套接字在有过多试图连接到达时引起过载
sysctl-w net.core.somaxconn=1024 #默认128，连接队列
sysctl-w net.ipv4.tcp_fin_timeout=10 # timewait的超时时间
sysctl -w net.ipv4.tcp_tw_reuse=1 #os直接使用timewait的连接
sysctl -w net.ipv4.tcp_tw_recycle=0 #回收禁用

3.Keepalive长连接
Nginx与upstream server：
upstream server_pool{
        server localhost:8080 weight=1 max_fails=2 fail_timeout=30s;
        keepalive 300;  #300个长连接
}
同时要在location中设置：
location /  {
            proxy_http_version 1.1;
	proxy_set_header Upgrade $http_upgrade;
	proxy_set_header Connection "upgrade";
}
客户端与nginx（默认是打开的）：
keepalive_timeout  60s; #长连接的超时时间
keepalive_requests 100; #100个请求之后就关闭连接，可以调大
keepalive_disable msie6; #ie6禁用

4.启用压缩
gzip on;
gzip_http_version 1.1;
gzip_disable "MSIE [1-6]\.(?!.*SV1)";
gzip_proxied any;
gzip_types text/plain text/css application/javascript application/x-javascript application/json application/xml application/vnd.ms-fontobject application/x-font-ttf application/svg+xml application/x-icon;
gzip_vary on; #Vary: Accept-Encoding
gzip_static on; #如果有压缩好的 直接使用

5.状态监控
location = /nginx_status {
	stub_status on;
	access_log off;
	allow <YOURIPADDRESS>;#比如allow 127.0.0.1;
	deny all;禁用所有ip，除了allow的
}
使用curl(或者wget)设置的ip/nginx_status,输出结果：
Active connections: 1 
server accepts handled requests
 17122 17122 34873 
Reading: 0 Writing: 1 Waiting: 0 
Active connections：当前实时的并发连接数
accepts：收到的总连接数，
handled：处理的总连接数
requests：处理的总请求数
Reading：当前有都少个读，读取客户端的请求
Writing：当前有多少个写，向客户端输出
Waiting：当前有多少个长连接（reading + writing）
reading – nginx reads request header
writing – nginx reads request body, processes request, or writes response to a client
waiting – keep-alive connections, actually it is active - (reading + writing)

6.实时请求信息统计ngxtop
https://github.com/lebinh/ngxtop
(1)安装python-pip
yum install epel-release
yum install python-pip
(2)安装ngxtop
pip install ngxtop
(3)使用
指定配置文件：           ngxtop -c ./conf/nginx.conf
查询状态是200：        ngxtop -c ./conf/nginx.conf  --filter 'status == 200'
查询那个ip访问最多： ngxtop -c ./conf/nginx.conf  --group-by remote_addr
~~~

以秒杀项目为例，完整的conf:

~~~shell
...
worker_rlimit_nofile 20480; #每个进程打开的最大的文件数=worker_connections*2是安全的，受限于操作系统/etc/security/limits.conf
worker_processes 4; #cpu，如果nginx单独在一台机器上
events {
    worker_connections 10240;#每一个进程打开的最大连接数，包含了nginx与客户端和nginx与upstream之间的连接
    multi_accept on; #可以一次建立多个连接
    use epoll;#使用epoll I/O多路复用
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    server_tokens off; #隐藏版本号
    client_max_body_size 10m; #文件上传需要调大

    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  logs/access.log  main;
    #默认写日志：打开文件写入关闭，max:缓存的文件描述符数量，inactive缓存时间，valid：检查时间间隔，min_uses：在inactive时间段内使用了多少次加入缓存
    open_log_file_cache max=200 inactive=20s valid=1m min_uses=2;
    
    #只有开启了sendfile，tcp_nopush才起作用
    #tcp_nodelay和tcp_nopush互斥，二者同时开启，nginx会： （1）确保数据包在发送给客户端之前是满的
   #（2）对于最后一个数据包，允许tcp立即发送，没有200ms的延迟
    tcp_nodelay on;
    sendfile       on;
    tcp_nopush     on;
    #与浏览器的长连接
    keepalive_timeout  65;#长连接超时时间
    keepalive_requests 500;#500个请求以后，关闭长连接
    keepalive_disable msie6;
    # 启用压缩
    gzip on;
    gzip_http_version 1.1;
    gzip_disable "MSIE [1-6]\.(?!.*SV1)";
    gzip_proxied any;
    gzip_types text/plain text/css application/javascript application/x-javascript application/json application/xml application/vnd.ms-fontobject application/x-font-ttf application/svg+xml application/x-icon;
    gzip_vary on; #Vary: Accept-Encoding
    gzip_static on; #如果有压缩好的 直接使用
    #超时时间
    proxy_connect_timeout 5; #连接proxy超时
    proxy_send_timeout 5; # proxy连接nginx超时
    proxy_read_timeout 60;# proxy响应超时
     # 开启缓存,2级目录。进入缓存的路径可以看到具体内容
    proxy_cache_path /usr/local/nginx/proxy_cache levels=1:2 keys_zone=cache_one:200m inactive=1d max_size=20g;
    proxy_ignore_headers X-Accel-Expires Expires Cache-Control;
    proxy_hide_header Cache-Control;
    proxy_hide_header Pragma;
    
    #反向代理服务器集群
    upstream myserver {
        server 192.168.63.128:8080 weight=1 max_fails=2 fail_timeout=30s;
        server 192.168.63.125:8080 weight=1 max_fails=2 fail_timeout=30s;#30内失败重试2次，超出后将当前服务剔除，30s后继续重试
        keepalive 200; # 最大的空闲的长连接数 
    }

    server {
        listen       80;
        server_name  localhost 192.168.63.128;
        
        location / {
            #长连接
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            #Tomcat获取真实用户ip
            proxy_set_header Host $http_host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $remote_addr;
            proxy_set_header X-Forwarded-Proto  $scheme;
            proxy_pass http://myserver;
        }
        # 状态监控
        location /nginx_status {
            stub_status on;
            access_log   off;
            allow 127.0.0.1;
            allow 192.168.63.128;
            deny all;#其他ip都不允许执行
        }
        #用于清除缓存
        location ~ /purge(/.*)
        {
            allow 127.0.0.1;
            allow 192.168.63.128;
            deny all;
            proxy_cache_purge cache_one $host$1$is_args$args;
        }
        # 静态文件加缓存
        location ~ .*\.(gif|jpg|jpeg|png|bmp|swf|js|css|ico)?$
        {
            expires 1d;#过期 时间是1天
            proxy_cache cache_one;
            proxy_cache_valid 200 304 1d;#200 304等方法缓存一天
            proxy_cache_valid any 1m;#其他都是1分钟
            proxy_cache_key $host$uri$is_args$args;
            proxy_pass http://myserver;
        }
    }
}
~~~

## nginx限流

#### 配置

1、按连接数限速，即并发数

~~~shell
ngx_http_limit_conn_module
~~~

2、按请求速率限速，按照ip限制单位时间内的请求数

~~~c
ngx_http_limit_req_module
~~~

#### 限流配置

创建规则(http模块中)

~~~c
limit_req_zone $remote_ip_addr zone=mylimit:10m rate=1r/s;
~~~

该规则应用的内存区域是10m，给某一个地址限制请求速率为每秒1个

只要请求超过1个请求每秒就会被拦截，拿到503页面

应用规则(在location块中)

~~~c
limit_req zone=mylimit burst=1 nodelay
~~~

这行命令表示应用mylimit规则，如果没有后面两个参数，那么就会按照上面的规则执行，但是加了后面两个参数则可以放宽请求，以此来应对突发流量。burst就是当突发流量来的时候不会去拒绝它，而是保留一定的缓存空间，这里是1，那么就是允许一个长度为1的排队队列，再加上开始设置的1个请求，也就是说每秒允许2个请求。nodelay作用：当burst设置的非常大，比如1000的时候，让这些请求不用去排队，而是立马去处理

## LVS四层负载均衡

所谓的四层就是ip+端口号

当并发量大的时候，可以在前端部署LVS，将请求转发到多台nginx上。

linux内核已经是支持lvs的，首先下载一个LVS管理工具：

~~~shell
yum install ipvsadm
~~~

准备3台虚拟机，其中一台用来做虚拟服务，这里是192.168.63.128。另外两台做真实的服务，分别是192.168.63.120，192.168.63.125。

#### 配置虚拟服务

首先在128这台主机上，进入/usr/local/bin目录下，新建lvs_dr.sh，写入如下配置

~~~shell
#! /bin/bash
echo 1 > /proc/sys/net/ipv4/ip_forward
ipv=/sbin/ipvsadm
vip=192.168.63.110 #虚拟一个ip出来
rs1=192.168.63.120 #真实服务
rs2=192.168.63.125 #真实服务
case $1 in
start)
    echo "Start LVS"
    ifconfig ens33:0 $vip broadcast $vip netmask 255.255.255.255 up #添加虚拟网卡
    route add -host $vip dev ens33:0          #添加到虚拟主机的路由
    $ipv -A -t $vip:80 -s lc                 #添加虚拟服务器，-s：调度算法 lc:最少连接
    $ipv -a -t $vip:80 -r $rs1:80 -g -w 1    #添加真实服务器，-g：DR，-w：权重
    $ipv -a -t $vip:80 -r $rs2:80 -g -w 1
;;
stop)
    echo "Stop LVS"
    route del -host $vip dev ens33:0  #删除虚拟网卡
    ifconfig ens33:0 down             #删除路由
    $ipv -C                          #删除虚拟主机
;;
*)
echo "Usage:$0 {start|stop}"
exit 1
esac
~~~

其中的ens33是当前通过ifconfig查看得到的网卡名称，有的虚拟机是eth0，这里注意。

然后添加权限：

~~~shell
chmod 755 lvs_dr.sh
~~~

然后执行

~~~shell
./lvs_dr.sh start
~~~

此时通过ifconfig就可以发现多了一个虚拟网卡ens330

删除网卡：

~~~shell
./lvs_dr.sh stop
~~~

#### 真实服务

当虚拟服务将数据转发给这两个真实服务后，这两个真实服务并不知道虚拟服务的存在，所以同样需要给真实服务添加配置，让他们指向虚拟服务。

在120和125这两台主机上，同样是进入usr/local/bin目录下，新建lvs_rs.sh

~~~shell
#!/bin/bash
vip=192.168.63.110 #指向虚拟服务
case $1 in
start)
    echo "Start LVS"
    ifconfig ens33:0 $vip broadcast $vip netmask 255.255.255.255 up
    route add -host $vip dev ens33:0
    echo "1" > /proc/sys/net/ipv4/conf/lo/arp_ignore
    echo "2" > /proc/sys/net/ipv4/conf/lo/arp_announce
    echo "1" > /proc/sys/net/ipv4/conf/all/arp_ignore
    echo "2" > /proc/sys/net/ipv4/conf/all/arp_announce
    sysctl -p > /dev/null 2>&1
;;
stop)
    echo "Stop LVS"
    route del -host $vip dev ens33:0
    /sbin/ifconfig ens33:0 down
    echo "0" > /proc/sys/net/ipv4/conf/lo/arp_ignore
    echo "0" > /proc/sys/net/ipv4/conf/lo/arp_announce
    echo "0" > /proc/sys/net/ipv4/conf/all/arp_ignore
    echo "0" > /proc/sys/net/ipv4/conf/all/arp_announce
    sysctl -p > /dev/null 2>&1
;;
*)
echo "Usage:$0 {start|stop}"
exit 1
esac
~~~

使用ipvsadm查看路由

~~~shell
IP Virtual Server version 1.2.1 (size=4096)
Prot LocalAddress:Port Scheduler Flags
  -> RemoteAddress:Port           Forward Weight ActiveConn InActConn
TCP  localhost:http lc
  -> 192.168.63.120:http          Route   1      0          0         
  -> 192.168.63.125:http  
~~~

lc表示采用最少连接的负载均衡算法

还可以使用ipvsadm -ln查看端口号

~~~shell
[root@localhost bin]# ipvsadm -ln
IP Virtual Server version 1.2.1 (size=4096)
Prot LocalAddress:Port Scheduler Flags
  -> RemoteAddress:Port           Forward Weight ActiveConn InActConn
TCP  192.168.63.110:80 lc
  -> 192.168.63.120:80            Route   1      0          0         
  -> 192.168.63.125:80            Route   1      0          0   
~~~

## Keepalived高可用

使用keepalived进行双机热备以及负载均衡。

主服务器:master

从服务器:backup

当主服务器挂掉后，使用从服务器完成工作。

#### 配置

下面几个步骤master和backup都是一样的。

master所在的主机ip为130，backup所在的主机为129.

1.首先安装依赖

~~~shell
yum install ipvsadm openssl-devel popt-devel  libnl libnl-devel  libnfnetlink-devel -y
~~~

2.官网下载压缩包，进行解压后，进入解压后的目录，使用如下命令安装

~~~shell
./configure --prefix=/usr/local/keepalived 
~~~

因为keepalived是c写的，所以需要有gcc环境，否则会报错。出现以下信息表示成功：

~~~shell
Use IPVS Framework       : Yes
IPVS use libnl           : Yes
Use VRRP Framework       : Yes
Use VRRP VMAC            : Yes
Use VRRP authentication  : Yes
~~~

3.make编译

4.make install

Keepalived做负载均衡是基于lvs的，所以先把前面配置的两台真实服务的lvs启动。

一、master配置：

进入/usr/local/keepalived目录下，将默认 的配置文件做个备份，不修改原来的配置文件：

~~~shell
mv ./etc/keepalived/keepalived.conf ./etc/keepalived/keepalived.bak.conf

vim ./etc/keepalived/keepalived.conf
~~~

然后添加如下配置：

~~~shell
# 全局定义，发送邮件这些，不用管这个
global_defs {
   notification_email {
         cxylikui@163.com
   }
   notification_email_from admin@163.com
   smtp_server 192.168.63.128
   smtp_connect_timeout 30
   router_id LVS_DEVEL
}

vrrp_instance VI_1 {
    state MASTER 
    interface ens33 #当前网卡
    virtual_router_id 51  #主从必须一致
    priority 100  #优先级，选举master用
    advert_int 1    #master与backup节点间同步检查的时间间隔，单位为秒 
    authentication {#验证类型和验证密码，通常使用PASS类型，同一vrrp实例MASTER与BACKUP使用相同的密码才能正
常通信
        auth_type PASS
        auth_pass 1111
    }
    virtual_ipaddress {#vip
        192.168.63.110
    }
}
virtual_server 192.168.63.110 80 {
        delay_loop 6 ##每隔 6 秒查询RealServer状态
        lb_algo rr   #负载均衡算法
        lb_kind DR    #DR转发模式
        persistence_timeout 10 #会话保持时间 
        protocol TCP 
        real_server 192.168.63.120 80 { #RS
                weight 1
                TCP_CHECK {
                        connect_timeout 10
                        connect_port 80
                }
        }
        real_server 192.168.63.125 80 {
                weight 1
                TCP_CHECK {
                        connect_timeout 10
                        connect_port 80
                }
        }
}

~~~

其中的ip110就是前面配置的虚拟主机，而120和125是真实服务。

二、backup配置，和master一样的步骤，不同的是将配置文件中的两个地方改掉

~~~shell
    state BACKUP
    ...
    priority 80  #优先级，选举master用
~~~

priority指明优先级，从机的priority一定要比主机小，如果配置了多个从机，那么多个从机的优先级也是不一样的，**当主机挂掉的时候，就会根据从机中优先级高的来选举主机**。

#### 双机热备

因为keepalived有Ip转发的功能，所以在主机和从机中还需要将Ip转发功能打开：

~~~shell
vi /etc/sysctl.conf
net.ipv4.ip_forward = 1
~~~

一、先启动master

~~~shell
./sbin/keepalived -f /usr/local/keepalived/etc/keepalived/keepalived.conf -D
~~~

-f：指定配置文件

-D：输出日志，位置在/var/log/messages

使用ps -ef|grep keepalived:

~~~shell
gdm        1917   1523  0 19:17 ?        00:00:00 /usr/libexec/gsd-housekeeping
root       8407      1  0 21:50 ?        00:00:00 ./sbin/keepalived -f /usr/local/keepalived/etc/keepalived/keepalived.conf -D
root       8408   8407  0 21:50 ?        00:00:00 ./sbin/keepalived -f /usr/local/keepalived/etc/keepalived/keepalived.conf -D
root       8409   8407  0 21:50 ?        00:00:00 ./sbin/keepalived -f /usr/local/keepalived/etc/keepalived/keepalived.conf -D
root       8424   3114  0 21:50 pts/1    00:00:00 grep --color=auto keep

~~~

8407相当于一个看门狗，8408和8409做lvs和健康检查。

使用ifconfig就可以发现ens33会有两个ip，但是在我的主机上只会显示原来的ip，110这个ip显示不了，通过查看message日志，发现有创建地址的记录，除了ip外还有mac地址，ens33显示的就是两个mac地址，而ip地址只显示了原来的ip。

可以使用如下命令**查看当前机器上的请求连接信息**

~~~shell
watch ipvsadm -L -n -c
~~~

这时候浏览器访问一下虚拟ip110就会有如下结果：

![](https://z3.ax1x.com/2021/04/08/cYEH3V.png)

可以看到，此时请求跑到了125这台真实服务上。

这个时候进行压测，以秒杀项目为例，因为虚拟ip那台服务器部署了很多东西，所以关闭它的nginx，只用开启两台真实服务器的nginx做负载均衡即可。另外，**虚拟Ip的lvs服务可以不用再开启，关闭即可**。这个时候压测60000个请求是一点压力都没有的。

二、启动backup

和master一样的启动方式。

然后**演示主从切换达到高可用**：

首先将master上的keepalived进程kill掉，然后此时查看backup的日志：

~~~shell
Apr  8 16:14:11 localhost Keepalived_vrrp[12055]: (VI_1) Backup received priority 0 advertisement
Apr  8 16:14:12 localhost Keepalived_vrrp[12055]: (VI_1) Entering MASTER STATE
Apr  8 16:14:12 localhost Keepalived_vrrp[12055]: (VI_1) setting protocol VIPs.
Apr  8 16:14:12 localhost Keepalived_vrrp[12055]: Sending gratuitous ARP on ens33 for 192.168.63.110
Apr  8 16:14:12 localhost Keepalived_vrrp[12055]: (VI_1) Sending/queueing gratuitous ARPs on ens33 for 192.168.63.110
~~~

可以看到，它会将110这个虚拟ip“抢”过来，然后此时再访问110这个ip，backup的请求信息：

![](https://z3.ax1x.com/2021/04/08/cYnUwF.png)

可以看到，当主机挂掉后，从机会继续工作，从而达到高可用。

然后再将master重新启动，这个时候backup日志如下：

~~~shell
Apr  8 16:22:45 localhost Keepalived_vrrp[12055]: (VI_1) Master received advert from 192.168.63.130 with higher priority 100, ours 80
Apr  8 16:22:45 localhost Keepalived_vrrp[12055]: (VI_1) Entering BACKUP STATE
Apr  8 16:22:45 localhost Keepalived_vrrp[12055]: (VI_1) removing protocol VIPs.
Apr  8 16:22:45 localhost avahi-daemon[806]: Withdrawing address record for 192.168.63.110 on ens33.

~~~

**然后再访问110这个ip，master又会继续工作**，因为它的优先级比从机高。

#### 轮询

观察上面的图片发现连接的expire很长，因为它默认的是900s也就是15分钟，

通过`ipvsadm -L --timeout`查看：

~~~shell
Timeout (tcp tcpfin udp): 900 120 300
~~~

第一个就是tcp的连接时间900s.

所以我们会发现即使在配置文件中配置了

~~~shell
persistence_timeout 10 #会话保持时间
~~~

不起作用。

解决办法：使用`ipvsadm --set`命令设置：

~~~shell
ipvsadm --set 1 2 1
~~~

将它设置为1s。

**注意**：nginx也需要修改，原来nginx是跟浏览器连接的，现在浏览器直接请求keepalived了，然后keepalive再访问nginx，所以现在是nginx跟keepalived之间连接的超时时间。为了演示效果，将两台真实服务的nginx.conf中的timeout改为1s

~~~shell
keepalive_timeout  1;#长连接超时时间
~~~

然后重启master的keepalived，超时时间就是配置文件中配置的10s。

这个时候再去访问110虚拟主机，如果第一次访问请求落到120的话，10s后再访问，此时请求就会落到125。

**keepalived是以连接为单位的，而不是以请求为单位，所以只要连接足够的话，那么请求都会落到一个主机上**。