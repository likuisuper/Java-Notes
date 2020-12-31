## node.js安装配置

安装略，主要讲下配置：

说明：这里的环境配置主要配置的是npm安装的全局模块所在的路径，以及缓存cache的路径，之所以要配置，是因为以后在执行类似：npm install express [-g] （后面的可选参数-g，g代表global全局安装的意思）的安装语句时，会将安装的模块安装到【C:\Users\用户名\AppData\Roaming\npm】路径中，占C盘空间。
例如：我希望将全模块所在路径和缓存路径放在我node.js安装的文件夹中，则在我安装的文件夹【D:\workspace\nodejs】下创建两个文件夹【node_global】及【node_cache】

创建完两个空文件夹之后，打开cmd命令窗口，输入

```cmd
npm config set prefix "D:\workspace\nodejs\node_global"
npm config set cache "D:\workspace\nodejs\node_cache"
```

接下来设置环境变量，关闭cmd窗口，“我的电脑”-右键-“属性”-“高级系统设置”-“高级”-“环境变量”

进入环境变量对话框，在【系统变量】下新建【NODE_PATH】，选择文件夹【D:\workspace\nodejs\node_global\node_modules】，将【用户变量】下的【Path】中原来在c盘中的npm修改为【D:\workspace\nodejs\node_global】

配置完后，安装个module测试下，我们就安装最常用的express模块，打开cmd窗口，
输入如下命令进行模块的全局安装：

```
npm install express -g     # -g是全局安装的意思
```

## Zookeeper安装配置

官网下载地址：http://www.apache.org/dyn/closer.cgi/zookeeper/，下载bin.tar.gz然后解压。

进入conf目录，有一个zoo_sample.cfg文件，将其复制修改为zoo.cfg，因为zookeeper启动时默认会找这个文件。然后打开该文件，修改 dataDir=D:\\\workspace\\\apache-zookeeper-3.5.8-bin\\\data,复制data路径的时候默认是单斜线，**一定要改为双斜线**。如果没有dataLogDir的话，可以加一个dataLogDir=D:\\\workspace\\\apache-zookeeper-3.5.8-bin\\\log。