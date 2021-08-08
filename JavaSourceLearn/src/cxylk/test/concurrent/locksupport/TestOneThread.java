package cxylk.test.concurrent.locksupport;

import java.util.concurrent.locks.LockSupport;

/**
 * @Classname TestPark
 * @Description 在只有一个线程的情况下使用park()方法与unpark()方法
 * @Author likui
 * @Date 2020/12/12 14:17
 **/
public class TestOneThread {
//    public static void main1(String[] args) {
//        System.out.println("begin park");
//        //默认情况下子线程没有持有许可证，调用park方法将会阻塞挂起
//        LockSupport.park();
//        System.out.println("end park");//不会输出
//    }

    //在只有main线程运行的情况下先park再unpark
    public static void main2(String[] args) {
        System.out.println("begin park");
        //当前线程(main线程调用park后被阻塞)
        LockSupport.park();
        //main线程都已经被阻塞了怎么唤醒自己呢？所以这种情况是不行的
        LockSupport.unpark(Thread.currentThread());
        //因为当前线程阻塞所以不会输出
        System.out.println("end park");
    }

    //在只有main线程的情况下先unpark再park
    public static void main3(String[] args) {
        //先调用unpark方法拿到许可
        LockSupport.unpark(Thread.currentThread());
        System.out.println("begin park");
        LockSupport.park();
        //可以输出
        System.out.println("end park");
    }

    public static void main(String[] args) {
        //多次调用unpark方法
        LockSupport.unpark(Thread.currentThread());
        LockSupport.unpark(Thread.currentThread());
        LockSupport.unpark(Thread.currentThread());
        System.out.println("begin park");
        LockSupport.park();
        //可以输出
        System.out.println("end park");
        LockSupport.park();
        //不能输出，多次调用unpark方法并不会让许可累加
        System.out.println("again park");
    }
}
