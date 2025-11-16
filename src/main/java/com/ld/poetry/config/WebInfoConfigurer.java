package com.ld.poetry.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Component  // 暂时禁用拦截器，减少启动时间
public class WebInfoConfigurer implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器已禁用，减少启动开销
        // registry.addInterceptor(new WebInfoHandlerInterceptor())
        //         .addPathPatterns("/**")
        //         .excludePathPatterns("/user/login", "/admin/**", "/webInfo/getWebInfo", "/webInfo/updateWebInfo");
    }
}
