package cxylk.test.concurrent.threadcommunication;

/**
 * @Classname SemaphoreDemo
 * @Description 信号量机制，这里是基于volatile关键字的自己实现的信号量通信
 * @Author likui
 * @Date 2020/11/24 20:45
 **/
public class SemaphoreDemo {
    private static volatile int signal=0;

    static class ThreadA implements Runnable {

        @Override
        public void run() {
            while(signal<5){
                if(signal%2==0){
                    System.out.println("threadA:"+signal);
                    signal++;
                }
            }
        }
    }

    static class ThreadB implements Runnable {

        @Override
        public void run() {
            while (signal<5){
                if(signal%2==1){
                    System.out.println("threadB:"+signal);
                    signal=signal+1;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(new ThreadA()).start();
        Thread.sleep(1000);
        new Thread(new ThreadB()).start();
    }
}
