#### 查看cpu核数

~~~shell
cat /proc/cpuinfo | grep processor
~~~

#### 上传文件

使用 rz 命令打开文件管理器上传到Linux中

#### 下载文件

使用 sz 命令将文件下载到windows中

~~~shell
sz 文件名
~~~

#### 查看文件尾

~~~shell
tail -f 文件名
~~~

#### jar包运行结果追加到nohup.out

~~~shell
nohup java -jar jar包名 &
~~~

然后可以使用tail -f  nohup.out查看是否成功



