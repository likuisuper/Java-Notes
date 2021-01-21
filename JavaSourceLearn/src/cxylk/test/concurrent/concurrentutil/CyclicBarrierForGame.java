package cxylk.test.concurrent.concurrentutil;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @Classname CyclicBarrierForGame
 * @Description 使用CycleBarrier模拟游戏关卡的加载
 * @Author likui
 * @Date 2021/1/7 20:17
 **/
public class CyclicBarrierForGame {
    static class PreTaskThread implements Runnable{
        private String task;
        private CyclicBarrier cyclicBarrier;

        public PreTaskThread(String task,CyclicBarrier cyclicBarrier){
            this.task=task;
            this.cyclicBarrier=cyclicBarrier;
        }

        /**
         * 执行的任务
         */
        @Override
        public void run() {
            //假设共三个模块
            for (int i = 1; i < 4; i++) {
                try {
                    Random random = new Random();
                    Thread.sleep(random.nextInt(1000));
                    System.out.println(String.format("关卡%d的任务%s完成",i,task));
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
                cyclicBarrier.reset();
            }
        }
    }

    public static void main(String[] args) {
        //第一个参数为计数器的初始值，第二个参数Runnable是当前计数器值为0时需要执行的任务
        CyclicBarrier cyclicBarrier=new CyclicBarrier(3,()->
            System.out.println("本关卡所有前置任务完成，开始游戏..."));

        //如果注释掉一个线程，则主线程和子线程会永远等待，因为没有第三个线程去执行await方法,
        //即没有第三个线程达到屏障，所以之前到达屏障的两个线程都不会执行
        new Thread(new PreTaskThread("加载地图数据",cyclicBarrier)).start();
        new Thread(new PreTaskThread("加载人物模型",cyclicBarrier)).start();
        new Thread(new PreTaskThread("加载背景音乐",cyclicBarrier)).start();
    }
}
