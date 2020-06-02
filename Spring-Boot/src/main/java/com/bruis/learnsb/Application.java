package com.bruis.learnsb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import java.util.Properties;

@SpringBootApplication
@PropertySource({"demo.properties"})
public class Application {

    public static void main(String[] args) {
        //SpringApplication.run(LearnjavaApplication.class, args);
        // 注册系统初始化器
        /*
        SpringApplication springApplication = new SpringApplication(LearnjavaApplication.class);
        springApplication.addInitializers(new SecondInitializer());
        springApplication.run(args);
        */

        // add Listeners
        //SpringApplication springApplication = new SpringApplication(LearnjavaApplication.class);
        //springApplication.addListeners(new SecondListener());
        //springApplication.run();

        SpringApplication springApplication = new SpringApplication(Application.class);
        Properties properties = new Properties();
        properties.setProperty("mooc.website.url", "http://www.mooc.cn");
        springApplication.setDefaultProperties(properties);
        springApplication.run(args);
    }

}
