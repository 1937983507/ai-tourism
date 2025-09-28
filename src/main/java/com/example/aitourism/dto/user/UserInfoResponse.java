package com.example.aitourism.dto.user;

import lombok.Data;
import java.util.List;

/**
 * 用户信息响应DTO
 */
@Data
public class UserInfoResponse {
    
    private String user_id;
    private String phone;
    private String nickname;
    private String avatar;
    private List<String> roles;
    
    public UserInfoResponse() {}
    
    public UserInfoResponse(String user_id, String phone, String nickname, String avatar, List<String> roles) {
        this.user_id = user_id;
        this.phone = phone;
        this.nickname = nickname;
        this.avatar = avatar;
        this.roles = roles;
    }
}
