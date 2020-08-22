package com.bruis.springsecurity.controller;

import com.bruis.springsecurity.model.HttpResult;
import com.bruis.springsecurity.model.LoginRequestParam;
import com.bruis.springsecurity.utils.security.JwtAuthenticationToken;
import com.bruis.springsecurity.utils.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author LuoHaiYang
 */
@RestController
public class LoginController {

    @Autowired
    AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public HttpResult login(@RequestBody LoginRequestParam loginRequestParam, HttpServletRequest request) throws IOException {
        String username = loginRequestParam.getUsername();
        String password = loginRequestParam.getPassword();

        JwtAuthenticationToken token = SecurityUtils.login(request, username, password, authenticationManager);

        return HttpResult.ok(token);
    }
}
