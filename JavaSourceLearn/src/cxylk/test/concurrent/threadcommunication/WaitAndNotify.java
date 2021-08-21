package cxylk.test.concurrent.threadcommunication;

/**
 * @Classname WaitAndNotify
 * @Description 等待/通知机制，基于Object的wait()方法和notify()、notifyAll()方法，完成进程间通信，轮流打印
 *              需要注意的时等待/通知机制使用的是同一个对象锁，如果两个线程使用的是不同的对象锁，那他们之间是
 *              不能用等待/通知机制的
 * @Author likui
 * @Date 2020/11/23 15:25
 **/
public class WaitAndNotify {
    private static Object lock=new Object();

    static class ThreadA implements Runnable {

        @Override
        public void run() {
            synchronized (lock){
                for (int i = 0; i < 5; i++) {
                    try {
                        System.out.println("ThreadA:"+i);
                        //先通知，再等待
                        lock.notify();
                        lock.wait();
//                        Thread.sleep(1000);//sleep方法先打印A线程，再打印B线程
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                lock.notify();
            }
        }
    }

    static class ThreadB implements Runnable {

        @Override
        public void run() {
            synchronized (lock){
                for (int i = 0; i < 5; i++) {
                    try {
                        System.out.println("ThreadB:"+i);
                        //先通知，再等待
                        lock.notify();
                        lock.wait();
//                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                lock.notify();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(new ThreadA()).start();
        Thread.sleep(1000);
        new Thread(new ThreadB()).start();
    }
}
