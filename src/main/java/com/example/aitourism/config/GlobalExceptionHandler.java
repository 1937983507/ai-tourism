package com.example.aitourism.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.aitourism.dto.ApiResponse;

import cn.dev33.satoken.exception.NotLoginException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLogin(NotLoginException e) {
        // 返回特定的错误码，告诉前端需要刷新 token
        return ApiResponse.error(1101, "token已过期，请刷新");
    }
}