package cxylk.test.concurrent.lockprinciple.aqs;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Classname NonReentrantLock
 * @Description 基于AQS实现的不可重入的独占锁，类似于ReentrantLock
 * @Author likui
 * @Date 2020/12/17 22:09
 **/
public class NonReentrantLock implements Lock, Serializable {
    //同步器的主要使用方式是继承，子类推荐被定义为自定义同步器的静态内部类(继承了同步器)
    static class Sync extends AbstractQueuedSynchronizer {
        //该线程是否正在独占资源
        @Override
        protected boolean isHeldExclusively() {
            return getState()==1;
        }


        /**
         * 独占方式。尝试获取资源,当state为0时尝试获取资源
         * @param arg 要获取的资源个数，独占模式下始终为1
         * @return 成功返回true,否则返回false
         */
        @Override
        protected boolean tryAcquire(int arg) {
            //为什么这里要用CAS?因为是独占模式，保证只有一个线程能获取资源
            if(compareAndSetState(0,1)){
                //设置当前线程为独占线程
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }


        /**
         * 独占方式。尝试释放资源,设置state为0
         * @param arg 要获取的资源个数。独占模式下始终为1
         * @return 成功则返回true，否则返回false
         */
        @Override
        protected boolean tryRelease(int arg) {
            if(getState()==0){
                throw new IllegalMonitorStateException();
            }
            //将当前的独占线程设置为null
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        //提供条件变量接口
        Condition newCondition(){
            return new ConditionObject();
        }
    }

    //创建一个Sync来做具体的工作
    private final Sync sync=new Sync();

    //获取锁
    @Override
    public void lock() {
        sync.acquire(1);
    }

    //尝试非阻塞的获取锁，能获取返回true，否则返回false
    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }


    //释放锁
    @Override
    public void unlock() {
        sync.release(1);
    }

    //可中断地获取锁，该方法会响应中断
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 超时获取锁。当前线程在以下三种情况为返回：
     * 1.当前线程在超时时间内获得了该锁
     * 2.当前线程在超时时间内被中断
     * 3.超时时间结束，返回false
     * @param time
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1,unit.toNanos(time));
    }


    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}
