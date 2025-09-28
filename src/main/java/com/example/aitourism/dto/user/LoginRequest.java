package com.example.aitourism.dto.user;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
