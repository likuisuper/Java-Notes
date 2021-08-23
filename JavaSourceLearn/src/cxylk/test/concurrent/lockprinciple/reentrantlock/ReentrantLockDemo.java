package cxylk.test.concurrent.lockprinciple.reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @Classname ReentrantLockDemo
 * @Description TODO
 * @Author likui
 * @Date 2021/8/23 14:53
 **/
public class ReentrantLockDemo {
    private static final ReentrantLock lock=new ReentrantLock();

    private static int a;

    public static void increase(int n){
        a+=n;
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread1=new Thread(()->{
            lock.lock();
            try {
                for (int i = 0; i < 1000; i++) {
                    increase(1);
                }
            }finally {
                lock.unlock();
            }
        },"线程1");

        Thread thread2=new Thread(()->{
            lock.lock();
            try {
                for (int i = 0; i < 1000; i++) {
                    increase(2);
                }
            }finally {
                lock.unlock();
            }

        },"线程2");

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println("当前a的值："+a);
    }
}
