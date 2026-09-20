package com.oa.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.system.domain.SysRole;

import java.util.List;

public interface ISysRoleService extends IService<SysRole> {
    List<SysRole> selectRoleList(SysRole role);
    SysRole selectRoleById(Long roleId);
    int saveRole(SysRole role);
    int updateRole(SysRole role);
}