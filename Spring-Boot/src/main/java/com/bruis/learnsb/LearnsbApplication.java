package com.bruis.learnsb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import java.util.Properties;

@SpringBootApplication
@PropertySource({"demo.properties"})
public class LearnsbApplication {

    public static void main(String[] args) {
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


        SpringApplication springApplication = new SpringApplication(LearnsbApplication.class);
        Properties properties = new Properties();
        // 从配置文件中获取
        properties.setProperty("bruis.github.url", "01-https://github.com/coderbruis/JavaSourceLearning");
        springApplication.setDefaultProperties(properties);
        springApplication.run(args);

    }

}
