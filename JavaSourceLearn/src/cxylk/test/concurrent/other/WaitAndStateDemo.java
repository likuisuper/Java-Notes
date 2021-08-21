package cxylk.test.concurrent.other;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * @Classname WaitDemo
 * @Description 当前调用共享变量的wait()方法后只会释放当前共享变量上的锁，如果当前线程
 *              还持有其他共享变量的锁，则这些锁是不会释放的。线程的状态。
 * @Author likui
 * @Date 2020/11/22 21:26
 **/
public class WaitAndStateDemo {
    //创建资源
    private static volatile Object resourceA=new Object();
    private static volatile Object resourceB=new Object();

    public static void main(String[] args) throws InterruptedException {
        //创建线程A
        Thread threadA=new Thread(()->{
            try {
                //获取resourceA共享资源的监视器锁
                synchronized (resourceA) {
                    System.out.println("threadA get resourceA lock");
                    //获取resourceB共享资源的监视器锁
                    synchronized (resourceB) {
                        System.out.println("threadA get resourceB lock");

                        //调用wait()方法必须事先获取该对象的监视器锁，否则将会
                        //抛出IllegalMonitorStateException异常
                        //线程A阻塞，并释放获取到的resourceA的锁
                        System.out.println("threadA release resourceA lock");
                        resourceA.wait();
                    }
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        },"threadA");

        //查看当前线程A的状态
        System.out.println(threadA.getState());//显示为新建(NEW)

        //创建线程B
        Thread threadB=new Thread(()->{
            try {
                //休眠1s,为了让A线程先获取到resourceA和resourceB上的锁
                Thread.sleep(1000);

                //获取resourceA共享资源的监视器锁
                synchronized (resourceA){
                    System.out.println("threadB get resourceA lock");

                    System.out.println("threadB try get resourceB lock...");
                    //获取resourceB共享资源的监视器锁
                    //线程A只是释放了resourceA的监视器锁，并没有释放resourceB的监视器锁
                    //查看此时线程A的状态
                    System.out.println("线程A调用wait方法后的状态->"+threadA.getState());//WAITING
                    synchronized (resourceB) {
                        System.out.println("threadB get resourceB lock");

                        //线程B阻塞，并释放获取到的resourceA的锁
                        System.out.println("threadB release resourceA lock");
                        //唤醒A线程后，当A线程执行完后，B线程变能获取到resourceB的监视器锁
                        //通知A线程不用等了，可以往下执行了
//                        resourceB.notify();
                        resourceA.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"threadB");


        //查看线程B的状态
        System.out.println(threadB.getState());//显示为新建(NEW)

//        //获取Java线程管理MXBean
//        ThreadMXBean threadMXBean=ManagementFactory.getThreadMXBean();
//        //不需要获取同步的monitor和synchronizer 信息，仅获取线程和线程堆栈信息
//        ThreadInfo[] threadInfos=threadMXBean.dumpAllThreads(true,true);
//        for (ThreadInfo threadInfo : threadInfos) {
//            System.out.println("["+threadInfo.getThreadId()+"]"+threadInfo.getThreadName());
//        }

        threadA.start();

        //再次调用线程A的start方法会抛出java.lang.IllegalThreadStateException异常
        //因为此时的threadStatus已经不等于0了
//        threadA.start();//第二次调用

        //join方法让当前线程陷入等待状态(这里是让main线程等待)，等join的这个线程执行完毕后(即threadA执行完毕)，
        //再继续执行当前线程
        //注意，当调用该方法时，sleep和wait方法要传入具体的时间，否则会一直等待下去
//        threadA.join();
//        System.out.println("调用join方法后，此时线程A的状态变为TERMINATED(即终止状态)->"+threadA.getState());

        threadB.start();

        //调用start方法后再查看线程此时的状态
        //RUNNABLE表示当前线程正在运行中，处于RUNNABLE状态的线程运行在JAVA虚拟机中，也有可能等待CPU分配资源
        //JAVA线程的RUNNABLE状态其实包含了传统操作系统的ready和running两个状态的
        System.out.println("调用start后的线程A状态->"+threadA.getState());//此时的状态为RUNNABLE
        System.out.println("调用start后的线程状B态->"+threadB.getState());//此时的状态为RUNNABLE

//        threadA.join();
//        threadB.join();

        //获取Java线程管理MXBean
        ThreadMXBean threadMXBean=ManagementFactory.getThreadMXBean();
        //不需要获取同步的monitor和synchronizer 信息，仅获取线程和线程堆栈信息
        ThreadInfo[] threadInfos=threadMXBean.dumpAllThreads(false,false);
        for (ThreadInfo threadInfo : threadInfos) {
            System.out.println("["+threadInfo.getThreadId()+"]"+threadInfo.getThreadName());
        }
    }
}
