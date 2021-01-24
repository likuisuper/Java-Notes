package cxylk.test.concurrent.concurrentutil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @Classname SemaphoreTest
 * @Description 使用信号量来模拟CyclicBarrier的功能，该信号量内部的计数器是递增的
 * @Author likui
 * @Date 2021/1/7 21:54
 **/
public class SemaphoreTest {
    //创建一个信号量实例,初始化信号量的初值为0
    private static volatile Semaphore semaphore=new Semaphore(0,true);

    public static void main(String[] args) throws InterruptedException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        //创建一个核心线程个数为2的线程池
        ExecutorService executorService= Executors.newFixedThreadPool(2);
        //将线程A加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"A task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //将线程B加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"A task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //等待子线程执行任务A完毕，传入2说明调用acquire方法的线程会一直阻塞，直到信号量的计数变为2才会返回
        //当该方法返回后，当前信号量变为0
        semaphore.acquire(2);

        //将线程C加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"B task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //将线程D加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"B task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //等待子线程执行B完毕
        semaphore.acquire(2);

        System.out.println("all task is over");

        //关闭线程池
        executorService.shutdown();
    }
}
