package com.example.aitourism.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import com.example.aitourism.dto.ApiResponse;
import com.example.aitourism.util.Constants;
import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLogin(NotLoginException e) {
        log.warn("NotLoginException caught: {}", e.getMessage());
        return ApiResponse.error(Constants.ERROR_CODE_TOKEN_EXPIRED, "token已过期，请刷新");
    }
}