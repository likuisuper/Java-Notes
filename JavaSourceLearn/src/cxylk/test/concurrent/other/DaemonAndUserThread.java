package cxylk.test.concurrent.other;

/**
 * @Classname DaemonAndUserThread
 * @Description Java中的线程分为两类，分别是daemon线程(守护线程)和user线程(用户线程)。
 *              两者的区别：当最后一个非守护线程结束时，JVM会正常退出，而不管当前是否有守护线程，
 *              也就是说，守护线程是否结束并不影响JVM的退出。言外之意，只要有一个用户线程还没结束，
 *              正常情况下，JVM就不会退出。main线程运行结束后，JVM会自动启动一个叫做DestroyJavaVM
 *              的线程，该线程会等待所有的用户线程结束后终止JVM进程。
 * @Author likui
 * @Date 2020/11/25 20:44
 **/
public class DaemonAndUserThread {
    public static void main(String[] args) {
        Thread thread=new Thread(()->{
            for(;;){}
        });

        //启动子线程
//        thread.start();
        //main线程以及结束了，但是JVM并没有退出，run窗口右边的方块还是红的，
        // 通过jps命令也能看出JVM进程并没有退出(还存在DaemonAndUserThread)
        //因为thread线程中执行了一个死循环，而他是一个用户线程
//        System.out.println("main thread is over");

        //将thread设置为守护线程
        //那么main()函数就是唯一的用户进程了，当main线程运行结束后，
        //JVM发现当前已经没有用户线程了，就会终止JVM进程。由于这里的守护线程执行的任务是一个
        //死循环，这也说明了如果当前进程中不存在用户线程，但是还存在正在执行任务的守护线程，则JVM不等
        //守护线程运行完毕就会结束JVM进程
        thread.setDaemon(true);
        thread.start();
        //JVM进程以及终止了，无论是输出结果，还是通过jps命令，或者任务管理器
        System.out.println("main thread is over");
    }
}
