package com.example.aitourism.dto.user;

import lombok.Data;

/**
 * 刷新Token响应DTO
 */
@Data
public class RefreshTokenResponse {
    
    private String token;
    private Long expires_in;
    
    public RefreshTokenResponse() {}
    
    public RefreshTokenResponse(String token, Long expires_in) {
        this.token = token;
        this.expires_in = expires_in;
    }
}
