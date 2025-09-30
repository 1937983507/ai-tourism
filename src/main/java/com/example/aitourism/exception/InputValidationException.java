package com.example.aitourism.exception;

/**
 * 输入校验异常：用于用户输入不合规时抛出，便于前端友好提示。
 */
public class InputValidationException extends RuntimeException {
    public InputValidationException(String message) {
        super(message);
    }
}
