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
* CallerRunsPolicy(使用调用者所在线程来运行任务)
* DiscardOldestPolicy(丢弃队列里最近的一个任务，执行当前任务)
* DiscardPolicy(不处理，直接丢弃掉)