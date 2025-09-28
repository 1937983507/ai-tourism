package com.example.aitourism.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.aitourism.dto.BaseResponse;
import com.example.aitourism.util.Constants;
import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<Void> handleNotLogin(NotLoginException e) {
        log.warn("NotLoginException caught: {}", e.getMessage());
        return BaseResponse.error(Constants.ERROR_CODE_TOKEN_EXPIRED, "token已过期，请刷新");
    }
}