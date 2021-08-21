package cxylk.test.concurrent.lockprinciple.aqs;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;

/**
 * @Classname ProducerAndConsumer
 * @Description 使用自定义锁实现生产-消费模型
 * @Author likui
 * @Date 2020/12/17 23:12
 **/
public class ProducerAndConsumer {
    static NonReentrantLock lock=new NonReentrantLock();
    //创建两个条件变量
    //不为满
    static Condition notFull=lock.newCondition();
    //不为空
    static Condition notEmpty=lock.newCondition();

    //维护一个队列
    static Queue<String> queue=new LinkedBlockingDeque<>();

    //定义队列大小
    static int queueSize=10;

    public static void main(String[] args) {
        Thread producer=new Thread(()->{
            //获取独占锁。不要将这一步写到try块中，因为如果在获取锁(自定义锁的实现)时发生了异常，
            //异常抛出的同时，也会导致锁无故释放
            lock.lock();
            try {
                //如果队列满了，则等待。这里用while循环是避免虚假唤醒。
                //即使线程被虚假唤醒，但是队列满了还是会等待
                while (queue.size()==queueSize){
                    notEmpty.await();
                }

                //走到这步说明生产者已被唤醒

                //添加元素到队列
                queue.add("element");

                //唤醒所有消费者线程
                notFull.signalAll();
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                //释放锁
                lock.unlock();
            }
        },"生产者");

        Thread consumer=new Thread(()->{
            lock.lock();
            try{
                //这里使用while仍然是为了避免虚假唤醒
                while (0==queue.size()){
                    notFull.await();
                }

                //消费一个元素
                String element=queue.poll();
                System.out.println(element);

                //唤醒生产线程
                notEmpty.signalAll();

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                lock.unlock();
            }
        },"消费者");

        producer.start();

        consumer.start();
     }
}
