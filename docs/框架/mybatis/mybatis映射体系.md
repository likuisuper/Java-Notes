## MetaObject

MetaObject是mybatis中一个很强大的反射工具类，可以对它设置值以及获取值。在它内部使用到了一个**分词器PropertyTokenizer**，这是个工具类，我们在开发中如果有需要的话可以直接拿来用。它的作用就是将多个以点号连接的查询字符串分隔，如果是list[0]这种列表形式查询的话，那么还会拿到这个索引0。

通过下面的例子来了解metaObject的实现原理：

~~~java
public class MetaObjectDemo {
    @Test
    public void test1(){
        Object blog=Mock.newBlog();
        Configuration configuration=new Configuration();
        MetaObject metaObject=configuration.newMetaObject(blog);
        System.out.println(metaObject.getValue("comments[0].user.name"));
    }
~~~

当调用到getValue的时候，会进入到该方法：

~~~java
  public Object getValue(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //判断是否存在子属性
    if (prop.hasNext()) {
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return null;
      } else {
        return metaValue.getValue(prop.getChildren());
      }
    } else {
      return objectWrapper.get(prop);
    }
  }
~~~

首先分词器PropertyTokenizer会将comments[0].user.name分成下面几个部分：

1.name="comments"

2.indexName="comments[0]"

3.index="0"

4.children="user.name"

prop.hasNext方法就是判断children是否为空，这里不为空，所以会进入metaObjectForProperty方法：

~~~java
  public MetaObject metaObjectForProperty(String name) {
    Object value = getValue(name);
    return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  }
~~~

可以看到，这里又会调用getValue方法，这时候传入的参数name="comments[0]"，没有子元素，所以会调用objectWrapper.get方法，该方法的实现在BeanWrapper类中：

~~~java
  @Override
  public Object get(PropertyTokenizer prop) {
    if (prop.getIndex() != null) {
      Object collection = resolveCollection(prop, object);
      return getCollectionValue(prop, collection);
    } else {
      return getBeanProperty(prop, object);
    }
  }
~~~

在这个方法中会判断分词器中的name是否存在索引：

1.如果存在，那么说明它是个集合查询，调用resolveCollection方法(BaseWrapper类中)

~~~java
  protected Object resolveCollection(PropertyTokenizer prop, Object object) {
    if ("".equals(prop.getName())) {
      return object;
    } else {
      return metaObject.getValue(prop.getName());
    }
  }
~~~

也就是返回metaObject中获取到的值，这个值是所有，比如这里是list列表

然后调用getCollectionValue方法(还是在BaseWrapper类中)基于索引来获取值，比如list[0]

~~~java
  protected Object getCollectionValue(PropertyTokenizer prop, Object collection) {
    if (collection instanceof Map) {
      return ((Map) collection).get(prop.getIndex());
    } else {
      int i = Integer.parseInt(prop.getIndex());
      if (collection instanceof List) {
        return ((List) collection).get(i);
      } else if (collection instanceof Object[]) {
          ...
      }
      ...
    }
~~~

2.然后会以递归的方式依次获取comments，user，name的值，这些都不是集合查询，所以会调用getBeanProperty方法(BeanWrapper类中)

~~~java
  private Object getBeanProperty(PropertyTokenizer prop, Object object) {
    try {
      //反射获取该属性的方法
      Invoker method = metaClass.getGetInvoker(prop.getName());
      try {
        return method.invoke(object, NO_ARGUMENTS);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable t) {
      throw new ReflectionException("Could not get property '" + prop.getName() + "' from " + object.getClass() + ".  Cause: " + t.toString(), t);
    }
  }
~~~

可以看到，**首先通过属性名通过反射拿到getting方法**，比如getName，然后用invoke进行调用。

反射是通过MetaClass中的getGetInvoke方法实现的：

~~~java
  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }
~~~

reflector是mybatis封装的反射类。

上面出现了很多概念，首先是objectWrapper，baseWrapper，beanWrapper三者之间的关系：

![](https://z3.ax1x.com/2021/04/02/cZocdA.png)

ObjectWrapper是个接口，封装了一些通用方法，比如get,set,add方法，而BaseWrapper主要是用来解决集合查询的，BeanWrapper就是用来解决单个bean的查询。

还有他们和metaClass，reflector之间的关系，以及每个类的作用：

![](https://z3.ax1x.com/2021/04/02/cZTFF1.png)

将上面的整个流程用图的形式表示出来就是下面这个样子：

![](https://z3.ax1x.com/2021/04/02/cZHvSP.png)

## 循环依赖流程

首先要思考为什么会出现循环依赖？就是当我们的查询是嵌套子查询的时候，比如如下sql:

~~~xml
<resultMap id="blogMap" type="blog">
        <result column="title" property="title"/>
        <collection property="comments" column="id" select="selectCommentsByBlogId"/>
    </resultMap>
    <resultMap id="commentsMap" type="comment">
        <association property="blog" column="blog_id" select="selectBlogById"/>
    </resultMap>
    <select id="selectCommentsByBlogId" resultMap="commentsMap">
        select * from comment where blog_id=#{blogId}
    </select>
    <select id="selectBlogById" resultMap="blogMap">
        select * from blog where id=#{id}
    </select>
~~~

当我们执行selectBlogById的时候，会去查询评论，而评论里面又会去查询博客，这就是嵌套子查询。如果不解决这种嵌套子查询的话，那么就会陷入一个死循环中。

那循环依赖是怎么解决的呢？通过debug源码的方式来了解其中的原理。

debug以下代码：

~~~java
    @Test
    public void test(){
        Blog blog = sqlSession.selectOne("selectBlogById",1);
        System.out.println(blog);
    }
~~~

进入到BaseExecutor中的query方法，前面的代理，会话以及二级缓存这些这里就略过了。

~~~java
//第一个query方法
@Override
  public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    if (queryStack == 0 && ms.isFlushCacheRequired()) {
      clearLocalCache();
    }
    List<E> list;
    try {
      queryStack++;
      //获取缓存，判断缓存中是否有值
      list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
      if (list != null) {
        handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
      } else {
        //没有值就从数据库查询
        list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
      }
    } finally {
      queryStack--;
    }
    if (queryStack == 0) {
      for (DeferredLoad deferredLoad : deferredLoads) {
        deferredLoad.load();
      }
      // issue #601
      deferredLoads.clear();
      if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
        // issue #482
        clearLocalCache();
      }
    }
    return list;
  }

~~~

第一次进来的时候，sql是select * from blog这条。

这个方法在前面的源码分析中有讲到过，所以这里主要讲以前没有说的地方，就是**queryStack和延迟加载**。

1.首先说下queryStack是什么东西，拿上面的sql语句为例，当出现嵌套子查询时：

![](https://z3.ax1x.com/2021/04/01/cZeyWt.png)

可以看到，每嵌套一个字查询，queryStack就会加1。

2.延迟加载，下面会讲到具体实现。

#### 查询数据库

回到上面代码，因为第一次是不存在缓存的，所以会去数据库查询：

~~~java
放入//第一个queryFromDatabase方法
private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    List<E> list;
    //这里首先会填充一个占位符，就是为了解决嵌套子查询的
    localCache.putObject(key, EXECUTION_PLACEHOLDER);
    try {
      //具体的查询实现
      list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    } finally {
      //查询过后，将占位符移除
      localCache.removeObject(key);
    }
    //将从数据库查询到的结果放入缓存
    localCache.putObject(key, list);
    if (ms.getStatementType() == StatementType.CALLABLE) {
      localOutputParameterCache.putObject(key, parameter);
    }
    return list;
  }

~~~

再往下会经过很多方法，比如结果集的处理，填充属性等。我们直接跳到获取属性值这里(DefaultResultSetHandle类中)

~~~java
private Object getNestedQueryMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    //获取子查询的id，也就是select * from comments这条
    final String nestedQueryId = propertyMapping.getNestedQueryId();
    final String property = propertyMapping.getProperty();
    final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
    final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
    final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rs, propertyMapping, nestedQueryParameterType, columnPrefix);
    Object value = null;
    if (nestedQueryParameterObject != null) {
      final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
      final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT, nestedBoundSql);
      final Class<?> targetType = propertyMapping.getJavaType();
      //是否命中一级缓存？
      if (executor.isCached(nestedQuery, key)) {
        //是，那么进行延迟加载
        executor.deferLoad(nestedQuery, metaResultObject, property, key, targetType);
        value = DEFERRED;
      } else {
        //否
        final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, key, nestedBoundSql);
        //判断是否是懒加载
        if (propertyMapping.isLazy()) {
          //如果是，那就执行懒加载
          lazyLoader.addLoader(property, metaResultObject, resultLoader);
          value = DEFERRED;
        } else {
          //否则执行实时加载
          value = resultLoader.loadResult();
        }
      }
    }
    return value;
  }
~~~

方法名就叫获取嵌套查询映射值。这里面会首先做一些准备工作，比如获取MapperStatement，获取动态sql，创建缓存key等。

#### 子查询

**第一次进来的时候，select * from comments这条sql显然是没有缓存的，所以不会进行延迟加载，并且也没有在配置文件中配置懒加载，所以这个时候会去实时加载**：

在ResultLoader类中：

~~~java
public Object loadResult() throws SQLException {
    List<Object> list = selectList();
    resultObject = resultExtractor.extractObjectFromList(list, targetType);
    return resultObject;
  }
  
  //重点看该方法
  private <E> List<E> selectList() throws SQLException {
    Executor localExecutor = executor;
    if (Thread.currentThread().getId() != this.creatorThreadId || localExecutor.isClosed()) {
      localExecutor = newExecutor();
    }
    try {
      //执行query方法
      return localExecutor.query(mappedStatement, parameterObject, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER, cacheKey, boundSql);
    } finally {
      if (localExecutor != executor) {
        localExecutor.close(false);
      }
    }
  }

~~~

可以看到，在selectList方法中，又会去执行BaseExecutor的query方法（假设这里是第二个query方法）。

但是这个时候的参数就发生了变化，mappedStatement的id是selectCommentsByBlogId，对应的sql就是select * from comments这条。

这个时候query方法中的queryStack就会从原来的1变成2，并且select * from comments这条sql也不存在缓存，所以这时又会去执行queryFromDatabase（假设这里是第二个queryFromDatabase方法）。

在queryFromDatabase中，一样会给当前key，也就是select * from comments这条sql填充缓存占位符，然后又到了getNestedQueryMappingValue这个方法。

还是和前面一样做准备工作，此时的nestedQueryId=selectBlogById，又回到了最开始的那条查询语句。

~~~java
if (executor.isCached(nestedQuery, key)) {
        executor.deferLoad(nestedQuery, metaResultObject, property, key, targetType);
        value = DEFERRED;
      } else {
        final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, key, nestedBoundSql);
        if (propertyMapping.isLazy()) {
          lazyLoader.addLoader(property, metaResultObject, resultLoader);
          value = DEFERRED;
        } else {
          value = resultLoader.loadResult();
        }
      }
~~~

这个时候executor.isCached是返回true的，因为在第一次查询的时候给它填充了缓存占位符的。所以此时会进入deferLoad方法中进入延迟加载

~~~java
public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    DeferredLoad deferredLoad = new DeferredLoad(resultObject, property, key, localCache, configuration, targetType);
    if (deferredLoad.canLoad()) {
      deferredLoad.load();
    } else {
      deferredLoads.add(new DeferredLoad(resultObject, property, key, localCache, configuration, targetType));
    }
  }
~~~

首先会判断是否可以进行加载，也就是当前key在缓存中有值，并且值不是缓存占位符EXECUTION_PLACEHOLDER，因为不可能把一个占位符进行加载的。

~~~java
    public boolean canLoad() {
      return localCache.getObject(key) != null && localCache.getObject(key) != EXECUTION_PLACEHOLDER;
    }
~~~

很明显，此时的value就是占位符，所以不能进行加载，会将它放入deferredLoads中，它是一个ConcurrentLinkedQueue队列。

~~~java
deferredLoads.add(new DeferredLoad(resultObject, property, key, localCache, configuration, targetType));
~~~

**注意这时候放入的是哪个sql，不要搞混了**：mappedStatement的id是selectBlogById，对应的sql是select * from blog。

回到上面的第二个queryFromDatabase方法，也就是selectCommentsByBlogId对应的查询。

~~~java
//1.首先移除selectCommentsByBlogId对应的值，也就是前面设置的占位符
localCache.removeObject(key);

//2.填充数据库查到的值，list中存放了查询到的值
localCache.putObject(key, list);
~~~

回到第二个query方法，此时queryStack--就变成了1。

**然后这时回到ResultLoader中的selectList方法，返回查询结果**：

~~~java
return localExecutor.query(mappedStatement, parameterObject, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER, cacheKey, boundSql);
~~~

也就是说，评论这条sql就查询完了。

一直返回到第一个queryFromDatabase方法中，也就是mapperStatement的id=selectBlogById，对应sql为select * from blog这个方法中。

~~~java
//1.首先移除selectBlogById对应的占位符
localCache.removeObject(key);
////2.填充数据库查到的值，list中存放了查询到的值
localCache.putObject(key, list);
~~~

向上返回到第一个query方法，这个时候queryStack=0，然后会执行下面的方法

~~~java
    if (queryStack == 0) {
      //遍历deferredLoads,其中key就是上面放入的selectBlogById
      for (DeferredLoad deferredLoad : deferredLoads) {
        //然后进行加载
        deferredLoad.load();
      }
      // issue #601
      deferredLoads.clear();
      if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
        // issue #482
        clearLocalCache();
      }
    }
    return list;
~~~

load方法：

~~~java
public void load() {
      @SuppressWarnings("unchecked")
      // we suppose we get back a List
      List<Object> list = (List<Object>) localCache.getObject(key);
      Object value = resultExtractor.extractObjectFromList(list, targetType);
      //resultObject是一个meatObject对象
      resultObject.setValue(property, value);
    }
~~~

流程很简单，就是从缓存中获取我们前面放入的从数据库查询到的对应selectBlogById的值，然后将它放入mateObject中。

#### 总结

当出现嵌套子查询的时候就会存在循环依赖的问题，解决的方法就是通过queryStack，一级缓存，延迟加载来解决。**这也是为什么一级缓存要一直打开的原因，如果关闭了，那么当出现嵌套子查询的时候就会出现死循环。虽然一级缓存的生命周期很短，但他还是很有用的**。至于上面出现的懒加载，我们后面再深入分析。最后通过一张图来加深理解：

![](https://z3.ax1x.com/2021/04/02/cZRqgJ.png)