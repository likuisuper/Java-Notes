package cxylk.test.concurrent.threadcommunication;

/**
 * @Classname NoInheritableThreadLocal
 * @Description ThreadLocal不支持继承性
 * @Author likui
 * @Date 2020/11/25 22:24
 **/
public class NoInheritableThreadLocal {
    //1.创建线程变量
    private static ThreadLocal<String> threadLocal=new ThreadLocal<>();

    public static void main(String[] args) {
        //2.设置线程变量(注意，set()方法中当前线程在这里为main线程)
        threadLocal.set("hello");

        //创建线程
        //子线程输出线程变量的值
        Thread thread=new Thread(()->{
            //获取的是当前线程(即thread)中threadLocals对应变量的值
            //也就是说子线程获取不到父线程中设置的值
            System.out.println("thread:"+threadLocal.get());//输出null
        });

        thread.start();

        //主线程输出线程变量的值
        System.out.println("main:"+threadLocal.get());//输出hello
    }
}
