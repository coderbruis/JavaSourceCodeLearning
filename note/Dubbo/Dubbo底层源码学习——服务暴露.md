```
package org.apache.dubbo.rpc;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class Protocol$Adaptive implements org.apache.dubbo.rpc.Protocol {
	public void destroy()  {
		throw new UnsupportedOperationException("The method public abstract void org.apache.dubbo.rpc.Protocol.destroy() of interface org.apache.dubbo.rpc.Protocol is not adaptive method!");
	}
	public int getDefaultPort()  {
		throw new UnsupportedOperationException("The method public abstract int org.apache.dubbo.rpc.Protocol.getDefaultPort() of interface org.apache.dubbo.rpc.Protocol is not adaptive method!");
	}
	public org.apache.dubbo.rpc.Invoker refer(java.lang.Class arg0, org.apache.dubbo.common.URL arg1) throws org.apache.dubbo.rpc.RpcException {
		if (arg1 == null) throw new IllegalArgumentException("url == null");
		org.apache.dubbo.common.URL url = arg1;
		String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );
		if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.Protocol) name from url (" + url.toString() + ") use keys([protocol])");
		org.apache.dubbo.rpc.Protocol extension = (org.apache.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Protocol.class).getExtension(extName);
		return extension.refer(arg0, arg1);
	}
	public org.apache.dubbo.rpc.Exporter export(org.apache.dubbo.rpc.Invoker arg0) throws org.apache.dubbo.rpc.RpcException {
		if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument == null");
		if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument getUrl() == null");
		org.apache.dubbo.common.URL url = arg0.getUrl();
		String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );
		if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.Protocol) name from url (" + url.toString() + ") use keys([protocol])");
		// 此处传入的extName为dubbo，extension获得的扩展点实现类为ProtocolFilterWrapper
		org.apache.dubbo.rpc.Protocol extension = (org.apache.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Protocol.class).getExtension(extName);
		return extension.export(arg0);
	}
	public java.util.List getServers()  {
		throw new UnsupportedOperationException("The method public default java.util.List org.apache.dubbo.rpc.Protocol.getServers() of interface org.apache.dubbo.rpc.Protocol is not adaptive method!");
	}
}
```

```
package org.apache.dubbo.rpc;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class ProxyFactory$Adaptive implements org.apache.dubbo.rpc.ProxyFactory {
	public java.lang.Object getProxy(org.apache.dubbo.rpc.Invoker arg0) throws org.apache.dubbo.rpc.RpcException {
		if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument == null");
		if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument getUrl() == null");
		org.apache.dubbo.common.URL url = arg0.getUrl();
		String extName = url.getParameter("proxy", "javassist");
		if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.ProxyFactory) name from url (" + url.toString() + ") use keys([proxy])");
		org.apache.dubbo.rpc.ProxyFactory extension = (org.apache.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.ProxyFactory.class).getExtension(extName);
		return extension.getProxy(arg0);
	}
	public java.lang.Object getProxy(org.apache.dubbo.rpc.Invoker arg0, Boolean arg1) throws org.apache.dubbo.rpc.RpcException {
		if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument == null");
		if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument getUrl() == null");
		org.apache.dubbo.common.URL url = arg0.getUrl();
		String extName = url.getParameter("proxy", "javassist");
		if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.ProxyFactory) name from url (" + url.toString() + ") use keys([proxy])");
		org.apache.dubbo.rpc.ProxyFactory extension = (org.apache.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.ProxyFactory.class).getExtension(extName);
		return extension.getProxy(arg0, arg1);
	}
	public org.apache.dubbo.rpc.Invoker getInvoker(java.lang.Object arg0, java.lang.Class arg1, org.apache.dubbo.common.URL arg2) throws org.apache.dubbo.rpc.RpcException {
		if (arg2 == null) throw new IllegalArgumentException("url == null");
		org.apache.dubbo.common.URL url = arg2;
		String extName = url.getParameter("proxy", "javassist");
		if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.ProxyFactory) name from url (" + url.toString() + ") use keys([proxy])");
		// 这里获取到的ProxyFactory的扩展点是StubProxyFactoryWrapper，StubProxyFactoryWrapper是ProxyFactory的实现类，由于ProxyFactory注解@SPI("javassist")，所以extName取javassist值
		org.apache.dubbo.rpc.ProxyFactory extension = (org.apache.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.ProxyFactory.class).getExtension(extName);
		// 所以去调用了StubProxyFactoryWrapper#getInvoker()方法，在该方法中调用的是JavassistProxyFactory的getInvoker方法，最终实际
		// 返回的是一个JavassistProxyFactory的一个匿名内部类：AbstractProxyInvoker
		return extension.getInvoker(arg0, arg1, arg2);
	}
}
```

```
package org.apache.dubbo.remoting;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class Transporter$Adaptive implements org.apache.dubbo.remoting.Transporter {
	public org.apache.dubbo.remoting.Client connect(org.apache.dubbo.common.URL arg0, org.apache.dubbo.remoting.ChannelHandler arg1) throws org.apache.dubbo.remoting.RemotingException {
		if (arg0 == null) throw new IllegalArgumentException("url == null");
		org.apache.dubbo.common.URL url = arg0;
		String extName = url.getParameter("client", url.getParameter("transporter", "netty"));
		if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Transporter) name from url (" + url.toString() + ") use keys([client, transporter])");
		org.apache.dubbo.remoting.Transporter extension = (org.apache.dubbo.remoting.Transporter)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Transporter.class).getExtension(extName);
		return extension.connect(arg0, arg1);
	}
	public org.apache.dubbo.remoting.RemotingServer bind(org.apache.dubbo.common.URL arg0, org.apache.dubbo.remoting.ChannelHandler arg1) throws org.apache.dubbo.remoting.RemotingException {
		if (arg0 == null) throw new IllegalArgumentException("url == null");
		org.apache.dubbo.common.URL url = arg0;
		String extName = url.getParameter("server", url.getParameter("transporter", "netty"));
		if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Transporter) name from url (" + url.toString() + ") use keys([server, transporter])");
		// 这里获取的扩展点实现类是netty4包下的NettyTransporter
		org.apache.dubbo.remoting.Transporter extension = (org.apache.dubbo.remoting.Transporter)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Transporter.class).getExtension(extName);
		return extension.bind(arg0, arg1);
	}
}
```

对于JavassistProxyFactory对象中Invoker类型的匿名内部类，类结构如下图所示
![export01](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/images/Dubbo/export01.png)

JavassistProxyFactory代码如下：
```
public class JavassistProxyFactory extends AbstractProxyFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // TODO Wrapper cannot handle this scenario correctly: the classname contains '$'
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }

}
```


- testDubboProtocol()调用流程

1. 首先protocol和proxy变量分别在DubboProtocolTest.java类加载过程时就已经分别加载好了，这里的Protocol和ProxyFactory对象都生成的是代理对象，分别为Protocol$Adaptive和ProxyFactory$Adaptive对象，动态代理处的代码在上面展示了；
2. 首先调用proxy#getInvoker()，调用的实际是ProxyFactory$Adaptive代理对象的getInvoker()方法，在上图可以看到具体逻辑，其方法逻辑中需要去获取ProxyFactory对象的扩展点实现类，经过调试ProxyFactory对象的扩展点实现类为StubProxyFactoryWrapper，最终会调用到JavassistProxyFactory的getInvoker()方法，它会最终生成一个AbstractProxyInvoker()匿名类对象，所以调试的时候可以发现invoker的引用是JavassistProxyFactory$1；
3. 获取到invoker之后，就传入到Protocol的扩展点实现类中去调用export()方法，由上图的Protocol$Adaptive可以知道，在export()方法中会去生成Protocol的扩展点实现类，这里的实现类是ProtocolFilterWrapper（这里为什么是这个实现类呢？去调研一下）。经过代理类的传递包装，最终来到了ProtocolFilterWrapper#export()方法；
4. 接着就是调用ProtocolFilterWrapper#buildInvocation()方法，构建调用链。就是先去获取Filter的扩展点实现类集合对象，然后倒叙遍历该集合对象然后将invoker对象和最后一个Filter对象封装为另一个Invoker，然后再继续传递到和上一个Filter对象继续封装成Invoker，以此往复封装。最终的结果就是将传入的invoker对象封装到了最后一个Filter之后，请求进来之后需要调用所有的Filter过后才会调用invoker对象，形成了一个Invoker链；

效果如下：
![export02](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/images/Dubbo/export02.png)

还需要注意的是，在ProtocolFilterWrapper#buildInvokerChain()方法中，生成的是匿名对象，即将3中传递过来的JavassistProxyFactory$1又包装起来放在了ProtocolFilterWrapper对象中，所以你会发现此时的invoker对象是ProtocolFilterWrapper$1；

5. 在ProtocolFilterWrapper#export()中，会调用ProtocolListenerWrapper#export()对象，在ProtocolListenerWrapper#export()中会返回一个ListenerExporterWrapper的匿名内部类对象，生成该匿名内部类之前会调用DubboProtocol#export()方法。
6. 千辛万苦，终于来到了DubboProtocol#export()的openServer()方法了，在openServer()方法中，会调用createServer(url)方法去创建Server；在DubboProtocol中会调用Exchangers#bind()去和Netty服务端进行绑定？
7. 随后调用Exchangers#getExchanger()方法去获取Exchanger的扩展点实现类，他的扩展点实现类是HeaderExchanger，即调用的是HeaderExchagner#bind()方法。在其方法中会构造一个HeaderExchangeServer对象，但是在传入入参时需要调用Transporters#bind()方法，将返回的对象作为入参。
8. 随后调用Transporters#getTransporter()方法，去获取Transporter的自适应扩展点实现类，这里实际获取的是一个代理类，是Transporter$Adaptive，类实现体在上面。接着就是调用的Transporter$Adaptive的bind()方法。最终去调用的netty4包下的NettyTransporter#bind()