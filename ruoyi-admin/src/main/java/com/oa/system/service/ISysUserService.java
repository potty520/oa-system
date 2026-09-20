package com.oa.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.security.model.LoginUser;
import com.oa.system.domain.SysUser;

import java.util.List;

public interface ISysUserService extends IService<SysUser> {
    LoginUser selectUserByUsername(String username);

    SysUser selectSysUserByUsername(String username);

    List<SysUser> selectUserList(SysUser user);

    SysUser selectUserById(Long userId);

    int insertUser(SysUser user);

    int updateUser(SysUser user);

    int deleteUserByIds(Long[] userIds);

    int changeStatus(SysUser user);

    int resetPassword(SysUser user);
}