package com.example.aitourism.dto.user;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求DTO
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private String nickname;
}
