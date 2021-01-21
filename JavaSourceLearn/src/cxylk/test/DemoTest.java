package cxylk.test;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @Classname DemoTest
 * @Description TODO
 * @Author likui
 * @Date 2021/1/21 14:45
 **/
public class DemoTest {
    @Test
    public void test(){
        Map<String,String> map=new HashMap<>();
        map.put("lk","xs");
        System.out.println(map.get("lk"));
    }
}
