package com.example.aitourism.dto;

import com.example.aitourism.util.Constants;
import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class BaseResponse<T> {
    private Integer code;
    private String msg;
    private T data;

    public BaseResponse() {}

    public BaseResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BaseResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(Constants.STATUS_SUCCESS, "ok", data);
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(Constants.STATUS_SUCCESS, "ok", null);
    }

    public static <T> BaseResponse<T> error(int code, String msg) {
        return new BaseResponse<>(code, msg, null);
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