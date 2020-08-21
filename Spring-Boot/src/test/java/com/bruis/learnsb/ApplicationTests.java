package com.bruis.learnsb;

import com.bruis.learnsb.condi.ConditionTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void contextLoads() {
    }

    /**
     * 测试当前SpringIOC容器中是否存在ConditionTest这个bean
     */
    @Test
    public void testConditionTest() {
        System.out.println(applicationContext.getBean(ConditionTest.class));
    }

}
