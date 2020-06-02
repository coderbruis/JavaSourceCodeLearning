package com.bruis.learnsb.bean;

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
public class SpringBeanTest {
    @Autowired
    private Person person;

    @Test
    public void test() {
        System.out.println(person.getName() + "'s age = " + person.getAge() + ", and phonto = " + person.getPhone());
    }
}
