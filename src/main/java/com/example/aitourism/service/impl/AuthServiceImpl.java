package com.example.aitourism.service.impl;

import com.example.aitourism.dto.user.LoginResponse;
import com.example.aitourism.dto.user.RefreshTokenResponse;
import com.example.aitourism.dto.user.UserInfoResponse;
import com.example.aitourism.entity.User;
import com.example.aitourism.mapper.UserMapper;
import com.example.aitourism.mapper.RoleMapper;
import com.example.aitourism.mapper.RefreshTokenMapper;
import com.example.aitourism.service.AuthService;
import com.example.aitourism.entity.RefreshToken;
import com.example.aitourism.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import cn.dev33.satoken.stp.StpUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserMapper userMapper, RoleMapper roleMapper, RefreshTokenMapper refreshTokenMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Override
    public LoginResponse login(String phone, String password) {
        User user = userMapper.findByPhone(phone);
        if (user == null || user.getStatus() == Constants.USER_STATUS_INACTIVE) {
            throw new RuntimeException(Constants.ERROR_CODE_ACCOUNT_OR_PASSWORD_INVALID + ": 账号或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException(Constants.ERROR_CODE_ACCOUNT_OR_PASSWORD_INVALID + ": 账号或密码错误");
        }
        StpUtil.login(user.getUserId());
        String token = StpUtil.getTokenValue();

        // 刷新令牌：先清空旧的
        refreshTokenMapper.deleteByUserId(user.getUserId());
        // 再新建新的
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getUserId());
        rt.setRefreshToken(UUID.randomUUID().toString().replace("-", ""));
        rt.setExpireAt(LocalDateTime.now().plusDays(30));
        refreshTokenMapper.insert(rt);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUser_id(user.getUserId());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpires_in(7200L);
        response.setRefresh_token(rt.getRefreshToken());
        response.setRefresh_expires_in(2592000L);
        response.setUser(userInfo);
        
        log.info("登陆成功，返回：" + response);
        return response;
    }

    @Override
    public String register(String phone, String password, String nickname) {
        User exists = userMapper.findByPhone(phone);
        if (exists != null) {
            throw new RuntimeException("2001: 注册手机号已存在");
        }
        User user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(1);
        userMapper.insert(user);
        // 默认授予 USER 角色
        roleMapper.grantRoleToUser(user.getUserId(), "USER");
        return user.getUserId();
    }

    @Override
    public UserInfoResponse me() {
        String userId = (String) StpUtil.getLoginIdDefaultNull();
        if (userId == null) {
            throw new RuntimeException("1101: 未认证或 token 失效");
        }
        User user = userMapper.findByUserId(userId);
        List<String> roles = roleMapper.findRoleCodesByUserId(userId);
        
        UserInfoResponse response = new UserInfoResponse();
        response.setUser_id(user.getUserId());
        response.setPhone(user.getPhone());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setRoles(roles);
        
        return response;
    }

    @Override
    public RefreshTokenResponse refresh(String refreshToken) {
        RefreshToken rt = refreshTokenMapper.findByToken(refreshToken);
        if (rt == null || rt.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("1101: 未认证或 token 失效");
        }
        // 重新签发访问令牌
        StpUtil.login(rt.getUserId());
        String token = StpUtil.getTokenValue();
        
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setToken(token);
        response.setExpires_in(7200L);
        
        return response;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void disableUser(String userId) {
        int rows = userMapper.updateStatusByUserId(userId, 0);
        if (rows == 0) {
            throw new RuntimeException("5000: 服务端错误");
        }
        // 使其现有会话失效
        try { StpUtil.logout(userId); } catch (Exception ignored) {}
    }

    @Override
    public void setUserAsRoot(String userId) {
        // 授予 ROOT 角色
        roleMapper.grantRoleToUser(userId, "ROOT");
    }
}


