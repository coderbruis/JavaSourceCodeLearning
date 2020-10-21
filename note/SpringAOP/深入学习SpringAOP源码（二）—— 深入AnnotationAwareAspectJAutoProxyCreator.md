## 前言

版本：【Spring 5.1.4】、【SpringAOP 5.1.4】

经过博文[深入学习SpringAOP源码（一）—— 注册AnnotationAwareAspectJAutoProxyCreator](https://blog.csdn.net/CoderBruis/article/details/100031756)的介绍之后，相信大家都了解到了AnnotationAwareAspectJAutoProxyCreator试如何被解析然后注册到SpringIOC中的。接下来开始深入学习AnnotationAwareAspectJAutoProxyCreator源码了。

【没看过深入学习SpringAOP源码（一）的小伙伴最好先去看下，不然看完文章之后可能会很懵】

## 正文

### 1. 学习AnnotationAwareAspectJAutoProxyCreator

先看下AnnotationAwareAspectJAutoProxyCreator的类结构图，如下：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823164851812.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

可以看到，AnnotationAwareAspectJAutoProxyCreator实现了BeanPostProcessor，相信大家都知道，实现了BeanPostProcessor之后，要去实现它的两个重要的方法：postProcessBeforeInitialization和postProcessAfterInitialization，在这里，而具体实现AOP逻辑的是方法postProcessAfterInitialization()。

找了一圈，都没有在AnnotationAwareAspectJAutoProxyCreator中找到postProcessAfterInitialization()方法，看上面的类结构图，挨着找了AspectJAwareAdvisorAutoProxyCreator、AbstractAdvisorAutoProxyCreator和AbstractAutoProxyCreator，才发现postProcessAfterInitialization()和postProcessAfterInitialization()实现类在AbstractAutoProxyCreator类里。看过Spring源码的小伙伴们，都应该知道Spring源码的风格就是一层包着一层，每一层代码的职责都不一样，习惯就好，小伙伴们也不要被这看似很长的类名给吓到了，多看几遍就顺眼了。

了解Spring源码或者看过本人深入Spring系列博文都知道，SpringIOC的refresh()方法包含了许多逻辑，其中在finishBeanFactoryInitialization()方法中，开始了解析AnnotationAwareAspectJAutoProxyCreator的工作。

接下来方法跳转的地方有点多，所以制作了流程图帮助我们更快理解Spring如何解析通知Advisor
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190826165617472.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190826165630570.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)


将视线转移到AbstractAutowireCapableBeanFactory：
```Java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {
    // 在实例化AnnotationAwareAspectJAutoProxyCreator之前进行解析
    @Nullable
    protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
        Object bean = null;
        if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
            if (!mbd.isSynthetic() && this.hasInstantiationAwareBeanPostProcessors()) {
                Class<?> targetType = this.determineTargetType(beanName, mbd);
                if (targetType != null) {
                    // 调用AnnotationAwareAspectJAutoProxyCreator的postProcessBeforeInitialization()
                    bean = this.applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                    if (bean != null) {
                        // 调用调用AnnotationAwareAspectJAutoProxyCreator的postProcessAfterInitialization()
                        bean = this.applyBeanPostProcessorsAfterInitialization(bean, beanName);
                    }
                }
            }

            mbd.beforeInstantiationResolved = bean != null;
        }

        return bean;
    }
}
```
resolveBeforeInstantiation()方法调用了AbstractAutoProxyCreator()的postProcessBeforeInstantiation()和postProcessAfterInstantiation()。

AbstractAutoProxyCreator.class
```Java
import ...
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    /* 
     * postProcessBeforeInstantiation方法中，以shoudSkip()为入口，完成了
     * AnnotationAwareAspectJAutoProxyCreator的获取通知注解的底层方法
     * 1. 从SpringIOC中筛选出@AspectJ注解修饰的类
     * 2. 通过反射获取该类的所有方法
     * 3. 解析所有通知，包装为InstantiationModelAwarePointcutAdvisorImpl类型，存放在List<Advisor>中
     * 4. 将解析的所有通知存放在advisorsCache中（Map<String, List<Advisor>>类型），方便postProcessAfterInstantiation调用
     * 
     * 
     */
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        // 查看缓存中是否有通知的key
        Object cacheKey = this.getCacheKey(beanClass, beanName);
        if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
            if (this.advisedBeans.containsKey(cacheKey)) {
                return null;
            }

            if (this.isInfrastructureClass(beanClass) || this.shouldSkip(beanClass, beanName)) {
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return null;
            }
        }

        TargetSource targetSource = this.getCustomTargetSource(beanClass, beanName);
        if (targetSource != null) {
            if (StringUtils.hasLength(beanName)) {
                this.targetSourcedBeans.add(beanName);
            }

            Object[] specificInterceptors = this.getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
            Object proxy = this.createProxy(beanClass, beanName, specificInterceptors, targetSource);
            this.proxyTypes.put(cacheKey, proxy.getClass());
            return proxy;
        } else {
            return null;
        }
    }
    
    public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
        if (bean != null) {
            Object cacheKey = this.getCacheKey(bean.getClass(), beanName);
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                return this.wrapIfNecessary(bean, beanName, cacheKey);
            }
        }

        return bean;
    }
    
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        } else if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        } else if (!this.isInfrastructureClass(bean.getClass()) && !this.shouldSkip(bean.getClass(), beanName)) {
            // 获取Bean的通知
            Object[] specificInterceptors = this.getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, (TargetSource)null);
            // 如果需要进行代理，则创建代理
            if (specificInterceptors != DO_NOT_PROXY) {
                this.advisedBeans.put(cacheKey, Boolean.TRUE);
                // 创建代理
                Object proxy = this.createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
                this.proxyTypes.put(cacheKey, proxy.getClass());
                return proxy;
            } else {
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return bean;
            }
        } else {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }
    }
}
```


#### 1.1 通过反射工具获取通知

这里跳过中间过程，直接看看底层是如何获得通知，并将通知包装成InstantiationModelAwarePointcutAdvisorI


ReflectiveAspectJAdvisorFactory的getAdvisors()中主要的工作是：迭代出@AspectJ注解修饰的类的方法，然后拿着这些方法区尝试获取Advisor，最后存在advisors集合里。
```Java
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {
    //Spring将@AspectJ注解的beanName和bean工厂封装为了MetadataAwareAspectInstanceFactory
    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
        Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
        this.validate(aspectClass);
        MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory = new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);
        // 用于存放通知
        List<Advisor> advisors = new ArrayList();
        Iterator var6 = this.getAdvisorMethods(aspectClass).iterator();

        while(var6.hasNext()) {
            // 获取带有@AspectJ注解的类的方法
            Method method = (Method)var6.next();
            // 获取这些方法上带有的通知，如果不为空则添加进advisors里
            Advisor advisor = this.getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
            Advisor instantiationAdvisor = new ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
            advisors.add(0, instantiationAdvisor);
        }

        Field[] var12 = aspectClass.getDeclaredFields();
        int var13 = var12.length;

        for(int var14 = 0; var14 < var13; ++var14) {
            Field field = var12[var14];
            Advisor advisor = this.getDeclareParentsAdvisor(field);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        return advisors;
    }
}
```

getAdvisorMethods方法中通过反射工具来获取Advisor方法。
```Java
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {
    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        List<Method> methods = new ArrayList();
        ReflectionUtils.doWithMethods(aspectClass, (method) -> {
            if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
                methods.add(method);
            }

        });
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }
}
```

视线来到ReflectiveAspectJAdvisorFactory的getAdvisor方法
```Java
    @Nullable
    public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrderInAspect, String aspectName) {
        this.validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());
        AspectJExpressionPointcut expressionPointcut = this.getPointcut(candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
        return expressionPointcut == null ? null : new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod, this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
    }
    
    @Nullable
    private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        // 找到方法的通知
        AspectJAnnotation<?> aspectJAnnotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (aspectJAnnotation == null) {
            return null;
        } else {
            AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class[0]);
            ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
            if (this.beanFactory != null) {
                ajexp.setBeanFactory(this.beanFactory);
            }

            return ajexp;
        }
    }
```
总结下getAdvisor的作用：
1. 获取切面
2. 将切面、通知方法、aspectName等包装成InstantiationModelAwarePointcutAdvisorImpl实例
3. Advisor是InstantiationModelAwarePointcutAdvisorImpl的父类
4. getAdvisor方法将InstantiationModelAwarePointcutAdvisorImpl一路返回，然后存放在advisor的集合中
5. 将advisor存放在缓存中

#### 1.2 获取通知，筛选通知


回到AbstractAutoProxyCreator中查看其wrapIfNecessary方法，可以简单总结为两步：

1. 从缓存中获取通知
2. 创建代理

```Java
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        } else if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        } else if (!this.isInfrastructureClass(bean.getClass()) && !this.shouldSkip(bean.getClass(), beanName)) {
            Object[] specificInterceptors = this.getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, (TargetSource)null);
            if (specificInterceptors != DO_NOT_PROXY) {
                // 从缓存中获取通知
                this.advisedBeans.put(cacheKey, Boolean.TRUE);
                // 创建代理
                Object proxy = this.createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
                this.proxyTypes.put(cacheKey, proxy.getClass());
                return proxy;
            } else {
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return bean;
            }
        } else {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }
    }
```

```Java
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {
    ...
    @Nullable
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
        List<Advisor> advisors = this.findEligibleAdvisors(beanClass, beanName);
        return advisors.isEmpty() ? DO_NOT_PROXY : advisors.toArray();
    }
    
    /*
     * 查询可用的通知
     */
    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        /*
         * 获取通知，this.findCandidateAdvisors()调用的是AnnotationAwareAspectJAutoProxyCreator的方法
         */
        List<Advisor> candidateAdvisors = this.findCandidateAdvisors();
        /*
         * 获取可用的通知
         */
        List<Advisor> eligibleAdvisors = this.findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        this.extendAdvisors(eligibleAdvisors);
        if (!eligibleAdvisors.isEmpty()) {
            eligibleAdvisors = this.sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }
}
```

```Java
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {
    protected List<Advisor> findCandidateAdvisors() {
        /*
         * 在这里调用父类方法加载配置文件中的AOP声明。在这里调用父类方法加载配置文件中的AOP声明。AnnotationAwareAspectJAutoProxyCreator间接
         * 继承了AbstractAdvisorsAutoProxyCreator，在实现获取通知的方法中除了保留了父类的获取配置文件中定义的通知外，
         * 同时还添加了获取Bean的注解通知的功能，这个功能就是下面的this.apectJAdvisorsBuilder....实现的
         * 
         */
        List<Advisor> advisors = super.findCandidateAdvisors();
        if (this.aspectJAdvisorsBuilder != null) {
            advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
        }
        return advisors;
    }
}
```

```Java
public class BeanFactoryAspectJAdvisorsBuilder {
    ...
    public List<Advisor> buildAspectJAdvisors() {
        // 这里将返回aspectJTest的类名
        List<String> aspectNames = this.aspectBeanNames;
        if (aspectNames == null) {
            synchronized(this) {
                aspectNames = this.aspectBeanNames;
                if (aspectNames == null) {
                    List<Advisor> advisors = new ArrayList();
                    List<String> aspectNames = new ArrayList();
                    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, Object.class, true, false);
                    String[] var18 = beanNames;
                    int var19 = beanNames.length;

                    for(int var7 = 0; var7 < var19; ++var7) {
                        String beanName = var18[var7];
                        if (this.isEligibleBean(beanName)) {
                            // 获取bean类型
                            Class<?> beanType = this.beanFactory.getType(beanName);
                            if (beanType != null && this.advisorFactory.isAspect(beanType)) {
                                // 将@AspectJ注解的bean的beanName存放在aspectNames集合中
                                aspectNames.add(beanName);
                                AspectMetadata amd = new AspectMetadata(beanType, beanName);
                                if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                                    MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
                                    // 获取通知类型，这里获取的是after、before和around通知
                                    List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                                    if (this.beanFactory.isSingleton(beanName)) {
                                        this.advisorsCache.put(beanName, classAdvisors);
                                    } else {
                                        this.aspectFactoryCache.put(beanName, factory);
                                    }

                                    advisors.addAll(classAdvisors);
                                } else {
                                    if (this.beanFactory.isSingleton(beanName)) {
                                        throw new IllegalArgumentException("Bean with name '" + beanName + "' is a singleton, but aspect instantiation model is not singleton");
                                    }

                                    MetadataAwareAspectInstanceFactory factory = new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                                    this.aspectFactoryCache.put(beanName, factory);
                                    advisors.addAll(this.advisorFactory.getAdvisors(factory));
                                }
                            }
                        }
                    }

                    this.aspectBeanNames = aspectNames;
                    return advisors;
                }
            }
        }

        if (aspectNames.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<Advisor> advisors = new ArrayList();
            Iterator var3 = aspectNames.iterator();

            while(var3.hasNext()) {
                String aspectName = (String)var3.next();
                List<Advisor> cachedAdvisors = (List)this.advisorsCache.get(aspectName);
                if (cachedAdvisors != null) {
                    advisors.addAll(cachedAdvisors);
                } else {
                    MetadataAwareAspectInstanceFactory factory = (MetadataAwareAspectInstanceFactory)this.aspectFactoryCache.get(aspectName);
                    advisors.addAll(this.advisorFactory.getAdvisors(factory));
                }
            }

            return advisors;
        }
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823164956637.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
获取通知之后的结果，如图与AspectJTest设置的是三种通知类型相同 
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190823164940860.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)

回到方法
```Java
protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        List<Advisor> candidateAdvisors = this.findCandidateAdvisors();
        /*
         * 获取可用的通知
         */
        List<Advisor> eligibleAdvisors = this.findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        this.extendAdvisors(eligibleAdvisors);
        if (!eligibleAdvisors.isEmpty()) {
            eligibleAdvisors = this.sortAdvisors(eligibleAdvisors);
        }

        return eligibleAdvisors;
    }
```

findCandidateAdvisors()完成的是通知的解析工作，但是并不是所有的通知都适用于当前bean的，还要选出适合的通知。选择逻辑在findAdvisorsTahtCanApply方法里。
```Java
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {
    ...
    protected List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
        ProxyCreationContext.setCurrentProxiedBeanName(beanName);

        List var4;
        try {
            var4 = AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        } finally {
            ProxyCreationContext.setCurrentProxiedBeanName((String)null);
        }

        return var4;
    }
}
```

```Java
public abstract class AopUtils {
    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
        if (candidateAdvisors.isEmpty()) {
            return candidateAdvisors;
        } else {
            List<Advisor> eligibleAdvisors = new ArrayList();
            // 迭代出candiateAdvisors里的通知，包括after、before和around通知
            Iterator var3 = candidateAdvisors.iterator();

            while(var3.hasNext()) {
                Advisor candidate = (Advisor)var3.next();
                if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
                    eligibleAdvisors.add(candidate);
                }
            }

            boolean hasIntroductions = !eligibleAdvisors.isEmpty();
            Iterator var7 = candidateAdvisors.iterator();

            while(var7.hasNext()) {
                // 这里遍历通知：after、before和around通知，通过canApply逐一去判断是否可以应用于bean
                Advisor candidate = (Advisor)var7.next();
                if (!(candidate instanceof IntroductionAdvisor) && canApply(candidate, clazz, hasIntroductions)) {
                    eligibleAdvisors.add(candidate);
                }
            }

            return eligibleAdvisors;
        }
    }
    
    public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
        if (advisor instanceof IntroductionAdvisor) {
            return ((IntroductionAdvisor)advisor).getClassFilter().matches(targetClass);
        } else if (advisor instanceof PointcutAdvisor) {
            // 因为在AspectJTest中设置了切点test()，所以程序会走到这里来
            PointcutAdvisor pca = (PointcutAdvisor)advisor;
            return canApply(pca.getPointcut(), targetClass, hasIntroductions);
        } else {
            return true;
        }
    }
    
    
    public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
        Assert.notNull(pc, "Pointcut must not be null");
        // targetClass为要代理的类，这里就是TestBean
        if (!pc.getClassFilter().matches(targetClass)) {
            return false;
        } else {
            // 从Pointcut中获取@Pointcut注解修饰的切点表达式，并封装成MethodMatcher对象
            MethodMatcher methodMatcher = pc.getMethodMatcher();
            if (methodMatcher == MethodMatcher.TRUE) {
                return true;
            } else {
                IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
                if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
                    introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher)methodMatcher;
                }

                Set<Class<?>> classes = new LinkedHashSet();
                if (!Proxy.isProxyClass(targetClass)) {
                    classes.add(ClassUtils.getUserClass(targetClass));
                }

                classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
                Iterator var6 = classes.iterator();

                while(var6.hasNext()) {
                    Class<?> clazz = (Class)var6.next();
                    // 通过反射工具类获取到被代理类的所有成员方法，包括其父类Object的所有方法
                    Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
                    Method[] var9 = methods;
                    int var10 = methods.length;

                    for(int var11 = 0; var11 < var10; ++var11) {
                        Method method = var9[var11];
                        if (introductionAwareMethodMatcher != null) {
                            if (introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions)) {
                                return true;
                            }
                        } else if (methodMatcher.matches(method, targetClass)) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }
}
```

这里总结下findAdvisorsThatCanApply所做的工作。将含有通知的candidateAdvisors集合逐一遍历，通过canApply方法来判断bean是否能够应用这些通知，而在canApply方法中，又通过ReflectionUtils.getAllDeclaredMethods()方法获取targetClass目标代理类的所有方法，包括了Object方法，看看candidateAdvisors集合中的通知都能否应用于targetClass代理类的方法。

这样，AnnotationAwareAspectJAutoProxyCreator就已经筛选出可应用于代理类的通知了，接下来就到了重头戏——对通知进行代理。

#### 1.3 对通知进行代理

**进行代理的前期准备工作**

啥是代理呢？em...这里简单介绍下代理的概念：就是为其他对象提供一种代理，用来控制对这个对象的访问。 这里先不对代理及代理模式展开讨论，后门再专门学习“代理模式”。

回到AbstractAutoProxyCreator的wrapIfNecessary方法中。经过this.getAdvicesAndAdvisorsForBean()方法的工作，获取到了可应用的通知对象数组，接下来的工作就是要对这些通知进行代理了。

```Java
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    ...
    protected Object createProxy(Class<?> beanClass, @Nullable String beanName, @Nullable Object[] specificInterceptors, TargetSource targetSource) {
        // 将beanName设置为目标代理类
        if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
            AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory)this.beanFactory, beanName, beanClass);
        }

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.copyFrom(this);
        if (!proxyFactory.isProxyTargetClass()) {
            if (this.shouldProxyTargetClass(beanClass, beanName)) {
                proxyFactory.setProxyTargetClass(true);
            } else {
                // 判断是否需要添加代理接口，由于这里设置的是<aop:aspectj-autoproxy/>，
                // 自然不会使用到代理接口的方式，所以该方法所做的工作为：proxyFactory.setProxyTargetClass(true);
                this.evaluateProxyInterfaces(beanClass, proxyFactory);
            }
        }

        // 封装通知，然后将advisors添加到ProxyFactory中
        Advisor[] advisors = this.buildAdvisors(beanName, specificInterceptors);
        proxyFactory.addAdvisors(advisors);
        // 设置dialing类
        proxyFactory.setTargetSource(targetSource);
        // 定制代理
        this.customizeProxyFactory(proxyFactory);
        // 用来控制代理工厂被配置之后，是否还允许修改通知；默认值为false，即在代理被配置之后，不允许修改代理的配置。
        proxyFactory.setFrozen(this.freezeProxy);
        if (this.advisorsPreFiltered()) {
            proxyFactory.setPreFiltered(true);
        }

        return proxyFactory.getProxy(this.getProxyClassLoader());
    }
    
    
    protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
        //解析拦截器名并进行注册
        Advisor[] commonInterceptors = this.resolveInterceptorNames();
        List<Object> allInterceptors = new ArrayList();
        if (specificInterceptors != null) {
            // 将通知都封装在allInterceptors中
            allInterceptors.addAll(Arrays.asList(specificInterceptors));
            if (commonInterceptors.length > 0) {
                if (this.applyCommonInterceptorsFirst) {
                    allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
                } else {
                    allInterceptors.addAll(Arrays.asList(commonInterceptors));
                }
            }
        }

        int i;
        if (this.logger.isTraceEnabled()) {
            int nrOfCommonInterceptors = commonInterceptors.length;
            i = specificInterceptors != null ? specificInterceptors.length : 0;
            this.logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors + " common interceptors and " + i + " specific interceptors");
        }

        Advisor[] advisors = new Advisor[allInterceptors.size()];

        for(i = 0; i < allInterceptors.size(); ++i) {
            // 通过advisorAdapterRegistry这个适配器来包装通知
            advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
        }

        return advisors;
    }
}
```

```Java
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {
    public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
        // 如果封装对象本身就是Advisor，则无需做任何处理
        if (adviceObject instanceof Advisor) {
            return (Advisor)adviceObject;
        } else if (!(adviceObject instanceof Advice)) { //如果封装对象不是Advice类型，则不能进行封装。注意：Advice和Adviser的区别！！
            throw new UnknownAdviceTypeException(adviceObject);
        } else { // 如果是advice类型的对象，则进行封装
            Advice advice = (Advice)adviceObject;
            
            if (advice instanceof MethodInterceptor) {
                return new DefaultPointcutAdvisor(advice);
            } else { 
                Iterator var3 = this.adapters.iterator();

                AdvisorAdapter adapter;
                do {
                    if (!var3.hasNext()) {
                        throw new UnknownAdviceTypeException(advice);
                    }

                    adapter = (AdvisorAdapter)var3.next();
                } while(!adapter.supportsAdvice(advice));

                return new DefaultPointcutAdvisor(advice);
            }
        }
    }
}    
```

看到这里，估计小伙伴们有点晕。这里总结一下createProxy方法，整理一下思路。在Spring中对于代理类的创建和处理，都是通过ProxyFactory来处理的，实际上createProxy方法都是围绕着ProxyFactory作初始化操作，一切都是为了创建代理做好准备，这是Spring源码的一贯风格，为某个逻辑做大量的前期准备工作。

1. 获取当前类中的属性
2. 添加代理接口，将拦截器封装为通知
3. 封装Advisor
4. 设置要代理的类
5. 对ProxyFactory进行定制化



**获取代理方式**

```Java
public class ProxyFactory extends ProxyCreatorSupport {
    ...
    public Object getProxy(@Nullable ClassLoader classLoader) {
        return this.createAopProxy().getProxy(classLoader);
    }
}
```
```Java
public class ProxyCreatorSupport extends AdvisedSupport {
    protected final synchronized AopProxy createAopProxy() {
        if (!this.active) {
            this.activate();
        }
        // 使用我们刚刚做完初始化工作的ProxyFactory来创建代理
        return this.getAopProxyFactory().createAopProxy(this);
    }
}
```

```Java
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {
    public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
        // 如果aop配置文件没有配置属性<aop:aspectj-autoproxy />属性，则返回JdkDynamicAopProxy的实例对象
        if (!config.isOptimize() && !config.isProxyTargetClass() && !this.hasNoUserSuppliedProxyInterfaces(config)) {
            return new JdkDynamicAopProxy(config);
        } else {
            Class<?> targetClass = config.getTargetClass();
            if (targetClass == null) {
                throw new AopConfigException("TargetSource cannot determine target class: Either an interface or a target is required for proxy creation.");
            } else {
                // targetClass就是示例中的TestBean，由于TestBean不是借口，并且不是代理类，所以要返回的ObjenesisCglibAopProxy实例对象，也就是CGLIB代理
                return (AopProxy)(!targetClass.isInterface() && !Proxy.isProxyClass(targetClass) ? new  ObjenesisCglibAopProxy(config) : new JdkDynamicAopProxy(config));
            }
        }
    }
}
```

这里是获取的是CGLIB代理。

### 2. 总结

深入学习了AnnotationAwareAspectJAutoProxyCreator源码之后，发现其工作可以概括为以下几点：
1. 解析通知存储到缓存中
2. 从缓存中获取通知，筛选通知
3. 包装通知，初始化ProxyFactory
4. 使用ProxyFactory来创建代理（JDK动态代理、CGLIB代理）
5. 代理最终实现AOP的核心逻辑



由于JDK动态代理和CGLIB代理源码介绍篇幅过长，源码介绍放在下一篇博文中。
