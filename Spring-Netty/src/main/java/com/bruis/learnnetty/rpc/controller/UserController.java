package com.bruis.learnnetty.rpc.controller;

import com.bruis.learnnetty.rpc.utils.Remote;
import org.springframework.stereotype.Controller;

/**
 * @author lhy
 * @date 2022/2/17
 */
@Controller
public class UserController {
    @Remote(value = "getUserNameById")
    public Object getUserNameById(String userId) {
        System.out.println(Thread.currentThread().getName() + "-> 接受到请求：" + userId);
        return "做了业务处理了，结果是用户编号userId为" + userId + "的用户姓名为张三";
    }
}
