package com.bruis.learnsb.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author LuoHaiYang
 */
@Configuration
public class BeanConfiguration {

    @Bean("person")
    public Person getPerson() {
        Person person = new Person();
        person.setName("Bruis");
        person.setAge(20);
        person.setPhone("18483222717");
        return person;
    }

}
