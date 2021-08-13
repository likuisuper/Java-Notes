## Class类文件结构

将.java文件编译后得到的.class文件就是字节码文件。它是一组以8个字节为基础单位的二进制流。格式采用类似于C语言结构体的伪结构来存储数据。这种伪结构中只有两种数据类型：**”无符号数“**，**”表“**。

* 无符号数：以u1、u2、u4、u8来分别代表1个字节、2个字节、4个字节、8个字节的无符号数
* 表：由多个无符号数或者其他表作为数据项构成的复合数据类型，为了便于区分，所有表的命名都以"_info"结尾。

将下面的.java文件编译后得到class文件，然后通过idea插件BinEd-Binary/Hexadecimal显示的结果

~~~java
public class ClassFileDemo {
    int[] v9;//数组类型 [I 二维数组就是[[I 后面类推
    String[] v10;//引用类型 [LJava/lang/String; 最后面有一个分号用来表示结束 只有引用类型有分号
    //(LJava/lang/String;)V
    public static void main(String[] args) {
        int c;
        System.out.println("hello word");
    }
}
~~~

![](https://z3.ax1x.com/2021/04/18/cIW61A.png)

class文件格式如下：

![](https://z3.ax1x.com/2021/04/18/cIfPj1.png)

#### 魔数

class文件的前4个字节，固定值：0xCAFEBABE

#### 版本号

紧跟魔数后面的4个字节，前2个字节是次版本号，后两个字节是主版本号。上面图一中的0x00000034，次版本号转换为10进制是0，主版本号转换为10进制是52

#### 常量池

紧跟着版本号后面的是常量池入口。常量池存放的是**字面量**和**符号引用**。字面量接近于java语言中被final修饰的常量、文本字符串，符号引用如类和接口的全限定名、字段的名称和描述符、方法的名称和描述符等，其实就是对应常量池的索引，通过索引可以得到Class_info，Utf-8_info等常量的值。常量池包含两部分，如下图所示

![](https://z3.ax1x.com/2021/04/18/cIfq8H.png)

* 常量池容量计数器，两个字节：**需要注意的是，这个容量计数是从1开始而不是从0开始，第0项常量表示**`不引用任何一个常量池项目`。上面图一中，常量池容量为0X0025，转换为10进制后是37，也就是有36项常量，索引为1~36

* 常量池数据区：由constant_pool_count-1个cp_info结构组成，一个cp_info对应一个常量。如下图所示：具体可以参考《Java虚拟机规范》

  ![](https://z3.ax1x.com/2021/04/18/cIhdiD.png)

以上面字节码为例：

| tag  | class_index | name_and_type_index |
| ---- | ----------- | ------------------- |
| 0x0A | 0x0006      | 0x0017              |
| 10   | 6           | 23                  |

通过tag=10可以知道该常量是CONSTANT_Methodref_info，class_index必须是对常量池的有效索引，常量池表在该索引处的项必须是CONSTANT_Class_info，这个常量表示一个类或接口，当前字段或方法是这个类或接口的成员。常量池表在索引name_and_type_index处的项必须是CONSTANT_NameAndType_info常量，该常量表示当前字段和方法的名称和描述。

其他的常量的结构不再分析，可以通过javap -verbose class文件名查看：

~~~java
Constant pool:
   #1 = Methodref          #6.#23         // java/lang/Object."<init>":()V
   #2 = Fieldref           #24.#25        // java/lang/System.out:Ljava/io/PrintStream;
   #3 = String             #26            // hello word
   #4 = Methodref          #27.#28        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #5 = Class              #29            // com/cxylk/partthree/ClassFileDemo
   #6 = Class              #30            // java/lang/Object
   #7 = Utf8               v9
   #8 = Utf8               [I
   #9 = Utf8               v10
  #10 = Utf8               [Ljava/lang/String;
  #11 = Utf8               <init>
  #12 = Utf8               ()V
  #13 = Utf8               Code
  #14 = Utf8               LineNumberTable
  #15 = Utf8               LocalVariableTable
  #16 = Utf8               this
  #17 = Utf8               Lcom/cxylk/partthree/ClassFileDemo;
  #18 = Utf8               main
  #19 = Utf8               ([Ljava/lang/String;)V
  #20 = Utf8               args
  #21 = Utf8               SourceFile
  #22 = Utf8               ClassFileDemo.java
  #23 = NameAndType        #11:#12        // "<init>":()V
  #24 = Class              #31            // java/lang/System
  #25 = NameAndType        #32:#33        // out:Ljava/io/PrintStream;
  #26 = Utf8               hello word
  #27 = Class              #34            // java/io/PrintStream
  #28 = NameAndType        #35:#36        // println:(Ljava/lang/String;)V
  #29 = Utf8               com/cxylk/partthree/ClassFileDemo
  #30 = Utf8               java/lang/Object
  #31 = Utf8               java/lang/System
  #32 = Utf8               out
  #33 = Utf8               Ljava/io/PrintStream;
  #34 = Utf8               java/io/PrintStream
  #35 = Utf8               println
  #36 = Utf8               (Ljava/lang/String;)V
~~~

#### 访问标志

常量池结束后，紧跟的2个字节是访问标志（access_flags）。具体如下：

![](https://z3.ax1x.com/2021/04/18/cI4amq.png)

需要注意的是，**没有使用到的标志位要求一律为零**。上面字节码的访问标志为0x0021，怎么得到的？首先，有public修饰符，值是0x0001，又因为它使用了JDK1.2之后的编译器进行编译，因此它的ACC_SUPER应该为真，值为0x0020，两者做或运算，也就是0x0001|0x0020=0x0021

#### 类索引

确定当前类的全限定名，是一个u2类型的数据。它指向一个类型为CONSTANT_Class_info的类描述符常量，通过该常量中的索引name_index可以找到CONSTANT_Utf8_info类型的常量中的全限定名字符串。比如上面字节码中，类索引是0x0005，它指向索引为5的class_info常量，该常量的name_index指向29这个utf8_info这个常量。

#### 父类索引

确定这个类的父类的全限定名，也是一个u2类型的数据。因为Java不支持多继承，所以父类索引只有1个，并且除了Object之外，所有的java类都有父类，也就是父类索引不为0。它指向的值和类索引一样。

#### 接口索引集合

一组u2类型的数据的集合。包含2部分：

* 接口计数器（interface_count），u2类型的数据：如果该类没有实现任何接口，那么值为0，后面接口的索引表不再占用任何字节
* 实现的接口（interfaces[]）

#### 字段表集合

字段表用于描述接口或者类中声明的类变量以及实例变量，但是不包括方法内部声明的局部变量。由2个部分组成：

![](https://z3.ax1x.com/2021/04/18/cIIHeI.png)

以上面的字节码为例：

| 0002                  | 0000                                    | 0007                       | 0008                       | 0000            |
| --------------------- | --------------------------------------- | -------------------------- | -------------------------- | --------------- |
| 字段表计数器，2个字段 | 访问标志，因为没有使用到标志位，所以为0 | 字段名称，#7，查常量表得v9 | 字段描述，#8，查常量表为[I | 字段属性个数为0 |

上面涉及到了字段描述符的概念，根据《Java虚拟机规范》，解释如下：

![](https://z3.ax1x.com/2021/04/18/cITp9O.png)

比如下面代码：

~~~java
public class ClassFileDemo {
    //字段描述符
    byte v1;//B
    short v2;//S
    int v3;//I
    long v4;//注意是J
    char v5;//C
    float v6;//F
    double v7;//D
    boolean v8;//Z
    int[] v9;//数组类型 [I 二维数组就是[[I 后面类推
    String[] v10;//引用类型 [LJava/lang/String; 最后面有一个分号用来表示结束 只有引用类型有分号

    //方法描述符
    (LJava/lang/String;)V
    public static void main(String[] args) {
        int c;
        System.out.println("hello word");
    }

    (LJava/lang/String;[II)J
    long test(String a,int[] b,int c){return 0;}
}
~~~

#### 方法表

方法表的内容和字段表类似，也是由两部分构成

![](https://z3.ax1x.com/2021/04/18/cITbPf.png)

修饰符这里，字段和方法还是有些区别的，在虚拟机规范给出的方法修饰符标志：

![](https://z3.ax1x.com/2021/04/18/cI7EM4.png)

还是以上面的字节码为例：

| 0002        | 0001                   | 000B                                  | 000C                             | 0001    | 000D                                          |
| ----------- | ---------------------- | ------------------------------------- | -------------------------------- | ------- | --------------------------------------------- |
| 方法个数为2 | 由上图可知01表示public | 方面名称，索引是#11，对应值为`<init>` | 方法描述，索引是#12，对应值为()V | 1个属性 | attribute_name_index，索引为#13，对应值为Code |

可以看到上面显示有两个方法，是哪两个方法呢？默认构造函数，也就是`<init>`，还有main函数。而方法中的代码则存放在方法属性表集合中一个名为“Code”的属性里面。

方法表中比较复杂的是方法属性表这一部分。

#### 属性表集合

每一个属性表的结构都是不一样的，但是它们都应该满足下面所定义的结构：

| 类型 | 名称                 | 数量            |
| ---- | -------------------- | --------------- |
| u2   | attribute_name_index | 1               |
| u4   | attribute_length     | 1               |
| u1   | info                 | attribut_length |

Class文件、字段表、方法表都可以携带自己的属性表集合。通过javap -verbose查看上面的字节码

~~~java
  public com.cxylk.partthree.ClassFileDemo(); //方法名
    descriptor: ()V							  //方法描述符
    flags: ACC_PUBLIC						  //访问标志
    Code:									  //Code开始
      stack=1, locals=1, args_size=1		  //最大栈深度，局部变量表大小，参数列表size
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 9: 0
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0       5     0  this   Lcom/cxylk/partthree/ClassFileDemo;

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=2, args_size=1
         0: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
         3: ldc           #3                  // String hello word
         5: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
         8: return
      LineNumberTable:						  //行号表，将上面的操作码和.java中的行号相对应。
        line 25: 0							  //前面是.java中的行号，后面是上面的操作码
        line 26: 8
      LocalVariableTable:					  //本地变量表，描述栈帧中局部变量表的变量与java源码中定义的变量之间的关系
        Start  Length  Slot  Name   Signature
            0       9     0  args   [Ljava/lang/String;
}

~~~

有几个地方需要注意：第一个方法是默认构造方法，但是并没有参数，那为什么args_size=1呢？因为在java语言中，**任何实例方法里面，都可以通过“this”关键字来访问到此方法所属的对象**。实现机制就是：通过在java编译器编译的时候把对this关键字的访问转变为对一个普通方法参数的访问，然后在虚拟机调用实例方法时自动传入此参数。**因此在实例方法的局部变量表中至少会存在一个指向当前对象实例的局部变量，局部变量表中也会预留出`第一个变量槽位`来存放对象实例的引用，所以实例方法参数值从1开始**。这里我们说的是实例方法，再看下面的main方法，它是一个静态方法，可以发现，它的slot为0，但是并不是this，而是args参数。

Code属性表中包含了很多属性，以上面字节码为例来看下常见的属性：

| attribute_name_index | attribute_length | max_stack        | max_locals             | Code_length          | code           |
| -------------------- | ---------------- | ---------------- | ---------------------- | -------------------- | -------------- |
| 0x000D               | 0x0000002F       | 0x0001           | 0x0001                 | 0x00000005           | 2A 87 00 01 B1 |
| 固定值="Code"        | 属性长度         | 操作数栈最大深度 | 局部变量表所需存储空间 | 存储生成的字节码指令 | 字节码指令     |

其中code属性是最重要的一个属性，每一个字节都对应一条指令。每条指令代表的含义可以参考《Java虚拟机规范》。