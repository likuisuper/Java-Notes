## JVM中对象的分配

首先来看下jvm中对关于对象分配的注释（src/share/vm/memory/allocation.hpp）：

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

翻译过来的意思：虚拟机中的所有类都是以下分配内存类的子类，这些类是：

对于在资源区分配的对象：ResourceObj

**对于在C堆中分配的对象：CHeapObj**

对于分配在堆栈中的对象：StackObj

对于嵌入对象：ValueObj

对于用作命名空间的类：AllStatic

**对于元空间中的类（类数据）：MetaspaceObj**

## Klass模型

Java的每个类，在JVM中都有一个对应的Klass类实例与之相对应，它是用C++实现的，用来存储类的元信息，比如常量池、属性信息、方法信息等。

下面是klass模型类的继承结构：

![](https://z3.ax1x.com/2021/04/11/cwxMTA.png)

从上图的继承关系可知，**元信息是存储在元空间的**，也就是JDK8中方法区的实现。

#### MetaspaceObj

对应的c++代码在`src/share/vm/memory/allocation.hpp`中：

~~~cpp
// Base class for objects stored in Metaspace.
// Calling delete will result in fatal error.
//
// Do not inherit from something with a vptr because this class does
// not introduce one.  This class is used to allocate both shared read-only
// and shared read-write classes.
//

class MetaspaceObj {...}
~~~

注释已经明确说明了**它是存储在元空间中的对象的基类**

#### Metadata

对应的c++代码在`src/share/vm/oops/metadata.hpp`中：

~~~cpp
// This is the base class for an internal Class related metadata
class Metadata : public MetaspaceObj {...}
~~~

注释明确说明了**它是内部class相关元数据的基类**

#### Klass

对应的c++代码在`src/share/vm/oops/klass.hpp`中：

~~~cpp
// A Klass provides:
//  1: language level class object (method dictionary etc.)
//  2: provide vm dispatch behavior for the object
// Both functions are combined into one C++ class.
class Klass : public Metadata {...}
~~~

从注释中可知，这个类提供语言级别的类对象，并且为该对象提供虚拟机的调度行为，它**对应的是c++中的类对象**。

这里要注意klass内存布局中的虚表指针

~~~cpp
// One reason for the oop/klass dichotomy in the implementation is
// that we don't want a C++ vtbl pointer in every object.  Thus,
// normal oops don't have any virtual functions.  Instead, they
// forward all "virtual" functions to their klass, which does have
// a vtbl and does the C++ dispatch depending on the object's
// actual type.  (See oop.inline.hpp for some of the forwarding code.)
// ALL FUNCTIONS IMPLEMENTING THIS DISPATCH ARE PREFIXED WITH "oop_"!

//  Klass layout:
//    [C++ vtbl ptr  ] (contained in Metadata)
//    ...
~~~

通过注释可知，为什么jvm将klass和oop（java中的对象在jvm中对应的c++对象）分开，这样就不用为每个对象都维护一个VPTR，所以普通的oops对象没有任何的虚函数，它们将所有的虚函数转发给对应的Klass对象，而Klass对象中有一个VTPR，它根据对象的实际类型进行C++调度。**虚表转发是多态的底层实现**，关于这部分内容，在java多态中已经详细分析过。

#### instanceKlass

**java类在jvm中的表示，它包含在执行运行时类所需的所有信息**

它有三个子类：

* InstanceMirroKlass:用于表示java.lang.Class，java代码中获取到的class对象（反射的原理），实际上就是这个C++的实例，**它存值在堆区**，学名镜像类

  源码中是这样解释这个类的：

  ~~~
  InstanceMirrorKlass是一个专门的InstanceKlass，用于
  java.lang.Class实例的专门实例类。 这些实例是特殊的，因为
  它们除了包含类的静态字段外，还包括
  类的正常字段。 这意味着它们是尺寸可变的
  实例，需要特殊的逻辑来计算它们的大小和
  迭代它们的OOPS。
  ~~~

* instanceRefKlass:用于表示java/lang/ref/Reference类的子类（Reference是抽象类）

  源码中对该类的解释：

  ~~~
  InstanceRefKlass是一个专门用于Java类的InstanceKlass，是java/lang/ref/Reference的子类。
  
  这些类被用来实现软oft/weak/final/phantom引用和终结，并且需要垃圾收集器的特殊处理。
  
  在GC过程中，被发现的引用对象被添加（链接）到下面四个列表中的一个，这取决于引用的类型。链接是通过java/lang/ref/Reference类中的下一个字段发生的。
  
  之后，被发现的引用按照可达性的递减顺序进行处理。符合通知条件的引用对象被链接到类java/lang/ref/Reference中的静态pending_list，同一类中的pending list锁对象被通知。
  ~~~

* instanceClassLoaderKlass:用于遍历某个加载器加载的类

在这里要注意：为什么有了instanceKlass后，还需要instanceMirroKlass呢？类的元信息我们是可以通过反射来获取的，实际上我们更需要的是这个class对象，分开的目的就是防止绕过一些安全检查。

#### ArrayKlass

所有数组类的抽象基类。

java中的数组不是静态数据类型，它不像String，有java.lang.String对应。它是动态的数据类型，也即是运行期生成的，Java数组的元信息用ArrayKlass的子类来表示：

* TypeArrayKlass:用于表示基本类型(8种)的数组
* ObjArrayKlass:用于表示引用类型的数组

#### HSDB工具

我们使用hsdb工具来查看一个JAVA类对应的C++类，来验证上面的内容

~~~java
public class Hello {
    public static final int a=3;

    public static int b=5;

    public static String c="sdf";

    public static int d;

    public final int e=0;
    
    public static void main(String[] args) {
        int[] intArr=new int[1];
        Hello[] hellos=new Hello[1];

        Hello hello=new Hello();

        Class<Hello> helloClass = Hello.class;

        System.out.println("hello");

        while (true);
    }
}
~~~

运行上述代码，然后进入jdk的lib目录下，执行

~~~powershell
java -cp .\sa-jdi.jar sun.jvm.hotspot.HSDB
~~~

然后使用jps -l命令查看运行的java程序的pid，然后在HSDB中使用attach连接该id即可。

1、查看非数组类。两种方式

* 类向导：ClassBrowser

  通过类向导找到该类对应的地址，然后点击inspector，输入

  ![](https://z3.ax1x.com/2021/04/11/c09jr8.png)

* 对象

  点击main线程，点击堆栈信息，将对象的地址输入到inspector中

  ![](https://z3.ax1x.com/2021/08/11/fUKuBd.png)

  可以看到，上面的结果是Oop，也就是对象模型，每一个类所对应的对象，这个后面再说。

  另外，在上图中也可以看到栈的结构，最下面对应的就是方法参数，然后往上依次是我们在代码中定义的int数组、对象数组、对象、class对象...
  
  我们可以查看该对象所对应类的Class对象，也就是instanceMirrorKlass：
  
  ![](https://z3.ax1x.com/2021/08/11/fUeELR.png)

这里会发现上面没有e字段，因为e没有被static修饰，所以在类对象中看不到，但在上面对象所对应的oop模型中可以看到

2、查看数组。

​	在上面说过，数组是一种动态类型，所以通过类向导是找不到的，只有通过堆栈	这种方式。

![](https://z3.ax1x.com/2021/04/11/c0i8ED.png)

​	可以看到，基本数据类型对应的typeArrayKlass，其中I代表的是描述符，这里是Int类型，所以对应的是I。而引用类型对应的是ObjArrayKlass。

## Oop模型

前面说过klass模型是java类的元数据在jvm中的存在形式，而oop模型就是java对象在jvm中的存在形式。

![](https://z3.ax1x.com/2021/04/24/cvQhhq.png)

比如有下面这段代码：

~~~java
public class OopModel {
    public static void main(String[] args) {
        //对象
        OopModel model=new OopModel();

        //数组
        int[] ints=new int[19];

        //对象数组。为什么要指定数组长度？如果数组长度不确定，将无法通过元数据中的信息推断出数组的大小
        OopModel[] models=new OopModel[5];

        while (true);
    }
}
~~~

使用HSDB工具查看，可以看到它们分别对应的klass为：InstanceKlass，TypeArrayKlass，ObjArrayKlass。我们可以查看源码来了解它们的存在形式。

#### 对象类的顶级基类

oopDesc是对象类的顶级基类

源码：hotspot/src/share/vm/oops/oop.hpp

~~~cpp
// oopDesc is the top baseclass for objects classes.  The {name}Desc classes describe
// the format of Java objects so the fields can be accessed from C++.
// oopDesc is abstract.
// (see oopHierarchy for complete oop class hierarchy)
//
// no virtual functions allowed
...
class oopDesc {
    ...
}
~~~

翻译过来就是这个意思：

~~~
// oopDesc是对象类的顶级基类。 {name}Desc类描述了
// Java对象的格式，因此可以从C++中访问这些字段。
// oopDesc是抽象的。
// (完整的op类层次结构见opHierarchy)
//
// 不允许使用虚拟函数
~~~

在前面讲Klass模型时候说过，将klass和oop模型分开来，就不用在每个对象中维护一个VPTR，所以普通的对象不允许有虚函数

#### java类的实例（非数组对象）

实例是用instanceOopDesc来描述的

源码：hotspot/src/share/vm/oops/instanceOop.hpp

~~~cpp
// An instanceOop is an instance of a Java Class
// Evaluating "new HashTable()" will create an instanceOop.

class instanceOopDesc : public oopDesc {
    ...
}
~~~

#### 对象头

使用markOopDesc描述，该头文件中的注释对它描述的很清楚，比如锁信息，分代年龄等，有必要仔细读一下。

源码：hotspot/src/share/vm/oops/markOop.hpp

~~~cpp
class markOopDesc: public oopDesc {
 private:
  // Conversion
  uintptr_t value() const { return (uintptr_t) this; }
  ...
}
~~~

#### 数组对象

源码：arrayOop.hpp

~~~cpp
// 数组Oops的布局是。
//
// markOop
// Klass* // 32位，如果压缩，但在LP64中声明为64位。
// length // 共享klass内存或在声明的字段后分配。
class arrayOopDesc : public oopDesc {..}
~~~

分为基本数据类型的数组对象和引用类型的数组对象

##### 基本数据类型

通过TypeArrayDescOop来描述

源码：hotspot/src/share/vm/oops/typeArrayOop.hpp

~~~cpp
// A typeArrayOop is an array containing basic types (non oop elements).
// It is used for arrays of {characters, singles, doubles, bytes, shorts, integers, longs}
#include <limits.h>
class typeArrayOopDesc : public arrayOopDesc {
    ...
}
~~~

##### 引用数据类型

通过ObjArrayOopDesc描述

源码：hotspot/src/share/vm/oops/objArrayOop.hpp

~~~cpp
// An objArrayOop is an array containing oops.
// Evaluating "String arg[10]" will create an objArrayOop.

class objArrayOopDesc : public arrayOopDesc {
    ...
}
~~~

