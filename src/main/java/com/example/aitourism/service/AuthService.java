package com.example.aitourism.service;

import com.example.aitourism.dto.user.LoginResponse;
import com.example.aitourism.dto.user.RefreshTokenResponse;
import com.example.aitourism.dto.user.UserInfoResponse;

public interface AuthService {

    LoginResponse login(String phone, String password);
    String register(String phone, String password, String nickname);
    UserInfoResponse me();
    RefreshTokenResponse refresh(String refreshToken);
    void logout();

    // Admin operations
    void disableUser(String userId);
    void setUserAsRoot(String userId);
}


