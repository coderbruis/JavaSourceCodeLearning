package com.bruis.learnsb.event;

/**
 * @author LuoHaiYang
 */
public class RainEvent extends WeatherEvent{
    @Override
    public String getWeather() {
        return "rain...";
    }
}
