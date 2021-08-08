package cxylk.test.concurrent.locksupport;

import java.util.concurrent.locks.LockSupport;

/**
 * @Classname FIFOMutex
 * @Description 使用park和unpark方法交替打印数字和字母
 * @Author likui
 * @Date 2020/12/13 18:58
 **/
public class NumLetter {
    //声明两个静态线程，因为要在线程内部调用unpark方法唤醒其他线程
    private static Thread threadOne, threadTwo;

    public static void main(String[] args) {
        threadOne = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                //先输出数字
                System.out.println(Thread.currentThread().getName() + ":" + i);
                //输出完之后唤醒被挂起的线程two
                LockSupport.unpark(threadTwo);
                //然后将当前线程挂起,一定要先unpark再park
                LockSupport.park();
            }
        }, "线程one");

        threadTwo = new Thread(() -> {
            for (char i = 'A'; i <= 'C'; i++) {
                //先调用当前线程的park方法，目的是让线程one先输出
                LockSupport.park();
                //输出字母
                System.out.println(Thread.currentThread().getName() + ":" + i);
                //然后唤醒被挂起的线程One，让其继续输出数字
                LockSupport.unpark(threadOne);
            }

        }, "线程two");

        threadOne.start();

        threadTwo.start();

    }
}
