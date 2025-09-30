package com.example.aitourism.dto.user;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    
    private String token;
    private Long expires_in;
    private String refresh_token;
    private Long refresh_expires_in;
    private UserInfo user;
    
    public LoginResponse() {}
    
    public LoginResponse(String token, Long expires_in, String refresh_token, Long refresh_expires_in, UserInfo user) {
        this.token = token;
        this.expires_in = expires_in;
        this.refresh_token = refresh_token;
        this.refresh_expires_in = refresh_expires_in;
        this.user = user;
    }
    
    @Data
    public static class UserInfo {
        private String user_id;
        private String nickname;
        private String avatar;
        
        public UserInfo() {}
        
        public UserInfo(String user_id, String nickname, String avatar) {
            this.user_id = user_id;
            this.nickname = nickname;
            this.avatar = avatar;
        }
    }
}
