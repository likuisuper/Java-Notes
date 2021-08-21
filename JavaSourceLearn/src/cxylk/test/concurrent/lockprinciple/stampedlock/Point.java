package cxylk.test.concurrent.lockprinciple.stampedlock;

import java.util.concurrent.locks.StampedLock;

/**
 * @Classname Point
 * @Description StampedLock号称锁的性能之王，通过一个官方的提供的一个管理二维点的例子来理解它
 * @Author likui
 * @Date 2020/12/24 20:53
 **/
public class Point {
    //成员变量，表示一个点的二维坐标
    private double x,y;

    //锁实例
    private final StampedLock stampedLock=new StampedLock();

    /**
     * 使用参数的增量值，改变当前的point坐标的位置。使用排它锁-写锁
     * @param deltaX
     * @param deltaY
     */
    public void move(double deltaX,double deltaY){
        long stamp=stampedLock.writeLock();
        try{
            x+=deltaX;
            y+=deltaY;
        }finally {
            stampedLock.unlockWrite(stamp);
        }
    }

    /**
     * 计算坐标上的点到原点的距离。一定要注意(2)和(3)的顺序不能换，这是最容易出错的地方
     * @return
     */
    public double distanceFromOrigin(){
        //(1)尝试获取乐观读锁,由于没有使用CAS,不需要显示释放锁
        long stamp=stampedLock.tryOptimisticRead();
        //(2)将全部变量复制到方法体栈内
        double currentX=x,currentY=y;
        //(3)检查在(1)处获取了读锁戳记后，锁有没有被其他写线程排他性独占
        if(!stampedLock.validate(stamp)){
            //(4)如果被抢占则获取一个共享读锁(悲观获取)
            stamp=stampedLock.readLock();
            try{
                //(5)将全部变量复制到方法体栈内
                currentX=x;
                currentY=y;
            }finally {
                //(6)释放共享读锁
                stampedLock.unlockRead(stamp);
            }
        }
        //(7)返回计算结果,根号下x平方+y平方
        return Math.sqrt(currentX*currentX+currentY*currentY);
    }

    /**
     * 如果当前坐标为原点则移动到指定位置。使用悲观锁获取读锁，并尝试转换为写锁
     * @param newX
     * @param newY
     */
    public void moveIfAtOrigin(double newX,double newY){
        //(1)获取悲观读锁(可以使用乐观读锁替换)
        long stamp=stampedLock.readLock();
        try{
            //(2)如果当前点在原点则移动
            while (x==0.0&&y==0.0){
                //(3)尝试将获取的读锁升级为写锁
                long ws=stampedLock.writeLock();
                if(ws!=0L){//升级成功
                    //(4)更新标记
                    stamp=ws;
                    //设置坐标值
                    x=newX;
                    y=newY;
                    //退出循环
                    break;
                }else {
                    //(5)升级失败则释放锁，显示获取独占写锁，然后循环重试
                    stampedLock.unlockRead(stamp);
                    stamp=stampedLock.writeLock();
                }
            }
        }finally {
            //(6)释放锁
            stampedLock.unlock(stamp);
        }
    }
}
