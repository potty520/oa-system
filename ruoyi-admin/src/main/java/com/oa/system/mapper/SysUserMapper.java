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

    @Select("SELECT DISTINCT m.perms FROM sys_user_role ur " +
            "INNER JOIN sys_role r ON r.role_id = ur.role_id " +
            "INNER JOIN sys_role_menu rm ON rm.role_id = r.role_id " +
            "INNER JOIN sys_menu m ON m.menu_id = rm.menu_id " +
            "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms != '' " +
            "AND r.status = '0' AND r.del_flag = '0' AND m.status = '0' AND m.del_flag = '0'")
    Set<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT u.* FROM sys_user u INNER JOIN sys_user_role ur ON ur.user_id = u.user_id " +
            "WHERE ur.role_id = #{roleId} AND u.status = '0' AND u.del_flag = '0'")
    List<SysUser> selectUsersByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT DISTINCT rd.dept_id FROM sys_role_dept rd " +
            "INNER JOIN sys_role r ON r.role_id = rd.role_id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rd.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = '0' AND r.del_flag = '0'")
    List<Long> selectDataScopeDeptIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT post_id FROM sys_user_post WHERE user_id = #{userId}")
    List<Long> selectPostIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    int deleteUserPostByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO sys_user_post(user_id, post_id) VALUES(#{userId}, #{postId})")
    int insertUserPost(@Param("userId") Long userId, @Param("postId") Long postId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoleByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}