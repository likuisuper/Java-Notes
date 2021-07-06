## mybatis对spring的扩展

我们在开发的时候，经常采用的是三层模型，在service中注入dao层（@Autowired注解），这个过程是spring帮我们注入的，而一个dao层要想让spring完成自动注入，应该有3个条件：

* 是一个对象（因为接口中的方法必须要有执行）
* 这个对象实现了dao层中的接口
* 这个对象还要在spring容器中

#### mybatis实现

前2个条件，是由**mybatis来保证的**，mybatis采用JDK动态代理的方式来帮我们实现

~~~java
  protected T newInstance(MapperProxy<T> mapperProxy) {
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }
~~~

我们其实可以手动模拟这个过程：

添加一个UserDao接口

~~~java
public interface UserDao {
	@Select("select * from user")
	void query();

	@Select("select * from user where id=2")
	void queryById();
}
~~~

模拟一个SqlSession:

~~~java
public class MySqlSession {
	public static Object getMapper(Class clazz){
		//拦截UserDao接口或其他Dao接口
		Class[] classes=new Class[]{clazz};
		Object proxy=Proxy.newProxyInstance(MySqlSession.class.getClassLoader(), classes, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				System.out.println("----conn jdbc");
				Select select = method.getAnnotation(Select.class);
				//获取sql语句
				String sql=select.value()[0];
				System.out.println("----exe sql= ---"+sql);
				return null;
			}
		});
		return proxy;
	}
}
~~~

然后进行测试：

~~~java
public class Test {
	public static void main(String[] args) {
		UserDao userMapper = (UserDao) MySqlSession.getMapper(UserDao.class);
		userMapper.queryById();
	}
}
~~~

#### mybatis-spring

但一般mybatis都是和spring集成的，所以需要将这个对象放入spring中，让spring来帮我们管理，而将对象放入spring通常有4种方式，我们就来模拟将我们代理的dao层接口对象放入spring容器中：

##### 1、使用注解，比如@service

这种方法相当于我们前面讲过的将一个类交给了spring来管理，但是这个实例化的过程应该由我们来控制，比如我要将这个对象通过动态代理的方式生成，而spring不应该干预这个过程，所以这种方式不合适

#### 2、手动向spring容器中注册对象

这个我们在前面说过，就是下面这种方式：

~~~java
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac=new AnnotationConfigApplicationContext();
		UserDao userMapper = (UserDao) MySqlSession.getMapper(UserDao.class);
		ac.register(App.class);
		ac.getBeanFactory().registerSingleton("dao",userMapper);
		ac.refresh();

		UserService service = ac.getBean(UserService.class);
		service.query();
	}
~~~

这时候在service中：

~~~java
@Component
public class UserService {
	@Autowired
	UserDao userDao;

	public void query(){
		System.out.println(userDao.query());
	}

	public void queryById(){
		System.out.println(userDao.queryById());
	}
}
~~~

userDao是能够被注入的，运行也没问题，但是这种方法存在一个问题：当有多个对象需要提供给spring容器的时候，每次都要手动提供，很明显，这种方法也不行

##### 3、@Bean注解

比如这样：

~~~java
	@Bean
	public UserDao getUserDao(){
		return (UserDao) MySqlSession.getMapper(UserDao.class);
	}
~~~

也是可以的，但依然存在问题：当有多个接口的时候，每次都得使用Bean注入，太麻烦

##### 4、factoryBean

factoryBean是一个特殊的Bean

* 必须实现FactoryBean
* 能够返回一个bean

当通过类型获取这个bean的时候，拿到的是它本身

~~~java
System.out.println(ac.getBean(MyFactoryBean.class));

//输出
com.cxylk.model3.util.MyFactoryBean@77caeb3e
~~~

当通过名字获取这个bean的时候，比如给这个factoryBean取个名字

~~~java
@Component("mf")
public class MyFactoryBean implements FactoryBean {
	@Override
	public Object getObject() throws Exception {
        //A类没有添加任何注解
		return new A();
	}

	@Override
	public Class<?> getObjectType() {
		return A.class;
	}
}
~~~

获取bean

~~~java
System.out.println(ac.getBean("mf"));

//输出
com.cxylk.model3.util.A@5b275dab
~~~

也就是说，不是给这个FactoryBean取名字，而是给创建的这个bean A取名字

如果想拿到这个FactoryBean，那么在前面加个`&`

~~~java
System.out.println(ac.getBean("&mf"));
~~~

而mybatis就是采用这种方式做的。但是我们在**FactoryBean中不能写死要产生的Bean，而应该动态配置，并且不能加Component注解，原因前面说过，不能让用户来扫描我们这个包**。但是factoryBean要生效，那么它就必须要在spring容器中，怎么把factorybean放入容器中？有3中方式：

* 使用注解比如@Component，这种方法不能传参已经分析过不可取

  ~~~java
  	Class mapperInterface;
  
  	public MyFactoryBean(){
  
  	}
  
  	public MyFactoryBean(Class mapperInterface){
  		this.mapperInterface=mapperInterface;
  	}
  
  	@Override
  	public Object getObject() throws Exception {
  		return MySqlSession.getMapper(mapperInterface);
  	}
  
  	public void setMapperInterface(Class mapperInterface) {
  		this.mapperInterface = mapperInterface;
  	}
  
  	@Override
  	public Class<?> getObjectType() {
  		return mapperInterface;
  	}
  ~~~

  那么这个参数mapperInterface是无法传递的，spring实例化的时候会报错

* 使用xml或者@Bean注解，可以传参，但是不能批量扫描

* 把factorybean对应的beandefinition放到map中

我们在开发中应该都使用过@MapperScan注解，使用它能扫描一个包下的类，而它也是通过第三种方式实现的，我们可以来模拟这个过程。

注意：**如果我们扩展的是BeanFactoryPostProcessor这个接口，那么是不能实现的。因为这个接口只能修改，而不能添加到bdmap中**。

我们将扩展的类实现`ImportBeanDefinitionRegistrar`接口，然后将MyFactoryBean注册进去

~~~java
public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		//这里调用的是myfactorybean的无参构造方法
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MyFactoryBean.class);
		//获取bd
		AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		//因为使用的是无参构造方法，也就是说传不了参数，所以需要手动传递参数
		//找到mapperInterface，看有没有该值，有的话就调用MyFactoryBean的set方法，第二个参数就是set方法需要的参数
		propertyValues.add("mapperInterface", "com.cxylk.model3.dao.UserDao");
		registry.registerBeanDefinition("myFactoryBean",beanDefinition);
	}
}
~~~

最后在App这个类上加上注册，扫描该类；

~~~java
@Import(MyImportBeanDefinitionRegistrar.class)
public class App {...}
~~~

这个时候MyFactoryBean能放入spring容器中，UserDao也能放入spring容器中。

现在是扫描一个bean的情况，但我们可以模拟扫描多个的情况：

~~~java
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {


		//模拟扫描多个bean
		List<Class> list=new ArrayList<>();
		list.add(UserDao.class);
		list.add(BlogDao.class);

		for (Class aClass : list) {
			//这里调用的是myfactorybean的无参构造方法
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MyFactoryBean.class);
			//获取bd
			AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
			MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
			//因为使用的是无参构造方法，也就是说传不了参数，所以需要手动传递参数
			//找到mapperInterface，看有没有该值，有的话就调用MyFactoryBean的set方法，第二个参数就是set方法需要的参数
			propertyValues.add("mapperInterface", aClass);
			registry.registerBeanDefinition(aClass.getSimpleName(),beanDefinition);
		}
	}
~~~

这个时候，我们将App上面的@Import注解换成我们自定义的注解：

~~~java
@MyScan
public class App {...}

@Retention(RetentionPolicy.RUNTIME)
@Import(MyImportBeanDefinitionRegistrar.class)
public @interface MyScan {
}
~~~

这样就达到了mybatis中@MapperScan注解的功能。

我们可以看下@MapperScan注解干了什么事情

~~~java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MapperScannerRegistrar.class)
@Repeatable(MapperScans.class)
public @interface MapperScan {...}
~~~

可以看到，它也添加了一个@Import的注解，参数是`MapperScannerRegistrar`这个类，这个类的位置：

![](https://z3.ax1x.com/2021/07/04/RfkluV.png)

对比我们的实现，可以发现，它有MapperScan，我们有MyScan，它有`MapperScannerRegistrar`，我们有MyImportBeanDefinitionRegistrar，并且`MapperScannerRegistrar`也是实现了`ImportBeanDefinitionRegistrar`这个接口，它里面的实现如下：

~~~java
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AnnotationAttributes mapperScanAttrs = AnnotationAttributes
        .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));
    if (mapperScanAttrs != null) {
      registerBeanDefinitions(importingClassMetadata, mapperScanAttrs, registry,
          generateBaseBeanName(importingClassMetadata, 0));
    }
  }
~~~

我们点进`registerBeanDefinitions`这个方法中，代码比较长，先看第一行代码干了什么事情

~~~java
 void registerBeanDefinitions(AnnotationMetadata annoMeta, AnnotationAttributes annoAttrs,
      BeanDefinitionRegistry registry, String beanName) {

    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
     ...
 }
~~~

我们在模拟的MyImportBeanDefinitionRegistrar类中也有这行代码：

~~~java
//这里调用的是myfactorybean的无参构造方法
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MyFactoryBean.class);
~~~

不同的是，我们传进的参数是实现了FactoryBean的类，而mybatis-spring传进的参数是`MapperScannerConfigurer`，我们看下这个类跟我们的有什么不同

~~~java
public class MapperScannerConfigurer
    implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {...}
~~~

可以看到，它实现了`BeanDefinitionRegistryPostProcessor`这个接口，然后在方法`postProcessBeanDefinitionRegistry`的实现如下：

~~~java
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    if (this.processPropertyPlaceHolders) {
      processPropertyPlaceHolders();
    }

    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
    scanner.setAddToConfig(this.addToConfig);
    scanner.setAnnotationClass(this.annotationClass);
    scanner.setMarkerInterface(this.markerInterface);
    scanner.setSqlSessionFactory(this.sqlSessionFactory);
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
    scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
    scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
    scanner.setResourceLoader(this.applicationContext);
    scanner.setBeanNameGenerator(this.nameGenerator);
    scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
    if (StringUtils.hasText(lazyInitialization)) {
      scanner.setLazyInitialization(Boolean.valueOf(lazyInitialization));
    }
    scanner.registerFilters();
    scanner.scan(
        StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  }
~~~

其中

~~~java
    scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
~~~

这行代码给`ClassPathMapperScanner`这个扫描类的`MapperFactoryBeanClass`属性赋值：

~~~java
 private Class<? extends MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;


 public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
    this.mapperFactoryBeanClass = mapperFactoryBeanClass == null ? MapperFactoryBean.class : mapperFactoryBeanClass;
  }
~~~

我们看下MapperFactory这个类

~~~java
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {...}
~~~

可以看到它实现了`FactoryBean`这个接口，而这个接口我们在前面模拟的实现也实现过，让它来**产生一个mapper接口的代理对象**，那`MapperFactoryBean`是不是也是这么做的呢？

~~~java
 private Class<T> mapperInterface;  

 @Override
  public T getObject() throws Exception {
    return getSqlSession().getMapper(this.mapperInterface);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getObjectType() {
    return this.mapperInterface;
  }
~~~

可以发现，在getObject这个方法中，它返回的就是一个通过JDK动态代理生成的接口对象，那这个mapperInterface是在哪里被赋值的呢？

~~~java
public MapperFactoryBean(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }
~~~

这个构造函数中的mapperInterface是在哪里被赋值的？

在`ClassPathMapperScaner`（mybatis-spring实现Spring的ClassPathBeanDefinitionScanner类的类）的doScan方法中的`processBeanDefinitions`方法有这么一行代码

~~~java
// the mapper interface is the original class of the bean
// but, the actual class of the bean is MapperFactoryBean
definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
~~~

**就是通过使用构造函数的方式注入了一个接口的值**

然后

~~~java
definition.setBeanClass(this.mapperFactoryBeanClass);
~~~

强行将该接口的类型转换为FactoryBean类型的。