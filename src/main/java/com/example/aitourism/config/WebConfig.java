package com.example.aitourism.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LogInterceptor logInterceptor;

    // 通过构造器注入拦截器
    public WebConfig(LogInterceptor logInterceptor) {
        this.logInterceptor = logInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册日志拦截器，并配置拦截路径
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**"); // 拦截所有路径

        // 如果您还有其他拦截器，可以继续添加
        // registry.addInterceptor(otherInterceptor)...
    }
}
