package cxylk.test.concurrent.atomic;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @Classname AtomicIntegerFieldUpdaterDemo
 * @Description 原子更新字段类AtomicIntegerFieldUpdater测试
 * @Author likui
 * @Date 2020/12/8 9:39
 **/
public class AtomicIntegerFieldUpdaterDemo {
    static AtomicIntegerFieldUpdater<User> aifu= AtomicIntegerFieldUpdater.newUpdater(User.class,"age");

    public static void main(String[] args) {
        User user=new User("lk",20);
        System.out.println(aifu.getAndIncrement(user));
        System.out.println(aifu.get(user));
    }


    static class User{
        private String name;

        public volatile int age;

        public User(String name, int age){
            this.name=name;
            this.age=age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}

