package cxylk.test.concurrent.threadinterrupted;

/**
 * @Classname Interrupted
 * @Description boolean interrupted()方法：检测当前线程是否被中断，如果是返回true，
 *              否则返回false。与isInterrupted()不同的是，该方法如果发现当前线程被中断，
 *              则会清楚中断标志，注意：在interrupted()内部是获取当前调用线程的中断标志
 *              而不是调用interrupted()方法的实例对象的中断标志
 * @Author likui
 * @Date 2020/11/24 22:51
 **/
public class Interrupted {
    public static void main(String[] args) throws InterruptedException {
        Thread threadOne=new Thread(()->{
            for(;;){

            }
        });

        //启动线程
        threadOne.start();

        //设置中断标志
        //注意：这里的意思是当前线程(这里是main线程)调用了threadOnd的interrupt()方法
        //设置threadOne的中断标志为true
        threadOne.interrupt();

        //获取中断标志
        System.out.println("isInterrupted:"+threadOne.isInterrupted());//true

        //获取中断标志并重置
        //对象也能调用静态方法
        System.out.println("isInterrupted:"+threadOne.interrupted());//false,因为当前线程即main线程没有被中断

        //获取中断标志并重置
        //注意：interrupted()是静态方法
        System.out.println("isInterrupted:"+ Thread.interrupted());//输出为false,因为当前线程即main线程没有被中断
        //threadOne.interrupted()和Thread.interrupted()方法的作用是一样的，都是获取当前线程的中断标志

        //获取中断标志
        System.out.println("isInterrupted:"+threadOne.isInterrupted());//任然为true

        //等待threadOnd执行完毕，因为在threadOne中执行的是死循环，所以下面的输出语句不会打印出来
//        threadOne.join();

        System.out.println("main thread is over");
    }
}
