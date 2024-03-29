package cxylk.test.concurrent.concurrentutil;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @Classname CountDownLatchDemo
 * @Description 使用CountDownLatch模拟游戏的加载
 * @Author likui
 * @Date 2021/1/6 21:30
 **/
public class CountDownLatchDemo {
    //定义前置任务线程
    static class PreTaskThread implements Runnable{
        private String task;
        private CountDownLatch countDownLatch;

        public PreTaskThread(String task,CountDownLatch countDownLatch){
            this.task=task;
            this.countDownLatch=countDownLatch;
        }

        @Override
        public void run() {
            try {
                Random random=new Random();
                Thread.sleep(random.nextInt(1000));
                System.out.println(task+"-任务完成");
                countDownLatch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        //假设有三个模块需要加载
        CountDownLatch countDownLatch=new CountDownLatch(3);

        //主任务
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("等待数据加载...");
                    System.out.println(String.format("还有%d个前置任务",countDownLatch.getCount()));
                    //等待子线程执行完毕
                    countDownLatch.await();
                    //数据加载完成，正式开始游戏
                    System.out.println("数据加载完成，正式开始游戏!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //前置任务
        new Thread(new PreTaskThread("加载地图数据",countDownLatch)).start();
        new Thread(new PreTaskThread("加载人物模型",countDownLatch)).start();
        new Thread(new PreTaskThread("加载背景音乐",countDownLatch)).start();
    }
}
