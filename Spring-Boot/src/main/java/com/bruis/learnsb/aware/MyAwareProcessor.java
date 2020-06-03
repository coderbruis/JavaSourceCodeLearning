package com.bruis.learnsb.aware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * 自定义实现的aware处理器
 *
 * @author LuoHaiYang
 */
@Component
public class MyAwareProcessor implements BeanPostProcessor {

    private final ConfigurableApplicationContext configurableApplicationContext;

    public MyAwareProcessor(ConfigurableApplicationContext configurableApplicationContext) {
        this.configurableApplicationContext = configurableApplicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof Aware) {
            if (bean instanceof MyAware) {
                ((MyAware) bean).setFlag((Flag) configurableApplicationContext.getBean("flag"));
            }
        }

        return bean;
    }

}
