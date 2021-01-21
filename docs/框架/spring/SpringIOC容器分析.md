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