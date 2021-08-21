package cxylk.test.concurrent.lockprinciple.reentrantlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Classname FairAndUnfairTest
 * @Description 测试公平和非公平锁在获取锁时的区别
 * @Author likui
 * @Date 2020/12/20 21:33
 **/
public class FairAndUnfairTest {
    //公平锁
    private static Lock fairLock = new ReentrantLock2(true);
    //非公平锁
    private static Lock unfairLock = new ReentrantLock2(false);

    //公平锁测试
    public void fair() throws InterruptedException {
        testLock("公平锁",fairLock);
    }

    //非公平锁测试
    public void unfair() throws InterruptedException {
        testLock("非公平锁",unfairLock);
    }

    //启动线程
    private void testLock(String type, Lock lock) throws InterruptedException {
        System.out.println(type);
        for (int i = 0; i < 5; i++) {
            Thread thread=new Thread(new Job(lock)){
                @Override
                public String toString() {
                    return getName();
                }
            };
            thread.setName(""+i);
            thread.start();
        }
        Thread.sleep(11000);
    }

    //重写实现ReentrantLock类是为了重写getQueuedThreads方法，便于观察
    private static class ReentrantLock2 extends ReentrantLock {
        public ReentrantLock2(boolean fair) {
            super(fair);
        }

        /**
         * 返回正在获取等待锁的列表(获取同步队列中的线程)
         *
         * @return
         */
        @Override
        protected Collection<Thread> getQueuedThreads() {
            List<Thread> arrayList = new ArrayList<>(super.getQueuedThreads());
            //因为getQueuedThreads()方法是从后往前遍历添加到集合的，所以需要反转输出
            Collections.reverse(arrayList);
            return arrayList;
        }
    }

    private static class Job implements Runnable {
        private Lock lock;

        public Job(Lock lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            //连续打印两次才能看出效果，一次的话两者没区别
            for (int i = 0; i < 2; i++) {
                lock.lock();
                try{
                    Thread.sleep(1000);
                    //((ReentrantLock2)lock).getQueuedThreads()),这里一定要注意必须这样写
                    //如果这里改成new ReentrantLock2(fair)的话,那么和传进来的lock是不相关的，不会输出结果
                    //父类转换成子类，就可以调用子类的方法
                    System.out.println("Lock by["+ Thread.currentThread().getName()+"]"+
                            ",Waiting buy"+((ReentrantLock2)lock).getQueuedThreads());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }
            }
        }

    }


    public static void main(String[] args) throws InterruptedException {
        FairAndUnfairTest test = new FairAndUnfairTest();
        test.fair();
        test.unfair();
    }
}
