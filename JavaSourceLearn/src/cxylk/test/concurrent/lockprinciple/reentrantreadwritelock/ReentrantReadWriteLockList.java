package cxylk.test.concurrent.lockprinciple.reentrantreadwritelock;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Classname ReentrantReandWriteLockList
 * @Description 使用ReentrantReadWriteLock读写锁实现安全的list,并且与ReentrantLock在性能上对比
 * @Author likui
 * @Date 2020/12/22 23:06
 **/
public class ReentrantReadWriteLockList {
    private ArrayList<Integer> list=new ArrayList<>();
    //独占锁
    private final ReentrantReadWriteLock lock=new ReentrantReadWriteLock();
    //读锁
    private final Lock readLock=lock.readLock();
    //写锁
    private final Lock writeLock=lock.writeLock();

    //添加元素(采用写锁加锁)
    public void add(Integer e){
        writeLock.lock();
        try{
            list.add(e);
        }finally {
            writeLock.unlock();
        }

    }

    //删除元素(写锁加锁)
    public void remove(Integer e){
        writeLock.lock();
        try{
            list.remove(e);
        }finally {
            writeLock.unlock();
        }
    }

    //获取元素(采用读锁加锁)
    public Integer get(int index){
        readLock.lock();
        try{
            return list.get(index);
        }finally {
            readLock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReentrantReadWriteLockList lockList=new ReentrantReadWriteLockList();
        Thread thread1=new Thread(()->{
            for (int i = 0; i < 1000; i++) {
                lockList.add(i);
            }
        },"线程1");

        Thread thread2=new Thread(()->{
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //定义一个集合保存第二个线程获取到的值
            ArrayList<Integer> copyList1=new ArrayList<>();
            for (int i = 0; i < lockList.list.size(); i++) {
                Integer number=lockList.get(i);
                copyList1.add(number);
            }
//            copyList1.forEach(System.out::println);
        },"线程2");

        Thread thread3=new Thread(()->{
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //顶一个一个集合保存第三个线程获取到的值
            ArrayList<Integer> copyList2=new ArrayList<>();
            for (int i = 0; i < lockList.list.size(); i++) {
                Integer number=lockList.get(i);
                copyList2.add(number);
            }
//            copyList2.forEach(System.out::println);
        },"线程3");

        Thread thread4=new Thread(()->{
            ArrayList<Integer> copyList3=new ArrayList<>();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < lockList.list.size(); i++) {
                Integer number=lockList.get(i);
                copyList3.add(number);
            }
        },"线程4");

        long startTime= System.currentTimeMillis();

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        long endTime= System.currentTimeMillis();
        System.out.println("使用ReentrantReadWriteLock耗时:"+(endTime-startTime));
    }
}
