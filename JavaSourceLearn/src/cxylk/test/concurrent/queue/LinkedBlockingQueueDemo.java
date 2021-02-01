package cxylk.test.concurrent.queue;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Classname LinkedBlockingQueueDemo
 * @Description TODO
 * @Author likui
 * @Date 2021/1/31 22:59
 **/
public class LinkedBlockingQueueDemo {
    //初始化一个队列，指定容量为5
    private static LinkedBlockingQueue<String> linkedBlockingQueue=new LinkedBlockingQueue<>(5);

    public static void main(String[] args) throws InterruptedException {
        linkedBlockingQueue.put("song");
        linkedBlockingQueue.put("i");
        linkedBlockingQueue.put("love");
        linkedBlockingQueue.put("you");
        linkedBlockingQueue.put("much");
        Thread thread1=new Thread(()->{
            try {
                String take = linkedBlockingQueue.take();
                System.out.println(take);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"thread1");
        Thread thread2=new Thread(()->{
            try {
                String take = linkedBlockingQueue.take();
                System.out.println(take);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"thread2");

        thread1.start();
        thread2.start();
    }
}
