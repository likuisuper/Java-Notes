package cxylk.test.concurrent.atomic;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @Classname AtomicReferenceFieldUpdaterDemo
 * @Description 原子更新引用类型中的字段，解决AtomicIntegerFieldUpdater或者LongUpdater只能更新
 *              int和long类型字段的问题
 * @Author likui
 * @Date 2020/12/8 11:02
 **/
public class AtomicReferenceFieldUpdaterDemo {
    private static AtomicReferenceFieldUpdater<User, Integer> arfu=
            AtomicReferenceFieldUpdater.newUpdater(User.class, Integer.class,"age");

    public static void main(String[] args) {
        User user=new User("likui",20);
        User newUser=new User("cxylk",22);
        arfu.compareAndSet(user,user.age,newUser.age);
        System.out.println(user.age);
    }
    static class User{
        private String name;

        public volatile Integer age;

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
