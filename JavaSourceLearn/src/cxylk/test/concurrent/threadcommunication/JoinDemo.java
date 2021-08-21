package cxylk.test.concurrent.threadcommunication;

/**
 * @Classname JoinDemo
 * @Description join()方法的作用就是让当前线程陷入等待状态，等join的这个线程执行完后，再执行当前线程
 *              有时候，主线程创建并启动了子线程，如果子线程中需要进行大量的耗时计算，主线程往往早于
 *              子线程结束之前结束。如果主线程想等待子线程执行完毕后，获得子线程中的处理完的某个数据，
 *              就需要用到join
 * @Author likui
 * @Date 2020/11/23 16:28
 **/
public class JoinDemo {
    static class ThreadA implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("我是子线程，我先睡1s");
                Thread.sleep(1000);
                System.out.println("我是子线程，我睡完了1s");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread=new Thread(new ThreadA());
        thread.start();
//        thread.join();//如果不加Join，下面的输出语句会先执行
        System.out.println("如果不加join方法，我会先被打出来，加了就不一样了");
    }
}
