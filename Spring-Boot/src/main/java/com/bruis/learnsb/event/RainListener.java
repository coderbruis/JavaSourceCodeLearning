package com.bruis.learnsb.event;

import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
public class RainListener implements WeatherListener{
    @Override
    public void onWeatherEvent(WeatherEvent event) {
        if (event instanceof RainEvent) {
            System.out.println("hello " + event.getWeather());
        }
    }
}
