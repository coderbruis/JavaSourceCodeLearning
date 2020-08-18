## 前言
相信了解过SpringSecurity或者是OAuth2的读者，会发现网上会有非常多的相关文章，或是纯概念的，或是带有demo的，无论是哪种类型的文章，本人去阅读之后，对于整个框架的概念还是一知半解，也仅仅是实现了某些功能、某些效果而已，若遇到某些问题时无从下手，只能去百度去Google。这是因为对于SpringSecurity和OAuth2的知识没有一个整体概念的把握，知识体系没有形成系统，遂决定写一个关于SpringSecurity和OAuth2的系列专栏，在建造自己知识体系的同时还希望能帮助有同样困惑的同学。

============== 分割线 ==============

本章重点介绍一下FilterChainProxy中几个重要的过滤器链。

## 正文
在系列文章中的第一篇中，已经用一张图介绍了FilterChainProxy在整个Filter中的所处位置以及包含在FilterChainProxy的各种Filter。
地址如下：

[从零开始系统学习SpringSecurity和OAuth2（一）—— 初识SpringSecurity](https://blog.csdn.net/CoderBruis/article/details/107297547)

本篇文章就讲解一下SecurityContextPersistenceFilter、ExceptionTranslationFilter和FilterSecurityInterceptor这三个过滤器，注意这里的FilterSecurityInterceptor就是一个过滤器。

### 1. SecurityContextPersistenceFilter
这个过滤器是FilterChainProxy过滤器链的中第一个调用的，先看下它的官方类注释的内容，总结为以下几点：
1. 在请求之前，使用从已配置的SecurityContextRepository中获取的认证信息来填充SecurityContextHolder，在请求完成后清除上下文所有者。
2. 默认情况下，SecurityContextRepository使用的是HttpSessionSecurityContextRepository作为实现类，有关于HttpSession的配置选项信息请查看HttpSessionSecurityContextRepository。
3. 这个过滤器会在每次请求时都会调用，为的是解决servlet容器的兼容性（特别是Weblogic）。
4. 这个过滤器必须在任何认证处理机制调用前执行，例如BASIC、CAS认证处理过滤器等都期望在它们执行时能从SecurityContextHolder中获取一个合法的SecurityContext。
5. 这个过滤器实质上是对HttpSessionSecurityContextRepository进行了重构，以将存储问题委托给了单独的策略，从而允许在请求之间维护安全上下文的方式进行更多自定义。

首先看下SecurityContextPersistenceFilter的类结构：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200819004933998.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70#pic_center)
首先它的父类GenericeFilterBean实现了很多接口，其中有三个XXAware的接口，表示的是具备注入XX到GenericFilterBean能力，而这一般都是通过setter来注入XX实现的。

而实现了DisposalbleBean表明了在注销bean时能进行额外的工作，实现InitializingBean表明了能在初始化时进行额外的工作。

> 上源码

```
public class SecurityContextPersistenceFilter extends GenericFilterBean {

	static final String FILTER_APPLIED = "__spring_security_scpf_applied";

	private SecurityContextRepository repo;

	private boolean forceEagerSessionCreation = false;

	// 默认构造方法，传入HttpSessionSecurityContextRepository
	public SecurityContextPersistenceFilter() {
		this(new HttpSessionSecurityContextRepository());
	}

	public SecurityContextPersistenceFilter(SecurityContextRepository repo) {
		this.repo = repo;
	}

	// 过滤核心方法：doFilter
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		
		// 判断请求中是否包含属性：__spring_security_scpf_applied，表示已经调用过SecurityContextPersistenceFilter了
		if (request.getAttribute(FILTER_APPLIED) != null) {
			// 确保每次请求只调用一次该过滤器
			chain.doFilter(request, response);
			return;
		}

		final boolean debug = logger.isDebugEnabled();
		// 为此次请求设置__spring_security_scpf_applied为true，防止下次同样请求再次调用进来时，重复执行以下逻辑
		request.setAttribute(FILTER_APPLIED, Boolean.TRUE);

		// 急切的想要创建Session
		if (forceEagerSessionCreation) {
			HttpSession session = request.getSession();

			if (debug && session.isNew()) {
				logger.debug("Eagerly created session: " + session.getId());
			}
		}
		
		HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request,
				response);
		// 从HttpSessionSecurityContextRepository中获取SecurityContext安全上下文
		SecurityContext contextBeforeChainExecution = repo.loadContext(holder);

		try {
			// 将获取的安全上下文存储到SecurityContextHolder中
			SecurityContextHolder.setContext(contextBeforeChainExecution);
			// 放过滤器链执行
			chain.doFilter(holder.getRequest(), holder.getResponse());

		}
		finally {
			// 等FilterChainProxy后面所有过滤器链都执行完毕时，进入finally块
			// 获取FilterChainProxy调用后的SecurityContext
			SecurityContext contextAfterChainExecution = SecurityContextHolder
					.getContext();
			// 清除SecurityContextHolder
			SecurityContextHolder.clearContext();
			// 将安全上下文存储到HttpSessionSecurityContextRepository中
			repo.saveContext(contextAfterChainExecution, holder.getRequest(),
					holder.getResponse());
			request.removeAttribute(FILTER_APPLIED);

			if (debug) {
				logger.debug("SecurityContextHolder now cleared, as request processing completed");
			}
		}
	}

	public void setForceEagerSessionCreation(boolean forceEagerSessionCreation) {
		this.forceEagerSessionCreation = forceEagerSessionCreation;
	}
}

```

### 2. ExceptionTranslationFilter

### 3. FilterSecurityInterceptor


.... 待续