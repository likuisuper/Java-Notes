package cxylk.test.concurrent.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Classname TestUnSafe
 * @Description UnSafe类使用，源码在jdk/src/classes/sun/misc/Unsafe
 * @Author likui
 * @Date 2020/12/1 21:17
 **/
public class TestUnSafe {
    //获取UnSafe的实例,当前环境下报错
    //由于UnSafe类是rt.jar包提供的，rt.jar包里面的类是使用Bootstrap类加载器加载的
    //而我们启动的main函数所在的类是使用AppClassLoader加载的，所以在main函数里面加载Unsafe类时，
    //根据委托机制，会委托给Bootstrap去加载UnSafe类
    //这样做的目的是不让开发人员在正规渠道使用Unsafe类，而是在rt.jar包里面的核心类中使用
//    static final Unsafe unsafe=Unsafe.getUnsafe();

    //使用反射获取
    static final Unsafe unsafe;

    //记录变量state在类TestUnSafe中的偏移值
    static final long stateOffset;

    //变量
    private volatile long state=0;

    static {

        try {
            System.out.println("静态代码块,只会初始化一次");
            //使用反射获取Unsafe的成员变量theUnsafe
            Field field=Unsafe.class.getDeclaredField("theUnsafe");
            System.out.println(field);

            //设置为可访问
            field.setAccessible(true);

            //获取该变量的值,
            // get(object) 返回指定对象object上此Field表示的字段的值
            //如果该字段不是静态字段的话，要传入反射类的对象，穿null会包NPE异常
            //如果字段是静态字段的话，传入任何对象都可以，包括null
            unsafe= (Unsafe) field.get(null);

            //获取state变量在TestUnSafe中的偏移值
            stateOffset=unsafe.objectFieldOffset(TestUnSafe.class.getDeclaredField("state"));
            System.out.println(stateOffset);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            throw new Error(e);
        }
    }

    public static void main(String[] args) {
        //创建实例，并且设置state值为1
        TestUnSafe testUnSafe=new TestUnSafe();
//        TestUnSafe testUnSafe1=new TestUnSafe();
        Boolean success=unsafe.compareAndSwapInt(testUnSafe,stateOffset,0,1);
//        Boolean success2=unsafe.compareAndSwapInt(testUnSafe1,stateOffset,0,1);
        // 报错 java.lang.SecurityException: Unsafe
        //	at sun.misc.Unsafe.getUnsafe(Unsafe.java:90)
        //设置为反射获取实例后正常运行
        System.out.println(success);//输出true
//        System.out.println(success2);
    }
}
