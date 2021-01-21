package cxylk.test.concurrent.forkjoin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

/**
 * @Classname CountTask
 * @Description 使用fork/join框架完成几个数相加。该框架通过递归将一个任务分成很多小任务来提高性能
 * @Author likui
 * @Date 2021/1/9 20:35
 **/
public class CountTask extends RecursiveTask<Long> {
    //定义一个临界值，定义有一个合适的临界值才能发挥出该框架的性能，太大太小都不行
    private static final int THRESHOLD=10000;

    private int start;

    private int end;

    public CountTask(int start,int end){
        this.start=start;
        this.end=end;
    }

    @Override
    protected Long compute() {
        long sum=0;
        if(end-start<=THRESHOLD){
            for (int i = start; i <= end; i++) {
                sum+=i;
            }
        }else {
            int middle=(start+end)/2;
            CountTask leftTask=new CountTask(start,middle);
            CountTask rightTask=new CountTask(middle+1,end);
            leftTask.fork();
            rightTask.fork();
            long leftResult=leftTask.join();
            long rightResult=rightTask.join();
            sum=leftResult+rightResult;

        }
        return sum;
    }

    public static void main(String[] args) {
        //---从1加到4-----
//        testForkJoin1();//2-3ms
//        testNoForkJoin1(4);//500nanos

        //----从1加到1000000000，当计算量很大的时候
        testForkJoin2();//临界值为1000 -> 快，差距在300ms左右， 临界值为10000.差距在600-700ms
        testNoForkJoin2(1000000000);// -> 慢
    }

    //1+2+3+4耗时2ms
    public static void testForkJoin1(){
        ForkJoinPool forkJoinPool=new ForkJoinPool();
        long startTime=System.currentTimeMillis();
        //计算1+2+3+4
        CountTask countTask=new CountTask(1,4);
        Future<Long> future=forkJoinPool.submit(countTask);
        try {
            System.out.println(future.get());
            long endTime=System.currentTimeMillis();
            System.out.println(String.format("使用fork/join从1加到4耗时：%d millis",endTime-startTime));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void testNoForkJoin1(int n){
        long startTime=System.nanoTime();
        int sum=0;
        for (int i = 1; i <= n; i++) {
            sum+=i;
        }
        long endTime=System.nanoTime();
        System.out.println(String.format("不使用fork/join从1加到4耗时：%d nanos",endTime-startTime));
    }

    //1加到100000，耗时42ms
    public static void testForkJoin2(){
        ForkJoinPool forkJoinPool=new ForkJoinPool();
        System.out.println("cpu核数:"+Runtime.getRuntime().availableProcessors());
        long startTime=System.currentTimeMillis();
        //计算1+2+3+4
        CountTask countTask=new CountTask(1,1000000000);
        Future<Long> future=forkJoinPool.submit(countTask);
        try {
            System.out.println(future.get());
            long endTime=System.currentTimeMillis();
            System.out.println(String.format("使用fork/join从1加到1000000000耗时：%d millis",endTime-startTime));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void testNoForkJoin2(int n){
        long startTime=System.currentTimeMillis();
        int sum=0;
        for (int i = 1; i <= n; i++) {
            sum+=i;
        }
        long endTime=System.currentTimeMillis();
        System.out.println(String.format("不使用fork/join从1加到1000000000耗时：%d millis",endTime-startTime));
    }
}
