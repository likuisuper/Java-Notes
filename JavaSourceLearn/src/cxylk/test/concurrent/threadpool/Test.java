package cxylk.test.concurrent.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Classname Test
 * @Description TODO
 * @Author likui
 * @Date 2021/1/20 10:35
 **/
public class Test {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(new Thread(()-> System.out.println("启动"),"线程A"));
    }
}
