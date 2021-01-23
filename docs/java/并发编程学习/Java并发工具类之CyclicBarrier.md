## 回环屏障CyclicBarrier

类图：

![](https://s3.ax1x.com/2021/01/23/s75tXQ.png)

#### 观察类图可知CyclicBarrier内部是采用lock+condition实现的

我们知道CountDownLatch可以在主线程中开启多个线程去并行执行任务，但是它有一个缺点：**CountDownLatch的计数器是一次性的，也就算等到计数器值变为0后，再调用await和countdown方法都会立刻返回，达不到同步效果**

还有以游戏加载为例，假设现在每个关卡都要加载三个前置任务，如果使用CountDownLatch显然不太合适，需要为每个关卡都创建一个实例。所以可以使用CyclicBarrier来实现。代码如下：

~~~java
/**
 * @Classname CyclicBarrierForGame
 * @Description 使用CycleBarrier模拟游戏关卡的加载
 * @Author likui
 * @Date 2021/1/7 20:17
 **/
public class CyclicBarrierForGame {
    static class PreTaskThread implements Runnable{
        private String task;
        private CyclicBarrier cyclicBarrier;

        public PreTaskThread(String task,CyclicBarrier cyclicBarrier){
            this.task=task;
            this.cyclicBarrier=cyclicBarrier;
        }

        /**
         * 执行的任务
         */
        @Override
        public void run() {
            //假设共三个模块
            for (int i = 1; i < 4; i++) {
                try {
                    Random random = new Random();
                    Thread.sleep(random.nextInt(1000));
                    System.out.println(String.format("关卡%d的任务%s完成",i,task));
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
                //cyclicBarrier.reset();会导致broken异常
            }
        }
    }

    public static void main(String[] args) {
        //第一个参数为计数器的初始值，第二个参数Runnable是当前计数器值为0时需要执行的任务
        CyclicBarrier cyclicBarrier=new CyclicBarrier(3,()->
            System.out.println("本关卡所有前置任务完成，开始游戏..."));

        //如果注释掉一个线程，则主线程和子线程会永远等待，因为没有第三个线程去执行await方法,
        //即没有第三个线程达到屏障，所以之前到达屏障的两个线程都不会执行
(1)     new Thread(new PreTaskThread("加载地图数据",cyclicBarrier)).start();
        new Thread(new PreTaskThread("加载人物模型",cyclicBarrier)).start();
        new Thread(new PreTaskThread("加载背景音乐",cyclicBarrier)).start();
    }
}
~~~

首先看一下几个关键属性

~~~java
    /** The lock for guarding barrier entry */
    private final ReentrantLock lock = new ReentrantLock();
    /** Condition to wait on until tripped */
    private final Condition trip = lock.newCondition();
    /** The number of parties */
    private final int parties;
    /* The command to run when tripped */
    private final Runnable barrierCommand;
    /** The current generation */
    private Generation generation = new Generation();
    /**
     * Number of parties still waiting. Counts down from parties to 0
     * on each generation.  It is reset to parties on each new
     * generation or when broken.
     */
    private int count;

    private static class Generation {
        boolean broken = false;
    }
~~~

**注意**：这里的parties,count,broken都没有被volatile修饰，因为都是在独占锁内使用变量，所以不需要声明。

首先我们通过构造函数传入了计数器初始值和任务，看下源码

~~~java
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @param barrierAction the command to execute when the barrier is
     *        tripped, or {@code null} if there is no action
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;//始终用来记录总的线程数
        this.count = parties;//因为cyclicbarrier是可复用的，当count=0后，将parties赋值给count
        this.barrierCommand = barrierAction;//当屏障被打破时所要执行的任务
    }
~~~

在代码(1)打个断点进去，会跑到我们实现的run方法里面执行。当运行到cyclicBarrier.await()时，f7进去，进入下面代码

~~~java
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        return dowait(false, 0L);//循环屏障的核心实现，第一个参数为false说明不设置超时时间，这时候第二个参数无意义
    } catch (TimeoutException toe) {
        throw new Error(toe); // cannot happen
    }
}
~~~

#### 核心实现dowait方法

~~~java
    /**
     * Main barrier code, covering the various policies.
     */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();//获取独占锁
        try {
            final Generation g = generation;

            if (g.broken)//如果borken被设置为true,说明屏障被破坏，抛出异常并返回
                throw new BrokenBarrierException();

            if (Thread.interrupted()) { //如果当前线程被打断
                breakBarrier();//打破屏障
                throw new InterruptedException();//抛出中断异常
            }

            int index = --count;//计数器值-1
(1)         if (index == 0) {  // tripped
                boolean ranAction = false;//标记位
                try {
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();//如果任务不为空就执行任务(先执行任务)
                    ranAction = true;
                    nextGeneration();//唤醒条件队列里的所有阻塞线程
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();//设置屏障状态为broken
                }
            }

            // loop until tripped, broken, interrupted, or timed out
(2)         for (;;) {
                try {
                    if (!timed)//没有设置超时时间
                        trip.await();//调用await方法进入trip条件变量的条件队列
                    else if (nanos > 0L)//设置了超时时间
                        nanos = trip.awaitNanos(nanos);//指定时间超时后自动被激活
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();//发生异常，将屏障设置为broken
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }
~~~

**大概流程**：在前面创建CycleBarrier时传递的参数为3，当进入这里后，第一个线程获取到独占锁，那么后面的两个线程就会被阻塞。然后index=--count=2，明显不等于0，所以会进入（2），然后进入trip条件变量的条件队列。

通过debug看下此时线程状态

![](https://s3.ax1x.com/2021/01/23/s7HOYR.png)

可以通过jstack命令查看原因：

![](https://s3.ax1x.com/2021/01/23/s7bK0g.png)

当一个线程由于**被阻塞释放锁后**，原来没有获取到锁而被阻塞的两个线程中会有一个竞争到锁，执行与第一个线程一样的操作。知道最后一个线程获取到lock锁，此时index=--count=0，所以执行代码（1）。因为传入任务不为null，所以调用run方法执行任务，并且调用nextGeneration方法唤醒条件队列里被阻塞的两个线程（要等当前线程释放lock锁后被唤醒的两个线程才会处于激活状态），并重置屏障。nextGeneration方法如下

~~~java
/**
* Updates state on barrier trip and wakes up everyone.
* Called only while holding lock.
*/
private void nextGeneration() {
    // signal completion of last generation
    trip.signalAll();
    // set up next generation
    count = parties;//重置屏障
    generation = new Generation();//更新broken为false
}
~~~

**这里有一个需要注意的是**：前面两个线程进来后直接调用await阻塞，当最后一个线程进来后才会调用

nextGeneration()方法，将count重置为3，然后在for循环中i=2时，进来的线程又会从3开始计数，从而达到复用。