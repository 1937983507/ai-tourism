package com.example.aitourism.controller;

import com.example.aitourism.dto.ApiResponse;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.aitourism.service.AuthService;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.stp.StpUtil;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            String phone = body.get("phone");
            String password = body.get("password");
            return ApiResponse.success(authService.login(phone, password));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = 5000;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return ApiResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return ApiResponse.error(5000, "服务端错误");
        }
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        try {
            String phone = body.get("phone");
            String password = body.get("password");
            String nickname = body.getOrDefault("nickname", "");
            String userId = authService.register(phone, password, nickname);
            return ApiResponse.success(Map.of("user_id", userId));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = 5000;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return ApiResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return ApiResponse.error(5000, "服务端错误");
        }
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        try {
            return ApiResponse.success(authService.me());
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = 5000;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return ApiResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return ApiResponse.error(5000, "服务端错误");
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refresh_token");
            return ApiResponse.success(authService.refresh(refreshToken));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = 5000;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return ApiResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return ApiResponse.error(5000, "服务端错误");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    // 禁用用户（将 status 置为 0）
    @SaCheckPermission("user:disable")
    @PostMapping("/disable")
    public ApiResponse<Void> disable(@RequestBody Map<String, String> body) {
        String userId = body.get("user_id");
        authService.disableUser(userId);
        return ApiResponse.success();
    }

    // 设为 ROOT（授予 ROOT 角色）
    @SaCheckPermission("user:set-root")
    @PostMapping("/set_root")
    public ApiResponse<Void> setRoot(@RequestBody Map<String, String> body) {
        String userId = body.get("user_id");
        authService.setUserAsRoot(userId);
        return ApiResponse.success();
    }
}


