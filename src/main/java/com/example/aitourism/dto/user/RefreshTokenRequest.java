package com.example.aitourism.dto.user;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 刷新Token请求DTO
 */
@Data
public class RefreshTokenRequest {
    
    @NotBlank(message = "刷新Token不能为空")
    private String refreshToken;
}
