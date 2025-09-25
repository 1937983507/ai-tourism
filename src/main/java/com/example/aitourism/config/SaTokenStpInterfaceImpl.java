package com.example.aitourism.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

import com.example.aitourism.mapper.RoleMapper;
import com.example.aitourism.mapper.PermissionMapper;

@Component
public class SaTokenStpInterfaceImpl implements StpInterface {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    public SaTokenStpInterfaceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionMapper.findPermissionsByUserId(String.valueOf(loginId));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleMapper.findRoleCodesByUserId(String.valueOf(loginId));
    }
}


