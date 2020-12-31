## 环境配置

zookeeper配置查看[zookeeper安装配置](../reference/soft_install.md)

1.dubbo-admin-2.6.0下载地址： https://github.com/apache/incubator-dubbo/releases/tag/dubbo-2.6.0 

解压后的目录

![](https://s3.ax1x.com/2020/12/31/rX5OoV.png)

2.打开dubbo-admin,进入cmd,使用mvn命令打成war包：mvn package -Dmaven.skip.test=true(跳过测试),然后将该war包复制到tomcat的webapps目录下，使用压缩工具将其解压到当前目录

![](https://s3.ax1x.com/2020/12/31/rXoEBn.png)

3.进入该目录下的WEB-INF目录，里面有一个dubbo.properties文件，里面有登录时候的密码

![](https://s3.ax1x.com/2020/12/31/rXosHI.png)

4.更改tomca端口：

打开tomcat中的conf目录下的server.xml文件，把启动端口改成8088,因为zookeeper默认使用的是8080,以免冲突

![](https://s3.ax1x.com/2020/12/31/rXT5RO.png)

5.访问管理平台并登录：

* 启动zookeeper,cmd中输入zkServer.cmd,如果出现8080端口被占用报错，在zoo.cfg中添加admin.serverPort=8888，重启，如果报错2181已被使用，将data中的目录删除，再重启
* 启动tomcat,bin目录下双击startup.bat
* 浏览器输入http://localhost:8088/dubbo-admin-2.6.0