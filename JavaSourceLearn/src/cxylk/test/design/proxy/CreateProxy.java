package cxylk.test.design.proxy;

import sun.misc.ProxyGenerator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * @Classname CreateProxy
 * @Description 手动实现一个代理类
 * @Author likui
 * @Date 2021/2/1 16:41
 **/
public class CreateProxy {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, IOException, InstantiationException {
        createProxyClass();
    }

    public static void createProxyClass() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        //1.通过generateProxyClass方法生成一个字节数组
        //第一个参数为要生成的代理类类名，第二个参数为要代理接口的class数组对象
        byte[] bytes = ProxyGenerator.generateProxyClass("UserServiceProxy$1",new Class[]{UserService.class});
//        System.out.println(Arrays.toString(bytes));

        //2.生成类
        Files.write(new File("D:\\github\\Java-Notes\\JavaSourceLearn\\src\\cxylk\\test\\design\\proxy\\UserServiceProxy$1.class").toPath(),
                bytes);

        //3.类装载
        //获取类加载器
        ClassLoader classLoader = UserService.class.getClassLoader();
        //获取defineClass方法
        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        //设置为可访问
        defineClass.setAccessible(true);
        //子loader
        URLClassLoader loader = new URLClassLoader(new URL[]{}, classLoader);
        Class<?> proxyClass = (Class<?>) defineClass.invoke(loader, "UserServiceProxy$1", bytes,0, bytes.length);

        //4.获取代理类
        Class<?> parentProxyClass = loader.loadClass("UserServiceProxy$1");
        System.out.println(parentProxyClass);//class UserServiceProxy$1

        //5.实例化
        Constructor<?> constructor = parentProxyClass.getConstructor(InvocationHandler.class);
        UserService userService = (UserService) constructor.newInstance((InvocationHandler) (proxy, method, args) -> {
            System.out.println(String.format("自定义动态代理实现：修改用户名称 id:%s,name:%s", args[0], args[1]));
            return null;
        });
        userService.editName(3,"song");
    }
}
