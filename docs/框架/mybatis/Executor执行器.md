## 二级缓存

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