package com.bruis.learnsb.property;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
public class ResultCommandLineRunner implements CommandLineRunner, EnvironmentAware {
    private Environment environment;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(environment.getProperty("mooc.website.url"));
    }

    @Override
    public void setEnvironment(Environment environment) {
        // 将SpringIOC容器中的
        this.environment = environment;
    }
}
