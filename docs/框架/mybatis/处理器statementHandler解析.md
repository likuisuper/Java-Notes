## StatementHandler执行流程

simleExecutor的duQuery方法

~~~java
  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.query(stmt, resultHandler);
    } finally {
      closeStatement(stmt);
    }
  }
~~~

prepareStatement方法主要做了两件事：

* 创建statement。具体代码在BaseStatementHandler方法中

  ~~~java
    @Override
    public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
      ErrorContext.instance().sql(boundSql.getSql());
      Statement statement = null;
      try {
        statement = instantiateStatement(connection);
        setStatementTimeout(statement, transactionTimeout);
        setFetchSize(statement);
        return statement;
      } catch (SQLException e) {
        closeStatement(statement);
        throw e;
      } catch (Exception e) {
        closeStatement(statement);
        throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
      }
    }
  ~~~

  instantiateStatement方法是个抽象方法，具体由子类来创建。

* 设置参数。hander.query方法最后会调用到DefaultParameterHandler类的setParameters方法

## 参数处理流程

怎么将javaBean转换成sql字段，分2种情况

![](https://z3.ax1x.com/2021/03/25/6XJuvR.png)

#### 参数转换

首先要说下这个ParamNameResolver类，它的功能就是参数转换。我们的sql执行会通过MapperMethod类的execute方法然后进入到上面这个类的getNamedParams方法

~~~java
public Object getNamedParams(Object[] args) {
    final int paramCount = names.size();
    if (args == null || paramCount == 0) {
      return null;
    } else if (!hasParamAnnotation && paramCount == 1) {
      return args[names.firstKey()];
    } else {
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        param.put(entry.getValue(), args[entry.getKey()]);
        // add generic param names (param1, param2, ...)
        final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
        // ensure not to overwrite parameter named with @Param
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
  }
~~~

这个names是一个SortedMap<Integer,String>，key就是参数的顺序，从0开始，String就是arg0，arg1...(通过反射得到的)

1.从上面源码可以看到，当参数是一个并且没有@Param注解的情况下，它直接返回参数的值。

2.当有多个参数，或者设置了@Param注解后，会遍历names这个map，假设有两个参数，第一个是name，并且添加了@Param注解，第二个参数是age，没有添加注解，那么这时候names的值就是：

~~~java
0->"name"，1->"arg1"。
~~~

第一次遍历，会将name作为Key，arg[0]也就是传递的参数，假如这里是"lk"，作为value放进param这个map中。然后生成一个参数名称，从param1开始。**为了确保不会覆盖以@Param注解命名的参数**，会判断names中的value是否包含生成的参数名param1，如果不包含的话，就将param1作为key，参数值为value

再放进param这个map中。

所以最终的param中会有四个值：

~~~java
"name"->"lk","param1"->"lk","arg1"->12，"param2"->12
~~~

总结下：**arg0,arg1是基于反射来的，而param,param2是基于顺序来的，name是添加了注解后得到的**

#### 参数映射：

经过前面的参数转换后，会在PreparedStatementHandler类中进入到DefaultParameterHandler的setParameters方法，看下源码实现：

~~~java
 @Override
  public void setParameters(PreparedStatement ps) {
    ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (parameterMappings != null) {
      for (int i = 0; i < parameterMappings.size(); i++) {
        ParameterMapping parameterMapping = parameterMappings.get(i);
        if (parameterMapping.getMode() != ParameterMode.OUT) {
          Object value;
          String propertyName = parameterMapping.getProperty();
          if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
            value = boundSql.getAdditionalParameter(propertyName);
          } else if (parameterObject == null) {
            value = null;
          } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            value = parameterObject;
          } else {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            value = metaObject.getValue(propertyName);
          }
          TypeHandler typeHandler = parameterMapping.getTypeHandler();
          JdbcType jdbcType = parameterMapping.getJdbcType();
          if (value == null && jdbcType == null) {
            jdbcType = configuration.getJdbcTypeForNull();
          }
          try {
            typeHandler.setParameter(ps, i + 1, value, jdbcType);
          } catch (TypeException | SQLException e) {
            throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
          }
        }
      }
    }
  }
~~~

在boundSql中，就能拿到我们前面经过参数转换后的参数，就是下面的property

![](https://z3.ax1x.com/2021/03/25/6Xwv1U.png)

拿到property的值后，通过MetaObject这个类就能拿到对应的value了，MetaObject是个很强大的类，后面再讨论它。

图中的ParameterMapping还有typeHandler和jdbcType这些属性。

#### 参数赋值

typeHandler会根据参数的不同类型来进行赋值。比如这里会把当前类型处理成String类型。然后将我们设置的参数值赋值给对应的参数。

## 结果集处理

![](https://z3.ax1x.com/2021/03/25/6XyZmF.png)

关于中间这个ResultContext，可以通过下面的代码来理解它的作用：

~~~java
	@Test
    public void testResultContext(){
        List<Object> list=new ArrayList<>();
        ResultHandler handler=new ResultHandler() {
            @Override
            public void handleResult(ResultContext resultContext) {
                //当记录=1的时候就stop，所以结果只有1条
                if(resultContext.getResultCount()==1){
                    resultContext.stop();
                }
                list.add(resultContext.getResultObject());
            }
        };
        sqlSession.select("cxylk.mybatis.UserMapper.selectById3",handler);
        System.out.println(list.size());
    }
~~~

ResultSetHandler是对结果集进行处理的，而ResultHandler是用来存放已经解析好的结果的，这两个不要搞混淆了。