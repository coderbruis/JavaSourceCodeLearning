package com.bruis.learnsb.event;

import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
public class SnowListener implements WeatherListener {
    @Override
    public void onWeatherEvent(WeatherEvent event) {
        if (event instanceof SnowEvent) {
            System.out.println("hello " + event.getWeather());
        }
    }
}
