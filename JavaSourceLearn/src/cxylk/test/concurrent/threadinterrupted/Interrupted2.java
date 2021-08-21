package cxylk.test.concurrent.threadinterrupted;

/**
 * @Classname Interrupted2
 * @Description 修改Interrupted中的代码如下
 * @Author likui
 * @Date 2020/11/24 23:16
 **/
public class Interrupted2 {
    public static void main(String[] args) throws InterruptedException {
        Thread threadOne=new Thread(()->{
            //中断标志为true时会推出循环，并且清楚中断标志
            while (!Thread.currentThread().interrupted()){

            }
            //输出结果为false,即调用interrupted()方法后中断标志被清除了
            System.out.println("threadOne isInterrupted:"+ Thread.currentThread().isInterrupted());
        });

        //启动线程
        threadOne.start();

        //设置中断标志
        threadOne.interrupt();

        //等待threadOne执行完成，while()循环有退出条件，所有下面的输出语句会输出
        threadOne.join();

        System.out.println("main thread is over");
    }
}
