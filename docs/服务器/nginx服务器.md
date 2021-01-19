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



**具体内容**

~~~json
#user  nobody;

#这是Nginx服务器并发处理服务的关键配置，worker_processes值越大，可以支持的并发处理量也越多，但是
#会受到硬件、软件等设备的制约
worker_processes  1;

#error_log  logs/error.log;
#error_log  logs/error.log  notice;
#error_log  logs/error.log  info;

#pid        logs/nginx.pid;


events {
    #支持的最大连接数为1024
    worker_connections  1024;
}


http {
    include       mime.types;
    default_type  application/octet-stream;

    #log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
    #                  '$status $body_bytes_sent "$http_referer" '
    #                  '"$http_user_agent" "$http_x_forwarded_for"';

    #access_log  logs/access.log  main;

    sendfile        on;
    #tcp_nopush     on;

    #keepalive_timeout  0;
    keepalive_timeout  65;

    #gzip  on;

    server {
    	#监听端口
        listen       80;
        server_name  localhost;

        #charset koi8-r;

        #access_log  logs/host.access.log  main;

        location / {
            root   html;
            index  index.html index.htm;
        }

        #error_page  404              /404.html;

        # redirect server error pages to the static page /50x.html
        #
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }

        # proxy the PHP scripts to Apache listening on 127.0.0.1:80
        #
        #location ~ \.php$ {
        #    proxy_pass   http://127.0.0.1;
        #}

        # pass the PHP scripts to FastCGI server listening on 127.0.0.1:9000
        #
        #location ~ \.php$ {
        #    root           html;
        #    fastcgi_pass   127.0.0.1:9000;
        #    fastcgi_index  index.php;
        #    fastcgi_param  SCRIPT_FILENAME  /scripts$fastcgi_script_name;
        #    include        fastcgi_params;
        #}

        # deny access to .htaccess files, if Apache's document root
        # concurs with nginx's one
        #
        #location ~ /\.ht {
        #    deny  all;
        #}
    }


    # another virtual host using mix of IP-, name-, and port-based configuration
    #
    #server {
    #    listen       8000;
    #    listen       somename:8080;
    #    server_name  somename  alias  another.alias;

    #    location / {
    #        root   html;
    #        index  index.html index.htm;
    #    }
    #}


    # HTTPS server
    #
    #server {
    #    listen       443 ssl;
    #    server_name  localhost;

    #    ssl_certificate      cert.pem;
    #    ssl_certificate_key  cert.key;

    #    ssl_session_cache    shared:SSL:1m;
    #    ssl_session_timeout  5m;

    #    ssl_ciphers  HIGH:!aNULL:!MD5;
    #    ssl_prefer_server_ciphers  on;

    #    location / {
    #        root   html;
    #        index  index.html index.htm;
    #    }
    #}

}
~~~

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

## 高可用

主服务器:master

从服务器:backup

当主服务器挂掉后，使用从服务器完成工作