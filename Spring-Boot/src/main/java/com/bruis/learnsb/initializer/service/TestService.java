package com.bruis.learnsb.initializer.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
public class TestService implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 将SpringIOC容器注册到该类的成员变量中
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String test() {
        // 从注册进Environment中的map中获取值
        return applicationContext.getEnvironment().getProperty("coderbruis");
    }

    public String test2() {
        // 从注册进Environment中的map中获取值
        return applicationContext.getEnvironment().getProperty("key");
    }

}
