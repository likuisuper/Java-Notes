## Mybatis执行流程

![](https://s3.ax1x.com/2021/03/17/6gANOx.png)

各执行器之间的关系

![](https://s3.ax1x.com/2021/03/17/6gAwTO.png)

## SimpleExecutor

简单执行器，也是mybatis默认的执行器

## CachingExecutor

CachingExecutor就是常说的二级缓存，它实现了Executor执行器，然后在内部委托了Executor，这样，它不仅可以使用Executor中的所有方法，还能自己进行扩展，添加自己的功能，这种设计模式就是**装饰器模式**。

~~~java
public class CachingExecutor implements Executor {

  private final Executor delegate;
    
  ...
}
~~~

来看下二级缓存使用的例子：

~~~java
   @Test
    public void cacheExecutorTest() throws SQLException {
        //BaseExecutor
        Executor executor=new BatchExecutor(configuration,jdbcTransaction);
        //装饰器模式
        Executor cacheExecutor=new CachingExecutor(executor);
        //mapper文件加上CacheNamespace注解
        cacheExecutor.query(ms,1,RowBounds.DEFAULT,Executor.NO_RESULT_HANDLER);
        cacheExecutor.commit(true);
        //二级缓存必须要提交才会生效
        cacheExecutor.query(ms,1,RowBounds.DEFAULT,Executor.NO_RESULT_HANDLER);
    }
~~~

在使用query方法查询前，要给Mapper接口添加@CacheNamespace注解，这时候这行的话缓存命中为0，

**二级缓存必须要提交才会生效**，所以当我们提交后在执行查询，缓存就能命中了。

## ReuseExecutor

可重用执行器，对相同的sql语句进行重复使用。比如下面的例子：

有两个相同的sql语句

~~~java
    @Select({"select * from user where id=#{0}"})
    User selectById(Integer id);


    @Select({"select * from user where id=#{0}"})
    User selectById2(Integer id);
~~~

然后使用可重用执行器查询

~~~java
    @Test
    public void sessionByReuseTest(){
        //指定执行器为可重用
        SqlSession sqlSession=sqlSessionFactory.openSession(ExecutorType.REUSE,true);
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        //执行两个相同的sql语句，只会编译一次
        mapper.selectById(1);
        mapper.selectById2(1);
    }
~~~

结果如下：

~~~java
Preparing: select * from user where id=? 
20:32:38,109 DEBUG cxylk.mybatis.UserMapper.selectById:143 - ==> Parameters: 1(Integer)
20:32:38,137 DEBUG cxylk.mybatis.UserMapper.selectById:143 - <==      Total: 1
20:32:38,141 DEBUG cxylk.mybatis.UserMapper.selectById:143 - ==> Parameters: 1(Integer)
20:32:38,145 DEBUG cxylk.mybatis.UserMapper.selectById:143 - <==      Total: 1

~~~

可以看到Preparing只出现了一次，说明只编译了一次。

现在将第一个sql语句改变下

~~~java
    @Select({"select * from user where id=#{0} and 1=1"})
    User selectById(Integer id);
~~~

输出结果

~~~java
20:37:23,278 DEBUG cxylk.mybatis.UserMapper.selectById:143 - ==>  Preparing: select * from user where id=? and 1=1 
20:37:23,314 DEBUG cxylk.mybatis.UserMapper.selectById:143 - ==> Parameters: 1(Integer)
20:37:23,338 DEBUG cxylk.mybatis.UserMapper.selectById:143 - <==      Total: 1
20:37:23,342 DEBUG cxylk.mybatis.UserMapper.selectById2:143 - ==>  Preparing: select * from user where id=? 
~~~

可以看到进行了两次编译。

如果两个sql语句一样，但是参数不一样，它们还是只会编译一次。

原理也很简单，ReuseExecutor维护了一个map

~~~java
private final Map<String, Statement> statementMap = new HashMap<>();
~~~

其中key就是sql语句，Statement是java.sql下的。当执行preparedStatement方法时，首先会判断当前map是否存在，如果存在并且当前sql的连接没有被关闭，那么就根据当前的sql获取对应的statement，否则新建连接，并且会放入到map中。具体源码在ReuseExecutor中。

## BatchExecutor

批处理执行器，对**增删改**操作进行批处理操作，并且要执行flushStatements后操作才会生效。

有下面这里例子来说明该执行器的特性：

~~~java
public void sessionByBatchTest(){
        //指定执行器为批处理
        SqlSession sqlSession=sqlSessionFactory.openSession(ExecutorType.BATCH,true);
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User user=new User();
        user.setAge(20);
        user.setName("lk01");
        //1.mapperstatement相同  2.sql相同  3.必须是连续的， 这样才会采用同一个jdbc statement
        mapper.setName(1,"lk0");//单独采用一个statement
        mapper.addUser(user);
        mapper.addUser(user);
        mapper.addUser(user);//这三个会采用同一个statement
        mapper.setName(2,"lk1");
        //必须要执行flushStatement才会生效
        List<BatchResult> batchResults = sqlSession.flushStatements();
        System.out.println(batchResults.size());
    }
~~~

主要来测试执行setName,addUser这些sql语句，批处理的返回的结果应该是多少。

首先说下mapperStatement是什么，它就是mapper接口中我们定义的方法名，而sql就是我们编写的sql。

输出结果是3，也就是说执行了5个sql语句，但是返回的结果是3。

原因就是三个addUser操作是连续的，又是同一个mapperStatement和sql语句(虽然参数不一样)，所以这三个插入操作其实用的是同一个statement。我们可以通过源码来了解其中的原理。

~~~java
public class BatchExecutor extends BaseExecutor {

  private final List<Statement> statementList = new ArrayList<>();
  private final List<BatchResult> batchResultList = new ArrayList<>();
  private String currentSql;
  private MappedStatement currentStatement;
    
  ....
  //不管是修改还是插入，但是update操作，所以都会进入这个方法
  @Override
  public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
    final Configuration configuration = ms.getConfiguration();
    final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT, null, null);
    final BoundSql boundSql = handler.getBoundSql();
    final String sql = boundSql.getSql();
    final Statement stmt;
    //1.第一次进来，sql和ms都不为空，而currentsql和currentStatement都为空，不会走这里
    //2.第二次进来，sql为add，而currentSql经过上一步变为了update,两者不相等，不走这里
    //3.第三次进来，sql为add，而currentSql经过上一步变为add，ms同理，所以这时会走这里、
    //....
    if (sql.equals(currentSql) && ms.equals(currentStatement)) {
      //此时的list大小为2，last=1
      int last = statementList.size() - 1;
      //获取第二个statement,也就是addUser方法那个
      stmt = statementList.get(last);
      applyTransactionTimeout(stmt);
      handler.parameterize(stmt);//fix Issues 322
      BatchResult batchResult = batchResultList.get(last);
      batchResult.addParameterObject(parameterObject);
    } else {
      //首先获取连接
      Connection connection = getConnection(ms.getStatementLog());
      stmt = handler.prepare(connection, transaction.getTimeout());
      handler.parameterize(stmt);    //fix Issues 322
      //1.将sql赋值给currentSql,当前sql变为update
      //2.将sql赋值给currentSql,当前sql变为add
      currentSql = sql;
      //同上操作
      currentStatement = ms;
      //放入list中
      statementList.add(stmt);
      //放入结果列表中
      batchResultList.add(new BatchResult(ms, sql, parameterObject));
    }
    handler.batch(stmt);
    return BATCH_UPDATE_RETURN_VALUE;
  }
}
~~~

可以看到上面有一个判断条件

~~~java
sql.equals(currentSql) && ms.equals(currentStatement
~~~

为什么要做这么一个判断呢？就是为了**确保当前语句的执行顺序**。

另外，在上面的操作中，比如currentSql=sql;currentStatement=ms，这在多线程环境中是不安全带，所以可以得出结论，**无论是Executor还是SqlSession都不能跨线程使用。**

## 总结

执行器的种类有：基础执行器、简单执行器、可重用执行器和批处理执行器，此外通过装饰器的形式添加了一个缓存执行器。对应功能包括缓存处理，事务管理，重用处理以及批处理。这些都是sql执行中的共性，而执行器存在的意义就是去处理这些共性，所以才需要有Executor这个组件。