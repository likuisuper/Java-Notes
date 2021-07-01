### 采集service服务

采集指标包括开始时间，耗时，方法名等

#### 采集jdbc日志

采集指标包括url，sql，参数，耗时等

根据**不对应用场景做假设**的原则，所以在JDBC API层做拦截，比如拦截connection接口生成我们的代理类，拦截preparedstatment接口生成代理类

#### 采集http性能

指标包括ip，端口，执行时间等。

拦截的目标类是`HttpServlet`，方法是service方法，但是这里有一个点需要注意：

Tomcat的类加载机制，我们的apm项目是有appclassloader加载器加载的，而servlet是由Tomcat自定义的commonclassloader下的webclassloader加载器加载的，这两个加载器是appclassloader加载器的父类，也就是说，apm项目是找不到httpservlet这个类的，因为父类访问不了子类。解决办法就是**采用适配器模式，利用反射来获取httpservlet的方法**。

#### fastjson的问题

当使用fastjson来将输出信息格式化成json形式的时候，会出现以下两个问题：

1、当前apm项目中引入了fastjson依赖是没问题的，要监控的项目中也引入了fastjson依赖，当 apm项目去监控该项目时，就会找不到fastjson相关的类，比如jsonformat，还是上面说的类加载器问题，这时可以将fastjson打包进当前apm项目中

2、第二个问题也是最主要的问题，fastjson会采用当前线程的上下文加载器来加载被使用的类，比如将servicestatics这个类输出为json格式，如果当前线程的上下文加载器中找不到servicestatics这个类，那么就会报错

可以查看ASMClassloader类的源码，其中会判断类加载器是否和当前线程的类加载器一致，不一致就会使用asm加载器。这个问题解决不了，所以在网上找了一个轻量级的json框架。

#### 如何集成到项目

将apm项目打包后，在要监控的项目中，在IEDA的run/debug configurations中的vm参数加上：

~~~java
-javaagent:apm的jar包目录=service.include=要监控的包路径
~~~

其中service.include表示包含哪些service，如果要指定生成日志文件的目录（默认是当前项目的根目录下），参数如下

~~~java
-javaagent:apm的jar包目录=log=日志文件目录,service.include=要监控的包路径
~~~

示例：

~~~java
-javaagent:D:\workspace\apm\apm-agent\target\apm-agent-1.0-SNAPSHOT.jar=log=/log,service.include=com.jiatu.catalog.service.impl.*ServiceImpl
~~~

