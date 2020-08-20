package com.bruis.learnsb.startup;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Order(1)
@Component
public class FirstApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("\u001B[32m >>> first application runner<<<");
    }
}
