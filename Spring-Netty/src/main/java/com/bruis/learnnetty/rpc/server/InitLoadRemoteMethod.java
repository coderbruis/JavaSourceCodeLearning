package com.bruis.learnnetty.rpc.server;

import com.bruis.learnnetty.rpc.utils.Mediator;
import com.bruis.learnnetty.rpc.utils.Remote;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author lhy
 * @date 2022/2/17
 */
@Component
public class InitLoadRemoteMethod implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent context) {
        // 获取Spring容器中带有@Controller的注解类
        Map<String, Object> controllerBeans = context.getApplicationContext()
                .getBeansWithAnnotation(Controller.class);
        for (String beanName : controllerBeans.keySet()) {
            Object beanObj = controllerBeans.get(beanName);
            // 获取这个bean的方法集合
            Method[] methods = beanObj.getClass().getMethods();
            for (Method method : methods) {
                // 判断这个方法是否带有@Remote注解
                if (method.isAnnotationPresent(Remote.class)) {
                    Remote remote = method.getAnnotation(Remote.class);
                    // 注解的值
                    String remoteValue = remote.value();
                    // 缓存这个类
                    Mediator.MethodBean methodBean = new Mediator.MethodBean();
                    methodBean.setBean(beanObj);
                    methodBean.setMethod(method);
                    // @Remote的value值作为key，MethodBean作为value
                    Mediator.methodBeans.put(remoteValue, methodBean);
                }
            }
        }
    }

    /**
     * 值越小优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
