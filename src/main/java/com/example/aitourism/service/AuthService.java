package com.example.aitourism.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(String phone, String password);
    String register(String phone, String password, String nickname);
    Map<String, Object> me();
    Map<String, Object> refresh(String refreshToken);
    void logout();

    // Admin operations
    void disableUser(String userId);
    void setUserAsRoot(String userId);
}


