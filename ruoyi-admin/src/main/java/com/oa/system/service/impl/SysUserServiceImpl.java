package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.common.constant.Constants;
import com.oa.security.model.LoginUser;
import com.oa.security.service.DataScopeResolver;
import com.oa.security.service.LoginUserCache;
import com.oa.security.utils.PasswordPolicy;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysDept;
import com.oa.system.domain.SysUser;
import com.oa.system.mapper.SysDeptMapper;
import com.oa.system.mapper.SysUserMapper;
import com.oa.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private LoginUserCache loginUserCache;

    @Autowired
    private DataScopeResolver dataScopeResolver;

    @Override
    public LoginUser selectUserByUsername(String username) {
        SysUser sysUser = userMapper.selectByUsername(username);
        if (sysUser == null) {
            return null;
        }
        Set<String> permissions = userMapper.selectPermsByUserId(sysUser.getUserId());
        LoginUser loginUser = new LoginUser(
                sysUser.getUserId(),
                sysUser.getUsername(),
                sysUser.getPassword(),
                sysUser.getNickname(),
                sysUser.getAvatar(),
                sysUser.getDeptId(),
                resolveDeptName(sysUser.getDeptId()),
                sysUser.getStatus()
        );
        loginUser.setPermissions(permissions);
        loginUser.setMustChangePwd(sysUser.getMustChangePwd());
        return loginUser;
    }

    @Override
    public SysUser selectSysUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public List<SysUser> selectUserList(SysUser user) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(user.getUsername()), SysUser::getUsername, user.getUsername())
                .like(StringUtils.isNotBlank(user.getPhone()), SysUser::getPhone, user.getPhone())
                .eq(StringUtils.isNotBlank(user.getStatus()), SysUser::getStatus, user.getStatus());
        applyDataScope(wrapper);
        wrapper.orderByAsc(SysUser::getUserId);
        List<SysUser> users = baseMapper.selectList(wrapper);
        users.forEach(this::enrichUser);
        return users;
    }

    @Override
    public SysUser selectUserById(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        if (user != null) {
            enrichUser(user);
        }
        return user;
    }

    @Override
    @Transactional
    public int insertUser(SysUser user) {
        if (StringUtils.isBlank(user.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        String violation = PasswordPolicy.validate(user.getPassword());
        if (violation != null) {
            throw new IllegalArgumentException(violation);
        }
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        if (StringUtils.isBlank(user.getMustChangePwd())) {
            // 由管理员代设的初始口令同样要求首次登录修改
            user.setMustChangePwd(Constants.MUST_CHANGE_PWD_YES);
        }
        int rows = baseMapper.insert(user);
        saveUserRoles(user.getUserId(), user.getRoleIds());
        saveUserPosts(user.getUserId(), user.getPostIds());
        loginUserCache.evict(user.getUsername());
        return rows;
    }

    @Override
    @Transactional
    public int updateUser(SysUser user) {
        SysUser existing = baseMapper.selectById(user.getUserId());
        if (existing == null) {
            return 0;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword(existing.getPassword());
        } else {
            String violation = PasswordPolicy.validate(user.getPassword());
            if (violation != null) {
                throw new IllegalArgumentException(violation);
            }
            user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        }
        // 强制改密标记只能由本人改密或管理员重置密码接口变更，避免通过编辑用户直接绕过
        user.setMustChangePwd(existing.getMustChangePwd());
        int rows = baseMapper.updateById(user);
        userMapper.deleteUserRoleByUserId(user.getUserId());
        userMapper.deleteUserPostByUserId(user.getUserId());
        saveUserRoles(user.getUserId(), user.getRoleIds());
        saveUserPosts(user.getUserId(), user.getPostIds());
        loginUserCache.evict(existing.getUsername());
        return rows;
    }

    @Override
    @Transactional
    public int deleteUserByIds(Long[] userIds) {
        int count = 0;
        for (Long userId : userIds) {
            SysUser existing = baseMapper.selectById(userId);
            userMapper.deleteUserRoleByUserId(userId);
            userMapper.deleteUserPostByUserId(userId);
            count += baseMapper.deleteById(userId);
            if (existing != null) {
                loginUserCache.evict(existing.getUsername());
            }
        }
        return count;
    }

    @Override
    public int changeStatus(SysUser user) {
        SysUser existing = baseMapper.selectById(user.getUserId());
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setStatus(user.getStatus());
        int rows = baseMapper.updateById(update);
        if (existing != null) {
            loginUserCache.evict(existing.getUsername());
        }
        return rows;
    }

    @Override
    public int resetPassword(SysUser user) {
        if (StringUtils.isBlank(user.getPassword())) {
            return 0;
        }
        String violation = PasswordPolicy.validate(user.getPassword());
        if (violation != null) {
            throw new IllegalArgumentException(violation);
        }
        SysUser existing = baseMapper.selectById(user.getUserId());
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        // 管理员重置后同样要求本人首次登录改密
        update.setMustChangePwd(Constants.MUST_CHANGE_PWD_YES);
        int rows = baseMapper.updateById(update);
        if (existing != null) {
            loginUserCache.evict(existing.getUsername());
        }
        return rows;
    }

    @Override
    @Transactional
    public String updateOwnPassword(Long userId, String oldPassword, String newPassword) {
        SysUser existing = baseMapper.selectById(userId);
        if (existing == null) {
            return "用户不存在";
        }
        if (!SecurityUtils.matchesPassword(oldPassword, existing.getPassword())) {
            return "原密码不正确";
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(SecurityUtils.encryptPassword(newPassword));
        update.setMustChangePwd(Constants.MUST_CHANGE_PWD_NO);
        baseMapper.updateById(update);
        loginUserCache.evict(existing.getUsername());
        return null;
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null || roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId != null) {
                userMapper.insertUserRole(userId, roleId);
            }
        }
    }


    private void applyDataScope(LambdaQueryWrapper<SysUser> wrapper) {
        dataScopeResolver.apply(wrapper, SysUser::getUserId, SysUser::getDeptId);
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null) {
            return;
        }
        for (Long postId : postIds) {
            if (postId != null) {
                userMapper.insertUserPost(userId, postId);
            }
        }
    }

    private void enrichUser(SysUser user) {
        user.setDept(resolveDept(user.getDeptId()));
        user.setRoleIds(userMapper.selectRoleIdsByUserId(user.getUserId()));
        user.setPostIds(userMapper.selectPostIdsByUserId(user.getUserId()));
    }

    private SysDept resolveDept(Long deptId) {
        return deptId == null ? null : deptMapper.selectById(deptId);
    }

    private String resolveDeptName(Long deptId) {
        SysDept dept = resolveDept(deptId);
        return dept == null ? null : dept.getDeptName();
    }
}