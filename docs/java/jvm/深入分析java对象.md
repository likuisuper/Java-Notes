## 对象的创建

java对象的创建过程如下

![](https://z3.ax1x.com/2021/08/18/fTYPSK.png)

#### 1:类加载检查

虚拟机遇到一条 new 指令时，首先将去检查这个指令的参数是否能在常量池中定位到这个类的符号引用，并且检查这个符号引用代表的类是否已被加载过、解析和初始化过。如果没有，那必须先执行相应的类加载过程。比如下面这条new指令

~~~assembly
new #3 <com/cxylk/partone/Hello>
~~~

后面便是它的参数在常量池中的符号引用

#### 2:分配内存

在**类加载检查**通过后，接下来虚拟机将会为新生对象**分配内存**。对象所需的内存大小在类加载完成后便可确定（在下面将对象内存布局的时候会看到），为对象分配空间的任务等同于把一块确定大小的内存从 Java 堆中划分出来。**分配方式**有 **“指针碰撞”** 和 **“空闲列表”** 两种，**选择哪种分配方式由 Java 堆是否规整决定，而 Java 堆是否规整又由所采用的垃圾收集器是否带有空间压缩整理的能力决定**

##### 内存分配的两种方式

指针碰撞：

* 使用场景：堆内存规整的情况
* 原理：将所有被使用过的内存放在一边，空闲的内存放在另一边。中间放一个指针作为分界点的指示器，当分配内存时仅仅是把这个指针向空闲空间方向挪动一段与对象大小相等的距离
* GC收集器：Serial、ParNew

空闲列表：

* 使用场景：内存不规整（已使用的内存和空闲的内存相互交错在一起）
* 原理：虚拟机维护一个列表，记录哪些内存块是可用的，在分配的时候从列表中找到一块足够大的空间划分给对象实例，并更新列表上的记录。
* GC收集器：CMS（理论上）

##### 内存分配并发问题

对象的创建在虚拟机中是一件很频繁的行为，虚拟机必须要保证这个行为是安全的。解决方案

* **CAS+失败重试**：虚拟机采用CAS+失败重试保证更新操作的原子性
* TLAB：把内存分配的动作按照线程划分在不同的空间中进行，即每个线程在java堆中预先分配一小块内存，称为本地线程分配缓冲（TLAB），只有本地缓冲区用完了，分配新的缓冲区时才需要同步锁定。

#### 3:初始化零值

将分配到的内存空间（不包含对象头）都初始化为零值。这一步保证了**对象的实例字段**在代码中不赋初值就能直接使用，使程序能访问到这些字段的数据类型所对应的零值。

#### 4:设置对象头

初始化零值完成之后，虚拟机要对对象进行必要的设置，例如这个对象是哪个类的实例、如何才能找到类的元数据信息、对象的哈希码（实际上对象的哈希码会延后到真正调用Object::hashCode()方法时才计算）、对象的 GC 分代年龄等信息， **这些信息存放在对象头中。** 另外，根据虚拟机当前运行状态的不同，如是否启用偏向锁等，对象头会有不同的设置方式

#### 5:执行init方法

在上面工作都完成之后，从虚拟机的视角来看，一个新的对象已经产生了，但从 Java 程序的视角来看，对象创建才刚开始，`<init>` 方法还没有执行，所有的字段都还为零。所以一般来说（由字节码流中new指令后面是否跟随`invokespecial`指令所决定，java编译器会在遇到new关键字的地方同时生成这两条字节码指令，但如果直接通过其他方式产生的则不一定如此），执行 new 指令之后会接着执行 `<init>` 方法，把对象按照程序员的意愿进行初始化，这样一个真正可用的对象才算完全被构造出来。

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

**如果开启了指针压缩（`-XX:+UseCompressedOops`，默认强制开启`-XX:+UseCompressedClassPointers`）或者JVM堆的最大值小于32G，那么它占用4个字节，否则占用8个字节。在JDK6以后，指针压缩默认是开启的**，可以使用如下命令查看

~~~java
jinfo -flag UseCompressedOops 进程id   
-XX:+UseCompressedOops
    
jinfo -flag UseCompressedClassPointers 进程id
-XX:+UseCompressedClassPointers
~~~

当然，使用`java -XX:+PrintFlagsFinal`也是可以的（jdk6以上）

当开启后，这个引用是32位的值，为了找到真正的64位地址，还需要加上一个base值。

![](https://z3.ax1x.com/2021/08/20/fOE4zQ.png)

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

规则：所有的对象大小都必须能被8整除，也就是8字节对齐，如果没有对齐，需要填充一定数量的0。这么做的原因是因为**虚拟机的自动内存管理系统要求对象起始地址必须是8字节的整数倍。**

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

下面分别计算三种不同类型的对象大小。注意：这里说的是计算对象大小，但是实际消耗内存还要加上指针大小

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
Instance size: 16 bytes //对象大小
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total //压缩了4个字节的空间大小
~~~

上面的结果就是下图所示（最下面的数字是OFFSET）：

![](https://z3.ax1x.com/2021/04/27/g9TW6S.png)

注意VALUE这一列，第一行和第二行也就是offset为0-8之间部分是对象头，并且从上面的输出结果来看，01是高位，对应的二进制是00000001，也就是括号中的的第一个，这里的顺序是反的，高位在前，HSDB查看的话是低位在前，比如这里的`05 c1 00 f8`在HSDB中是`f8 00 c1 05`。mark word对应的地址完整的值是`00 00 00 00 00 00 00 01`，用HSDB查看是在一行，即一个内存单元是8个字节，这不过jol输出为2行，并且高位放在了前面显示

而在上面介绍对象头的Mark Word部分说过，最后3位是lock信息，从上面的结果可以看出是001，表示该对象现在处于无锁状态（0表示未偏向）

我们通过HSDB查看当前对象的内存布局：

![](https://z3.ax1x.com/2021/08/17/f41T9H.png)

其中`_mark`是`MarkOopDesc`类型的，为1表示处于无锁（001），`_metadata`是联合体（下面会细说）

~~~oop
  union _metadata {
    Klass*      _klass;//8个字节
    narrowKlass _compressed_klass;//4个字节
  } _metadata;
~~~

这里用的是它里面的`_compressed_klass`表示此时是开启指针压缩的。

2、关闭指针压缩（-XX:-UseCompressedOops）:

除了类型指针变为8个字节外，其余不变，所以图省略

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           28 30 b8 1b (00101000 00110000 10111000 00011011) (465055784)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
~~~

关闭指针压缩后，可以看到，没有对齐填充部分，Mark Word和类型指针分别占了8个字节，加起来等于16，是8的整数倍。而且VALUE这一列的最后一行即类型指针此时多出来的4个字节都是0，相当于是浪费了4个字节。

我们还是通过HSDB来查看下当前对象

![](https://z3.ax1x.com/2021/08/17/f4Jvss.png)

发现此时的类型指针变成了`_klass`，确实是占用了8个字节。

**总结**：

开启指针压缩，那么会在内存中消耗20字节，其中指针obj占4字节，CountEmptyObj对象占16字节

关闭指针压缩，那么会在内存中消耗24字节，其中指针obj占8字节，CountEmptyObj对象占16字节

`补充`：知道类指针后，发现开启对象指针压缩会默认开启类指针压缩，**所以类型指针应该是由类指针压缩的**，而obj由8字节压缩成4字节才是对象指针压缩的（关于对象指针和类型指针可以看对象访问定位那张图），比如上面的测试代码加上启动参数`-XX:+UseCompressedOops -XX:-UseCompressedClassPointers`：

~~~java
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           28 30 34 1c (00101000 00110000 00110100 00011100) (473182248)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
~~~

发现这就是上面关闭对象指针压缩的结果！！！

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
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
~~~

HSDB结果：

![](https://z3.ax1x.com/2021/08/17/f4sBsf.png)

与开启指针压缩的空对象相比，多了a,b这两个实例数据

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
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
~~~

关闭指针压缩后对象大小也是24字节（少了对齐填充），HSDB中的`_compressed_klass`变成`_klass`。

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
      8     4        (object header)                           28 30 5b 1c (00101000 00110000 01011011 00011100) (475738152)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
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
     12     4        (object header)                           03 00 00 00 (00000011 00000000 00000000 00000000) (3) //表示数组的容量是3
     16    12    int [I.<elements>                             N/A
     28     4        (loss due to the next object alignment)
Instance size: 32 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
~~~

HSDB结果：

![](https://z3.ax1x.com/2021/08/17/f4O1ZF.png)

**2、关闭指针压缩**

关闭指针压缩后，类型指针变为8个字节，那么对象大小应该是：8+8+4+12=32字节，看输出结果：

~~~java
[I object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           68 0b 52 1c (01101000 00001011 01010010 00011100) (472976232)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
     16     4        (object header)                           03 00 00 00 (00000011 00000000 00000000 00000000) (3)
     20     4        (alignment/padding gap)                  
     24    12    int [I.<elements>                             N/A
     36     4        (loss due to the next object alignment)
Instance size: 40 bytes
Space losses: 4 bytes internal + 4 bytes external = 8 bytes total
~~~

结果是40字节，并不是期望的32字节，这是怎么回事？仔细观察上面的输出结果：在数组长度后面，多了一部分区域，叫alignment/padding gap，也是填充的意思，它占了4个字节，这个值怎么计算出来的呢？先不看整个对象的大小，现在只关注对象头，它的大小=8+8+4=20B，它不是8的整数倍，所以需要填充4个字节，这时候对象头的大小变为24字节，整个对象大小=24+12=36字节，所以还需要在填充4字节，最终它的大小=40字节。也就是说**当数组对象关闭指针压缩后（比开启时多4B），那么它的头部大小就不满足是8字节的整数倍，所以需要对齐填充来补齐。如果实例数据也不是8字节整数倍的话，还要填充对齐填充这部分**。

此时前面所说的对象内存布局将变为这样：

![](https://z3.ax1x.com/2021/04/27/gCWlY8.png)

这个时候我们便能看到指针压缩节省内存的效果，它节省了4B的内存。这个时候你可能会有疑问：开启指针压缩比没有开启指针压缩好像也没有节省多少内存，并且有的情况开启和不开启是一样的，比如前面的空对象和普通对象。其实现在的虚拟机已经很成熟了，不可能说开发一个技术就能让它的性能提升2倍或者更多，这只存在早期虚拟机开发的情况，所以现在要提升虚拟机的性能，都是在一些细节方面，能省一点内存是一点，总比没有好。

另外，为什么关闭指针压缩后，需要在对象头填充呢？在下面将指针压缩的时候会提到，如果开启指针压缩，那么一个内存单元（8字节，从jol的输出结果和HSDB中地址对应的值占一行即8个字节可以看出）刚好能放入类型指针和数组长度，但是关闭指针压缩后，一个类型指针就占了8个字节，数组长度前面不填充的话，在它后面的实例数据就不好解析，如果是一个int类型的还好，把它放数组长度前面，两者刚好占一个内存单元，但如果实例数据不是int而是其他比如char的这种，每次都需要去计算大小，显然不好，所以干脆在数组长度前面填充4字节，实例数据从下一个内存单元存放即可，这样方便解析。

## 指针压缩

### 对象指针压缩

会默认开启类指针压缩，压缩`klass *`大小，所以这里分析的对象指针压缩实际上是在分析类指针压缩。

#### 实现

下面具体探究指针压缩以及实现原理。

还是以上面的数组对象为例，通过HSDB查看关闭指针压缩和开启指针压缩的内存布局（上面是关闭，下面是开启）：

![](https://z3.ax1x.com/2021/04/28/gicrZt.png)

需要注意，HSDB中地址中的值和jol输出的值顺序是反的，jol是从高位到低位，HSDB是从低位到高位显示的。

可以看到，关闭指针压缩时，数组对象地址对应的值0x00000003在第三格最后（前面是填充部分），而开启指针压缩后它“跑”在了第二格。这是为什么呢？

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

union代表这个结构是一个联合体，联合体所占的空间不仅取决于最宽成员，还跟所有成员有关系，即其大小必须满足两个条件：

1)大小足够容纳最宽的成员；

2)大小能被其包含的所有基本数据类型的大小所整除。

所以这个联合体至少需要8字节大小的空间。所表达的意思就是如果没开启指针压缩，那么类型指针就是8字节，否则使用`_compressed_klass`，也就是4字节。

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

联合体还是占8字节，但是类型指针现在只占4个字节，也就是说8个字节只用了4个字节（高位），**有4字节（低位）是浪费的，所以会把数组长度（正好占4字节）放到没有用到的4个字节（低位）中去，这样就省了8字节的存储空间（因为数组长度这格没了）**

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

以64G为例，**只需要将8字节对齐改成16字节对齐**即可，2^32*2^4=64G，但是为什么虚拟机最大只支持32G而不支持64G呢？原因有两点：

* 64位机下，其实只用到了48位虚拟地址空间，还有16位是保留位。为什么只用48位呢？因为现在还用不到完整的64位寻址空间，所以硬件也没有必要支持那么多位的地址。为什么用不到呢？因为CPU的计算能力还跟不上，它没法在短时间内去寻址这么大的内存。
* 浪费内存更严重，使用指针压缩的目的就是为了节省内存，但是如果使用16字节对齐的话节省出来的内存可能又被浪费了，比如现在有一个对象占用17个字节，使用8字节对齐的话只需要补7个字节（17+7=24，能被8整除），但是使用16个字节的话需要补15个字节（17+15=32能被16整除）

### 类指针压缩

关于这部分内容可以阅读这篇文章[什么是元空间](https://stuefe.de/posts/metaspace/what-is-metaspace/)；[中文翻译](https://javadoop.com/post/metaspace)

开启`UseCompressedOops`，默认会开启`UseCompressedClassPointers`，会压缩klass pointer 这部分的大小。

由于`UseCompressedClassPointers`的开启是依赖于`UseCompressedOops`的开启，因此，要使`UseCompressedClassPointers`起作用，得先开启`UseCompressedOops`，并且开启`UseCompressedOops` 也默认强制开启`UseCompressedClassPointers`，关闭`UseCompressedOops` 默认关闭`UseCompressedClassPointers`

如果开启类指针压缩，`+UseCompressedClassPointers`，并关闭普通对象指针压缩，`-UseCompressedOops`，此时会警告，
`UseCompressedClassPointers requires UseCompressedOops`，源码如下：

```
// UseCompressedOops must be on for UseCompressedClassPointers to be on.
  if (!UseCompressedOops) {
    if (UseCompressedClassPointers) {
      warning("UseCompressedClassPointers requires UseCompressedOops");
    }
    FLAG_SET_DEFAULT(UseCompressedClassPointers, false);	
```

## 对象的访问定位

建立对象就是为了使用对象，我们的 Java 程序通过栈上的 reference 数据来操作堆上的具体对象。对象的访问方式由虚拟机实现而定，目前主流的访问方式有**① 使用句柄**和**② 直接指针**两种：

1. **句柄：** 如果使用句柄的话，那么 Java 堆中将会划分出一块内存来作为句柄池，reference 中存储的就是对象的句柄地址，而句柄中包含了对象实例数据与类型数据各自的具体地址信息；

   ![](https://z3.ax1x.com/2021/08/18/fTEy8I.png)

2. **直接指针：** 如果使用直接指针访问，那么 Java 堆对象的布局中就必须考虑如何放置访问类型数据的相关信息，而 reference 中存储的直接就是对象的地址。

   ![](https://z3.ax1x.com/2021/08/18/fTEYgx.png)

**这两种对象访问方式各有优势。使用句柄来访问的最大好处是 reference 中存储的是稳定的句柄地址，在对象被移动时只会改变句柄中的实例数据指针，而 reference 本身不需要修改。使用直接指针访问方式最大的好处就是速度快，它节省了一次指针定位的时间开销。**

对于`HotSpot`虚拟机而言，它是使用第二种方式进行对象访问的。