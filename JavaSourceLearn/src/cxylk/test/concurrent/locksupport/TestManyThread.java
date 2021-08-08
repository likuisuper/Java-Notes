package cxylk.test.concurrent.locksupport;

import java.util.concurrent.locks.LockSupport;

/**
 * @Classname TestManyThread
 * @Description 多个线程的情况下使用park与unpark
 * @Author likui
 * @Date 2020/12/12 15:33
 **/
public class TestManyThread {
    public static void main(String[] args) throws InterruptedException {
        Thread thread1=new Thread(()->{
            System.out.println("child1 thread begin park");
            LockSupport.park();
            System.out.println("child1 thread end park");
        },"Thread1");

//        thread1.start();

        //不能使用join，因为子线程是阻塞挂起的
//        thread1.join();

        //为了在主线程调用unpark方法前让子线程输出并阻塞
        Thread.sleep(1000);


//        System.out.println("main1 thread begin unpark");
        //让thread1持有许可证
        LockSupport.unpark(thread1);


        //================================================
        Thread thread2=new Thread(()->{
            System.out.println("child2 thread begin park");

            //调用park方法，挂起自己，只有被中断才会退出循环
            while (!Thread.currentThread().isInterrupted()){
                LockSupport.park();
            }
            System.out.println("child2 thread unpark");
        });

//        thread2.start();

        Thread.sleep(1000);
//        System.out.println("main thread2 begin unpark");

        //即使调用unpark方法子线程也不会结束，因为中断没有结束
//        LockSupport.unpark(thread2);

        //中断子线程
        thread2.interrupt();


        //========================调用park后中断线程也会返回
        Thread thread3=new Thread(()->{
            System.out.println("child3 thread begin park");
            LockSupport.park();
            //被中断后返回，所以能正常输出
            System.out.println("child3 thread end park");
        });
        thread3.start();
        Thread.sleep(1000);
        System.out.println("main thread3 invoke interrupt");
        thread3.interrupt();

    }
}
