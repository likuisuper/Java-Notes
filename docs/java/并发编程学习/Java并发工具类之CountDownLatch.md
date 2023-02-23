## CountDownLatch

从这个名字也可以看出，CountDown代表计数递减，Latch是“门闩”的意思。假设某个线程在执行任务之前，需要等待其它线程完成一些前置任务，必须等所有的前置任务都完成，才能开始执行本线程的任务。

#### 类图

![](https://s3.ax1x.com/2021/01/22/sIpWPH.png)

可以看到它有一个内部类Sync继承了AbstractQueuedSynchronizer类，我们看下该类中都有哪些字段和方法

~~~java
    /**
     * Synchronization control For CountDownLatch.
     * Uses AQS state to represent count.使用AQS状态来表示计数
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        //通过构造函数来设置state值
        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }
~~~

下面通过一个具体例子来讲解

~~~java
package cxylk.test.concurrent.concurrentutil;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @Classname CountDownLatchDemo
 * @Description 使用CountDownLatch模拟游戏的加载
 * @Author likui
 * @Date 2021/1/6 21:30
 **/
public class CountDownLatchDemo {
    //定义前置任务线程
    static class PreTaskThread implements Runnable{
        private String task;
        private CountDownLatch countDownLatch;

        public PreTaskThread(String task,CountDownLatch countDownLatch){
            this.task=task;
            this.countDownLatch=countDownLatch;
        }

        @Override
        public void run() {
            try {
                Random random=new Random();
                Thread.sleep(random.nextInt(1000));
                System.out.println(task+"-任务完成");
                countDownLatch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        //假设有三个模块需要加载
        CountDownLatch countDownLatch=new CountDownLatch(3);

        //主任务
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("等待数据加载...");
                    System.out.println(String.format("还有%d个前置任务",countDownLatch.getCount()));
                    //等待子线程执行完毕
                    countDownLatch.await();
                    //数据加载完成，正式开始游戏
                    System.out.println("数据加载完成，正式开始游戏!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //前置任务
        new Thread(new PreTaskThread("加载地图数据",countDownLatch)).start();
        new Thread(new PreTaskThread("加载人物模型",countDownLatch)).start();
        new Thread(new PreTaskThread("加载背景音乐",countDownLatch)).start();
    }
}
~~~

这个例子模拟的是游戏正式开始之前需要完成的前置加载，比如加载地图数据、加载人物模型、加载背景音乐等。

在构造函数中，我们传入了3表示这里需要加载三个模块，从这里也可以看出count的含义：**假如我们我们需要等待N个点完成，那么构造函数中就传入N，也可以理解为子线程的数量。**而count值底层其实就是AQS中的state值。

经过构造函数设置后，state=3，此时执行任务“加载地图数据”

~~~java
new Thread(new PreTaskThread("加载地图数据",countDownLatch)).start();
~~~

这里说下怎么进入主任务中的run方法，首先F7进入start()方法，由于里面真正启动线程的是start0()方法，而该方法是native方法，无法进入，但是它的底层会调用run方法，如下

~~~java
    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }
~~~

我们只需要在target.run()方法这里打个断点就好了，target是Runnable类型的，就会跑到实现Runnable接口中run方法的具体实现，也就是我们的主任务中。

执行这条语句

~~~java
System.out.println(String.format("还有%d个前置任务",countDownLatch.getCount()));
~~~

进入getCount方法，该方法会调用Sync的getCount()方法，而Sync的getCount()方法会调用AQS中的getState获取state的值，前面说了此时的state就是构造函数传过去的值，也就是3.

下面进入CountDownLatch的两个重要方法之一awit()方法

#### void awit()方法

该下该方法的实现

~~~java
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
~~~

可以看到，底层还是调用了AQS的acquireSharedInterruptibly方法

~~~java
public final void acquireSharedInterruptibly(int arg)
    throws InterruptedException {
    //如果该线程被中断，返回
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
~~~

AQS用到了模板设计模式，一些方法交给了具体的子类来实现，比如tryAcquireShared(arg)，看看CountDownLatch中的内部类Sync实现的tryAcquireShared(arg)方法

~~~java
protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;//state=0就返回1，否则返回-1
}
~~~

该方法实现很简单，并没有用到传进来的acquires。就是判断state的值是否等于0，如果不等于0，那么会调用doAcquireSharedInterruptibly(arg)方法让当前线程阻塞。

很明显，当我们第一次进来的时候该方法返回-1，所以该线程会被阻塞。

那么什么情况awit会返回呢？有下面两种情况

**1.当所有线程都调用了CountDownLatch对象的countDown方法后，也就是计数器的值为0时**

**2.其他线程调用了当前线程的interrupt()方法中断了当前线程，当前线程就会抛出InterruptedException异常**

#### boolean awit (long timeout,Time unit)方法

和awit()方法不同的是，该方法在下面这种情况也会返回：

**设置的timeout时间到了，因为超时而返回false**

#### countDown()方法

该方法委托sync调用了AQS的releaseShared(int arg)方法，然后会调用sync实现的tryReleseShared方法，代码如下

~~~java
protected boolean tryReleaseShared(int releases) {
    // Decrement count; signal when transition to zero
    for (;;) {//循环进行cas,直到当前线程成功完成cas使state减1并更新到state
        int c = getState();//获取当前的计数值(state)
        if (c == 0)//等于0直接返回，防止出现负数
            return false;
        int nextc = c-1;//将计数值-1
        if (compareAndSetState(c, nextc))//更新state值
            return nextc == 0;//减1后如果==0返回true
    }
}
~~~

如果该方法返回true，说明是最后一个线程调用countDown方法，那么该线程还需要唤醒因调用awit方法而被阻塞的线程，具体是调用AQS的doReleaseShared()，该方法底层调用了unpark()方法。

#### 总结

CountDownLatch相比join更加灵活和方便。它使用AQS来实现，**使用AQS的状态变量来存放计数器的值**。首先在实例化CountDownLatch时设置状态值，当多个线程调用countdown方法时实际是原子性递减AQS的状态值。而当线程调用awit方法时会被放入AQS的阻塞队列等待计数器为0。直到最后一个线程将计数器变为0后唤醒由于调用了awit方法而被阻塞的线程。