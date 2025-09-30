package com.example.aitourism.controller;

import com.example.aitourism.dto.*;
import com.example.aitourism.dto.user.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.aitourism.service.AuthService;
import org.springframework.web.bind.annotation.*;
import com.example.aitourism.util.Constants;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return BaseResponse.success(authService.login(request.getPhone(), request.getPassword()));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = Constants.ERROR_CODE_SERVER_ERROR;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return BaseResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return BaseResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "服务端错误");
        }
    }

    @PostMapping("/register")
    public BaseResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String userId = authService.register(request.getPhone(), request.getPassword(), 
                request.getNickname() != null ? request.getNickname() : "");
            return BaseResponse.success(new RegisterResponse(userId));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = Constants.ERROR_CODE_SERVER_ERROR;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return BaseResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return BaseResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "服务端错误");
        }
    }

    @GetMapping("/me")
    public BaseResponse<UserInfoResponse> me() {
        try {
            return BaseResponse.success(authService.me());
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = Constants.ERROR_CODE_SERVER_ERROR;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return BaseResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return BaseResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "服务端错误");
        }
    }

    @PostMapping("/refresh")
    public BaseResponse<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            return BaseResponse.success(authService.refresh(request.getRefreshToken()));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int code = Constants.ERROR_CODE_SERVER_ERROR;
            if (msg != null && msg.matches("^\\d{4}:.*")) {
                code = Integer.parseInt(msg.substring(0, 4));
                msg = msg.substring(6);
            }
            return BaseResponse.error(code, msg != null ? msg : "服务端错误");
        } catch (Exception e) {
            return BaseResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "服务端错误");
        }
    }

    @PostMapping("/logout")
    public BaseResponse<Void> logout() {
        authService.logout();
        return BaseResponse.success();
    }

    // 禁用用户（将 status 置为 0）
    @SaCheckPermission("user:disable")
    @PostMapping("/disable")
    public BaseResponse<Void> disable(@Valid @RequestBody DisableUserRequest request) {
        authService.disableUser(request.getUserId());
        return BaseResponse.success();
    }

    // 设为 ROOT（授予 ROOT 角色）
    @SaCheckPermission("user:set-root")
    @PostMapping("/set_root")
    public BaseResponse<Void> setRoot(@Valid @RequestBody SetRootUserRequest request) {
        authService.setUserAsRoot(request.getUserId());
        return BaseResponse.success();
    }
}


