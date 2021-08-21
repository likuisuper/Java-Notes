package cxylk.test.concurrent.fakeshared;

/**
 * @Classname TestForContent2
 * @Description 伪共享2
 * @Author likui
 * @Date 2020/11/30 22:12
 **/
public class TestForContent2 {
    private static int LINE_NUM=1024;
    private static int COLUM_NUM=1024;

    public static void main(String[] args) {
        long[][] array=new long[LINE_NUM][COLUM_NUM];
        long startTime= System.currentTimeMillis();
        for (int i = 0; i < COLUM_NUM; i++) {
            for (int j = 0; j < LINE_NUM; j++) {
                array[j][i]=i*2+j;
            }
        }
        long endTime= System.currentTimeMillis();
        long cacheTime=endTime-startTime;
        //执行多次，耗时在10ms以上次数明显增多
        System.out.println("cache time:"+cacheTime);
    }
}
