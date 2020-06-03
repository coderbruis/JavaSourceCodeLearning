package com.bruis.learnsb.property;

import com.bruis.learnsb.aware.Flag;
import com.bruis.learnsb.aware.MyAware;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
public class ResultCommandLineRunner implements CommandLineRunner, EnvironmentAware, MyAware {

    private Environment environment;

    private Flag flag;

    @Override
    public void run(String... args) throws Exception {
        // 启动框架时获取配置信息
        System.out.println(environment.getProperty("bruis.github.url"));
        System.out.println(environment.getProperty("random.value"));
        System.out.println(environment.getProperty("avg.age"));
        System.out.println(environment.getProperty("system.path"));
        System.out.println(environment.getProperty("java.vm"));
        System.out.println("flag = " + flag.isCanOperate());
    }

    @Override
    public void setEnvironment(Environment environment) {
        // 将SpringIOC容器中的
        this.environment = environment;
    }

    @Override
    public void setFlag(Flag flag) {
        this.flag = flag;
    }
}
