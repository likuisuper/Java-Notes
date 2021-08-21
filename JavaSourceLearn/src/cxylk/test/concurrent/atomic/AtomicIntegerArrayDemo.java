package cxylk.test.concurrent.atomic;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * @Classname AtomicIntegerArrayDemo
 * @Description 原子更新整型数组里的元素
 * @Author likui
 * @Date 2020/12/7 21:37
 **/
public class AtomicIntegerArrayDemo {
    private static int[] value=new int[]{1,2,3};
    //当构造函数传入值的时候，AtomicIntegerArray会将当前值复制一份 this.array=value.clone()
    private static AtomicIntegerArray ai=new AtomicIntegerArray(value);

    public static void main(String[] args) {
        System.out.println(ai.getAndSet(0, 3));//输入的是原始值
        System.out.println(ai.get(0));//这时候索引0的值变为了3
        //上面说了当前传进去的value被复制了一份，所以当AtomicIntegerArray
        //对内部元素进行修改时，不会影响传入的数组
        System.out.println(value[0]);//还是1
    }
}
