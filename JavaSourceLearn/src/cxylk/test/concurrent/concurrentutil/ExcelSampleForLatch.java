package cxylk.test.concurrent.concurrentutil;

import java.util.concurrent.CountDownLatch;

/**
 * @Classname ExcelSampleForLatch
 * @Description 假设现在有一个需求，解析一个excel中的多个sheet数据，每个线程解析一个sheet里的数据
 *              等到所以sheet都解析完以后。即实现主线程等待所以线程完成sheet的解析操作。使用join也
 *              可以，但是使用CountDownLatch更灵活
 * @Author likui
 * @Date 2021/1/5 23:19
 **/
public class ExcelSampleForLatch {
    //创建CountDownLatch实例
    private static CountDownLatch countDownLatch=new CountDownLatch(2);

    public static void main(String[] args) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    System.out.println(1);
                    countDownLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    System.out.println(2);
                    countDownLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        countDownLatch.await();
        System.out.println(3);
    }
}
