package cxylk.test.concurrent.lockprinciple.reentrantlock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Classname ReentrantLockList
 * @Description 使用ReentrantLock实现一个简单的线程安全的list
 * @Author likui
 * @Date 2020/12/20 20:47
 **/
public class ReentrantLockList {
    //线程不安全的list
    private ArrayList<Integer> arrayList = new ArrayList<>();
    //独占锁
    private volatile ReentrantLock lock = new ReentrantLock();

    //添加元素
    public void add(Integer e) {
        lock.lock();
        try {
            arrayList.add(e);
        } finally {
            lock.unlock();
        }
    }

    //删除元素
    public void remove(Integer e) {
        lock.lock();
        try {
            arrayList.remove(e);
        } finally {
            lock.unlock();
        }
    }

    //获取数据
    public Integer get(int index) {
        lock.lock();
        try {
            return arrayList.get(index);
        }finally {
            lock.unlock();
        }

    }

    public static void main(String[] args) throws InterruptedException {
        ReentrantLockList lockList = new ReentrantLockList();
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                lockList.add(i);
            }
        }, "线程1");

        Thread thread2 = new Thread(() -> {
            ArrayList<Integer> copyList=new ArrayList<>();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < lockList.arrayList.size(); i++) {
                Integer number=lockList.get(i);
                copyList.add(number);
            }
//            copyList.forEach(System.out::println);
        }, "线程2");

        Thread thread3=new Thread(()->{
            List<Integer> copyList2=new ArrayList<>();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < lockList.arrayList.size(); i++) {
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
            for (int i = 0; i < lockList.arrayList.size(); i++) {
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
        System.out.println("耗时:"+(endTime-startTime));

    }
}
