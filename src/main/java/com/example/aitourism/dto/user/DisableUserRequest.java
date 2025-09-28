package com.example.aitourism.dto.user;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 禁用用户请求DTO
 */
@Data
public class DisableUserRequest {
    
    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
