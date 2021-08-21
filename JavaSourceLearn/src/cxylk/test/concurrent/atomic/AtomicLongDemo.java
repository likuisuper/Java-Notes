package cxylk.test.concurrent.atomic;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @Classname AtomicLongDemo
 * @Description 通过一个多线程统计0的个数加深对AtomicLong原子变量操作类的理解
 * @Author likui
 * @Date 2020/12/6 21:37
 **/
public class AtomicLongDemo {
    //创建Long型原子计数器
    private static AtomicLong atomicLong=new AtomicLong();
    //创建数组存放数据
    private static Integer[] arrayOne=new Integer[]{0,1,2,3,0,5,6,0,56,0};
    private static Integer[] arrayTwo=new Integer[]{10,1,2,3,0,5,6,0,45,0};

    public static void main(String[] args) throws InterruptedException {
        //线程one统计arrayOne中0的个数
        Thread threadOne=new Thread(()->{
            int size=arrayOne.length;
            for (int i = 0; i < size; i++) {
                if(arrayOne[i].intValue() ==0){
                    //自增后的值，从名字上也可以看出，先increment，再get
                    atomicLong.incrementAndGet();
                }
            }
        },"线程one");

        //线程two统计arrayTwo中0的个数
        Thread threadTwo=new Thread(()->{
            int size=arrayTwo.length;
            for (int i = 0; i < size; i++) {
                if(arrayTwo[i].intValue()==0){
                    atomicLong.incrementAndGet();
                }
            }
        },"线程two");

        //启动子线程
        threadOne.start();
        threadTwo.start();

        //等待线程执行完毕,这里一定要加上这步，防止main线程不等子线程执行完就返回，0的个数将是0
        threadOne.join();
        threadTwo.join();

        System.out.println("0的个数:"+atomicLong.get());
    }
}
