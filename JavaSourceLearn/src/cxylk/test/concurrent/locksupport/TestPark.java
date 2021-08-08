package cxylk.test.concurrent.locksupport;

import java.util.concurrent.locks.LockSupport;

/**
 * @Classname TestPark
 * @Description park(Object blocker)方法,当线程调用parker方法阻塞挂起时，
 *              这个blocker对象会被记录到线程内部，JDK推荐使用该方法，有助于分析阻塞原因
 * @Author likui
 * @Date 2020/12/12 16:54
 **/
public class TestPark {
    public void testPark(){
        //无法知道阻塞原因
//        LockSupport.park();

        //使用park(Object blocker),其中blocker被设置为this
        //使用jstack pid输出-
        // parking to wait for  <0x000000076e384e98> (a com.cxylk.thread.locksupport.TestPark)
        LockSupport.park(this);
    }

    public static void main(String[] args) {
        TestPark testPark=new TestPark();
        testPark.testPark();
    }
}
