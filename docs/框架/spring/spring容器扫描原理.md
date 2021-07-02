## 深入解析invokeBeanFactoryPostProcessors方法

前面简单说了invokeBeanFactoryPostProcessors方法，现在来彻底搞懂这个方法。

#### 实现子类BeanDefinitionRegistryPostProcessor的执行时机

首先有一个A类,，它实现了BeanDefinitionRegistryPostProcessor这个接口：

~~~java
@Component
public class A implements BeanDefinitionRegistryPostProcessor {
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		System.out.println("A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法");
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法");
	}
}
~~~

然后有个B类，它实现了BeanFactoryPostProcessor接口：

~~~java
@Component
public class B implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("B ----------------实现BeanFactoryPostProcessor的postProcessBeanFactory方法");
	}
}
~~~

将断点打在PostProcessRegistrationDelegate这个类中的invokeBeanFactoryPostProcessor这个方法内，其中每一步都做了注释，我们先看if内的这段代码：

~~~
public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		//这个processedBeans存放的是所有Bean的名字
		Set<String> processedBeans = new HashSet<>();

		//beanFactory就是DefaultListableBeanFactory，它实现了BeanDefinitionRegistry接口
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			
			//直接提bean给spring才会用到，后面会将
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			
			//它和processedBeans的区别：processedBeans存放的是所有执行过的BeanFactoryPostProcessor和BeanDefinitionRegistryPostProcessor
			//而registryProcessors存放的是所有BeanDefinitionRegistryPostProcessor类型的bean（已经执行过的，不管加是扫描的还是手动提供的），它不会被clear
			//BeanDefinitionRegistryPostProcessor一定是BeanFactoryPostProcessor
			//为什么要存？为了直接执行BeanFactoryPostProcessor，不用再找了
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			//默认是不会走这个for循环的
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			//存放当前BeanDefinitionRegistryPostProcessor类型的Bean，每次执行完就会被clear
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//首先拿到的是internalConfigurationAnnotationProcessor名称，它对应的beanClass就是ConfigurationClassPostProcessor这个类
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				//ConfigurationClassPostProcessor实现了PriorityOrdered接口
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//将ConfigurationClassPostProcessor加入当前这个list
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//并且加入存放所有beanName的set集合中
					processedBeans.add(ppName);
				}
			}
			//排序，现在来说不重要
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//将当前list合并到registryProcessors中
			registryProcessors.addAll(currentRegistryProcessors);
			//现在执行的是ConfigurationClassPostProcessor的postProcessBeanDefinitionRegistry方法，确定候选组件类
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			//执行完上述方法后，将当前list clear掉
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			//现在，A类实现了BeanDefinitionRegistryPostProcessor接口
			//所以此时还能拿到A类
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				//ConfigurationClassPostProcessor这个类已经在set集合中了，所以不会进入if体
				//此时我们提供的A类没有实现Ordered接口，也不会进入if体
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				//此时拿到的还是ConfigurationClassPostProcessor和a
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					//此时只有A类没有加入set集合中，所以进入if体
					if (!processedBeans.contains(ppName)) {
						//将A加入当前的list
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						//加入set集合
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//合并到registryProcessors这个list
				registryProcessors.addAll(currentRegistryProcessors);
				//此时执行A类的postProcessBeanDefinitionRegistry方法
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				//从当前list中清除
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			//不考虑程序员扩展的情况，只会执行ConfigurationClassPostProcessor的postProcessorBeanDefinition方法
			//如果做了扩展，比如A和C，就会执行A类或其他实现了子类bdpp的postProcessBeanFactory的方法，并且是实现了Ordered接口的先执行
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			
			//这里还有个invoke方法，但是这里执行后并没有输出，后面分析
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}
		...
	}
~~~

当执行完第一个invokeBeanDefinitionRegistryPostProcessors方法后，

~~~java
invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry)
~~~

参数currentRegistryProcessors此时存放的是ConfigurationClassPostProcessor，打印结果如下

![](https://z3.ax1x.com/2021/07/02/R6SEZV.png)

意思就是确定候选组件类，这些类是我们实现BeanDefinitionRegistryPostProcessor和BeanFactoryPostProcessor接口的类

当执行完上面代码倒数第四行的invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);方法后，控制台打印结果如下：

~~~java
A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
~~~

当执行完上面代码倒数第二行的invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);方法后，控制台打印结果如下：

~~~java
A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法
~~~

上面代码都执行完后，控制台并没有打印B类中的信息，**也就是说上面的代码都是在执行子类逻辑**。

上面的执行流程如下图所示：

![](https://z3.ax1x.com/2021/07/02/R6YD8U.png)

关于上面的第二步，我们可以验证一下：

添加一个类，并且让它实现BeanDefinitonRegistryPostProcessor和Order接口

~~~java
@Component
public class C implements BeanDefinitionRegistryPostProcessor, Ordered {
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		System.out.println("C ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法");
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("C ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法");
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
~~~

上面代码中的这一段：

~~~java
			//现在，A类实现了BeanDefinitionRegistryPostProcessor接口
			//所以此时还能拿到A类，如果此时添加一个C类同样实现bdpp这个接口，并且实现Ordered接口
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				//ConfigurationClassPostProcessor这个类已经在set集合中了，所以不会进入if体
				//此时我们提供的A类没有实现Ordered接口，也不会进入if体，C会进入if体
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			//此时会执行实现Ordered接口的C类的postProcessBeanDefinitionRegistry方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();
~~~

这时，控制台首先打印的是：

~~~java
C ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
~~~

最后，执行完上面所有代码后，打印结果如下：

~~~java
C ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
C ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法
A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法
~~~



上面代码还有两个地方存在疑问？

**1、第一个for循环为什么不会走？什么情况下才会走？**

**2、代码最后为什么还有一个invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);方法？**

解决这两个问题，需要知道向spring提供bean的方式有哪些

* 第一种方式，也是常见的方式，就是加上@Component注解，这是让spring来扫描这个类，这样的话就等于将这个类的实例化过程全部交给spring容器来帮我们管理

* 第二种方式，不提供注解，比如一个X类，它没有添加@Component注解

  ~~~java
  AnnotationConfigApplicationContext ac=new AnnotationConfigApplicationContext();
  //手动提供给spring，而不是以扫描的方式
  ac.getBeanFactory().registerSingleton("x",new X());
  ac.register(App.class);
  ac.refresh();
  
  ac.getBean("x");
  ~~~

  其实就是将原来在构造方法中spring自动帮我们完成的事情变成我们手动来控制。

  这种方式我将对象提供给spring，一般当我们使用第三方jar包的时候，它并没有提供包名让spring来扫描，因为提供包名的话可能会引起冲突，所以我们就可以通过这种方式将对象提供给spring，这也引出了一道常见的面试题：**如何将一个对象提供给spring**，还有一个方法是**factorybean**，这个后面再说。

下面提供一个Y类：

~~~java
public class Y implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("Y ----------------实现BeanFactoryPostProcessor的postProcessBeanFactory方法");
	}
}
~~~

然后我们这种方法将它提供给spring：

~~~java
AnnotationConfigApplicationContext ac=new AnnotationConfigApplicationContext();

//手动提供
ac.addBeanFactoryPostProcessor(new Y());
ac.register(App.class);
ac.refresh();
~~~

这时候，第一个for循环就会执行了

~~~java
for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
    if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
        BeanDefinitionRegistryPostProcessor registryProcessor =
            (BeanDefinitionRegistryPostProcessor) postProcessor;
        registryProcessor.postProcessBeanDefinitionRegistry(registry);
        registryProcessors.add(registryProcessor);
    }
    else {
        regularPostProcessors.add(postProcessor);
    }
}
~~~

这个for循环能执行是因为beanFactoryPostProcessors这个集合有值了，能够对它进行遍历，而这个list是通过方法参数传入的，我们来看看它是什么有值的。

上面的整个方法是由`refresh`方法中的`invokeBeanFactoryPostProcessors`方法调用的，而这个方法会调用到`AbstractApplicationContext`中的`invokeBeanFactoryPostProcessors`方法：

~~~java
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}
~~~

这个方法中会调用PostProcessorRegistrationDelegate的invokeBeanFactoryPostProcessors方法，也就是最上面我们分析的那个方法，而它的第二个参数就是beanFactoryPostProcessors，看一下这个参数：

~~~java
public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
    return this.beanFactoryPostProcessors;
}

private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
~~~

这个list的值默认是空的，但现在它是有值的，所以上面的for循环才会走，那什么时候给它赋值的呢？

就是我们手动将Y提供给spring的时候

~~~java
ac.addBeanFactoryPostProcessor(new Y());
~~~

进入该方法：

~~~java
@Override
public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
    this.beanFactoryPostProcessors.add(postProcessor);
}
~~~

可以看到，它将Y添加到了这个集合中。

我们继续分析这个for循环：

~~~java
for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
    //如果实现的是BeanDefinitionRegistryPostProcessor接口，进入for循环
    if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
        BeanDefinitionRegistryPostProcessor registryProcessor =
            (BeanDefinitionRegistryPostProcessor) postProcessor;
        //执行它的postProcessBeanDefinitionRegistry方法
        //从这里看出，这个方法比spring内置的还要先执行
        registryProcessor.postProcessBeanDefinitionRegistry(registry);
        registryProcessors.add(registryProcessor);
    }
    //如果实现的是BeanFactoryPostProcessor接口
    else {
        //加入到regularPostProcessors集合中
        //这个集合在该if循环内的最后一个invoke方法被调用
        regularPostProcessors.add(postProcessor);
    }
}
~~~

这时，前面一个没有用到的list即regularPostProcessors就派上用场了，这个list就是在上面代码的最后一个invoke被使用的：

~~~java
/**
 * 执行直接实现了BeanFactoryPostProcessor接口的类，但是一定是程序提供的
* 提供有两种方式：
* 1、注解扫描（不会执行）
* 2、直接给spring（执行这个）
*/
invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
~~~

B类也是实现了`BeanFactoryPostProcessor`接口的类，Y类也是，但是执行完这个方法后，只会输出Y类中方法的内容：

~~~java
C ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
C ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法
A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法
Y ----------------实现BeanFactoryPostProcessor的postProcessBeanFactory方法
~~~

这个时候我们再添加一个Z类，让它实现`BeanDefinitionRegistryPostProcessor`这个接口，同样直接提供给spring：

~~~java
public class Z implements BeanDefinitionRegistryPostProcessor {
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		System.out.println("A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法");
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("A ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法");
	}
}
~~~

从上面的for循环可知，类Z是BeanDefinitionRegistryPostProcessor类型的，所以进入这个if

~~~java
if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
    BeanDefinitionRegistryPostProcessor registryProcessor =
        (BeanDefinitionRegistryPostProcessor) postProcessor;
    //执行它的postProcessBeanDefinitionRegistry方法
    //从这里看出，这个方法比spring内置的还要先执行
    registryProcessor.postProcessBeanDefinitionRegistry(registry);
    registryProcessors.add(registryProcessor);
}
~~~

然后直接执行`postProcessBeanDefinitionRegistry`方法，也就是说，它比我们前面分析的：spring内置的BeanDefinitionRegistryPostProcessor并且实现了PriorityOrdered接口的执行时机还要早，打印结果：

~~~java
Z ----------------实现BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
2021-07-02 16:33:17,467 DEBUG [org.springframework.beans.factory.support.DefaultListableBeanFactory] - Creating shared instance of singleton bean 'org.springframework.context.annotation.internalConfigurationAnnotationProcessor'
~~~

可以看到，它在创建`ConfigurationClassPostProcessor`之前就执行了。

这时候的流程图如下图所示：

![](https://z3.ax1x.com/2021/07/02/RcEjKO.png)

#### 实现父类BeanFactoryPostProcessor的执行时机

上面说过，Y类是没有加注解，并且实现BeanFactoryPostProcessor接口，手动提供给spring的类，而B类是加注解的，也实现了BeanFactoryPostProcessor接口的类，它们两者的执行顺序是手动提供的先执行，加注解的后执行。

看下源码中执行父类方法的片段：

~~~java
		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		//拿到实现BeanFactoryPostProcessor接口的类名
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		//存放实现了BeanFactoryPostProcessor接口和PriorityOrdered接口的类
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		//存放实现了BeanFactoryPostProcessor接口和Ordered接口的类名
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//存放两个排序接口都没有实现的类名
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
~~~

通过分析和源码注释，可以得出实现父类BeanFactoryPostProcessor的执行时机

![](https://z3.ax1x.com/2021/07/02/RcuY5Q.png)

最上面的箭头是连着子类执行顺序的，因为图太大放不下就把父类的截取了。

最后放个全图：

![](https://z3.ax1x.com/2021/07/02/RcGLuj.png)

总结：其实就是分为了两大步骤

一、执行实现子类当中的方法

​	1、执行postProcessBeanDefinitionRegistry

​		根据手动提供->实现顺序的接口->扫描（注解）

​	2、执行postProcessBeanFactory

​		手动提供->扫描

二、执行实现父类当中的方法

​	手动提供->实现顺序的接口->所有的