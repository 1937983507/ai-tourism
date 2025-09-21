package com.example.aitourism.dto;

import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class ApiResponse<T> {
    private Integer code;
    private String msg;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(0);
        r.setMsg("OK");
        r.setData(data);
        return r;
    }

    public static <T> ApiResponse<T> fail(String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(-1);
        r.setMsg(msg);
        return r;
    }
}