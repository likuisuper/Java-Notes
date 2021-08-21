package cxylk.test.concurrent.utilqueue;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @Classname TestPriorityBlockingQueue
 * @Description 体验优先级队列PriorityBlockingQueue的使用方法
 *              把具有优先级的任务放入队列，然后从队列里面逐个获取优先级最高的任务来执行
 * @Author likui
 * @Date 2020/12/30 22:07
 **/
public class TestPriorityBlockingQueue {
    //必须实现Comparable接口(实现内部比较)
    //或者实现Comparator接口(自定义排序)
    static class Task implements Comparable<Task> {
        //优先级
        private int priority=0;

        //任务名称
        private String taskName;

        public void setPriority(int priority){
            this.priority=priority;
        }

        public void setTaskName(String taskName){
            this.taskName=taskName;
        }

        public int getPriority(){
            return priority;
        }

        public String getTaskName(){
            return taskName;
        }

        /**
         * 默认排序
         * @param o
         * @return
         */
        @Override
        public int compareTo(Task o) {
            if(this.priority>=o.getPriority()){
                return 1;
            }else {
                return -1;
            }
        }

//        /**
//         * 实现Comparator接口，自定义排序
//         * @param o1
//         * @param o2
//         * @return
//         */
//        @Override
//        public int compare(Task o1,Task o2) {
//            if(o1.getPriority()>=o2.getPriority()){
//                return 1;
//            }else {
//                return -1;
//            }
//        }

        //业务逻辑
        public void doSomething(){
            System.out.println(taskName+":"+priority);
        }

        public static void main(String[] args) {
            //默认排序，必须实现Comparable接口，重写compareTo方法，否则报ClassNotCastException
            PriorityBlockingQueue<Task> priorityQueue=new PriorityBlockingQueue<>();
            //使用自定义排序，必须实现Comparator接口，重写compare方法，调用优先队列的带参构造,否则报ClassNotCastException
            //PriorityBlockingQueue<Task> priorityQueue=new PriorityBlockingQueue<>(8,new Task());
            Random random=new Random();
            for (int i = 0; i < 10; i++) {
                Task task=new Task();
                //设置优先级为[0-10)
                task.setPriority(random.nextInt(10));
                task.setTaskName("taskName"+i);
                //将任务加入队列
                priorityQueue.offer(task);
            }
            //取出任务并执行,使用while是为了避免虚假唤醒，就算线程被虚假唤醒，
            //但是队列不为空，执行业务逻辑
            while (!priorityQueue.isEmpty()){
                Task task=priorityQueue.poll();
                if(null!=task){
                    task.doSomething();
                }
            }
        }
    }
}
