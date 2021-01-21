package cxylk.test.concurrent.concurrentutil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Classname SimpleCountDownLatchTest
 * @Description CountDownLatch允许一个线程或多个线程等待其他线程完成操作
 * @Author likui
 * @Date 2021/1/5 22:54
 **/
public class SimpleCountDownLatchTest {
    //创建一个CountDownLatch实例,这里假设开启两个线程
    private static CountDownLatch countDownLatch=new CountDownLatch(2);

    public static void main(String[] args) throws InterruptedException {
        //创建线程池
        ExecutorService executorService= Executors.newFixedThreadPool(2);
        //将线程A添加到线程池中
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    System.out.println("child threadOne over!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    countDownLatch.countDown();
                }
            }
        });
        //将线程B添加到线程池中
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    System.out.println("child threadTwo over!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    countDownLatch.countDown();
                }
            }
        });

        System.out.println("wait all child thread over!");
        //等待子线程执行完毕，返回
        countDownLatch.await();
        System.out.println("all child thread over!");
        //关闭线程池
        executorService.shutdown();
    }
}
