package cxylk.test.design.proxy;

/**
 * @Classname UserServiceImpl
 * @Description TODO
 * @Author likui
 * @Date 2021/2/1 15:34
 **/
public class UserServiceImpl implements UserService {
    @Override
    public void editName(int id, String name) {
        System.out.println("修改名称成功...");
    }
}
