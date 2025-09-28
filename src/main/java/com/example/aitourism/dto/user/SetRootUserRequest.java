package com.example.aitourism.dto.user;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 设置ROOT用户请求DTO
 */
@Data
public class SetRootUserRequest {
    
    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
