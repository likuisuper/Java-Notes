package cxylk.test.concurrent.unsafe;

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
