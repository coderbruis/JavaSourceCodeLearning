package com.bruis.learnsb.bean;

import com.bruis.learnsb.initializer.service.TestService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author LuoHaiYang
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBeanTest {

    @Autowired
    private Person person;

    @Autowired
    private TestService testService;

    @Test
    public void testInitializer() {
        System.out.println("Initializer: " + testService.test());
    }

    @Test
    public void test() {
        System.out.println(person.getName() + "'s age = " + person.getAge() + ", and phonto = " + person.getPhone());
    }
}
