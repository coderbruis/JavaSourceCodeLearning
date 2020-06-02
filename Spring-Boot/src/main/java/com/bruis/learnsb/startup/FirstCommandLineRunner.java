package com.bruis.learnsb.startup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 *
 * 启动加载器
 *
 * @author LuoHaiYang
 */
@Component
@Order(1)
public class FirstCommandLineRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // 给\u001B 给字段加颜色
        System.out.println("\u001B[32m >>> startup first runner<<<");
    }

}
