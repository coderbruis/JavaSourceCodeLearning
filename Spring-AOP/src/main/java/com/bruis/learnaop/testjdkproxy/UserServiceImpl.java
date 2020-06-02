package com.bruis.learnaop.testjdkproxy;

public class UserServiceImpl implements UserService {
    @Override
    public void purchase() {
        /**
         * 需要代理的实现类
         */
        System.out.println("--------------------购买东西-----------------");
    }
}
