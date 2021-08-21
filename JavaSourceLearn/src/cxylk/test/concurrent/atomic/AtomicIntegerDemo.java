package cxylk.test.concurrent.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Classname AtomicIntegerDemo
 * @Description TODO
 * @Author likui
 * @Date 2020/12/6 22:42
 **/
public class AtomicIntegerDemo {
    private static final Unsafe unsafe;

    private static final long stateOffset;

    private volatile long state=1;

    private static AtomicInteger integer=new AtomicInteger();

    static {
        Field field= null;
        try {
            field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe= (Unsafe) field.get(null);
            stateOffset=unsafe.objectFieldOffset(AtomicIntegerDemo.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void main(String[] args) {
//        System.out.println(integer.incrementAndGet());
        //这里调用到的Unsafe中getAndAddInt方法中的原始值是AtomicInteger中的value
//        System.out.println("返回旧值"+integer.getAndIncrement());//输出0,先get再increment
//        System.out.println("获取当前值"+integer.get());//这时候的值就是加1后的值

        System.out.println("========================");

        System.out.println("返回新值"+integer.incrementAndGet());//返回加1后的值
        System.out.println("获取当前值"+integer.get());//这时候获取的值就是返回的值
        //这里传过去的原始值就是该类中定义的state
        int result=get(new AtomicIntegerDemo(),stateOffset,1);
        System.out.println(result);
    }

    /**
     * 实现Unsafe类中的getAndAddInt方法，获取对象obj中偏移量为offset的变量对应volatile语义的当前值，
     * 并设置变量值为原始值+addValue
     * @param obj 所要操作的对象
     * @param offset 在对象中的偏移地址
     * @param addValue 增量
     * @return 注意，这里返回的是当前变量的值，不是原始值+addValue
     */
    public static int get(Object obj, long offset, int addValue){
        int var5;
        do{
            //在该类中，这里获取的就是state值，所以，state初始值大小决定了这里的变量值
            //也就是说，这里的变量值就是原始变量值
            var5=unsafe.getIntVolatile(obj,offset);
        }while (!unsafe.compareAndSwapInt(obj,offset,var5,addValue+var5));
        return var5;
    }
}
