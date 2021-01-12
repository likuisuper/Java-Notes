## Unsafe 功能

### 为什么叫unsafe?

因为它提供了可以直接操作内存的功能，所以它是不安全的

### 提供两个功能：

* 绕过JVM直接修改内存(对象)
* 使用硬件CPU指令实现CAS原子操作

源码位置：hotspot/src/share/vm/prims/unsafe.cpp

也可以在在线网址上找到：http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file

### 获取Unsafe实例

先来看一下构造函数

~~~java
public final class Unsafe {
        private Unsafe() {}
}
~~~

可以看到构造函数是private的，也就是说不能通过构造函数获取该类实例

但是Unsafe提供了getUnsafe（）方法

~~~java
    //类变量
	private static final Unsafe theUnsafe;

    @CallerSensitive
    public static Unsafe getUnsafe() {
        //反射获取调用当前方法的类
        Class var0 = Reflection.getCallerClass();
        //如果是不是当前系统域的类加载器(Unsafe类是rt.jar包提供的，而rt.jar包是Bootstrap加载的，所以这里的类加载器就是Bootstrap)加载该类
        if (!VM.isSystemDomainLoader(var0.getClassLoader())) {
            //如果不是Bootstrap加载的，将会抛出异常
            throw new SecurityException("Unsafe");
        } else {
            //如果是的话，返回类变量
            return theUnsafe;
        }
    }
~~~

其中@CallerSensitive注解表示调用这个方法是很敏感的。如果我们在自己编写的类中调用这个方法的话是会抛出"Unsafe"异常的，因为我们启动main函数所在的类是使用AppClassLoader加载的，所以在main函数里面加载Unsafe类时，根据委托机制，会委托给Bootstrap去加载Unsafel类。如果没有这个限制，那么我们的应用程序就可以随意使用Unsafe做事情，而Unsafe类可以直接操作内存，是不安全的。目的就是不让开发人员在正规渠道使用Unsafe类，而是在rt.jar包里面的核心类中使用。

那如果真的要实例化Unsafe怎么办呢?用万能的反射

~~~java
//使用反射获取Unsafe类的成员变量theUnsafe
//getDeclaredField方法用来获取该类中声明的字段，不管是public还是private的
Field field=Unsafe.class.getDeclaredField("theUnsafe");
//将该字段设置为可访问，因为它是private的
field.setAccessible(true);
//获取该字段的值
unsafe=(Unsafe)field.get(null);
~~~

注意field.get()方法，如果要获取的字段是static类型的，传入任何对象都是可以的，包括null,

如果该字段不是静态字段的话，要传入反射类的对象，传null的话会报空指针异常。

为什么呢?很好理解，因为类变量属于类，不与实例关联，而实例变量与实例共生死，它依赖与实例。

### 获取对象字段的偏移地址

首先，为什么要获取字段的偏移地址呢？

一个java对象可以看成是一段内存，各个字段都得按照一定的顺序放在这段内存里，同时考虑到对齐要求，可能这些字段不是连续放置的。这里有必要了解对象的内存布局(具体内容在深入理解JVM中的51页)。对象可以分为三分部分，对象头，实例数据，对齐填充。其中在实例数据部分中，HotSpot虚拟机将字段的默认分配顺序设为longs/doubles、ints、shorts/chars、bytes/booleans、oops(普通对象指针)。

当我们获取到该字段的偏移地址后，再加上对象的起始地址(也就是内存地址),那么我们就可以通过Unsafe直接获取字段的值了。

获取方式根据字段是实例变量还是静态变量分为两种

一、获取实例变量相对与对象起始内存地址的地址偏移量

二、获取静态变量的地址偏移量

~~~java
package com.cxylk.thread.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Classname VO
 * @Description 测试Unsafe中的方法
 * @Author likui
 * @Date 2020/12/5 16:50
 **/
public class VO {
    public int a=0;

    public int a1=0;

    public long b=0;

    public Double aDouble=0d;

    public String ss="string";

    public static String c="123";

    public static Object d=null;

    public static int e=100;

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Field field=Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe= (Unsafe) field.get(null);//因为theSafe是静态字段，所有可以传入null值
        System.out.println(unsafe);//该字段的内存地址
        //----------------获取实例字段的内存偏移地址--------------
        System.out.println("a的偏移地址:"+unsafe.objectFieldOffset(VO.class.getDeclaredField("a")));
        System.out.println("a1的偏移地址:"+unsafe.objectFieldOffset(VO.class.getDeclaredField("a1")));
        System.out.println("b的偏移地址:"+unsafe.objectFieldOffset(VO.class.getDeclaredField("b")));
        System.out.println("aDouble的偏移地址:"+unsafe.objectFieldOffset(VO.class.getDeclaredField("aDouble")));
        System.out.println("ss的偏移地址:"+unsafe.objectFieldOffset(VO.class.getDeclaredField("ss")));
        //不能获取静态字段
//        System.out.println("c的偏移地址:"+unsafe.objectFieldOffset(VO.class.getDeclaredField("c")));
        //不能获取静态字段
//        System.out.println("d的偏移地址:"+unsafe.objectFieldOffset(VO.class.getDeclaredField("d")));

        //--------------------获取静态字段的偏移地址
        System.out.println("c的偏移地址:"+unsafe.staticFieldOffset(VO.class.getDeclaredField("c")));
        System.out.println("d的偏移地址:"+unsafe.staticFieldOffset(VO.class.getDeclaredField("d")));
        System.out.println("e的偏移地址:"+unsafe.staticFieldOffset(VO.class.getDeclaredField("e")));
        //获取静态字段的起始地址(内存地址),类型不是Long,而是Object,等价于unsafe.staticFieldBase(VO.class)
        //输出class com.cxylk.thread.unsafe.VO
        System.out.println("e的起始地址:"+unsafe.staticFieldBase(VO.class.getDeclaredField("e")));

        //获取操作系统的位数 返回4代表32位，返回8代表64位
        System.out.println(unsafe.addressSize());

        //有了偏移地址，再加上对象的起始地址，就能通过Unsafe获取到字段的值了

        //获取实例字段的值
        VO vo=new VO();
        vo.a=100;
        //获取偏移量
        long offset=unsafe.objectFieldOffset(VO.class.getDeclaredField("a"));
        //通过对象和偏移地址获取到值
        int value=unsafe.getInt(vo,offset);
        System.out.println(value);

        //获取静态字段的属性值
        VO.e=1000;
        //静态字段的起始地址，返回object，为什么呢?因为静态字段属性类的，而所有类都是Object的子类
        //这也是为什么静态字段多了一个staticFieldBase的方法，因为它不依赖于对象
        Object staticOffset=unsafe.staticFieldBase(VO.class.getDeclaredField("e"));
        //静态字段的偏移地址
        long offset2=unsafe.staticFieldOffset(VO.class.getDeclaredField("e"));
        System.out.println(unsafe.getInt(staticOffset, offset2));
    }
}

~~~

