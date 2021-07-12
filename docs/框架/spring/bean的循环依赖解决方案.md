## spring注入模型

按照spring的官网介绍：

~~~text
DI exists in two major variants: Constructor-based dependency injection and Setter-based dependency injection.
~~~

意思就是依赖注入有两种**主要**的**变体**：基于构造函数的依赖注入和基于setter方法的依赖注入

而在spring中，**注入模型**有以下4种，源码在`AutowireCapableBeanFactory`中

~~~java
/**
	 * Constant that indicates no externally defined autowiring. Note that
	 * BeanFactoryAware etc and annotation-driven injection will still be applied.
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_NO = 0;

	/**
	 * Constant that indicates autowiring bean properties by name
	 * (applying to all bean property setters).
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_NAME = 1;

	/**
	 * Constant that indicates autowiring bean properties by type
	 * (applying to all bean property setters).
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_TYPE = 2;

	/**
	 * Constant that indicates autowiring the greediest constructor that
	 * can be satisfied (involves resolving the appropriate constructor).
	 * @see #createBean
	 * @see #autowire
	 */
	int AUTOWIRE_CONSTRUCTOR = 3;

	/**
	 * Constant that indicates determining an appropriate autowire strategy
	 * through introspection of the bean class.
	 * @see #createBean
	 * @see #autowire
	 * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
	 * prefer annotation-based autowiring for clearer demarcation of autowiring needs.
	 */
	@Deprecated
	//过期
	int AUTOWIRE_AUTODETECT = 4;
~~~

不管是by_name或者by_type它们可以说都是上面两种注入方式的变体。

拿我们平常使用比较多的@Autowired注解来说，它严格意思来说并不是手动注入，它只是根据bean的名字或者类型去容器找到这个bean的过程（可以看bean属性填充populateBean方法的实现)，它并不会改变一个bean实例化的方式，但是如果指定了注入模型，那么spring就会根据指定的注入模型来实例化bean。默认情况下，使用的是`AUTOWIRE_NO`注入模型。

## 循环依赖

比如有以下两个类：

~~~java
@Component
public class M {
	@Autowired
	N n;

	public M(){
		System.out.println("create m");
	}
}

@Component
public class N {
	@Autowired
	M m;

	public N(){
		System.out.println("crate n");
	}
}
~~~

这就是spring中的循环依赖，只要理解了bean的生命周期后，理解循环依赖就不难了，spring通过三个缓存来解决。

#### 一级缓存

就是前面反复提到的单例池：**singletonObjects**，缓存已经实例化好的单例bean

#### 二级缓存

**earlySingletonObjects**，存放的已经实例化好的对象，并没有经过程序员扩展的（默认没有扩展）。

#### 三级缓存

**singletonFactories**，存放的是一个对象，半成品的bean，spring刚刚把一个对象实例化好的时候放进去的，但是放进去的不是一个M对象，放进去的一个工厂对象(ObjectFactory)，这个工厂可以产生M，产生M之后放到二级缓存

#### 流程

首先回顾下bean初始化的大概流程：验证->创建对象->填充属性->...

在`preInstantiateSingletons`方法中验证bean是否是抽象的、是否是单例的、是否是factoryBean后，进入真正获取`doGetBean`的第二个`getSingletion`方法中（此时一个getSingleton是获取不到的），看下这个方法：

~~~java
synchronized (this.singletonObjects) {
			//从单例池中获取
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				//检查并将beanName放入正在创建bean的set集合，为了解决循环依赖的问题
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					//执行lambda表达式，进入doGetBean中的createBean方法
					//执行完bean的实例化后继续回到这里
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
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
					//实例化bean之后，继续回到该方法，然后执行到这里
					//将beanName作为key，bean对象作为value放入单例池singletonObjects中
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
~~~

在`beforeSingletonCreation`方法中：

~~~java
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}
~~~

第一个判断条件是当前bean是否需要跳过检查的判断，这里是false，取反就是true，然后进入第二个判断条件，**它会将当前beanName加入当前bean是否正在创建的set集合中**，这是什么意思呢？

```
比如M中注入了N，N里面注入了M，即发生了循环依赖
首先会将M放入singletonsCurrentlyInCreation这个集合中，当创建完M对象并将它放入三级缓存中，然后进行属性填充的时候，会触发N的实例化和属性填充，而创建完N对象并将它放入三级缓存中后，
又会去填充M属性，此时的M就是正在创建的bean，它会在这个set集合中，后面会分析到
```

代码继续执行，然后会走到getSingleton方法中的lambda表达式，进入实例化bean的过程，当对象被创建后（注意此时只是创建了一个java对象），下面代码会走到这里对循环依赖做准备：

~~~java
		//判断是否支持循环引用。循环依赖的提前准备
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			//将创建好的对象放入三级缓存 this.registeredSingletons.add(beanName);
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}
~~~

addSingletonFactory方法就是将**当前beanName作为key，对象工厂作为value放入三级缓存singletonFactories中**。

~~~java
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}
~~~

注意这个对象工厂，它是个函数式接口，具体的实现在getEarlyBeanReference这个方法中，这个方法如果我们不做扩展的话，就是返回一个对象

~~~java
	protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
		Object exposedObject = bean;
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
				}
			}
		}
		return exposedObject;
	}
~~~

做完这步后，会进行属性的填充。前面说过，默认的注入方式是`AUTOWIRE_NO`，所以它不会走by_name和by_type的逻辑，而是会走这个方法

~~~java
PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
~~~

代码会一直执行，最后会进入`DefaultListableBeanFactory`中的`doResolveDependency`方法，然后进入这个方法

~~~java
			if (instanceCandidate instanceof Class) {
				//进入这里触发需要注入的属性的实例化和依赖注入
				instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
			}
~~~

看下resolveCandidate的具体实现：

~~~java
	public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
			throws BeansException {

		return beanFactory.getBean(beanName);
	}
~~~

此时的beanName，就是M中所需要注入的属性N，可以看到，此时又会触发N的实例化和依赖注入，将上面的流程又走一遍，然后将N放入当前正在创建bean的set集合和三级缓存中，N最终也会走到这里，而此时的beanName就是N中的M，然后进入获取M的`doGetBean`方法，来看第一个`getSingletion`方法：

~~~java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// Quick check for existing instance without full singleton lock
    	//此时还在创建M的过程中，而放入一级缓存是在创建完bean M后放入的
		Object singletonObject = this.singletonObjects.get(beanName);
		//如果单例池即一级缓存中为空，并且当前bean在正在被创建的集合中（什么意思？比如M中注入了N，N里面注入了M，即发生了循环依赖)
		//首先会将M放入singletonsCurrentlyInCreation这个集合中，当创建完M对象并将它放入三级缓存中，然后进行属性填充的时候，会触发N的实例化和属性填充，而创建完N对象并将它放入三级缓存中后，
		//又会去填充M属性，此时走到这里，beanName就是M，这M就在这个set集合中，所以会走这个if循环
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			//先从二级缓存中找
			singletonObject = this.earlySingletonObjects.get(beanName);
			//为空并且允许循环依赖
			if (singletonObject == null && allowEarlyReference) {
				synchronized (this.singletonObjects) {
					// Consistent creation of early reference within full singleton lock
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						singletonObject = this.earlySingletonObjects.get(beanName);
						if (singletonObject == null) {
							//从三级缓存中获取
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								singletonObject = singletonFactory.getObject();
								//放入二级缓存
								this.earlySingletonObjects.put(beanName, singletonObject);
								//然后将当前beanName从三级缓存中remove掉
								this.singletonFactories.remove(beanName);
							}
						}
					}
				}
			}
		}
		return singletonObject;
	}
~~~

进过前面的分析，很容易得出此时从三级缓存中是能获取到M对象的，然后将这个M对象放入了二级缓存中，最后从三级缓存中移除了M。然后将M这个bean返回，此时还是在获取bean N的过程中，最后执行`applyPropertyValues`方法，完成N中对属性M的注入。同样的，第M中N的属性注入也是这个流程。这样就完成了bean的循环依赖。

最后将上面的流程整理成了一张流程图放在了processon上：https://www.processon.com/diagraming/60eaaac2f346fb6bcd263898