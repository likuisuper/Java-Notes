## OOM

即out of memory，内存溢出。在jvm中，除了pc不会发生OOM外，其他区域都会发生OOM。

导致oom的原因：

1、业务本身吃内存，没有调优空间，也没有调优需求，比如erp、报表系统等，分配2G内存，那么就是吃2个G的内存

2、回收不到内存（内存泄露）对象回收不了

​	参考：https://zhuanlan.zhihu.com/p/32540739

3、回收的没有用的快，比如回收的速度是10M/s，但是用的速度是20M/s

#### 堆

参数：-Xms、-Xmx、-XX:+PrintGCDetails、 **-XX:+TraceSafepoint（安全带相关信息）**

出现OOM时生成堆dump：

~~~shell
-XX:+HeapDumpOnOutOfMemoryError
~~~

指定生成堆文件的地址：

~~~shell
-XX:+HeapDumpPath=/home/lk/jvmlogs/
~~~

在堆的OOM dump文件中给出的位置（main线程中）全是创建对象相关的代码，比如<init>，因为我们都知道堆区是负责分配内存的

#### 栈

主要说的是虚拟机栈，可通过-Xss调节，需要注意的是，栈太小会导致程序启动失败，比如设置栈大小只有160K（每个机器上的大小不一样），默认1M。

栈的oom在开发的时候我们就应该已经知道

#### 元空间

元空间发生OOM的话，dump文件中给出的位置全是loadClass，defineClass相关代码（解析类，类加载器，动态字节码，cglib）

#### 直接内存

调优：

* 自身
* JVM的堆也会影响

当直接内存发生OOM后，如果是在linux上，那么linux内核会发出kill -9信号杀死该进程，linux有内核保护机制**OOM Killer**，用来保护操作系统能够正常运行。关于这个机制，可以看这篇文章https://blog.csdn.net/s_lisheng/article/details/82192613。可以通过`dmesg -T`查看内核日志，-T表示输出时间戳。

需要注意，**直接内存发生OOM后，是不会产生日志的**，因为直接内存不属于运行时数据区域了，只有OS才知道，这也是为什么会有OOM Killer保护机制的原因

## 调优

调优本质：**通过调节内存大小来平衡gc频率与单次gc时长**

调优参数有三种类型：

1、kv形式：

~~~shell
-XX:MetaspaceSize=10m
~~~

2、bool

~~~shell
-XX:+/-UseCompressedOops
~~~

3、简写

~~~shell
堆 -Xms10m -Xmx10m
栈 -Xss230k
~~~

可以通过visualVM，arthas查看日志信息。

gc日志：

~~~shell
1 [GC (Allocation Failure) [PSYoungGen: 1344K->320K(2048K)] 7894K->7
118K(9216K), 0.0071516 secs] [Times: user=0.01 sys=0.00, real=0.00
secs]
2
3 [GC类型 (GC原因) [新⽣代垃圾收集器: gc前新⽣代的内存使⽤情况->gc后新⽣代的内存
使⽤情况(新⽣代总内存)] gc前堆内存的使⽤情况->gc后堆内存的使⽤情况(堆总内存),
gc耗时] [Times: gc阶段⽤户空间耗时 gc阶段内核空间耗时, gc阶段实际耗时]
4
5 [Full GC (Ergonomics) [PSYoungGen: 320K->0K(2048K)] [ParOldGen: 67
98K->5930K(7168K)] 7118K->5930K(9216K), [Metaspace: 9296K->9233K(1
058816K)], 0.6733958 secs] [Times: user=1.76 sys=0.00, real=0.68 s
ecs]
6
7 [GC类型 (GC原因) [新⽣代垃圾收集器: gc前新⽣代的内存使⽤情况->gc后新⽣代的内存
使⽤情况(新⽣代总内存)] [⽼年代垃圾收集器: gc前⽼年代的内存使⽤情况->gc后⽼年代
的内存使⽤情况(新⽣代总内存)] gc前堆内存的使⽤情况->gc后堆内存的使⽤情况(堆总内
存), [Metaspace: gc前元空间的内存使⽤情况->gc后元空间的内存使⽤情况(元空间总
内存)], gc耗时] [Times: gc阶段⽤户空间耗时 gc阶段内核空间耗时, gc阶段实际耗
时]
~~~



## 工具

#### jps

源码：/openjdk/jdk/src/share/classes/sun/tools/jps，使用java编写

**它是如何识别java进程的呢？**

jps输出的信息全是Java进程的信息，是如何做到的？

java进程在创建的时候，会生成相应的文件，进程相关的信息会写入该文件中，Windows下默认路径是`C:\Users\username\AppData\Local\Temp\hsperfdata_username`，linux下默认路径是`/tmp/hsperfdata_username`

#### jstat

可**实时**了解某个进程的的class、compile、gc、memory的相关信息。为什么说是实时？因为底层实现是**mmap，及内存映射文件**。

**PerfData文件**：

1、文件创建

取决于两个参数

~~~shell
-XX:-/+UsePerfDate
~~~

默认开启，如果关闭，那么就不会创建PerfDate文件，依赖于PerfData文件的工具就无法正常工作。

~~~shell
禁用共享内存
-XX:-/+PerfDisableShareMem
~~~

默认关闭，即支持共享内存，如果禁用，依赖于PerfData文件的工具也无法正常工作。

2、文件删除

默认情况下随java进程的结束而销毁

3、文件更新

-XX:PerfDataSamplingInterval = 50ms 即内存与PerfData⽂件的数据延迟为50ms

#### jinfo

#### jmap

#### jconsole

#### visualVM

#### arthas

## Java Agent

参考：：https://www.jianshu.com/p/f5efc53ced5d