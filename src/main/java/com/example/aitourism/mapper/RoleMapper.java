package com.example.aitourism.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT role_code FROM t_user_role WHERE user_id = #{userId}")
    List<String> findRoleCodesByUserId(String userId);

    @Insert("INSERT INTO t_user_role(user_id, role_code) VALUES(#{userId}, #{roleCode})")
    int grantRoleToUser(@Param("userId") String userId, @Param("roleCode") String roleCode);

    @Delete("DELETE FROM t_user_role WHERE user_id = #{userId} AND role_code = #{roleCode}")
    int revokeRoleFromUser(@Param("userId") String userId, @Param("roleCode") String roleCode);
}


