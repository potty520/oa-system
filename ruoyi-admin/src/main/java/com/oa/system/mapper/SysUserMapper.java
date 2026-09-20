package com.oa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.system.domain.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT u.* FROM sys_user u WHERE u.username = #{username} AND u.del_flag = '0'")
    SysUser selectByUsername(@Param("username") String username);

    @Select("SELECT DISTINCT m.perms FROM sys_role r " +
            "LEFT JOIN sys_user_role ur ON ur.role_id = r.role_id " +
            "LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id " +
            "LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id " +
            "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms != '' AND r.status = '0' AND m.status = '0'")
    Set<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoleByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}