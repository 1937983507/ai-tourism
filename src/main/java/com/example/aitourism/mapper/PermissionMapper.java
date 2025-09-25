package com.example.aitourism.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper {

    @Select("SELECT p.perm_code FROM t_permission p " +
            "JOIN t_role_permission rp ON p.perm_code = rp.perm_code " +
            "JOIN t_user_role ur ON ur.role_code = rp.role_code " +
            "WHERE ur.user_id = #{userId}")
    List<String> findPermissionsByUserId(String userId);
}


