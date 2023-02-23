## 线程池

#### 解决问题：

主要解决两个问题：

* 一是当执行大量异步任务时线程池能够提供比较好的性能。
  * 当不使用线程池时，每当执行一个异步任务都是直接new一个线程来运行，而线程的创建和销毁都需要开销。而线程池里面的线程都是可复用的
* 二是线程池提供了一种资源限制和管理的手段。

#### 好处

* **第一是降低消耗资源**
  * 通过重复利用已创建的线程来降低线程创建和销毁带来的消耗
* **第二是提高响应速度**
  * 当任务到达时，任务可以不需要等待线程创建就能立即执行。
* **第三是提高线程的可管理性**
  * 线程是稀缺资源，如果无限制地创建，不仅会消耗资源，还会降低系统的稳定性，使用线程池可以进行统一的分配、调优和监控。

先来看下类图

![](https://s3.ax1x.com/2021/01/24/sbE1Fs.png)

看下ThreadPoolExecutor中几个重要的字段和方法

~~~java
    //假设这里的Integer是4个字节表示，即32位(平台不同，Integer表示的字节也就不同)
	private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));//高3位用来表示线程池状态，低29位用来表示线程个数,所以默认是RUNNING
    private static final int COUNT_BITS = Integer.SIZE - 3;//29位来表示工作线程数量
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;//容量(线程池最大个数)：000 29个1

    // runState is stored in the high-order bits
    private static final int RUNNING    = -1 << COUNT_BITS;//-1用二进制表示位32个1，左移29位就是111 00000000000000000000000000000
    private static final int SHUTDOWN   =  0 << COUNT_BITS;//32个0
    private static final int STOP       =  1 << COUNT_BITS;//001 29个0
    private static final int TIDYING    =  2 << COUNT_BITS;//010 29个0
    private static final int TERMINATED =  3 << COUNT_BITS;//011 29个0

    // Packing and unpacking ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; }//获取高3位(运行状态),CAPACITY取反后就是高3位为1，低29位为0
    private static int workerCountOf(int c)  { return c & CAPACITY; }//获取低29位(线程数)
    private static int ctlOf(int rs, int wc) { return rs | wc; }//计算ctl新值(线程状态与线程个数)
~~~

#### 线程池状态含义如下：

RUNNING：接受新任务并且处理阻塞队列里的任务

SHUTDOWN：拒绝新任务，但是处理阻塞队列里的任务

STOP：拒绝新任务并且抛弃阻塞队列里的任务，同时会中断正在处理的任务

TIDYING：所以任务都执行完（包含阻塞队列里的任务）后当前线程池活动线程数为0，将要调用terminated方法。

TERMINATED：终止状态。terminated方法调用完成以后的状态。

#### 线程池参数：

corePoolSize：线程池核心线程个数

workQueue：用于保存等待执行的任务的阻塞队列。比如基于数组的有界队列ArrayBlockingQueue、基于链表的无界LinkedBlockingQueue、最多只有一个元素的同步队列SynchronousQueue以及优先级队列PriorityBlockingQueue等。

maximumPoolSize：线程池最大线程数量

ThreadFactory：创建线程的工厂

RejectedExecutionHandle：饱和策略，就是当队列满并且线程个数达到maximunPoolSize后需要采取的策略。主要有四种：

* AbortPolicy(抛出异常)

  丢弃任务并抛出异常。默认策略，在任务不能再提交的时候，抛出异常，及时反馈程序运行状态。如果是比较关键的业务，推荐使用此拒接策略，这样在系统不能承载更大的并发量的时候，能够及时的通过异常发现。

* CallerRunsPolicy(使用调用者所在线程来运行任务)

  由调用线程（提交任务的线程）处理该任务。这种情况是需要让所有任务都执行完毕，那么就适合大量计算的任务类型去执行，多线程仅仅是增大吞吐量的手段，最终必须要让每个任务都执行完毕

* DiscardOldestPolicy(丢弃队列里最近的一个任务，执行当前任务)

  丢弃队列最前面的任务，然后重新提交被拒绝的任务。是否要采用此种策略，还得根据实际业务是否允许丢弃老任务来认真衡量

* DiscardPolicy(不处理，直接丢弃掉)

  丢弃任务，但是不抛出异常。使用此策略，可能会使我们无法发现系统的异常状态。建议是一些无关紧要的业务采用此策略

KeepAliveTime：存活时间，空闲的非核心线程的存活时间。

TimeUnit：存活时间的时间单位。

#### 实现原理

查看execute方法的源码了解其实现

~~~java
public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();//如果任务为null，抛出NPE异常
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         */
        int c = ctl.get();//获取当前线程池的状态+线程数
        if (workerCountOf(c) < corePoolSize) { //1.如果线程个数<核心线程池的大小
            if (addWorker(command, true))//添加线程，第二个参数为true代表当前线程为核心线程
                return;//添加成功则返回
            c = ctl.get();//添加失败则检查当前状态
        }
        if (isRunning(c) && workQueue.offer(command)) {//2.走到这里说明线程个数已经>=核心线程池大小，则判断线程池状态是否为Running,并且入队
            int recheck = ctl.get();//双重检查，假如此时是非Running状态，那么command永远不会执行
            if (! isRunning(recheck) && remove(command))//如果上一步获取的线程池状态不是Running,则从队列移除
                reject(command);//执行拒接策略
            else if (workerCountOf(recheck) == 0)//确实是Running，但是没有线程
                addWorker(null, false);//3.则创建线程(非核心线程)
        }
        else if (!addWorker(command, false))//4.走到这里说明队列满了，新增线程，如果失败执行拒接策略
            reject(command); 
    }
~~~

线程池采取上述步骤，**是为了在执行execute()方法时，尽可能地避免获取全局锁(那将会是一个严重的可伸缩瓶颈)。在ThreadPoolExecutor完成预热之后（当前线程的线程数>=corePoolSize）,几乎所有的execute()调用都是执行步骤2，而步骤2不需要获取全局锁(因为不用执行addWork方法)**

流程就是这样：

1.判断核心线程池的线程是否都在执行任务，如果不是，那么创建新的线程，否则进入下一步

2.线程池判断工作队列是否已满，如果没有，就将新加入的任务放入工作队列。否则进入下一步。这里会进行一个Double Check的过程。目的是判断加入到阻塞队列中的线程是否可以被执行。如果线程池不是RUNNING状态，则调用remove()方法从阻塞队列中删除该任务，然后调用reject方法处理任务。否则需要确保还有线程执行。

3.如果当前队列已满，线程池判断线程池的线程是否都在执行任务，如果没有，那么创建新的工作线程，否则执行拒绝策略

通过《Java并发编程》中的一张图来看下这个流程

![](https://s3.ax1x.com/2021/01/26/sv9yid.png)

![](https://s3.ax1x.com/2021/01/26/svMfOg.png)

下面两张动图分别演示了执行流程以及空闲线程的销毁

![](https://s3.ax1x.com/2021/01/26/svMvm4.gif)



![](https://s3.ax1x.com/2021/01/27/svo9Lq.gif)

到这里，我们再通过一个具体的例子来看看核心实现。

首先创建一个任务

~~~java
public class MyRunnable implements Runnable{
    private String task;

    public MyRunnable(String task){
        this.task=task;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName()+":开始执行任务"+task+" time="+new Date());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName()+":执行任务完毕"+" time="+new Date());
    }
}
~~~

然后使用线程池来创建线程并执行任务

~~~java
/**
 * @Classname ThreadPoolDemo
 * @Description 使用线程池创建线程
 * @Author likui
 * @Date 2021/1/21 21:44
 **/
public class ThreadPoolDemo {
    //核心线程数
    private static final int CORE_POOL_SIZE=5;
    //线程池最大线程数量
    private static final int MAXIMUM_POOL_SIZE=10;
    //空闲线程(非核心线程)的存活时间
    private static final long KEEP_ALIVE_TIME=1L;
    
    public static void main(String[] args) {
        //使用ArrayBlockingQueue作为阻塞队列，拒接策略为callerRunsPolicy,即使用调用者所在的线程来运行任务
        ThreadPoolExecutor poolExecutor=new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.CallerRunsPolicy());
        for (int i = 0; i < 10; i++) {
            Runnable runnable=new MyRunnable(""+i);
            poolExecutor.execute(runnable);
        }
        //终止线程池
        poolExecutor.shutdown();
        //如果线程池没有终止就死循环直到终止
        while (!poolExecutor.isTerminated()){

        }
        System.out.println("all threads is finished");
    }

}
~~~

debug进入execute方法，看一下第一次进入的流程

~~~java
public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();//如果任务为null，抛出NPE异常
      	//第一次进来，这里为Running状态
        int c = ctl.get();
    	//c&CAPACITY,即111+29个0&000+29个1，所以此时工作线程数为0
        if (workerCountOf(c) < corePoolSize) { //0<5
            if (addWorker(command, true))//此时进入addWorker方法
                return;
            c = ctl.get();
        }
    	//当i=5，即任务>=核心线程池大小时将进入这里
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command))
                reject(command);
            //当i=5时，已经有线程了，所以不会走这个分支
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        else if (!addWorker(command, false))
            reject(command); 
    }
~~~

#### 核心实现addWorker方法

~~~java
    private boolean addWorker(Runnable firstTask, boolean core) {
        //标记位，用于break和continue
        retry:
        for (;;) {
            （1）
            int c = ctl.get();//-537068912
          	//获取当前线程状态
            int rs = runStateOf(c);//-537068912

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;

            for (;;) {
                //（2）进入这里
              	//线程数量
                int wc = workerCountOf(c);//0
                //如果线程个数超限则返回false
                //传进来的是核心线程，就判断是否超过核心线程池的大小，否则判断线程池的大小
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                //cas加1，成功则跳出循环
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                //CAS失败，查看线程池状态是否变化，变化则跳到外层循环重新尝试获取线程池状态
                //否则内层循环重新CAS
                c = ctl.get();  // Re-read ctl
              	//如果状态不等于之前获取的state，跳出内层循环，继续去外层循环判断
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        //（3）跳出循环后走到这里,说明CAS成功了
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
          	//新建线程：Worker
            w = new Worker(firstTask);
          	//当前线程
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                //获取独占锁
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    //上一步已经将ctl加1了，所以此时的rs=-536870911
                    int rs = runStateOf(ctl.get());
									
                  	//rs < SHUTDOWN==>线程处于RUNNING状态，或者线程处于SHUTDOWN，且firstTask==null(可能是workQueue中仍有未执行完成的任务，创建没有初始化任务的worker线程执行)
                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {
                        //此时线程都还没启动，所以这里为false
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        workers.add(w);//添加任务
                        int s = workers.size();//此时s=1
                        if (s > largestPoolSize)//largestPoolSize默认0
                            largestPoolSize = s;//将LargestPoolSize设置为s=1
                        workerAdded = true//设置添加成功标记为true
                    }
                } finally {
                    mainLock.unlock();//释放锁
                }
                if (workerAdded) {//上面已经设置为true
                    t.start();//启动一个线程
                    workerStarted = true;//将启动线程标记设置为true
                }
            }
        } finally {
          	//线程启动失败
            if (! workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }
~~~

上面有个注意的点：

~~~java
            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;
~~~

即判断当前线程是否可以添加新任务，如果可以则进行下一步，否则return false

将上面的代码展开

~~~java
if(rs>=SHUTDOWN&&(rs!=SHUTDOWN||firstTask!=null||workQueue.isEmpty()))
    return false;
~~~

由上面可知，在下面几种情况下会返回false:

(1)当前线程池状态为SHUTDOWN、STOP、TIDYING、或TREMINATED

(2)当前线程为SHUTDOWN，并且已经有了第一个任务

(3)当前线程为SHUTDOWN，并且队列为空

现在回到addWork方法中，代码现在执行到

~~~java
if (workerAdded) {//上面已经设置为true
    t.start();//启动一个线程
    workerStarted = true;//将启动线程标记设置为true
}
~~~

t.start()方法会触发Worker类的run方法被JVM调用

~~~java
/** Delegates main run loop to outer runWorker  */
public void run() {
    runWorker(this);
}
~~~

#### 工作线程Worker

首先来看下Worker类是什么。

~~~java
    private final class Worker
        extends AbstractQueuedSynchronizer
        implements Runnable
~~~

可以看到Worker类实现了Runnable接口，所以它也是一个线程任务。

看构造方法

~~~java
        /**
         * Creates with given first task and thread from ThreadFactory.
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {
          	//设置AQS的同步状态，大于0代表锁已经被获取
            setState(-1); // inhibit interrupts until runWorker
            this.firstTask = firstTask;
            //创建一个线程(线程的任务就是自己)
            this.thread = getThreadFactory().newThread(this);
        }
~~~

构造函数中首先将state也就是Worker状态设置为了-1，这是为什么呢？

**避免当前Worker在调用runWorker方法前被中断(当其他线程调用了线程的shutdownNow时，如果当前Worker状态>=0就会被中断)**。这里设置了-1就不会被中断了。具体看源码：

~~~java
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }
~~~

其中的interruptWorkers()方法中：

~~~java
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }
~~~

查看w.interruptIfStarted实现（该方法在Worker类中）

~~~java
        void interruptIfStarted() {
            Thread t;
            //Worker状态>0
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();//中断线程
                } catch (SecurityException ignore) {
                }
            }
        }
~~~

另外，**worker是保存在一个set集合中的**

~~~java
private final HashSet<Worker> workers = new HashSet<Worker>();
~~~

#### RunWorker方法

前面说了当调用run方法时会执行RunWorker方法，看下该方法的具体实现

~~~java
    final void runWorker(Worker w) {
      	//当前线程
        Thread wt = Thread.currentThread();
      	//要执行的任务
        Runnable task = w.firstTask;
        w.firstTask = null;
        //释放锁，允许中断，因为调用unlock()方法可以将state设置为0
      	//interruptWorkers()方法只有在state>=0时才会执行
        w.unlock(); //(1) allow interrupts
        boolean completedAbruptly = true;
        try {
            //(1)
            //如果第一个任务不为空，获取getTask()方法不返回null，循环不退出
            while (task != null || (task = getTask()) != null) {
                //进行加锁操作，保证thread不被其他线程中断(除非线程池被中断)
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                //检查线程池状态，如果线程池处于中断状态，则当前线程将中断
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    //具体任务执行前做一些事情
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        //执行任务
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        //执行任务完毕后做一些事情
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    //统计当前Worker完成了多少任务
                    w.completedTasks++;
                    //释放锁
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            //(2)执行清理工作
            processWorkerExit(w, completedAbruptly);
        }
    }
~~~

在如上代码(1)中通过w.unlock允许线程中断，看下是为什么

~~~java
public void unlock()      { release(1); }
~~~

调用AQS的release方法

~~~java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
~~~

在tryRelease中

~~~java
protected boolean tryRelease(int unused) {
    setExclusiveOwnerThread(null);
    setState(0);
    return true;
}
~~~

可以看到将state状态设置为了0

继续回到runWorker方法中。

重点看代码(1)，这是线程池为什么能复用的原因

#### 线程池复用

线程池的复用就是指一个线程执行完毕后不销毁，继续执行另外的线程任务。为什么能复用呢？

因为ThreadPoolExecutor在创建线程时，会将线程封装成**工作线程Worker**，并放入工作线程组中，然后这个worker反复从阻塞队列中获取任务执行。也就是上面的代码(1)部分。

在这里会调用getTask()方法获取任务，看具体的代码实现：

~~~java
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?

        for (;;) {
            //获取ctl的值，也就是线程池状态+线程池个数
            int c = ctl.get();
            //获取运行状态
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            //如果线程池状态>=SHUTDOWN并且(状态>=STOP或者队列为空)
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                //cas将线程个数-1
                decrementWorkerCount();
                return null;
            }

            //获取工作线程数
            int wc = workerCountOf(c);

            // Are workers subject to culling?
            //allowCoreThreadTimeOut默认是false,即核心线程空闲也不会被销毁
            //如果为true,核心线程即使空闲也会被销毁
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
			
            //如果运行线程数超过了最大线程数，但是队列为空，递减worker数量
            //如果设置了允许线程超时或者线程数超过核心线程数
            //并且线程在规定时间内均未poll到任务且队列为空则递减workr数量
            if ((wc > maximumPoolSize || (timed && timedOut))
                && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                //当timed为true,也就是设置核心线程即使空闲也会被销毁或者worker数量>核心线程数，
                //这时就会调用poll方法获取任务。超时时间为keepAlivTime,单位为ns。如果超过该时长，                 //上面的while循环就会退出，线程执行完毕。
                //如果timd为false，说明核心线程空闲被销毁，并且workr数量<核心线程数，则调用take方				   //法。队列中由任务加入时，线程被唤醒，take方法返回任务，并执行。
                Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();
                if (r != null)
                    return r;
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }
~~~

如果runWorker方法中task==null或者调用getTask从任务队列获取的任务返回null，则执行代码(2)processWorkerExit(w, completedAbruptly)进行清理工作，源码实现：

~~~java
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
      	//true：用户线程运行异常，需要扣减
      	//false：getTask方法中扣减线程数量
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //统计整个线程池完成的任务个数
            completedTaskCount += w.completedTasks;
            //从工作集里面删除Worker
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        //尝试设置线程池状态为TERMINATED，如果当前是SHUTDOWN状态并且工作队列为空
        //或者当前是STOP状态，当前线程池里面没有活动线程。
        //如果设置为TERMINATED，还需要调用条件变量termination的signalAll()方法唤醒所有因为调用
        //线程池的awaitTermination方法而被阻塞的线程
        tryTerminate();

        int c = ctl.get();
      	//如果线程为running或shutdown状态，即tryTerminate()没有成功终止线程池，则判断
      	//是否有必要一个worker
        if (runStateLessThan(c, STOP)) {
          	//正常退出，计算min：需要维护的最小线程数量
            if (!completedAbruptly) {
              	//是否需要维持核心线程的数量
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
              	//如果线程数量大于最少数量min，直接返回，不需要新增线程
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
          	//添加一个没有firstTask的worker
            addWorker(null, false);
        }
    }
~~~

最后来看下上面的例子的输出结果

![](https://s3.ax1x.com/2021/01/27/svTYNT.png)

![](https://s3.ax1x.com/2021/01/27/svTK3Q.png)

可以看到，由于核心线程池大小设置为5，所以最开始只会执行0，1，2，3，4这5个任务，剩下的5，6，7，8，9这5个任务将会被放入工作队列，然后当5个线程将这0，1，2，3，4这5个任务执行完之后就会去工作队列中获取剩下的任务执行。

![](https://s3.ax1x.com/2021/01/27/svTfvd.png)

我们如果将代码改成这样

~~~java
        ThreadPoolExecutor poolExecutor=new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,new ArrayBlockingQueue<>(10),new ThreadPoolExecutor.CallerRunsPolicy());
        for (int i = 0; i < 20; i++) {
            Runnable runnable=new MyRunnable(""+i);
            poolExecutor.execute(runnable);
        }
~~~

将有界队列的大小改为10，任务变为20，此时的输出结果如下

![](https://s3.ax1x.com/2021/01/27/svq6Ld.png)

解释下为什么：因为任务有20个，而核心线程池大小为5，所以核心线程池执行5个任务后，剩下的15个任务会进入队列，但是队列大小为10，也就是只能容纳10个任务，所以还有5个任务不会进入队列。而线程池大小为10，除去核心线程的大小，还能容纳5个线程，所以线程池会新建5个非核心线程，也就是上图用红线标记的线程。最后执行完毕：

![](https://s3.ax1x.com/2021/01/27/svqaIx.png)

#### 合理配置线程池

要想合理地配置线程池，就必须首先分析任务特性，可以从以下几个角度来分析。 

* 任务的性质：CPU密集型任务、IO密集型任务和混合型任务。
  * CPU密集型任务应配置尽可能小的 线程，如配置Ncpu+1个线程的线程池
  * IO密集型任务线程并不是一直在执行任务，则应配 置尽可能多的线程，如2*Ncpu。混合型的任务， 
* 任务的优先级：高、中和低。 
* 任务的执行时间：长、中和短。 
* 任务的依赖性：是否依赖其他系统资源，如数据库连接。

除此之外，还**建议使用有界队列**：使用无界队列，队列中的任务就会越来越多，有可能会撑满内存

#### 线程池的监控

可以通过线程池提供的参数进行监控，在监控线程池的 时候可以使用以下属性。 

* taskCount：线程池需要执行的任务数量。 
* completedTaskCount：线程池在运行过程中已完成的任务数量，小于或等于taskCount。 
* largestPoolSize：线程池里曾经创建过的最大线程数量。通过这个数据可以知道线程池是 否曾经满过。如该数值等于线程池的最大大小，则表示线程池曾经满过。 
* getPoolSize：线程池的线程数量。如果线程池不销毁的话，线程池里的线程不会自动销 毁，所以这个大小只增不减
* getActiveCount：获取活动的线程数。

通过扩展线程池进行监控。可以通过继承线程池来自定义线程池，重写线程池的 beforeExecute、afterExecute和terminated方法，也可以在任务执行前、执行后和线程池关闭前执 行一些代码来进行监控

## 四种常见线程池

#### newCachedThreadPool

~~~java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}
~~~

`CacheThreadPool`的**运行流程**如下：

1. 提交任务进线程池。
2. 因为**corePoolSize**为0的关系，不创建核心线程，线程池最大为Integer.MAX_VALUE。
3. 尝试将任务添加到**SynchronousQueue**队列。
4. 如果SynchronousQueue入列成功，等待被当前运行的线程空闲后拉取执行。如果当前没有空闲线程，那么就创建一个非核心线程，然后从SynchronousQueue拉取任务并在当前线程执行。
5. 如果SynchronousQueue已有任务在等待，入列操作将会阻塞。

当需要执行很多**短时间**的任务时，CacheThreadPool的线程复用率比较高， 会显著的**提高性能**。而且线程60s后会回收，意味着即使没有任务进来，CacheThreadPool并不会占用很多资源。

#### newFixedThreadPool

~~~java
public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
}
~~~

核心线程数量和总线程数量相等，都是传入的参数nThreads，所以只能创建核心线程，不能创建非核心线程。因为LinkedBlockingQueue的默认大小是Integer.MAX_VALUE，故如果核心线程空闲，则交给核心线程处理；如果核心线程不空闲，则入列等待，直到核心线程空闲。

**与CachedThreadPool的区别**：

- 因为 corePoolSize == maximumPoolSize ，所以FixedThreadPool只会创建核心线程。 而CachedThreadPool因为corePoolSize=0，所以只会创建非核心线程。
- 在 getTask() 方法，如果队列里没有任务可取，线程会一直阻塞在 LinkedBlockingQueue.take() ，线程不会被回收。 CachedThreadPool会在60s后收回。
- 由于线程不会被回收，会一直卡在阻塞，所以**没有任务的情况下， FixedThreadPool占用资源更多**。
- 都几乎不会触发拒绝策略，但是原理不同。FixedThreadPool是因为阻塞队列可以很大（最大为Integer最大值），故几乎不会触发拒绝策略；CachedThreadPool是因为线程池很大（最大为Integer最大值），几乎不会导致线程数量大于最大线程数，故几乎不会触发拒绝策略。

#### newSingleThreadPool

~~~java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}
~~~

有且仅有一个核心线程（ corePoolSize == maximumPoolSize=1），使用了LinkedBlockingQueue（容量很大），所以，**不会创建非核心线程**。所有任务按照**先来先执行**的顺序执行。如果这个唯一的线程不空闲，那么新来的任务会存储在任务队列里等待执行。

#### newSchedualedThreadPool

创建一个定长线程池，支持定时及周期性任务执行。

~~~java
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
    return new ScheduledThreadPoolExecutor(corePoolSize);
}

//ScheduledThreadPoolExecutor():
public ScheduledThreadPoolExecutor(int corePoolSize) {
    super(corePoolSize, Integer.MAX_VALUE,
          DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS,
          new DelayedWorkQueue());
}
~~~

《阿里巴巴开发手册》不建议直接使用Executors类中的线程池，而是通过`ThreadPoolExecutor`的方式，这样的处理方式让写的同学需要更加明确线程池的运行规则，规避资源耗尽的风险。