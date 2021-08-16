## 对象内存布局

前面说过了oop模型，明白了java对象在jvm中的存在形式。下面来探究对象的内存布局，知道了对象的内存布局后，我们就可以算出一个对象到底占用了多少内存。

#### 对象头

对象头又包含了三部分

##### Mark Word

Mark Word主要存放了对象运行时的数据，比如哈希码、gc分代年龄、锁信息等。在32位和64位的虚拟机中大小分别为4B和8B:

32位：hash(25)+age(4)+lock(3)=32bit

64位：unused(25+1)+hash(31)+age(4)+lock(3)=64bit

关于Mark Word的具体信息可以查看源码oops/markOop.hpp，其中有详细的解释。

##### 类型指针

类型指针（klass pointer）:即对象指向它的类型元数据（在方法区中）的指针。jvm通过它可以确定该对象是哪个类的实例。

![](https://z3.ax1x.com/2021/08/16/fW1cGR.png)

在32位的虚拟机中它占4个字节（**32位虚拟机没有指针压缩**），但是在64位虚拟机中需要特别注意：

**如果开启了指针压缩（`-XX:+UseCompressedOops`）或者JVM堆的最大值小于32G，那么它占用4个字节，否则占用8个字节。在JDK6以后，指针压缩默认是开启的**，可以使用如下命令查看

~~~java
jinfo -flag UseCompressedOops 进程id
    
-XX:+UseCompressedOops
~~~

当然，使用`java -XX:+PrintFlagsFinal`也是可以的（jdk6以上）

##### 数组长度

这部分内容并不是必需要有的，只有当对象是一个数组时它会用来记录数组的长度。因为一个数组长度不确定话，那么jvm是无法通过元数据中的信息来推断出数组大小的。数组长度占用4个字节

#### 实例数据

对象真正存储的有效信息。也就是代码中定义的各种类型的字段内容，注意是**类的实例字段，不包括静态字段，因为静态字段只会存在一份，所有对象共享它**。基本类型如下：

| boolean | byte | short | int  | long | float | double | char |
| ------- | ---- | ----- | ---- | ---- | ----- | ------ | ---- |
| 1B      | 1B   | 2B    | 4B   | 8B   | 4B    | 8B     | 2B   |

引用类型：

| 开启指针压缩 | 关闭指针压缩 |
| ------------ | ------------ |
| 4B           | 8B           |

#### 对齐填充

规则：所有的对象大小都必须能被8整除，也就是8字节对齐，如果没有对齐，需要填充一定数量的0。这么做的原因是因为虚拟机的自动内存管理系统要求对象起始地址必须是8字节的整数倍。

由上面的分析得到对象的内存布局图：

![](https://z3.ax1x.com/2021/04/27/gC2m1H.png)

需要注意的是，数组关闭了指针压缩后，内存布局将会是另外一种情况，下面会详细讨论。

## 对象大小

知道对象内存布局后，就可以来计算一个对象所占用的内存了。在这里需要引入jol依赖用来输出对象大小，用来验证计算结果是否正确：

~~~xml
        <dependency>
            <groupId>org.openjdk.jol</groupId>
            <artifactId>jol-core</artifactId>
            <version>0.10</version>
        </dependency>
~~~

下面分别计算三种不同类型的对象大小

#### 空对象

该类中不存在实例数据

测试代码如下：

~~~java
public class CountEmptyObj {
    public static void main(String[] args) {
        CountEmptyObj obj=new CountEmptyObj();
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
    }
}
~~~

1、开启指针压缩（默认开启）：

![](https://z3.ax1x.com/2021/04/27/gpoAYV.png)

对象大小=8+4+0+0+4=16字节

输出结果：

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           05 c1 00 f8 (00000101 11000001 00000000 11111000) (-134168315)
     12     4        (loss due to the next object alignment)   //对齐填充
Instance size: 16 bytes
~~~

上面的结果就是下图所示：

![](https://z3.ax1x.com/2021/04/27/g9TW6S.png)

2、关闭指针压缩（-XX:-UseCompressedOops）:

除了类型指针变为8个字节外，其余不变，所以图省略

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           28 30 b8 1b (00101000 00110000 10111000 00011011) (465055784)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
Instance size: 16 bytes
~~~

关闭指针压缩后，可以看到，没有对齐填充部分，Mark Word和类型指针分别占了8个字节，加起来等于16，是8的整数倍。

#### 普通对象

测试代码：

~~~java
public class CountObjSize {
    private int a=10;
    private int b=20;

    public static void main(String[] args) {
        CountObjSize countObjSize=new CountObjSize();
        System.out.println(ClassLayout.parseInstance(countObjSize).toPrintable());
    }
}
~~~

1、开启指针压缩

一个int占4个字节，代码中有两个int类型的字段，所以实例数据占8个字节：

![](https://z3.ax1x.com/2021/04/27/gCy9dU.png)

对象大小=8+4+0+8+4=24字节

输出结果：

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           05 c1 00 f8 (00000101 11000001 00000000 11111000) (-134168315)
     12     4    int CountObjSize.a                            10
     16     4    int CountObjSize.b                            20
     20     4        (loss due to the next object alignment)
Instance size: 24 bytes
~~~

2、关闭指针压缩

输出结果：

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           28 30 02 1c (00101000 00110000 00000010 00011100) (469905448)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
     16     4    int CountObjSize.a                            10
     20     4    int CountObjSize.b                            20
Instance size: 24 bytes
~~~

关闭指针压缩后对象大小也是24字节

上面说过，**实例数据不包括静态字段**，所以将字段加上static后

~~~java
    private static int a=10;
    private static int b=20;

    public static void main(String[] args) {
        CountObjSize countObjSize=new CountObjSize();
        System.out.println(ClassLayout.parseInstance(countObjSize).toPrintable());
    }
~~~

输出结果如下：

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           05 c1 00 f8 (00000101 11000001 00000000 11111000) (-134168315)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
~~~

这说明实例数据的确不包含静态字段。

#### 数组对象

测试代码，以int数组为例

~~~java
public class CountArrayObjSize {

    static int[] arr={1,2,3};

    public static void main(String[] args) {
        System.out.println(ClassLayout.parseInstance(arr).toPrintable());
    }
}
~~~

1、开启指针压缩

![](https://z3.ax1x.com/2021/04/27/gCg6OI.png)

int数组中存储了三个int类型的数据，所以实例数据=3*4=12B

数组对象大小：8+4+4+12+4=32

输出结果：

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           6d 01 00 f8 (01101101 00000001 00000000 11111000) (-134217363)
     12     4        (object header)                           03 00 00 00 (00000011 00000000 00000000 00000000) (3)
     16    12    int [I.<elements>                             N/A
     28     4        (loss due to the next object alignment)
Instance size: 32 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
~~~

**2、关闭指针压缩**

关闭指针压缩后，类型指针变为8个字节，那么对象大小应该是：8+8+4+12=32字节，看输出结果：

~~~java
[I object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           68 0b 31 1c (01101000 00001011 00110001 00011100) (472976232)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
     16     4        (object header)                           03 00 00 00 (00000011 00000000 00000000 00000000) (3)
     20     4        (alignment/padding gap)                  
     24    12    int [I.<elements>                             N/A
     36     4        (loss due to the next object alignment)
Instance size: 40 bytes
Space losses: 4 bytes internal + 4 bytes external = 8 bytes total
~~~

结果是40字节，并不是期望的32字节，这是怎么回事？仔细观察上面的输出结果：在数组长度后面，多了一部分区域，叫alignment/padding gap，也是填充的意思，它占了4个字节，这个值怎么计算出来的呢？先不看整个对象的大小，现在只关注对象头，它的大小=8+8+4=20B，它不是8的整数倍，所以需要填充4个字节，这时候对象头的大小变为24字节，整个对象大小=24+12=36字节，所以还需要在填充4字节，最终它的大小=40字节。也就是说**当数组对象关闭指针压缩后（比开启时多4B），那么它的头部大小就不满足是8字节的整数倍，所以需要对齐填充来补齐。如果实例数据也不是8字节整数倍的话，还要对齐填充这部分**。

此时前面所说的对象内存布局将变为这样：

![](https://z3.ax1x.com/2021/04/27/gCWlY8.png)

这个时候我们便能看到指针压缩节省内存的效果，它节省了4B的内存。这个时候你可能会有疑问：开启指针压缩比没有开启指针压缩好像也没有节省多少内存，并且有的情况开启和不开启是一样的，比如前面的空对象和普通对象。其实现在的虚拟机已经很成熟了，不可能说开发一个技术就能让它的性能提升2倍或者更多，这只存在早期虚拟机开发的情况，所以现在要提升虚拟机的性能，都是在一些细节方面，能省一点内存是一点，总比没有好。

另外，为什么关闭指针压缩后，需要在对象头填充呢？在下面将指针压缩的时候会提到，如果开启指针压缩，那么一个内存单元（8字节）刚好能放入类型指针和数组长度，但是关闭指针压缩后，一个类型指针就占了8个字节，数组长度前面不填充的话，在它后面的实例数据就不好解析，如果是一个int类型的还好，把它放数组长度前面，两者刚好占一个内存单元，但如果实例数据不是int而是其他比如char的这种，每次都需要去计算大小，显然不好，所以干脆在数组长度前面填充4字节，实例数据从下一个内存单元存放即可，这样方便解析。

## 指针压缩

#### 实现

下面具体探究指针压缩以及实现原理。

还是以上面的数组对象为例，通过HSDB查看关闭指针压缩和开启指针压缩的内存布局（上面是关闭，下面是开启）：

![](https://z3.ax1x.com/2021/04/28/gicrZt.png)

可以看到，关闭指针压缩时，数组对象的地址0x00000003在第三格最后（前面是填充部分），而开启指针压缩后它“跑”在了第二格。这是为什么呢？

在这里不妨看下源码：oop.hpp

~~~cpp
  union _metadata {
    Klass*      _klass;//8个字节
    narrowKlass _compressed_klass;//4个字节
  } _metadata;
~~~

其中`_klass`是一个指针，在64位下占8个字节，而`_compressed_klass`是一个narrowKlass类型的数据

~~~cpp
// If compressed klass pointers then use narrowKlass.
//开启指针压缩，使用它
typedef juint  narrowKlass;

//junit也就是无符号的整型
typedef unsigned int     juint;
~~~

union代表这个结构是一个联合体，联合体的大小取决于其中最大的变量的长度，所以这个联合体 的大小就是8字节。所表达的意思就是如果没开启指针压缩，那么类型指针就是8字节，否则使用`_compressed_klass`，也就是4字节。

假设现在类型指针和数组长度如下：

~~~java
//类型指针
0xffffffff
//数组长度
0x00000003
~~~

没开启指针压缩，那么联合体占8个字节，类型指针占8个字节，所以数组长度自然被“挤”到了下一格。

当开启指针压缩后：

~~~java
//类型指针
0x0003ffff
~~~

联合体还是占8字节，但是类型指针现在只占4个字节，也就是说8个字节只用了4个字节，**有4字节是浪费的，所以会把数组长度（正好占4字节）放到没有用到的4个字节中去，这样就省了8字节的存储空间（因为数组长度这格没了）**

#### 底层原理

假设现在有三个对象，它们的大小以及内存地址如下：

![](https://z3.ax1x.com/2021/04/28/gifZ38.png)

真实地址：

test1=0	0000

test2=16	10000

test3=48	110000

前面说过一个规律，**8字节对齐**：内存地址是8的整数倍，所以在存储的时候抹掉后3个0，取的时候再加上即可，如下：

在存储的时候，>>3

test1=0	000 0

test2=16	000 10

test3=48	000 110

用的时候，<<3

test1=0	000 0 000

test2=16	000 10 000

test3=48	000 110 000

为什么要存储的时候抹掉后3个0？因为这就相当于右移3位，变为原来的1/8，也就是能节省8字节内存。

#### 32G瓶颈

开启指针压缩的情况下，类型指针占4个字节，并且是按8字节对齐，所以说当超过2^32*2^3=32G的时候就会关闭指针压缩。

如果说要将32G扩容成64G或者128G该怎么办呢？

以64G为例，**只需要将8字节对齐改成16字节对齐**即可，2^32*2^4=16G，但是为什么虚拟机最大只支持32G而不支持64G呢？原因有两点：

* 64位机下，其实只用到了48位虚拟地址空间，还有16位是保留位。为什么只用48位呢？因为现在还用不到完整的64位寻址空间，所以硬件也没有必要支持那么多位的地址。为什么用不到呢？因为CPU的计算能力还跟不上，它没法在短时间内去寻址这么大的内存。
* 浪费内存更严重，使用指针压缩的目的就是为了节省内存，但是如果使用16字节对齐的话节省出来的内存可能又被浪费了，比如现在有一个对象占用17个字节，使用8字节对齐的话只需要补7个字节（17+7=24，能被8整除），但是使用16个字节的话需要补15个字节（17+15=32能被16整除）

#### 类指针压缩

上面说的都是对象指针压缩，其实还有类指针压缩，关于这部分内容可以阅读这篇文章https://stuefe.de/posts/metaspace/what-is-metaspace/；中文：https://javadoop.com/post/metaspace

## 亿级流量调优

这里以亿级流量秒杀电商系统为例：

1、如果每个用户平均访问20个商品详情页，那访客数约等于500w（一亿/20）

2、如果按转化率10%来算，那日均订单约等于50w（500w*10%）

3、如果30%的订单是在秒杀前两分钟完成的，那么每秒产生1200笔订单（50%*30%/120s）

4、订单⽀付⼜涉及到发起⽀付流程、物流、优惠券、推荐、积分等环节，导致产⽣⼤量对象，这⾥我们假 设整个⽀付流程⽣成的对象约等于20K，那每秒在Eden区⽣成的对象约等于20M（1200笔 * 20K） 

5、在⽣产环境中，订单模块还涉及到百万商家查询订单、改价、包邮、发货等其他操作，⼜会产⽣⼤量对 象，我们放⼤10倍，即每秒在Eden区⽣成的对象约等于200M（其实这⾥就是在⼤并发时刻可以考虑服务 降级的地⽅，架构其实就是取舍） 这⾥的假设数据都是⼤部分电商系统的通⽤概率，是有⼀定代表性的。

假设分配给堆的初始大小为8G（生产环境肯定比这个大），那么堆中各区域的内存布局如下：

![](https://z3.ax1x.com/2021/05/01/gVNvND.png)

依照上面的分析，每秒在Eden区生成的对象约等于200M，那么2.2G/200M/s=11s，也就是说11s内Eden区就会被占满。如果一个请求占3s，那么这时来了一个请求，就会产生3*200M=600M对象，这600M对象都还在使用，也就是说还能通过引用找到他们，那么他们就无法被回收，而这600MSurvivor区是放不下的，所以这时候会触发**空间担保机制，提前将Survivor放不下的对象也就是这600M提前转移到老年代去**。而老年代大小是5.4G，那么经历5.4G/600M=9次ygc后，就会触发一次fgc。而一次ygc触发时间是11s，那么9次ygc就是99s，也就是说**99s触发一次fgc**。

经过上面的分析，我们知道在堆内存8G的情况下，11s触发一次ygc，99s触发一次fgc，而正常的频率应该是

**5分钟一次ygc，一天一次fgc**。所以需要对当前系统调优。

调优怎么调呢？当前系统不要触发空间担保即可，言外之意就是Survivor的大小要大于600M，假设就以600M来算，根据Eden:Survivor=8:1，那么Eden就需要4800M，那么新生代大概需要4800+600*2=6G，而老年代是新生代的2倍，那么老年代就需要12G，整个堆就需要6+12=18G的大小。由于我们估算的比价大，所以单机有16G内存就足够了。当然，实际情况可能秒杀的流量更大，产生的对象也不一定和分析一致，所以还是要以系统实际情况为主。

调优的目的：避免OOM<--避免full gc<--避免young gc

单次gc时间在100ms以内（0.1s）

