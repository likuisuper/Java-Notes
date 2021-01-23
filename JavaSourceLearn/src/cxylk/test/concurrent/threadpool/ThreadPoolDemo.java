package cxylk.test.concurrent.threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Classname ThreadPoolDemo
 * @Description 使用线程池创建线程
 * @Author likui
 * @Date 2021/1/21 21:44
 **/
public class ThreadPoolDemo {
    //核心线程数
    private static final int CORE_POOL_SIZE=5;
    //线程池最大线程数量
    private static final int MAXIMUM_POOL_SIZE=10;
    //空闲线程(非核心线程)的存活时间
    private static final long KEEP_ALIVE_TIME=1L;
    
    public static void main(String[] args) {
        //使用ArrayBlockingQueue作为阻塞队列，拒接策略为callerRunsPolicy,即使用调用者所在的线程来运行任务
        ThreadPoolExecutor poolExecutor=new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.CallerRunsPolicy());
        for (int i = 0; i < 10; i++) {
            Runnable runnable=new MyRunnable(""+i);
            poolExecutor.execute(runnable);
        }
        //终止线程池
        poolExecutor.shutdown();
        //如果线程池没有终止就死循环知道终止
        while (!poolExecutor.isTerminated()){

        }
        System.out.println("all threads is finished");
    }

}
