## os和jvm两者的内存模型

假如现在在操作系统上运行了不同的几个进程，如下图：

![](https://z3.ax1x.com/2021/04/20/c7m5gP.png)

其中的Java进程中，包含下面几部分；

* 代码段

* 数据区

* 栈

  ----上面三个都是操作系统控制的，我们程序员是无法操作的

* 堆

  **其中的运行时数据区就是我们所说的jvm内存模型**

  * 程序计数器（字节码指令前面的序号，如int pc=0）
  * 虚拟机栈
  * 本地方法栈
  * 堆
    * 方法区（jdk1.8之前的写法，实现为永久代，在堆区）

  方法区（jdk1.8，实现为元空间，在直接内存，如上图所示）

根据前面所学知识，我们知道，.class文件通过类加载器加载到内存后，会生成一个klass，它是c++实现的，在java堆区。klass包含instanceKlass和mirrorKlass（还有其他的），前者用来描述类的元信息，在方法区，后者是Class对象，在堆区。**那么mirrorKlass是怎么从java堆区跑到jvm堆区的呢？是用c++中的操作符重写实现的**。我们可以看看这部分的源码：

源码：hotspot/src/share/vm/memory/allocation.hpp

~~~cpp
template <MEMFLAGS F> class CHeapObj ALLOCATION_SUPER_CLASS_SPEC {
 public:
  _NOINLINE_ void* operator new(size_t size, address caller_pc = 0) throw();
  _NOINLINE_ void* operator new (size_t size, const std::nothrow_t&  nothrow_constant,
                               address caller_pc = 0) throw();
  _NOINLINE_ void* operator new [](size_t size, address caller_pc = 0) throw();
  _NOINLINE_ void* operator new [](size_t size, const std::nothrow_t&  nothrow_constant,
                               address caller_pc = 0) throw();
  void  operator delete(void* p);
  void  operator delete [] (void* p);
};
~~~

可以看到，通过对new关键字进行operator重写，来自定义new对象的这个过程。并且在上面代码我们还看到了一个类CHeapObj，也就是C堆，除了它之外，还定义了其他一些类，源码中对这些类的解释是：虚拟机中所有的类都必须是他们的子类，并且通过这些类型来分配对象：

~~~cpp
// All classes in the virtual machine must be subclassed
// by one of the following allocation classes:
//
// For objects allocated in the resource area (see resourceArea.hpp).
// - ResourceObj
//
// For objects allocated in the C-heap (managed by: free & malloc).
// - CHeapObj
//
// For objects allocated on the stack.
// - StackObj
//
// For embedded objects.
// - ValueObj
//
// For classes used as name spaces.
// - AllStatic
//
// For classes in Metaspace (class data)
// - MetaspaceObj
~~~

举个例子，比如前面说到的Klass类，它是java类在c++中的映射，在前面我们看过klass模型的继承结构，在源码中是这样的：

源码：hotspot/src/share/vm/oops/klass.hpp

~~~cpp
class Klass : public Metadata {
    ...
}
~~~

而Metadata又继承于MetaspaceObj

源码：hotspot/src/share/vm/oops/metadata.hpp

~~~
// This is the base class for an internal Class related metadata
class Metadata : public MetaspaceObj {

}
~~~

**jvm内存模型如下**：

![](https://z3.ax1x.com/2021/04/20/c7UW5t.png)



下面都基于这张图来讲解

首先需要搞清几个名词：

**1、字节码文件**

class content

类加载器将硬盘上的.class文件读入内存的那一块内存区域。其实就是字节流stream

源码位置：hotspot/src/share/vm/classfile/classFileParser.cpp

~~~cpp
//parseClassFile方法
...
ClassFileStream* cfs=stream();//直接内存中
...
~~~

字节流何时释放？当.class文件中的内容全部被解析并生成klass模型（放在方法区，jdk8中的元空间）。注意这个解析并不是类加载阶段的解析。

**2、klass模型**

这个是反复提到过的东西：java中的每个类，在jvm中都有对应的一个klass实例与之相对应，它是用c++实现的，用来存储类的元信息，常量池，属性信息。。。它存储在元空间。

**3、class对象**

堆区，也就是mirrorKlass

**4、对象**

java中通过new关键字实现。

#### 方法区

方法区用来存储类的信息，常量，即时编译器编译后的代码等数据。**它是线程共享的一块区域**。它是一种规范，有两种实现，分别是永久代和元空间

* 永久代
  * jdk8之前
  * jvm堆区
* 元空间
  * jdk8以后
  * 直接内存（也就是java进程的堆）

思考：为什么要把对方法区的实现从永久代改为元空间？

* 永久代缺点：
  * 回收条件苛刻
    * 该类所有的实例都已经被回收，堆中不存在该类的任何实例
    * 加载该类的ClassLoader已经被回收
    * 无法通过反射访问该类的方法
  * 释放的内存很少
* 元空间如何解决
  * 基本的调优
* 元空间内部如何存储？存在的问题？如何优化？
  * 具体参考https://stuefe.de/posts/metaspace/what-is-metaspace/，中文：https://javadoop.com/post/metaspace
  * 存在问题：碎片化问题
  * 优化：比如内存合并算法

#### 本地方法栈

为JNI服务，最常见的便是在java源码中所看的被native关键字修饰的方法

#### 虚拟机方法栈

线程私有的，所以**一个线程对应一个虚拟机方法栈**。用于描述方法执行的内存模型。每一个方法从调用到执行完成的过程，就对应者一个栈帧（方法执行时创建）在虚拟机栈中入栈到出栈的过程。

**一个虚拟机栈中有多少个栈帧取决于方法的调用次数**。

##### 栈帧

* 局部变量表（具体参考深入理解jvm第8章）

  * 大小：编译时已知（Code属性的max_locals属性）

  * 静态方法跟非静态方法，区别

    * 非静态方法：slot=0的位置->this指针

  * 存储的是什么？按顺序如下

    * 方法参照
    * 局部变量

  * 注意点：

    * long和double占8个字节，需要两个slot存储。比如一个double类型的数转换成16进制是：11 22 33 44 55 66 77 88，那么可以将前4个字节用int存储，后四个字节用int存储：

      11 22 33 44 -> int；55 66 77 88 -> int

* 操作数栈（jvm的解释执行引擎是基于操作数栈的执行引擎）

  * 大小：编译时已知（Code属性的max_stacks属性）
  * 存储元素：任意java数据类型
    * 32位数据类型所占的栈容量为1
    * 64位数据类型所占的栈容量为2
  * 操作
    * 写入，也就是pop操作
    * 提取，也就是push操作

* 动态连接

  * 间接引用->直接引用，相对于静态解析

* 返回地址

  * 保存现场，恢复现场

* 附加信息

## 创建对象

下面看看使用new关键字创建一个对象，底层需要几步操作

演示代码：

~~~java
public class TestClass {
    public void test1(){
        TestClass t=new TestClass();
    }
}
~~~

通过jclasslib可以得到下面信息：

~~~java
操作数栈最大深度：2
局部变量表最大槽数：2
字节码长度：9
~~~

字节码：

~~~java
0 new #2 <com/cxylk/partfour/TestClass>
3 dup
4 invokespecial #3 <com/cxylk/partfour/TestClass.<init>>
7 astore_1
8 return
~~~

1、执行第一条指令0 new #2 <com/cxylk/partfour/TestClass>

new：创建一个对象，并将其引用值压入栈顶

![](https://z3.ax1x.com/2021/04/20/c75GY6.png)

因为该方法是实例方法，所以局部变量表的第一个slot存放的是this（但是此时this=null）。执行这条指令，会做如下事情：

* 向堆区申请内存（该对象现在是空的，不完全的），构造方法还未执行
* 将对象的内存地址压入栈

2、执行第二条指令dup

dup是duplicate的缩写，也就是复制的意思，含义：复制栈顶元素并将复制值压入栈顶。

![](https://z3.ax1x.com/2021/04/20/c7XoqK.png)

思考：为什么要复制一份到栈顶呢？

因为上面说过了，该方法不是静态方法，所以还需要为局部变量表中的位于索引0的slot中的this赋值，不然就没法玩了。这也是为什么我们可以放心使用this的原因，jvm在底层会给它赋好值。

3、执行第三条指令invokespecial #3 <com/cxylk/partfour/TestClass.<init>>

invokespecial：调用超类的构造方法，实例初始化方法，私有方法。也就是说这条指令会执行<init>方法去初始化。一个对象初始化的过程会做下面几件事

* 构建环境
  * 创建栈帧
  * 传参
  * 保存现场
  * 给this指针赋值
* 执行

执行完后，上图中堆中的不完全对象就变成了完全对象，栈顶元素被弹出赋值给了索引0处slot中的this，此时的this保存的是完整对象的地址

![](https://z3.ax1x.com/2021/04/20/c7jtL6.png)

4、执行第四条指令astore_1

astore_1含义：将操作数栈顶的引用类型值出栈并存放到第2个本地变量slot中。

![](https://z3.ax1x.com/2021/04/20/c7vCOx.png)

5、执行指令return

return指令是方法返回指令之一，它将结束方法并返回操作数栈顶的值给方法调用者。

上面就是执行一条new语句所要执行的字节码指令操作，可以看到，它需要经过这么多步骤，而且这些指令还要经过c++执行，再转换成机器指令让机器执行。。。所以说，创建对象不是一个轻松的事情。更别说后续对创建对象的gc等。