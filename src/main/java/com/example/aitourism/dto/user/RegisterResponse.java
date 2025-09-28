package com.example.aitourism.dto.user;

import lombok.Data;

/**
 * 注册响应DTO
 */
@Data
public class RegisterResponse {
    
    private String user_id;
    
    public RegisterResponse() {}
    
    public RegisterResponse(String user_id) {
        this.user_id = user_id;
    }
}
