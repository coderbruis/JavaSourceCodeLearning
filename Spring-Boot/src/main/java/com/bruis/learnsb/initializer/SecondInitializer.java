package com.bruis.learnsb.initializer;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LuoHaiYang
 *
 * 模拟SpringBoot的系统初始化器的调用
 *
 */
@Order(2)
public class SecondInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        ConfigurableEnvironment environment = configurableApplicationContext.getEnvironment();
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        MapPropertySource mapPropertySource = new MapPropertySource("SecondInitializer", map);
        environment.getPropertySources().addLast(mapPropertySource);
        System.out.println("run SecondInitializer");
    }
}
