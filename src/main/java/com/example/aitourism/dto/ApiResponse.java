package com.example.aitourism.dto;

import com.example.aitourism.util.Constants;
import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class ApiResponse<T> {
    private Integer code;
    private String msg;
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Constants.STATUS_SUCCESS, "ok", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(Constants.STATUS_SUCCESS, "ok", null);
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}