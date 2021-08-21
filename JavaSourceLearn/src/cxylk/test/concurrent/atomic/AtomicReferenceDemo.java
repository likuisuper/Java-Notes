package cxylk.test.concurrent.atomic;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @Classname AtomicReferenceDemo
 * @Description 原子更新引用类型。原子更新基本类型AtomicInteger只能更新一个变量，如果
 *              需要原子更新多个变量，就需要使用原子更新应用类型提供的这个类
 * @Author likui
 * @Date 2020/12/7 22:37
 **/
public class AtomicReferenceDemo {
    static AtomicReference<User> atomicReference=new AtomicReference<User>();

    public static void main(String[] args) {
        User user=new User("likui","20");
        atomicReference.set(user);
        User newUser=new User("cxylk","22");
        //更新值
        atomicReference.compareAndSet(user,newUser);
        //返回更新后的值
        System.out.println(atomicReference.get().getName());
        System.out.println(atomicReference.get().getAge());
    }

    static class User{
        private String name;

        private String age;

        public User(String name, String age){
            this.name=name;
            this.age=age;
        }

        public String getName() {
            return name;
        }

        public String getAge() {
            return age;
        }
    }
}
