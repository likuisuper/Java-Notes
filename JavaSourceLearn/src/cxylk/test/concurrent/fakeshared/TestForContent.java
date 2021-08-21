package cxylk.test.concurrent.fakeshared;

/**
 * @Classname TestForContent
 * @Description 伪共享1，所谓伪共享：多个变量被放入了一个缓存行中，并且多个线程同时去写入缓存行
 *              中不同的变量
 * @Author likui
 * @Date 2020/11/30 22:07
 **/
public class TestForContent {
    private static int LINE_NUM=1024;
    private static int COLUM_NUM=1024;

    public static void main(String[] args) {
        long[][] array=new long[LINE_NUM][COLUM_NUM];

        long startTime= System.currentTimeMillis();
        for (int i = 0; i < LINE_NUM; i++) {
            for (int j = 0; j < COLUM_NUM; j++) {
                array[i][j]=i*2+j;
            }
        }
        long endTime= System.currentTimeMillis();
        long cacheTime=endTime-startTime;
        //执行多次，耗时均在10ms以下
        System.out.println("cache time:"+cacheTime);
    }
}
