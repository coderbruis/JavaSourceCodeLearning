package com.bruis.springsecurity.utils.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author LuoHaiYang
 *
 * 登录检查过滤器, 原逻辑是在BasicAuthenticationFilter类中.
 * 即判断请求头中是否包含：Authorization, 是否包含basic。
 *
 * 这个类非常重要。
 *
 */
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 获取token，并检查登录状态
        // super.doFilterInternal(request, response, chain);
        SecurityUtils.checkAuthentication(request);
        chain.doFilter(request, response);
    }
}
