package cxylk.test.concurrent.threadcommunication;

/**
 * @Classname ThreadLocalTest
 * @Description ThreadLocal提供了线程本地变量，也就是说你如果创建了一个ThreadLocal变量，
 *              那么访问这个变量的每个线程都会有这个变量的一个本地副本。当线程操作这个变量时，
 *              实际上时操作自己本地内存里面的变量。
 * @Author likui
 * @Date 2020/11/25 21:21
 **/
public class ThreadLocalTest {
    //创建一个打印函数
    static void print(String str){
        //1-1打印当前线程本地内存中localVariable的值(通过get方法取值)
        System.out.println(str+":"+localVariable.get());

        //1-2清除当前线程本地内存中的localVariable变量
        localVariable.remove();
    }

    //创建一个ThreadLocal变量
    public static ThreadLocal<String> localVariable=new ThreadLocal<>();

    public static void main(String[] args) {
        //创建线程One
        Thread threadOne=new Thread(()->{
            //设置线程One中本地变量localVariable的值
            localVariable.set("threadOne local variable");
            //调用打印函数
            print("threadOne");
            //打印本地变量值
            //清除本次内存中的变量后，获取到的是null值
            System.out.println("threadOne remove after"+":"+localVariable.get());
        });

        //创建线程two
        Thread threadTwo=new Thread(()->{
            //设置线程Two中本地变量localVariable的值
            localVariable.set("threadTwo local variable");
            //调用打印函数
            print("threadTwo");
            //打印本地变量值
            //清除本地内存中的变量后，获取到的是null值
            System.out.println("threadTwo remove after"+":"+localVariable.get());
        });

        threadOne.start();

        threadTwo.start();
    }
}
