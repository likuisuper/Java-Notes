## 简介

Vert.x是一个在JVM开发reactive应用的框架，可用于开发异步、可伸缩、高并发的Web应用(虽然不限于web应用)。其目的在于为JVM提供一个Node.js的替代方案。开发者可以通过它使用Java、Ruby、Groovy，甚至是混合语言来编写应用。

## Vert.x机制

Vert.x其实就是建立了一个Verticle内部的线程安全机制，让用户可以排除多线程并发冲突的干扰，专注于业务逻辑的实现。Verticle内部代码，除非声明Verticle是Worker Verticle，否则Verticle内部环境全部都是线程安全的，不会出现多个线程同时访问同一个Verticle内部代码的情况。

## Verticle类型

有三种不同类型的verticles;

* **Standard Verticle(标准Verticles)**

  最常见和最有用的类型--他们总是使用Event Loop(时间循环线程)执行。

  标准verticles当创建和调用start方法时分配一个event loop。调用执行都在相同的event loop上。

  这意味着我们可以保证您的verticles实例中的所有代码总是都执行相同的事件循环上 (只要你不调用它自己创建的线程!)。

  这意味着可以在程序里作为单线程编写所有的代码，把担心线程和扩展的问题交给Vert.x。没有更多令人担忧的同步和更多不稳定的问题，也避免了多线程死锁的问题。

* **Worker Verticles**

  一个实例是永远不会有多个线程并发执行。

  Worker verticles就像标准的verticles一样，但不使用事件循环执行，从 Vert.x worker线程池使用一个线程。

  worker verticles 专为调用阻塞的代码，因为他们不会阻止任何事件循环。

  如果你不想使用worker verticles运行阻塞的代码，可以在事件循环上直接运行内联阻塞代码。

  如果您要以worker verticles的方式部署verticle，需要调用 setworker

  *Worker verticle实例永远不会有多个线程并发执行 ，但可以在不同的时间由不同的线程执行。*

* **Multi-threaded worker verticle(多线程的worker verticles)**

  一个实例可以由多个线程同时执行

  多线程的worker verticle就像正常worker verticle，但它是可以由不同的线程同时执行。多线程的worker verticle 是一项高级的功能，大多数应用程序会对他们来说没有必要。因为在这些 verticles 并发，你必须非常小心，使用标准的 Java 技术的多线程编程，以保持verticle一致状态。

## Start

两种 方式实现

### 1.**Main类里面通过main方法部署实例**

maven工程中在pom.xml导入相关依赖

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.example</groupId>
    <artifactId>vertx-example</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <vertx.version>3.4.2</vertx.version>
        <main.class>io.example.Main</main.class>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-core</artifactId>
            <version>${vertx.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.3</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>2.4.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <manifestEntries>
                                        <Main-Class>${main.class}</Main-Class>
                                    </manifestEntries>
                                </transformer>
                            </transformers>
                            <artifactSet />
                            <outputFile>${project.build.directory}/${project.artifactId}-${project.version}-prod.jar</outputFile>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
~~~

其中引入了两个Maven插件，分别是`maven-compiler-plugin`和`maven-shade-plugin`，前者用来将.java的源文件编译成.class的字节码文件，后者可将编译后的.class字节码文件打包成可执行的jar文件，俗称`fat-jar`。

新建两个类

Main

~~~java
package io.example;

import io.vertx.core.Vertx;

/**
 * @author cxylk
 * @Classname Main
 * @Description TODO
 * @Date 2020/11/12 14:05
 **/
public class Main {
    public static void main(String[] args) {
        Vertx vertx=Vertx.vertx();
        //部署具有括号内名称的实例
        vertx.deployVerticle(MyFirstVerticle.class.getName());
    }
}
~~~

其中void deployVerticle(String name)方法：部署具有name的verticle实例。给定名称后，Vert.x选择一个VerticleFactory实例来实例化verticle.

MyFirstVerticle类

~~~java
package io.example;

import io.vertx.core.AbstractVerticle;

/**
 * @author cxylk
 * @Classname MyFirstVerticle
 * @Description TODO
 * @Date 2020/11/12 14:10
 **/
public class MyFirstVerticle extends AbstractVerticle {
    @Override
    public void start(){
        vertx.createHttpServer().requestHandler(req->{
            req.response()
                    .putHeader("content-type","text/plain")
                    .end("hello world");
        }).listen(8080);
    }
}
~~~

这里说一下Verticle对象和处理器(Handler)的关系：Verticle对象往往包含有一个或多个处理器，在Java中，后者经常是以Lambda的形式出现。比如上面代码中的req->{...}



该类继承AbstractVerticle，并重写start方法来开始一个实例。其中createHttpServer()用于创建一个http服务，返回类型为HttpServer，然后调用HttpServer中的方法requestHandler来处理请求。HttpServer requestHandler(Handler<HttpServerRequest> var1);

该方法中的参数是一个函数式接口

~~~java
@FunctionalInterface
public interface Handler<E> {
    void handle(E var1);
}
~~~

使用lambda表达式传入一个HttpServerRequest类型的参数req,调用response方法(返回类型HttpServerResponse)，其中putHeader方法用于设置请求头，void end(String chunk)在结束响应之前以UTF-8编码写入String。chunk：在结束响应之前要写入的字符串。最后调用HttpServer中的listen(int i)方法来监听端口。

最后使用mvn package打包，会出现vertx-example-1.0-SNAPSHOT.jar，vertx-example-1.0-SNAPSHOT-prod.jar两个jar包文件，后者是可执行文件。使用命令：java -jar vertx-example-1.0-SNAPSHOT-prod.jar执行。

**注意** **如果出现无法访问jar包的情况，要把jar包的绝对路径写全。**

### 2.启动器

使用Launcher来替代Main类，这是官方推荐的方式。在`pom.xml`中加入`main.verticle`属性，并将该属性值设置为`maven-shade-plugin`插件的`manifestEntries`的`Main-Verticle`对应的值，最后修改`main.class`为`io.vertx.core.Launcher`，修改后的`pom.xml`如下：

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.example</groupId>
    <artifactId>vertx-example</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <vertx.version>3.4.2</vertx.version>
        <main.class>io.vertx.core.Launcher</main.class>
        <main.verticle>io.example.MainVerticle</main.verticle>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-core</artifactId>
            <version>${vertx.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.3</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>2.4.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <manifestEntries>
                                        <Main-Class>${main.class}</Main-Class>
                                        <Main-Verticle>${main.verticle}</Main-Verticle>
                                    </manifestEntries>
                                </transformer>
                            </transformers>
                            <artifactSet />
                            <outputFile>${project.build.directory}/${project.artifactId}-${project.version}-prod.jar</outputFile>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
~~~

新增MainVerticle.java文件：

~~~java
package io.example;

import io.vertx.core.AbstractVerticle;

/**
 * @author cxylk
 * @Classname MainVerticle
 * @Description TODO
 * @Date 2020/11/12 14:59
 **/
public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        vertx.deployVerticle(MyFirstVerticle.class.getName());
    }
}
~~~

