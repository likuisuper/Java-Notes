package cxylk.test.concurrent.threadinterrupted;

/**
 * @Classname ThreadIsInterrupted
 * @Description 检测当前线程是否被中断
 * @Author likui
 * @Date 2020/11/24 21:15
 **/
public class ThreadIsInterrupted {
    public static void main(String[] args) throws InterruptedException {
        Thread thread=new Thread(()->{
            //如果当前线程被中断就退出循环
            while (!Thread.currentThread().isInterrupted()){
                System.out.println(Thread.currentThread()+"hell0");
            }
        });
        //启动子线程
        thread.start();

        //主线程休眠1s，以便中断前让子线程输出
        Thread.sleep(1000);

        //中断子线程
        System.out.println("main thread interrupt thread");
        thread.interrupt();

        //等待子线程执行完毕
        thread.join();

        System.out.println("main is over");
    }
}
