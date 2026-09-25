package com.oa.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.security.model.LoginUser;
import com.oa.system.domain.SysUser;

import java.util.List;

public interface ISysUserService extends IService<SysUser> {
    LoginUser selectUserByUsername(String username);

    SysUser selectSysUserByUsername(String username);

    List<SysUser> selectUserList(SysUser user);

    /** 用户下拉选项：只含 id/姓名/部门，不受数据范围限制 */
    List<SysUser> selectUserOptions();

    SysUser selectUserById(Long userId);

    int insertUser(SysUser user);

    int updateUser(SysUser user);

    int deleteUserByIds(Long[] userIds);

    int changeStatus(SysUser user);

    int resetPassword(SysUser user);

    /** 校验原密码后修改本人密码；成功返回 null，失败返回可直接展示给用户的中文原因。 */
    String updateOwnPassword(Long userId, String oldPassword, String newPassword);
}