## elasticsearch搭建

1.使用docker拉取镜像，这里以7.9.3版本为例

~~~shell
docker pull elasticsearch:7.9.3
~~~

2.设置系统内核参数，否则会因为内存不足无法启动

~~~shell
# 打开配置文件
vim /etc/sysctl.conf
# 添加配置
vm.max_map_count=263000
~~~

使用sysctl -p立即生效

3.修改/etc/security/limits.conf，加入以下内容

~~~shell
soft nofile 204800
hard nofile 204800
soft nproc 2048
hard nproc 4096
~~~

前面两个是在使用nginx进行负载均衡时候设置的，设置这么大是因为在压测的时候不报错。根据自己需求配置。

4.创建挂载目录

~~~shell
#创建目录
mkdir /lkuser/elasticsearch/data/
#修改权限
chmod 777 data
~~~

5.这里使用docker-compose启动，方便集成logstash和kibana

6.访问9200端口查看是否成功

#### 安装中文分词器

~~~shell
docker exec -it elasticsearch /bin/bash
#需在容器中运行
elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.4.0/elasticsearch-analysis-ik-6.4.0.zip
#重启
docker restart elasticsearch
~~~

## logstash

一样拉取7.9.3版本的镜像

#### 安装json_lines插件

使用下面的docker-compose文件启动容器后

~~~shell
#进入logstash容器
docker exec -it logstash /bin/bash
#进入bin目录
cd /bin/
#安装插件
logstash-plugin install logstash-codec-json_lines
#退出容器
exit
#重启
docker restart logstash
~~~

#### 配置文件

创建一个存放logstash配置的目录并上传配置文件

logstash-springboot.conf配置文件内容：

~~~xml
#数据输入源
input {
  tcp {
    mode => "server"
    host => "0.0.0.0"
    port => 4560
    codec => json_lines
  }
}
#数据输出
output {
  elasticsearch {
    hosts => "es:9200"
    index => "springboot-logstash-%{+YYYY.MM.dd}"
  }
}
#还有filter，但它不是必须的，如果需要logstash的过滤功能，可以加上
filter{

}
~~~



## kibana

一样拉取7.9.3版本的镜像

关闭防火墙后访问5601端口即可。

设置中文：

进入容器的config目录

~~~shell
vi kibana.yml
#添加配置
i18n.locale: "zh-CN"
~~~

## docker-compos配置

~~~shell
version: '3'
services:
  elasticsearch:
    image: elasticsearch:7.9.3
    container_name: elasticsearch
    environment:
      - "cluster.name=elasticsearch" #设置集群名称为elasticsearch
      - "discovery.type=single-node" #以单一节点模式启动
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m" #设置使用jvm内存大小
    volumes:
    - /lkuse/elasticsearch/data:/usr/share/elasticsearch/data #数据文件挂载
    - /lkuse/elasticsearch/plugins:/usr/share/elasticsearch/plugins #插件文件挂载
    ports:
    - 9200:9200
    - 9300:9300
  logstash:
    image: logstash:7.9.3
    container_name: logstash
    links:
      - elasticsearch:es #可以用es这个域名访问elasticsearch服务
    volumes:
    - /lkuse/logstash/logstash-springboot.conf:/usr/share/logstash/pipeline/logstash.conf
    depends_on:
      - elasticsearch #logstash在elasticsearch启动之后再启动
    ports:
    - 4560:4560
  kibana:
    image: kibana:7.9.3
    container_name: kibana
    links:
      - elasticsearch:es
    depends_on:
      - elasticsearch
    environment:
      - "elasticsearch.hosts=http://es:9200"
    ports:
    - 5601:5601
~~~

将文件上传到服务器，然后在目录下执行：

~~~shell
docker-compose -f docker-compose-elk.yml up -d
~~~

不用-f指定文件的话会报错的，因为当前的docker-compose-elk不是默认的docker-compose命名的。

~~~shell
[root@localhost seckill]# docker-compose -f docker-compose-elk.yml up -d
Creating elasticsearch ... done
Creating logstash      ... done
Creating kibana        ... done
~~~

使用docker ps和docker-compose -f docker-compose-elk.yml ps都可以看到当前运行的容器。

## spring集成logstash

pom中添加依赖

~~~xml
<!--集成logstash-->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>5.3</version>
</dependency>
~~~

#### 简易配置

添加配置文件logback-spring.xml让logback的日志输出到logstash

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
    <!--应用名称-->
    <property name="APP_NAME" value="mall-admin"/>
    <!--日志文件保存路径-->
    <property name="LOG_FILE_PATH" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}/logs}"/>
    <contextName>${APP_NAME}</contextName>
    <!--每天记录日志到文件appender-->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE_PATH}/${APP_NAME}-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>
    <!--输出到logstash的appender-->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <!--可以访问的logstash日志收集端口-->
        <destination>192.168.63.128:4560</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>

~~~

#### 分场景收集日志

将日志分为4种：

* 调式日志：最全的日志，包含了应用中所有`DEBUG`级别以上的日志，仅在开发、测试环境中开启收集
* 错误日志：只包含应用中所有`ERROR`级别的日志，所有环境都开启收集
* 业务日志：在我们应用的`对应包下`打印的日志，可用于查看我们自己在应用中打印的业务日志
* 记录日志：每个接口`访问记录`，可用来查看接口的执行时长，获取接口访问参数，一般是配置的切面类

完整配置如下：

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration>
<configuration>
    <!--引用默认日志配置-->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <!--使用默认的控制台日志输出实现-->
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
    <!--应用名称-->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springBoot"/>
    <!--日志文件保存路径-->
    <property name="LOG_FILE_PATH" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}/logs}"/>
    <!--LogStash访问host，source的值访问的是配置文件中配置的属性-->
    <springProperty name="LOG_STASH_HOST" scope="context" source="logstash.host" defaultValue="192.168.63.128"/>

    <!--DEBUG日志输出到文件-->
    <appender name="FILE_DEBUG"
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!--输出DEBUG以上级别日志-->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>DEBUG</level>
        </filter>
        <encoder>
            <!--设置为默认的文件日志格式-->
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!--设置文件命名格式-->
            <fileNamePattern>${LOG_FILE_PATH}/debug/${APP_NAME}-%d{yyyy-MM-dd}-%i.log</fileNamePattern>
            <!--设置日志文件大小，超过就重新生成文件，默认10M-->
            <maxFileSize>${LOG_FILE_MAX_SIZE:-10MB}</maxFileSize>
            <!--日志文件保留天数，默认30天-->
            <maxHistory>${LOG_FILE_MAX_HISTORY:-30}</maxHistory>
        </rollingPolicy>
    </appender>

    <!--ERROR日志输出到文件-->
    <appender name="FILE_ERROR"
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!--只输出ERROR级别的日志-->
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
            <!--设置为默认的文件日志格式-->
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!--设置文件命名格式-->
            <fileNamePattern>${LOG_FILE_PATH}/error/${APP_NAME}-%d{yyyy-MM-dd}-%i.log</fileNamePattern>
            <!--设置日志文件大小，超过就重新生成文件，默认10M-->
            <maxFileSize>${LOG_FILE_MAX_SIZE:-10MB}</maxFileSize>
            <!--日志文件保留天数，默认30天-->
            <maxHistory>${LOG_FILE_MAX_HISTORY:-30}</maxHistory>
        </rollingPolicy>
    </appender>

    <!--DEBUG日志输出到LogStash-->
    <appender name="LOG_STASH_DEBUG" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>DEBUG</level>
        </filter>
        <destination>${LOG_STASH_HOST}:4560</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>Asia/Shanghai</timeZone>
                </timestamp>
                <!--自定义日志输出格式-->
                <pattern>
                    <pattern>
                        {
                        "project": "mall-tiny",
                        "level": "%level",
                        "service": "${APP_NAME:-}",
                        "pid": "${PID:-}",
                        "thread": "%thread",
                        "class": "%logger",
                        "message": "%message",
                        "stack_trace": "%exception{20}"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
        <!--当有多个LogStash服务时，设置访问策略为轮询-->
        <connectionStrategy>
            <roundRobin>
                <connectionTTL>5 minutes</connectionTTL>
            </roundRobin>
        </connectionStrategy>
    </appender>

    <!--ERROR日志输出到LogStash-->
    <appender name="LOG_STASH_ERROR" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <destination>${LOG_STASH_HOST}:4561</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>Asia/Shanghai</timeZone>
                </timestamp>
                <!--自定义日志输出格式-->
                <pattern>
                    <pattern>
                        {
                        "project": "mall-tiny",
                        "level": "%level",
                        "service": "${APP_NAME:-}",
                        "pid": "${PID:-}",
                        "thread": "%thread",
                        "class": "%logger",
                        "message": "%message",
                        "stack_trace": "%exception{20}"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
        <!--当有多个LogStash服务时，设置访问策略为轮询-->
        <connectionStrategy>
            <roundRobin>
                <connectionTTL>5 minutes</connectionTTL>
            </roundRobin>
        </connectionStrategy>
    </appender>

    <!--业务日志输出到LogStash-->
    <appender name="LOG_STASH_BUSINESS" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${LOG_STASH_HOST}:4562</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>Asia/Shanghai</timeZone>
                </timestamp>
                <!--自定义日志输出格式-->
                <pattern>
                    <pattern>
                        {
                        "project": "mall-tiny",
                        "level": "%level",
                        "service": "${APP_NAME:-}",
                        "pid": "${PID:-}",
                        "thread": "%thread",
                        "class": "%logger",
                        "message": "%message",
                        "stack_trace": "%exception{20}"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
        <!--当有多个LogStash服务时，设置访问策略为轮询-->
        <connectionStrategy>
            <roundRobin>
                <connectionTTL>5 minutes</connectionTTL>
            </roundRobin>
        </connectionStrategy>
    </appender>

    <!--接口访问记录日志输出到LogStash-->
    <appender name="LOG_STASH_RECORD" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${LOG_STASH_HOST}:4563</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>Asia/Shanghai</timeZone>
                </timestamp>
                <!--自定义日志输出格式-->
                <pattern>
                    <pattern>
                        {
                        "project": "mall-tiny",
                        "level": "%level",
                        "service": "${APP_NAME:-}",
                        "class": "%logger",
                        "message": "%message"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
        <!--当有多个LogStash服务时，设置访问策略为轮询-->
        <connectionStrategy>
            <roundRobin>
                <connectionTTL>5 minutes</connectionTTL>
            </roundRobin>
        </connectionStrategy>
    </appender>

    <!--控制框架输出日志-->
    <logger name="org.slf4j" level="INFO"/>
    <logger name="springfox" level="INFO"/>
    <logger name="io.swagger" level="INFO"/>
    <logger name="org.springframework" level="INFO"/>
    <logger name="org.hibernate.validator" level="INFO"/>

    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
        <!--<appender-ref ref="FILE_DEBUG"/>-->
        <!--<appender-ref ref="FILE_ERROR"/>-->
        <appender-ref ref="LOG_STASH_DEBUG"/>
        <appender-ref ref="LOG_STASH_ERROR"/>
    </root>

<!--    只有配置在logger节点上的appender才会被使用-->
<!--    记录日志，configuration.LogAspect类下所有的DEBUG级别以上的日志，统计接口访问信息的切面类-->
    <logger name="com.cxylk.configuration" level="DEBUG">
        <appender-ref ref="LOG_STASH_RECORD"/>
    </logger>

<!--    业务日志，com.cxylk包下所有DEBUG级别以上的日志-->
    <logger name="com.cxylk" level="DEBUG">
        <appender-ref ref="LOG_STASH_BUSINESS"/>
    </logger>
</configuration>
~~~

需要注意的是在properties或者yml中的logging.level.root配置会覆盖root level的配置

在docker-compose中要去开启logstash对应的端口,也就是ports的值是4560，4561,4562,4563。

然后在logstash的conf配置文件添加如下配置：

~~~xml
input {
  tcp {
    mode => "server"
    host => "0.0.0.0"
    port => 4560
    codec => json_lines
    type => "debug"
  }
  tcp {
    mode => "server"
    host => "0.0.0.0"
    port => 4561
    codec => json_lines
    type => "error"
  }
  tcp {
    mode => "server"
    host => "0.0.0.0"
    port => 4562
    codec => json_lines
    type => "business"
  }
  tcp {
    mode => "server"
    host => "0.0.0.0"
    port => 4563
    codec => json_lines
    type => "record"
  }
}
filter {
  if [type] == "record" {
    mutate {
      remove_field => "port"
      remove_field => "host"
      remove_field => "@version"
    }
    json {
      source => "message"
      remove_field => ["message"]
    }
  }
}
output {
  elasticsearch {
    hosts => ["es:9200"]
    action => "index"
    codec => json
    index => "mod-seckill-%{type}-%{+YYYY.MM.dd}"
    template_name => "mod-seckill"
  }
}
~~~

**配置要点**：

- input：使用不同端口收集不同类型的日志，从4560~4563开启四个端口；
- filter：对于记录类型的日志，直接将JSON格式的message转化到source中去，便于搜索查看；
- output：按类型、时间自定义索引格式。

运行spring-boot应用

## kibana查看日志信息

点击左侧Stack Management，选择Kibana下的index Patterns，点击创建index pattern。**注意：**：需要先运行应用产生一些日志信息到logstash，然后logstash再发送给es，否则创建索引会提示找不到数据。

![](https://z3.ax1x.com/2021/04/13/csbjDe.png)

点击左侧的Discover查看日志信息

![](https://z3.ax1x.com/2021/04/13/csqFv8.png)

分场景创建索引如下

![](https://z3.ax1x.com/2021/04/14/c6gyLt.png)

将日期替换换为*创建索引即可。然后就可以根据索引类型来搜索不同的日志类型。