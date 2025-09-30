package com.example.aitourism.exception;

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

    /**
     * 处理 AI 输入校验异常，返回友好提示给前端
     */
    @ExceptionHandler(InputValidationException.class)
    public BaseResponse<Void> handleInputValidationException(InputValidationException e) {
        log.warn("接收到AI输入校验异常: {}", e.getMessage());
        return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, e.getMessage());
    }
}