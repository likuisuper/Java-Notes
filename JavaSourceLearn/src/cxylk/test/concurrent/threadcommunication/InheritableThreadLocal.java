package cxylk.test.concurrent.threadcommunication;

/**
 * @Classname InheritableThreadLocal
 * @Description 支持继承的ThreadLocal,它继承自ThreadLocal,提供了一个特性，
 *              就是让子线程可以访问到父线程中设置的本地变量
 * @Author likui
 * @Date 2020/11/25 22:35
 **/
public class InheritableThreadLocal {
    //1.创建线程变量
    //只需将ThreadLocal改成InheritableThreadLocal即可
    private static ThreadLocal<String> threadLocal=new java.lang.InheritableThreadLocal<>();

    public static void main(String[] args) {
        //2.设置线程变量(注意，set()方法中当前线程在这里为main线程)
        threadLocal.set("hello");

        //创建线程
        //子线程输出线程变量的值
        Thread thread=new Thread(()->{
            //获取的是当前线程(即thread)中threadLocals对应变量的值
            //现在子线程可以访问到父线程中设置的本地变量值
            System.out.println("thread:"+threadLocal.get());//输出hello
        });

        thread.start();

        //主线程输出线程变量的值
        System.out.println("main:"+threadLocal.get());//输出hello
    }
}
