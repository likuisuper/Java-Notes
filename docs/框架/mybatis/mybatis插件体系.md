## 插件体系

按照mybatis官网的说法，我们可以在映射语句执行过程中的某一点进行拦截调用。默认情况下，mybatis运行使用插件来拦截的方法调用包括：

- Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
- ParameterHandler (getParameterObject, setParameters)
- ResultSetHandler (handleResultSets, handleOutputParameters)
- StatementHandler (prepare, parameterize, batch, update, query)

可以看到，这四个类都是mybatis中的顶级接口，可以说是贯穿了整个mybatis，也是插件的一个入口：

![](https://z3.ax1x.com/2021/04/22/cLVknK.png)

## 底层机制

![](https://z3.ax1x.com/2021/04/22/cLVvKP.png)

所以我们在需要获取Executor、StatementHandler、ParameterHandler、ResultSetHandler等组件时最好是通过Configuration这个“大管家”来获取。

## 实现自动分页

#### 分析

我们在平常开发中应该都用过pageHelper分页插件，它的实现原理是什么呢？我们通过自己实现一个分页插件就知道其中的原理了。

插件也是属于一种中间件，所以，在开发这个插件的时候需要考虑这个插件的功能特性：

* 易用性：不需额外配置，参数带上page即可分页
* 不对使用场景作假设：支持多场景调用
* 友好性：不符合分页条件下作出友好提示

经过上面的分析，可以知道插件的入口有4个，那么应该选择哪个来拦截呢？首先可以排除两个：一个是ParameterHandler，也就是参数处理器，因为它并不能改变我们的sql语句；还有一个就是ResultSetHandler，这个是结果集处理器，既然都能对结果集做处理了，那么肯定已经执行过查询了，那么拦截就没什么意义了。最后剩下Executor和StatementHandler这两个入口。属性mybatis执行体系的话，应该都知道，首先会经过二级缓存这个执行器，然后是一级缓存，所以要拦截Executor的话，还得去实现处理二级缓存的逻辑。为了简单，我们拦截Executor下一个组件StatementHandler的prepare方法。

#### 实现

当我们使用sql语句实现分页的时候，是通过limit和offset来实现的，而自动分页的原理就是对查询的sql添加这两个参数，所以构建一个实体类来封装分页所需要的一些参数：

~~~java
public class Page {
    //总的条数
    private int total;

    //每页条数
    private int size;

    //页数
    private int index;
    
    public int getOffset(){
        return size*(index-1);
    }
    //省略get set方法
}
~~~

然后实现对目标方法拦截，通过@InterCepts可以定义需要拦截的类，方法，参数，然后实现Interceptor接口：

~~~java
@Intercepts(@Signature(type = StatementHandler.class,
        method = "prepare",args = {Connection.class,Integer.class}))
public class PageInterceptor implements Interceptor {
    /**
     * 对目标方法进行拦截
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //如果什么都不做，就是下面的操作，等价于proceed方法
//        return invocation.getMethod().invoke(invocation.getTarget(),invocation.getArgs());

        //1.判断是否符合分页条件，即参数是否是分页参数
        StatementHandler statementHandler= (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        Object parameterObject = boundSql.getParameterObject();
        //当参数是1个的时候，parameterObject的值是参数名;当参数是多个的时候，parameterObject是一个map
        //如果参数添加了@Param注解，那么key就是注解值，value是参数名，否则key是arg0，arg1这种形式
        Page page=null;
        if(parameterObject instanceof Page){
            page= (Page) parameterObject;
        }else if(parameterObject instanceof Map){
            page=(Page)((Map) parameterObject).values().stream().filter(value->value instanceof Page).findFirst().orElse(null);
        }
        if(page==null){
            return invocation.proceed();
        }

        //2.设置总行数
        page.setTotal(getCount(invocation));

        //3.改变sql语句，添加offset和limit
        String newSql=String.format("%s limit %d offset %d",boundSql.getSql(),page.getSize(),page.getOffset());
        //通过metaObject来改变boundSql中原来的sql
        SystemMetaObject.forObject(boundSql).setValue("sql",newSql);
        //拦截器放行
        return invocation.proceed();
    }

    /**
     * 将拦截器插入目标对象。没有这一步的话会报错
     * @param target 目标对象。这个方法会执行四次，target先后代表四个类，也就是上面提到的四个类。当target为StatementHandler的子类时，才会执行上面的拦截方法
     * @return
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target,this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    private int getCount(Invocation invocation) throws SQLException {
        int count=0;
        StatementHandler statementHandler= (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql=String.format("select count(*) from ( %s) as _page",boundSql.getSql());
        //获取参数中的连接
        Connection connection = (Connection) invocation.getArgs()[0];
        //获取statement
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            //设置参数，否则当查询语句加了参数后会报错：没有给该参数指定值
            statementHandler.getParameterHandler().setParameters(statement);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                //获取上面sql中的第一个参数值，也就是count
                count = resultSet.getInt(1);
            }
        }
        return count;
    }
}
~~~

其实就是做了三步：

* 判断是否符合分页条件，这里是以参数是否是Page判断的
* 获取总的条数
* 改变原有sql，添加limit和offset

然后需要在mybatis配置文件中配置：

~~~xml
    <plugins>
        <plugin interceptor="cxylk.mybatis.plugin.PageInterceptor"/>
    </plugins>
~~~

#### 测试

一个中间件的开发，测试代码往往需要比实现的代码还要多。这里跳过简单的测试，主要测试二级缓存能不能命中：

~~~java
/**
     * 测试是否能够命中二级缓存。注意，mapper接口和xml文件都需要开启缓存空间，因为selectByUserPage方法是映射到xml的
     */
    @Test
    public void pageTest2(){
        {
            SqlSession sqlSession1 = factory.openSession(ExecutorType.REUSE);
            UserMapper mapper1 = sqlSession1.getMapper(UserMapper.class);
            Page page = new Page(5, 1);
            User user = new User();
            user.setName("lk01");
            List<User> users = mapper1.selectByUserPage(user, page);
            System.out.println("总行数：" + page.getTotal());
            System.out.println("实际查询条数：" + users.size());
            //关闭，二级缓存才有效（底层就是做了commit操作）
            sqlSession1.close();
        }
        {
            SqlSession sqlSession2 = factory.openSession(ExecutorType.REUSE);
            UserMapper mapper1 = sqlSession2.getMapper(UserMapper.class);
            Page page = new Page(5, 1);
            User user=new User();
            user.setName("lk01");
            List<User> users = mapper1.selectByUserPage(user, page);
            //会走缓存，命中率为0.5
            System.out.println("总行数："+page.getTotal());
            System.out.println("实际查询条数："+users.size());
            sqlSession2.close();
        }
    }
~~~

上面写了两个代码块，主要是懒，不想多写个方法。。然后测试，发现二级缓存可以命中。

#### 原理

上面就是一个简单分页插件的实现，原理如下：

![](https://z3.ax1x.com/2021/04/22/cLnUaT.png)

