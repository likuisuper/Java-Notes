package cxylk.test.design.proxy;

/**
 * @Classname UserServiceProxy
 * @Description 代理类
 * @Author likui
 * @Date 2021/2/1 15:35
 **/
public class UserServiceProxy implements UserService{
    private UserService userService;

    public UserServiceProxy(UserService userService){
        this.userService=userService;
    }

    @Override
    public void editName(int id, String name) {
        System.out.println(String.format("修改用户名称 id:%d,name:%s",id,name));
        this.userService.editName(id,name);
    }
}
