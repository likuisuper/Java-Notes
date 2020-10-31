# 什么是Spring框架

Spring是一个轻量级的开发框架，旨在提高开发人员的开发效率以及系统的可维护性。是为Java应用程序提供基础性服务的一套框架，目的是用于简化企业应用程序的开发，它使得开发者只需要关心业务需求。常见的配置方式有三种：基于XML配置，基于注解的配置，基于Java的配置。

## Spring的7个模块

![](https://raw.githubusercontent.com/likuisuper/Picked_PicGo/master/img/springModel.png)

* Spring Core:核心类库，提供IOC服务
* Spring Context:提供框架式的Bean访问方式，以及企业级功能；
* Spring AOP:AOP服务
* Spring DAO:对JDBC的抽象，简化了数据访问异常的处理
* Spring ORM:对现有的ORM框架的支持
* Spring Web:提供了基本的面向Web的综合特性，例如多方文件上传
* Spring MVC:提供面向Web应用的Model-View-Controller实现

## Spring中的bean

### 什么叫bean

在Spring中，那些组成应用程序的主体及由Spring IOC容器所管理的对象，被称之为bean。简单地讲，bean就是由IOC容器初始化、装配及管理的对象，除此之外，bean就与应用程序中的其他对象没有什么区别了。

## 什么是面向切面编程AOP

**在运行时，动态地将代码切入到类的指定方法、指定位置上的编程思想就是面向切面的编程**

一般而言，我们管切入到指定类指定方法的代码片段称为切面，而切入到哪些类 、哪些方法则叫切入点。有了AOP,我们就可以把几个类公有的代码，抽取到一个切片中，等到需要时再切入到对象中去，从而改变其原有的行为。