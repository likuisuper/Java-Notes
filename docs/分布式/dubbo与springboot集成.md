## 与spring boot集成

xml的方式过于繁琐，所以现在都是与spring boot集成。

### 基本配置

#### 服务提供者

配置如下，首先是启动类开启dubbo

~~~java
@EnableDubbo
@SpringBootApplication
public class BootServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootServerApplication.class, args);
    }

}
~~~

然后声明一个接口

~~~java
public interface UserService {
    User getUser(Integer id);

    List<User> findUsersByLabel(String label, Integer age);
}
~~~

接口实现

~~~java
public class UserServiceImpl implements UserService {
    private Integer port;

    @Override
    public User getUser(Integer id) {
        User user = createUser(id);
        user.setDesc("当前端口:" + port);
        if(id==1){
            System.out.println("调用服务...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return user;
    }

    @Override
    public List<User> findUsersByLabel(String label, Integer age) {
        List<User> list = new ArrayList<>();
        list.add(createUser(1));
        list.add(createUser(1));
        list.add(createUser(1));
        return list;
    }

    static User createUser(Integer id) {
        User user = new User();
        user.setId(id);
        user.setName("lk");
        user.setBirthday("1998");
        user.setAge(22);
        return user;
    }
}
~~~

配置文件，这里只用到了一些基本的必须配置

~~~properties
dubbo.application.name=boot-server
#协议
dubbo.protocol.name=dubbo
#端口随机生成
dubbo.protocol.port=-1
#注册中心，使用zk
dubbo.registry.address=zookeeper://192.168.63.128:2181
dubbo.registry.timeout=30000
~~~

最后需要暴露服务，在接口实现类上添加`@DubboService`即可

#### 服务消费者

首先在启动类上开启dubbo，和上面一样

然后是配置文件

~~~properties
dubbo.application.name=boot-client
#协议
dubbo.protocol.name=dubbo
#端口随机生成
dubbo.protocol.port=-1
#注册中心，使用zk
dubbo.registry.address=zookeeper://192.168.63.128:2181
dubbo.registry.timeout=30000
~~~

最后对服务提供者暴露的服务进行消费，通过注解`@DubboReference`注解引用

~~~java
@Controller
public class WebController {
    @DubboReference
    private UserService userService;

    @RequestMapping("/getUser")
    @ResponseBody
    public User getUser(Integer id){
        return userService.getUser(id);
    }
}
~~~

### 集中配置

上面的基本配置存在一个问题：`@DubboReference`注解中是可以配置很多属性的，比如timeout等，而如果在多个地方都引用了该服务的话，当需要修改这些配置属性时，就会显得很麻烦，每个引用的地方都要改。所以可以在一个配置类中来暴露服务，然后通过spring注解的方式返回

~~~java
@Configuration
public class DubboConfig {

    /**
     * 使用DubboReferencee已经把这个bean注入了，但是这个bean的id和下面的bean的id是一样的，不会存在两个bean的冲突。
     */
    @DubboReference(group = "${server.member.group}",timeout = 5000,methods = {@Method(name = "getUser",timeout = 5000)})
    private UserService userService;

    //bean的id就是方法名，所以方法名不能写成其他
    @Bean
    public UserService userService(){
        return userService;
    }
}
~~~

然后在上面的WebController类中，使用@Autowired注入即可

~~~java
@Controller
public class WebController {
    @Autowired
    private UserService userService;

    @RequestMapping("/getUser")
    @ResponseBody
    public User getUser(Integer id){
        return userService.getUser(id);
    }
}
~~~

## 配置标签

主要分为三类

#### 公共

比如application、registry、method、argument

#### 服务

service、protocol、provider（不推荐）

#### 引用

reference、consumer（不推荐）

## 配置分类

#### 服务发现

用于服务的注册与发现，为了让消费者找到提供者

#### 服务治理

治理服务间的关系

#### 服务调优

用于调优性能

## 配置传递

服务端设置超时时间

![](https://s3.ax1x.com/2021/03/05/6m7Rzj.png)

超时时间配置为1000ms，但是睡眠了1200ms，然后启动服务端。客户端不做任何更改，

启动客服端后访问服务，比如在命令端输入curl命令来访问接口地址。这时**服务端并不会报超时异常，但是客服端会报超时异常**

~~~java
org.apache.dubbo.remoting.TimeoutException: Waiting server-side response timeout by scan timer
~~~

进入docker启动的zk客户端，进入bin目录，输入

~~~shell
zkCli.sh
~~~

然后输入

~~~shell
ls /dubbo/服务名称/providers
~~~

比如该项目就是

~~~shell
ls /dubbo/cxylk.dubbo.UserService/providers
~~~

如果查询结果不为空，就是有服务

![](https://s3.ax1x.com/2021/03/05/6mHg76.png)

粘贴到url解码网站，可以看到服务详细信息

~~~shell
dubbo://192.168.63.15:20880/cxylk.dubbo.UserService?anyhost=true&application=boot-server&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=cxylk.dubbo.UserService&metadata-type=remote&methods=getUser&pid=22412&release=2.7.8&side=provider&timeout=1000&timestamp=1614952379834
~~~

可以看到超时时间为1000

现在在客户端配置超时时间

![](https://s3.ax1x.com/2021/03/05/6mHzcj.png)

这时候访问服务就不会出现超时异常。

**也就是说，服务端配置的超时时间只是一个参考，一个建议，如果客户端没有配置超时时间，那么就是以服务端为准，否则就是以客户端为准**

## 配置继承

如果服务端或者客户端没有配置超时时间，但是在yml文件或者properties文件配置了超时时间，那么就会去继承文件中的配置。

## 配置优先级

![](https://s3.ax1x.com/2021/03/05/6mboPU.png)

以上面的配置为例，客户端配置超时时间为3000，服务端配置超时时间，并且新加配置如下图

![](https://s3.ax1x.com/2021/03/05/6mLpT0.png)

在DefaultFuter类的newFuter方法第一行打个断点，debug启动客户端，这个时候的超时时间就不再是以客户端的配置为准了，而是以服务端的超时配置为准，因为我们在@DubboService注解中加了@Method注解，**这是最小粒度的配置**。配置后，可以看到当前的超时时间为1500。

