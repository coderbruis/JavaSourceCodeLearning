package com.bruis.springsecurity.utils.security;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bruis.springsecurity.utils.HttpUtils;
import com.bruis.springsecurity.utils.JwtTokenUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * 启动登录认证流程过滤器
 * @author LuoHaiYang
 */
public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

    public static final String UFT8 = "UTF-8";

    public JwtLoginFilter(AuthenticationManager authenticationManager) {
        setAuthenticationManager(authenticationManager);
    }

    /**
     *
     * super.doFilter() 主要调用的是AbstractAuthenticationProcessingFilter方法中的doFilter()
     *
     * @param req
     * @param res
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        // POST请求时，对 /login 登录请求进行拦截，由此方法触发执行的登录认证流程，可以在此覆写整个登录认证流程
        super.doFilter(req, res, chain);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // 可以在此覆写尝试进行登录认证的逻辑，登录成功之后等操作不再此方法内
        // 如果使用此过滤器来触发登录认证流程，注意登录请求数据格式的问题
        // 此过滤器的用户名密码默认从request.getParameter()获取，但是这种
        // 读取方式不能读取到如 application/json 等 post 请求数据，需要把
        // 用户名密码的读取逻辑修改为到流中读取request.getInputStream()

        String body = getBody(request);
        JSONObject jsonObject = JSON.parseObject(body);
        // 从json类型请求中获取username
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        JwtAuthenticationToken token = new JwtAuthenticationToken(username, password);

        // 调用UsernamePasswordAuthenticationFilter#setDetails方法。
        setDetails(request,token);

        // return super.attemptAuthentication(request, response);
        return this.getAuthenticationManager().authenticate(token);
    }

    /**
     * 认证成功之后
     * @param request
     * @param response
     * @param chain
     * @param authResult
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        // super.successfulAuthentication(request, response, chain, authResult);
        // 存储登录认证信息到上下文中
        SecurityContextHolder.getContext().setAuthentication(authResult);
        // 记住我, 调用父类的getRememberMeServices
        getRememberMeServices().loginSuccess(request, response, authResult);
        // 触发事件监听器
        if (this.eventPublisher != null) {
            eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
        }
        // 生成并返回token给客户端，后续访问携带此token
        JwtAuthenticationToken token = new JwtAuthenticationToken(null, null, JwtTokenUtils.generateToken(authResult));
        HttpUtils.write(response, token);
    }

    /**
     * 获取请求Body
     * @param request
     * @return
     */
    public String getBody(HttpServletRequest request) {
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            inputStream = request.getInputStream();
            // 将输入流中报装为BufferedReader
            reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(UFT8)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return stringBuilder.toString();
    }
}
