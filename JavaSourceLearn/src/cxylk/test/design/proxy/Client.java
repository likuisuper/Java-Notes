package cxylk.test.design.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Classname Client
 * @Description 客服端测试代理模式
 * @Author likui
 * @Date 2021/2/1 15:34
 **/
public class Client {
    public static void main(String[] args) {
        //------------静态代理-----------
        UserServiceImpl target=new UserServiceImpl();
        UserService userService=new UserServiceProxy(target);
        //打日志
//        userService.editName(1,"lk");

        //------------动态代理-------------
        UserService userService2= (UserService) Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        //前置逻辑
                        System.out.println(String.format("动态代理:修改用户名称 id:%s,name:%s",args[0],args[1]));
                        try{
                            return method.invoke(target,args[0],args[1]);
                        }finally {
                            System.out.println("结束 代理后置逻辑");
                        }
                    }
                });
        userService2.editName(2,"xs");
    }
}
