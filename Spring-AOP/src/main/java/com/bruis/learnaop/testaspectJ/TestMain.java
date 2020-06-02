package com.bruis.learnaop.testaspectJ;

import com.bruis.learnaop.model.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestMain {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("aspectTest.xml");
        TestBean bean = (TestBean)context.getBean("testaspectJ");
        bean.test();
    }
}
