## 信号量Semaphore

Semaphore也是一个同步器，不过和CountDownLatch和CyclicBarrier不同的是，它**内部的计数器是递增的**，并且在一开始初始化时可以指定一个初始化值，但是并不需要知道需要同步的线程个数，而是在需要同步的地方调用acquire方法时指定需要同步的线程个数。

类图：

![](https://s3.ax1x.com/2021/01/24/sHoj2V.png)

可以看到，Semaphore还是使用AQS实现的，Sync继承了AQS，有两个实现类，用来指定获取信号量时是否采用公平策略。

通过一个例子来看看Semaphore是怎么工作的

~~~java
/**
 * @Classname SemaphoreTest
 * @Description 使用信号量来模拟CyclicBarrier的功能，该信号量内部的计数器是递增的
 * @Author likui
 * @Date 2021/1/7 21:54
 **/
public class SemaphoreTest {
    //创建一个信号量实例,初始化信号量的初值为0
    private static volatile Semaphore semaphore=new Semaphore(0);

    public static void main(String[] args) throws InterruptedException {
        //创建一个核心线程个数为2的线程池
        ExecutorService executorService= Executors.newFixedThreadPool(2);
        //将线程A加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"A task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //将线程B加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"A task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //等待子线程执行任务A完毕，传入2说明调用acquire方法的线程会一直阻塞，直到信号量的计数变为2才会返回
        //当该方法返回后，当前信号量变为0
        semaphore.acquire(2);

        //将线程C加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"B task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //将线程D加入线程池
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"B task over");
                //计数器递增1
                semaphore.release();
            }
        });
        //等待子线程执行B完毕
        semaphore.acquire(2);

        System.out.println("all task is over");

        //关闭线程池
        executorService.shutdown();
    }
~~~

首先通过构造函数传递了一个初始值0，默认采用**非公平策略**

~~~java
public Semaphore(int permits) {
    sync = new NonfairSync(permits);
}
~~~

调用继承了AQS的Sync类的构造方法

~~~java
abstract static class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = 1192457210091910933L;

    Sync(int permits) {
        setState(permits);
    }
    ....
}
~~~

可以看到还是将初始值赋给state状态变量，这里的AQS的state值也表示当前持有信号量个数。

然后线程池提交任务，debug进入release方法

#### release方法

~~~java
//把当前信号量值增加1
public void release() {
    //参数为1
    sync.releaseShared(1);
}
~~~

由于sync继承了AQS，所以会调用AQS的releaseShared(int arg)方法，传入参数为1

~~~java
public final boolean releaseShared(int arg) {
    //尝试释放资源
    if (tryReleaseShared(arg)) {
        doReleaseShared();//调用unpark激活被阻塞的线程
        return true;
    }
    return false;
}
~~~

如果tryReleaseShared(arg)方法返回true，那么就会调用AQS的doReleaseShared()方法激活被阻塞的线程。看一下该方法的具体实现

~~~java
protected final boolean tryReleaseShared(int releases) {
    for (;;) {
        int current = getState();//获取当前信号量的值
        int next = current + releases;//将当前信号量的值+1
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        if (compareAndSetState(current, next))//cas设置state的值，成功则返回,失败重试
            return true;
    }
}
~~~

当线程A执行完release后，此时的state被置为1

然后B线程执行与A线程同样的操作，将state置为2返回

#### 非公平策略的acquire(permits)方法

此时调用semaphore.acquire(2);方法，debug进入该方法：

~~~java
public void acquire(int permits) throws InterruptedException {
    if (permits < 0) throw new IllegalArgumentException();
    //传递参数为permits，说明要获取permits个信号量资源
    sync.acquireSharedInterruptibly(permits);
}
~~~

调用AQS的acquireSharedInterruptibly方法

~~~java
public final void acquireSharedInterruptibly(int arg)
    throws InterruptedException {
    if (Thread.interrupted())//如果被中断则抛出中断异常
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);//获取失败则放入阻塞队列，调用park挂起当前线程
}
~~~

tryAcquireShared是由子类Sync来实现的，根据构造函数来确定是公平还是不公平策略，这里采用的是非公平策略，所以先分析非公平策略

~~~java
final int nonfairTryAcquireShared(int acquires) {
    for (;;) {
        int available = getState();//获取当前信号量值
        int remaining = available - acquires;//计数当前剩余值
        if (remaining < 0 ||//如果当前剩余值<0
            compareAndSetState(available, remaining))//或者cas设置成功则返回剩余值
            return remaining;
    }
}
~~~

从上面也可以看出，**将信号量的值也就是state重新设置为剩余值(在这里也就是0)**，所以当下面的C和D线程在去调用release方法时又会重新从0开始计数。

#### 公平策略的acquire(int permits)

如果构造函数传入的是true，则表示使用公平策略

~~~java
public Semaphore(int permits,true) {
    sync = new NonfairSync(permits);
}
~~~

具体实现

~~~java
protected int tryAcquireShared(int acquires) {
    for (;;) {
        if (hasQueuedPredecessors())//查看当前线程的前驱节点是否也在等待获取该资源，是则放弃并放入AQS阻塞队列
            return -1;
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
    }
}
~~~

#### acquire()方法

该方法与acquire(int permits)不同的是，后者获取permits个信号量，而前者获取一个

~~~java
public void acquire() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
~~~

**总结**：acquire方法表示获取一个或permits个信号量资源，当出现下面几种情况该方法会返回：

（1）如果当前信号量个数>0，则当前信号量的计数会减1，然后返回

（2）如果信号量个数=0（0-1后将变成了-1），则当前线程会被放入AQS的阻塞队列

它有公平策略和非公平策略两种实现。release方法将当前Semaphore对象的信号量值加1。

需要注意的是：Semaphore的**计数器是不可自动重置的，不过通过变相改变acquire方法的参数还是可以实现CyclicBarrier的效果**。