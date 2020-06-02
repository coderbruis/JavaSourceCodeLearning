package com.learnjava.event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author LuoHaiYang
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootWatchEventTest {

    @Autowired
    private WeatherRunListener weatherRunListener;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testEvent() {
        weatherRunListener.rain();
        weatherRunListener.snow();
    }

}
