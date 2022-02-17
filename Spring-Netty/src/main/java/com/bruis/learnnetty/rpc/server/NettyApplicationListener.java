package com.bruis.learnnetty.rpc.server;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * @author lhy
 * @date 2022/2/17
 */
@Component
public class NettyApplicationListener implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 开启额外线程启动Netty服务
        new Thread() {
            @Override
            public void run() {
                NettyServer.start();
            }
        }.start();
    }
}
