### 使用IDEA搭建Spring源码环境学习Spring

* 进入spring官网，找到PROJECTS，点击SPRING FRAMEWORK，进入github链接。

* 下载zip文件

* spring4.0之后采用gradle构建，所以先下载gradle并配置环境变量
  * 下载gradle，地址：https://gradle.org/，选择所需版本
  
    **注意**：一定要选spring项目构建的版本，在spring-framework-master\gradle\wrapper目录下
  
    有一个gradle-wrapper.properties文件，里面有提示需要下载的gradle版本。如果不下载到本地，idea导入项目时也会自动去下载。
  
  * 解压，并配置环境变量。path添加%GRADLE_HOME%\bin
  
  * 完成后，cmd输入gradle -v验证是否成功
  
* 进入下载的spring项目根目录，执行gradlew.bat文件，双击等待一段时间。

* gradle设置

  ![](https://s3.ax1x.com/2021/01/22/s5yLZQ.png)

use gradle from这里它默认是这个，会去下载gradle，可以配置本地下载的gradle路径，但是我会报错，所以选择默认的。

其他错误还是百度吧，会遇到各种奇奇怪怪的问题。。。。