package cxylk.test.concurrent.threadpool;

import java.util.Date;

/**
 * @Classname MyRunnable
 * @Description TODO
 * @Author likui
 * @Date 2021/1/21 21:44
 **/
public class MyRunnable implements Runnable{
    private String task;

    public MyRunnable(String task){
        this.task=task;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName()+":开始执行任务"+task+" time="+new Date());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName()+":执行任务完毕"+" time="+new Date());
    }
}
