#### 单例池

spring容器：spring中各种组件组合在一起的，是一个抽象的概念

而我们获取的bean是从单例池中获取的，单例池也是spring容器的一部分，这个单例池是`singleObjects`，它是一个map

在前面说spring容器的扫描原理的时候，我们知道，当执行完refresh方法中的`invokeBeanFactoryPostProcessors(beanFactory);`

后，会将bean扫描进容器bdmp中。

#### 测试环境

现在有两个测试类A和B，它们都加了注解

~~~java
@Component
public class X {
	public X(){
		System.out.println("create x");
	}
}

@Component
public class Y {
	public Y(){
		System.out.println("create y");
	}
}
~~~

我们看这个类的构造方法是在什么时候执行的。当执行完refresh方法中的

~~~java
// Instantiate all remaining (non-lazy-init) singletons.
finishBeanFactoryInitialization(beanFactory);
~~~

该方法时，控制台就会打印构造函数中的语句，说明在这个函数中完成了对象的创建和初始化。在这里要注意java对象和spring bean的区别：**java对象不一定是spring bean，它不一定存在spring容器中，它没有spring bean的生命周期，但是spring bean一定是java对象，它有生命周期**。

现在我们想知道执行完这个方法后，这两个对象是否已经变成了bean，那么通过上面所说的单例池，可以到singleObjects中查看其中是否存在：

![](https://z3.ax1x.com/2021/07/06/RTMEo4.png)

两个对象已经存在单例池中。

所以接下来，我们重点看`finishBeanFactoryInitialization(beanFactory);`这个方法到底干了什么。

#### finishBeanFactoryInitialization

在开始调试前，为了更好的了解bean的生命周期，我们将上面的X类修改成如下：

~~~java
@Component
public class X implements ApplicationContextAware, BeanNameAware, InitializingBean {

	//得到一个spring容器
	ApplicationContext applicationContext;

	@Autowired
	A a;

	public X(){
		System.out.println("create x");
	}

	public X(Y y){
		System.out.println("create x with y");
	}

	public X(Y y,Z z){
		System.out.println("create x with y and z");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		System.out.println("applicationContext的回调");
		this.applicationContext=applicationContext;
	}

	@Override
	public void setBeanName(String name) {
		System.out.println("setBeanName的回调");
	}
    
    @Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("lifecycle  callback from  InitializingBean");
	}
    
    @PostConstruct
	public void jsrInitMethod(){
		System.out.println("jsr-250 annotation init");
	}
}
~~~

一、首先进入`AbstractApplicationContext`中的`finishBeanFactoryInitialization`方法：

~~~java
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		...

		// Instantiate all remaining (non-lazy-init) singletons.
		//完成单例bean的实例化
		beanFactory.preInstantiateSingletons();
	}
~~~

点击最后一个方法，会进入`DefaultListableBeanFactory`类中，代码很长，这里截取需要用到的部分：

~~~java
public void preInstantiateSingletons() throws BeansException {
		if (logger.isTraceEnabled()) {
			logger.trace("Pre-instantiating singletons in " + this);
		}

		// Iterate over a copy to allow for init methods which in turn register new bean definitions.
		// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// Trigger initialization of all non-lazy singleton beans...
		for (String beanName : beanNames) {
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			//如果bean不是抽象的并且是单例的并且不是懒加载
			if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
				//判断当前bean是否是一个factorybean
				if (isFactoryBean(beanName)) {
					Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
					if (bean instanceof FactoryBean) {
						FactoryBean<?> factory = (FactoryBean<?>) bean;
						boolean isEagerInit;
						if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
							isEagerInit = AccessController.doPrivileged(
									(PrivilegedAction<Boolean>) ((SmartFactoryBean<?>) factory)::isEagerInit,
									getAccessControlContext());
						}
						else {
							isEagerInit = (factory instanceof SmartFactoryBean &&
									((SmartFactoryBean<?>) factory).isEagerInit());
						}
						if (isEagerInit) {
							getBean(beanName);
						}
					}
				}
				else {
					//非factorybean会走这里，根据beanName获取bean
					getBean(beanName);
				}
			}
		}
    ...
~~~

二、因为当前bean X不是一个factorybean，所以会进入最后一个getBean方法

~~~java
	@Override
	public Object getBean(String name) throws BeansException {
		//真正获取bean的方法
		return doGetBean(name, null, null, false);
	}
~~~

三、进入`AbstractBeanFactory`中的doGetBean方法，这是真正执行获取bean的方法：

```
protected <T> T doGetBean(
      String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
      throws BeansException {

   String beanName = transformedBeanName(name);
   Object bean;

   // Eagerly check singleton cache for manually registered singletons.
   //从单例池中检查是否已经存在
   Object sharedInstance = getSingleton(beanName);
   if (sharedInstance != null && args == null) {
      if (logger.isTraceEnabled()) {
         if (isSingletonCurrentlyInCreation(beanName)) {
            logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                  "' that is not fully initialized yet - a consequence of a circular reference");
         }
         else {
            logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
         }
      }
      bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
   }

   else {
      // Fail if we're already creating this bean instance:
      // We're assumably within a circular reference.
      //判断当前创建的bean是否在正在被创建的原型bean的集合中
      if (isPrototypeCurrentlyInCreation(beanName)) {
         throw new BeanCurrentlyInCreationException(beanName);
      }

      ...

      try {
 		...

         // Create bean instance.
         if (mbd.isSingleton()) {
            //获取单例的bean实例
            //第二个参数是一个lambda表达式，执行getSingleton方法时会进入creatBean
            sharedInstance = getSingleton(beanName, () -> {
               try {
                  //创造bean
                  return createBean(beanName, mbd, args);
               }
               catch (BeansException ex) {
                  // Explicitly remove instance from singleton cache: It might have been put there
                  // eagerly by the creation process, to allow for circular reference resolution.
                  // Also remove any beans that received a temporary reference to the bean.
                  destroySingleton(beanName);
                  throw ex;
               }
            });
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
         }

        }
	...
	

   return (T) bean;
}
```

其中没有被用到的代码就被截取了，上面的代码会做两件事情：

* 先根据beanName从单例池中获取，如果获取到，那么返回

* 没有获取到，会调用getsingleton(beanName,ObjectFactory)这个方法去获取，这个方法的第二个参数是一个函数式接口，它会在执行DefaultSingletonBeanRegistry类的getSingleton方法中的

  ~~~java
  try {
      singletonObject = singletonFactory.getObject();
      newSingleton = true;
  }
  ~~~

  第二行代码进入。

四、上面方法的二个参数中会执行`createBean`方法，该方法的实现在AbstractAutowireCapableBeanFactory中，其中真正去创建bean实例的方法是`doCreateBean`，这个方法**完成了bean的实例化和初始化**

~~~java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		// Instantiate the bean.
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			//真正的去创建bean实例
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					//合并BeanDefinition
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		//判断是否支持循环引用
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// Initialize the bean instance.
		Object exposedObject = bean;
		try {
			//属性填充（比如填充加了@Autowired注解的属性）
			populateBean(beanName, mbd, instanceWrapper);
			//初始化bean，各种增强
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			Object earlySingletonReference = getSingleton(beanName, false);
			if (earlySingletonReference != null) {
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}
~~~

上面代码主要完成这三件事

##### bean的实例化

通过这行代码完成：

~~~java
		if (instanceWrapper == null) {
			//真正的去创建bean实例
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
~~~

进入该方法中，其中有一段代码需要注意：

~~~java
		// Candidate constructors for autowiring?
		//通过class对象和beanName去推断构造函数，一般都是当前bean的默认构造函数，所以这里一般是null，与注入模型有关
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			return autowireConstructor(beanName, mbd, ctors, args);
		}
~~~

spring会根据当前bean的class和name去推断到底该使用bean的无参构造函数，还是有参构造函数，这个算法很复杂，而且一般都是使用的默认构造函数。

~~~java
		// No special handling: simply use no-arg constructor.
		//默认使用无参构造函数反射创建实例
		return instantiateBean(beanName, mbd);
~~~

进入该方法，通过这行代码完成实例化：

~~~java
beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, this);
~~~

instantiate就是实例化的意思，这个方法内部会**通过当前bean的class对象反射获取默认的构造函数，然后通过构造函数new出一个对象**。

至此，bean就完成了实例化，控制台会打印构造方法中的输出语句

继续回到`doCreateBean`方法中，此时需要注意，**X类中的A类已经在单例池中了，因为spring是按字母顺序来实例化bean的，所以A类在X之前已经实例化并且放到了单例池中，但是此时X还并不是一个bean，它只是一个普通的java对象**。我们可以通过两个方法来验证

查看当前放回的创建实例

![](https://z3.ax1x.com/2021/07/07/RbMtxA.png)

可以发现此时X中需要注入的A还是null，也就是说没有完成自动注入，我们再来看下此时单例池中到底有没有x，这种方式更直观

![](https://z3.ax1x.com/2021/07/07/RbQZo8.png)

可以看到，单例池也不存在x。那这个x到底是什么时候放入单例池的？我们接着分析。

##### 填充属性

当代码执行到这里

~~~java
Object exposedObject = bean;

populateBean(beanName, mbd, instanceWrapper);
~~~

此时会完成属性的填充，具体实现先不看，我们看下代码执行完后，到底完成X中A的属性注入没有，查看exposedObject的值

![](https://z3.ax1x.com/2021/07/07/RblvCD.png)

答案很明显。具体的代码实现主要干了三件事

1、判断bean是否设置自动填充

InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation

2、获取需要自动注入的值

InstantiationAwareBeanPostProcessor#postProcessPropertyValues

3、将上面返回的值进行填充进属性

applyPropertyValues


##### bean的初始化

接下来会执行bean的初始化，主要是实现aware接口的方法回调、bean后置处理器初始化之前的方法回调、接口版的生命周期回调、bean后置处理器初始化之后的方法回调。具体我们看下`initializeBean`方法

~~~java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				invokeAwareMethods(beanName, bean);
				return null;
			}, getAccessControlContext());
		}
		else {
			//执行aware的回调方法
			invokeAwareMethods(beanName, bean);
		}

		Object wrappedBean = bean;
		if (mbd == null || !mbd.isSynthetic()) {
			//bean后置处理器的初始化之前回调,实现其它aware接口的方法回调，加了@PostConstruct注解的方法也会被调用
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
		}

		try {
			//执行接口版的生命周期回调，比如实现InitializingBean接口的方法回调
			invokeInitMethods(beanName, wrappedBean, mbd);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					(mbd != null ? mbd.getResourceDescription() : null),
					beanName, "Invocation of init method failed", ex);
		}
		if (mbd == null || !mbd.isSynthetic()) {
			//bean后置处理器的初始化之后回调
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}

		return wrappedBean;
	}
~~~

分别看下这几个方法：

1、`invokeAwareMethods(beanName, bean);`

~~~java
	private void invokeAwareMethods(String beanName, Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof BeanNameAware) {
				((BeanNameAware) bean).setBeanName(beanName);
			}
			if (bean instanceof BeanClassLoaderAware) {
				ClassLoader bcl = getBeanClassLoader();
				if (bcl != null) {
					((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
				}
			}
			if (bean instanceof BeanFactoryAware) {
				((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
			}
		}
	}
~~~

可以看到，**执行顺序是实现了BeanNameAware接口的方法回调、BeanClassLoaderAware接口的方法回调、BeanFactoryAware方法的接口回调**。由于X类实现了BeanNameAware接口，所以此时控制台会输出`setBeanName`方法中的内容

~~~java
setBeanName的回调
~~~

2、`applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName)`

~~~java
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			Object current = processor.postProcessBeforeInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}
~~~

for循环对getBeanPostProcessors这个list进行遍历，这个list是CopyOnWriteArraylist类型的，其中一个值是`ApplicationContextAwareProcessor`。看到这有点蒙，我们的X实现的是ApplicationContextAware，根本没实现什么`ApplicationContextAwareProcessor`，为什么会走这里呢？

进入这个类，上面有段注释：

~~~java
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * implementation that passes the ApplicationContext to beans that
 * implement the {@link EnvironmentAware}, {@link EmbeddedValueResolverAware},
 * {@link ResourceLoaderAware}, {@link ApplicationEventPublisherAware},
 * {@link MessageSourceAware} and/or {@link ApplicationContextAware} interfaces.
~~~

意思就是：BeanPostProcessor实现将 ApplicationContext 传递给实现EnvironmentAware 、 EmbeddedValueResolverAware 、 ResourceLoaderAware 、 ApplicationEventPublisherAware 、 MessageSourceAware和ApplicationContextAware接口的 bean

，并且会按这个顺序执行。而它实现了BeanPostProcessor接口，所以会执行自己的postProcessBeforeInitialization方法：

~~~java
	public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
		AccessControlContext acc = null;

		if (System.getSecurityManager() != null &&
				(bean instanceof EnvironmentAware || bean instanceof EmbeddedValueResolverAware ||
						bean instanceof ResourceLoaderAware || bean instanceof ApplicationEventPublisherAware ||
						bean instanceof MessageSourceAware || bean instanceof ApplicationContextAware)) {
			acc = this.applicationContext.getBeanFactory().getAccessControlContext();
		}

		if (acc != null) {
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				invokeAwareInterfaces(bean);
				return null;
			}, acc);
		}
		else {
			invokeAwareInterfaces(bean);
		}

		return bean;
	}
~~~

在invokeAwareInterfaces方法会按上面所说的顺序执行

~~~java
	private void invokeAwareInterfaces(Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof EnvironmentAware) {
				((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
			}
			if (bean instanceof EmbeddedValueResolverAware) {
				((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);
			}
			if (bean instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
			}
			if (bean instanceof ApplicationEventPublisherAware) {
				((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
			}
			if (bean instanceof MessageSourceAware) {
				((MessageSourceAware) bean).setMessageSource(this.applicationContext);
			}
			if (bean instanceof ApplicationContextAware) {
				((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
			}
		}
	}
~~~

因为X实现了ApplicationContextAware接口，所以执行完最后一个if时，会输出

~~~java
applicationContext的回调
~~~

除此之外，当for循环遍历到`CommonAnnotationBeanPostProcessor`时，还会输出我们在X类中其中在方法上加了@PostConstruct注解中的内容

~~~java
jsr-250 annotation init
~~~

原因在`CommonAnnotationBeanPostProcessor`类上的注释说的很清楚，它支持javax.annotation包中的 JSR-250 注释。

3、`invokeInitMethods(beanName, wrappedBean, mbd)`

该方法会执行接口版的生命周期回调， 它spring会检查当前bean 是否实现了 InitializingBean 或定义了自定义的 init 方法，如果是，则调用必要的回调。

~~~java
protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd)
			throws Throwable {

		boolean isInitializingBean = (bean instanceof InitializingBean);
		if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
			}
			if (System.getSecurityManager() != null) {
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((InitializingBean) bean).afterPropertiesSet();
						return null;
					}, getAccessControlContext());
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
                //一般会走这里
				((InitializingBean) bean).afterPropertiesSet();
			}
		}

		if (mbd != null && bean.getClass() != NullBean.class) {
			String initMethodName = mbd.getInitMethodName();
			if (StringUtils.hasLength(initMethodName) &&
					!(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
					!mbd.isExternallyManagedInitMethod(initMethodName)) {
				invokeCustomInitMethod(beanName, bean, mbd);
			}
		}
	}
~~~

由于X实现了InitializingBean接口，所以会执行afterPropertiesSet()方法，输出

~~~java
lifecycle  callback from  InitializingBean
~~~

其实从InitializingBean名字也可以猜到，它是执行正在初始化的bean的方法回调，在它之前执行的是bean后置处理器的初始化之前的回调方法，在它后面是bean后置处理器的初始化之后的回调方法，而且从方法afterPropertiesSet名字也可以看出它是在bean的属性设置之后，也就是在bean完成属性的自动填充之后。

4、`applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName)`

这个方法的执行时间在bean后置处理器的初始化之后调用，主要完成事件发布，并且它是**Spring AOP实现的重要一步**。我们可以验证一下：

首先引入aspectj相关依赖，然后添加切面拦截

~~~java
@Aspect
@Component
public class AspectjTest {

	@Pointcut("within(com.cxylk.model4.bean.X)")
	public void pointCut(){

	}

	@Before("pointCut()")
	public void before(){
		System.out.println("aop before");
	}
}
~~~

开启aspectj功能

~~~java
@ComponentScan("com.cxylk.model4")
@EnableAspectJAutoProxy
public class AppConfig {
}
~~~

然后在X类中添加一个方法测试

~~~java
	public void testAop(){
		System.out.println("loginc aop");
	}
~~~

main方法中调用该方法，然后debug，进入到这里

~~~java
		if (mbd == null || !mbd.isSynthetic()) {
			//bean后置处理器的初始化之后回调
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}
~~~

当这个方法还没执行完的时候，看下此时wrappedBean

![](https://z3.ax1x.com/2021/07/08/RLAh5j.png)

当执行完这行代码之后，会感觉到时间有一点长，此时再来看wrappedBean的值

![](https://z3.ax1x.com/2021/07/08/RLEqSI.png)

可以看到，此时的X已经通过CGLIB的方式增强了，也就是说我们的aop起到作用了，这也说明了applyBeanPostProcessorsAfterInitialization方法的回调时机。

##### 加入单例池

经过上面bean的创建和初始化后，此时代码一路返回到`createBean`这个方法，而这个方法还在AbstractBeanFactory类中的doGetBean方法中的`getSingleton`方法中，而`getSingleton`这个方法前面说过，它的实现在DefaultSingletonBeanRegistry类中

~~~java
				try {
                    //返回到这里，此时singletonObject不再是null，而是X
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					//进入doGetBean中的createBean方法
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					addSingleton(beanName, singletonObject);
				}
~~~

**此时单例池中仍然没有X**，代码往下执行，然后到addSingleton(beanName, singletonObject);方法：

~~~java
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}
~~~

可以看到，这个方法将当前beanName作为key，singletonObject作为value放入了map中，然后此时再来看单例池中的值：

![](https://z3.ax1x.com/2021/07/07/Rba6Rx.png)

至此，就完成了bean的实例化、初始化、放入单例池的过程。

#### bean的销毁

在初始化后，如果当前bean需要被销毁，则会进入销毁

~~~java
		// Register bean as disposable.
		try {
			//销毁
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}
~~~

点进该方法：

~~~java
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		//不是原型并且需要被销毁，实现了DisposableBean接口
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
				// Register a DisposableBean implementation that performs all destruction
				// work for the given bean: DestructionAwareBeanPostProcessors,
				// DisposableBean interface, custom destroy method.
				//回调destroy方法
				registerDisposableBean(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
			else {
				// A bean with a custom scope...
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
		}
	}
~~~

简单来说，就是：

* 如果当前bean实现了`DisposableBean`接口，则会回调该接口的`destroy`方法
* 如果配置了`destroy-method`方法，则会配置`destroy-method`方法

#### 总结

下面我们就来总结下spring扫描bean和bean的生命周期这个过程，**bean的生命周期其实在`BeanFactory`这个类中，spring官方已经在注释中说明了**。

根据前面分析的`invokeBeanFactoryPostProcessor`和`finishBeanFactoryInitialization`两个方法，得到如下流程：

首先是spring容器被实例化，然后进入refresh进行初始化

* 扫描给定的包-->得到beanDefinition-->放入bdmp
* 遍历bdmp，依次获取beanDefinition对象
* 解析，得到beanDefinition中描述bean的信息（比如name）
* 验证，是否是抽象，是否是单例，是否是懒加载，是否是一个factoryBean 
* 获取bean-->获取单例bean-->创建bean，得到class对象，推断构造方法
* 得到构造方法（一般都是默认的无参构造方法），通过反射创建实例，这个时候**只是完成了对象的创建，还不是一个bean**
* 合并BeanDefinition（不需要关心）
* 做一个循环依赖的判断，然后对循环依赖提供支持
* 属性填充-->判断是否需要完成自动注入，如果需要，完成属性填充（自动注入）
* 执行部分的Aware接口（BeanNameAware、BeanClassLoaderAware、BeanFactoryAware）
* （BeanPostProcessor before）执行部分生命周期的初始胡回调（注解版本），部分Aware接口的回调
* （initMethods）接口版的生命周期回调，比如实现InitializingBean接口
* （BeanPostProcessor after）完成事件发布，完成aop代理
* bean的销毁

最后总结成如下一张图：

![](https://z3.ax1x.com/2021/07/08/ROpvQS.png)

上面这张图包含了容器初始化，bdmp扫描的过程，过于复杂了点。并且没有画出bean销毁的流程。

下面这张图是bean完整生命周期的简单流程

![](https://z3.ax1x.com/2021/07/16/WMpgLn.png)

