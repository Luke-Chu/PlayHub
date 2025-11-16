package com.luke.playhub.config;

import com.luke.playhub.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器，并指定拦截所有请求（可根据需要调整路径）
        registry.addInterceptor(new UserInterceptor())
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns("/user/login", "/user/register"); // 排除不需要拦截的路径（如登录、注册）
    }
}