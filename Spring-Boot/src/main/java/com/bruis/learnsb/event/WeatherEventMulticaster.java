package com.bruis.learnsb.event;

import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
public class WeatherEventMulticaster extends AbstractEventMulticaster{

    @Override
    void doStart() {
        System.out.println("begin broadcast weather event");
    }

    @Override
    void doEnd() {
        System.out.println("end broadcast weather event");
    }
}
