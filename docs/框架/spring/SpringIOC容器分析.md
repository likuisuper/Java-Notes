## 一些关键的类和接口

#### Resource接口

![](https://s3.ax1x.com/2021/01/23/sHShE6.png)

该接口是一个资源描述符接口，从基础资源的类型中抽象出来，例如文件或类路径资源。它对各种形式的BeanDefinition的使用都提供了统一接口。

#### ResourceLoader接口

该接口用于加载资源(例如类路径或文件系统资源)

~~~java
public interface ResourceLoader {

	/** Pseudo URL prefix for loading from the class path: "classpath:". */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

    //返回一个Resource
	Resource getResource(String location);

	@Nullable
	ClassLoader getClassLoader();

}
~~~

## 容器刷新流程入口

### 1.刷新准备



### 2.获取容器bean工厂



### 3.配置bean工厂

设置Beanfactory的类加载器，beanFactory需要加载类，也就需要类加载器

回调：定义一个接口，该接口有一个实现类，实现了该接口中的方法，另外一个类的方法中传递的参数是接口，然后调用接口的实现类中的方法，也就是将接口作为参数

























## Bean的生命周期

## 循环依赖

## 三级缓存

## FactoryBean和BeanFactory

## ApplicationContext与BeanFactory的区别

## 设计模式



# spring中ClassPathXmlApplicationContext和FileSystemXmlApplicationContext的区别

ClassPathXmlApplicationContext ：默认文件路径是src下那一级。classpath前缀是缺省的

classpath:和classpath*:的区别: 

classpath: 只能加载一个配置文件,如果配置了多个,则只加载第一个 

classpath*: 可以加载多个配置文件,如果有多个配置文件,就用这个

 

FileSystemXmlApplicationContext 

这个类,默认获取的是项目路径,默认文件路径是项目名下一级，与src同级，从文件的绝对路径加载配置文件，灵活性较差

如果前边加了file:则说明后边的路径就要写全路径了,就是绝对路径：file:D:/workspace/applicationContext.xml