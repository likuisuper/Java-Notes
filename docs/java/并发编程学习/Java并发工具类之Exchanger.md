## Exchanger

用于两个线程交换数据。

通过一个例子来看实现过程

~~~java
/**
 * @Classname ExchangerDemo
 * @Description Exchanger用于两个线程间交换数据。它支持泛型，也就是说可以在两个线程之间交换两个数据
 *              如果出现了三个线程交换数据，那么只有前两个线程会交换数据，第三个线程进入阻塞状态。
 *              而且Exchanger是重复的，可以使用Exchanger在内存中不断的交换数据
 * @Author likui
 * @Date 2021/1/8 20:56
 **/
public class ExchangerDemo {
    private static Exchanger<String> exchanger=new Exchanger<>();

    public static void main(String[] args) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("这是线程A,得到了另外一个线程的数据:"+
                            exchanger.exchange("这是来自线程A的数据"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("这个时候线程A是阻塞的，在等待线程B的数据");

        Thread.sleep(1000);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("这是线程B，得到了另外一个线程的数据:"+
                            exchanger.exchange("这是来自线程B的数据"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //假设有第三个线程,这个时候线程C会被阻塞
//        new Thread(()-> {
//            try {
//                System.out.println("这是线程C，想要得到另外一个线程的数据:"+
//                        exchanger.exchange("这是来自其他线程的数据"));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
    }
}

~~~

exchange方法中调用了slotExchange方法完成具体的数据交换，底层使用了Unsafe的一系列CAS方法、Park和Put方法等。这里不做具体的探究，知道使用就好。