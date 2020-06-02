package com.bruis.learnsb.event;

/**
 * @author LuoHaiYang
 */
public interface EventMulticaster {
    void multicastEvent(WeatherEvent event);
    void addListener(WeatherListener weatherListener);
    void removeListener(WeatherListener weatherListener);
}
