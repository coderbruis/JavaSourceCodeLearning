package com.bruis.learnsb.initializer.controller;

import com.bruis.learnsb.initializer.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LuoHaiYang
 */
@RestController
public class TestController {

    @Autowired
    private TestService testService;

    @GetMapping("/testBootInitializer")
    public String test() {
        return testService.test();
    }

    @GetMapping("/testBootInitializer2")
    public String test2() {
        return testService.test2();
    }

}
