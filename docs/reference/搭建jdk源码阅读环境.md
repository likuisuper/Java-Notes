## 怎么阅读源码

常用的源码阅读技巧

* ctrl+alt+<-，返回上一步，再也不用担心回不到上一步去啦
* ctrl+alt+->，返回下一步
* ctrl+e，列出最近打开的文件
* ctrl+f12，列出该类中的方法和字段
* 其他一些搜索快捷键，ctrl+f，double shift等
* 时序图插件sequence diagram，选中方法生成时序图，查看调用流程

## IDEA搭建环境

#### 1.新建一个普通的Java项目，略

#### 2.进入jdk目录，将src.zip复制到新建项目下，然后解压

#### 3.替换SDKS

为了不影响其他项目使用的sourcePath,新建一个SDKS。

进入Project Settings,新建一个SDKS，选择jdk(java8下面那个jdk)，然后将SourcePath中的默认路径换成当前项目下的src目录

#### 3.新建一个测试包，新建测试类测试

开始测试的时候，会发现电脑风扇很响，而且cpu完全被占满，解决办法：

进入file->settings->Build,Execution,Deployment->Compiler

将Build process heap size (Mbytes)适当调大，比如调成1800

#### 4.解决类找不到问题

* 程序包com.sun.tools.javac.api不存在

  解决办法：将jdk目录下的lib目录中的tools.jar添加到：project settings->libraries

* 找不到sun.awt.UNIXToolkit

  进入[OpenJDK官网](http://openjdk.java.net/)，点击Mercurial，选择jdk8u版本，找到src/solaris/classes/sun/awt/UNIXToolkit.java，将其复制。然后在我们的项目中新建一个sun.awt包，里面新建一个类UNIXToolkit，将复制内容粘贴进去。

* 找不到sun.font.FontConfigManager

  和上面一样，找到src/solaris/classes/sun/font/FontConfigManager.java，复制内容，新建sun.font包，新建FontConfigManager类，将复制内容粘贴进去。

这时候在启动项目就可以了，会报警告，但不会报错，可以正常调试了。

项目结构：

![](https://s3.ax1x.com/2021/01/21/shvFOg.png)



