package com.oa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.system.domain.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_dept WHERE role_id = #{roleId}")
    int deleteRoleDeptByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_dept(role_id, dept_id) VALUES(#{roleId}, #{deptId})")
    int insertRoleDept(@Param("roleId") Long roleId, @Param("deptId") Long deptId);
}