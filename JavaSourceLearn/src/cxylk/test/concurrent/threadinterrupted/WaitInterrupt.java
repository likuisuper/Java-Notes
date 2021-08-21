package cxylk.test.concurrent.threadinterrupted;

/**
 * @Classname WaitInterupt
 * @Description 当一个线程调用对象的wait()方法被阻塞挂起后，如果其他线程中断了
 *              该线程，则该线程会抛出InterruptedException异常并返回
 * @Author likui
 * @Date 2020/11/23 22:58
 **/
public class WaitInterrupt {
    private static Object obj=new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread threadA=new Thread(()->{

            synchronized(obj){
                try {
                    System.out.println("---begin---");
                    //阻塞当前线程
                    obj.wait();
                    System.out.println("--end---");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        threadA.start();

        Thread.sleep(1000);

        System.out.println("--begin interrupt threadA--");
        threadA.interrupt();
        System.out.println("--end interrupt threadA--");
    }

}
