package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.security.model.LoginUser;
import com.oa.security.service.LoginUserCache;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysDept;
import com.oa.system.domain.SysRole;
import com.oa.system.domain.SysUser;
import com.oa.system.mapper.SysDeptMapper;
import com.oa.system.mapper.SysRoleMapper;
import com.oa.system.mapper.SysUserMapper;
import com.oa.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private LoginUserCache loginUserCache;

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
        if (StringUtils.isNotBlank(user.getPassword())) {
            user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
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
            user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        }
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
        SysUser existing = baseMapper.selectById(user.getUserId());
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        int rows = baseMapper.updateById(update);
        if (existing != null) {
            loginUserCache.evict(existing.getUsername());
        }
        return rows;
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
        LoginUser current = SecurityUtils.getLoginUser();
        if (current == null || SecurityUtils.isAdmin(current.getUserId())) {
            return;
        }
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(current.getUserId());
        if (roleIds == null || roleIds.isEmpty()) {
            wrapper.eq(SysUser::getUserId, current.getUserId());
            return;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        Set<String> scopes = new HashSet<>();
        for (SysRole role : roles) {
            if (role.getDataScope() == null) {
                scopes.add("5");
            } else {
                scopes.add(role.getDataScope());
            }
        }
        if (scopes.contains("1")) {
            return;
        }
        Set<Long> deptIds = new HashSet<>();
        if (scopes.contains("2")) {
            deptIds.addAll(userMapper.selectDataScopeDeptIdsByUserId(current.getUserId()));
        }
        if (scopes.contains("3") && current.getDeptId() != null) {
            deptIds.add(current.getDeptId());
        }
        if (scopes.contains("4") && current.getDeptId() != null) {
            List<SysDept> depts = deptMapper.selectList(null);
            Set<Long> parents = new HashSet<>();
            parents.add(current.getDeptId());
            boolean changed = true;
            while (changed) {
                changed = false;
                for (SysDept dept : depts) {
                    if (parents.contains(dept.getParentId()) && parents.add(dept.getDeptId())) {
                        changed = true;
                    }
                }
            }
            deptIds.addAll(parents);
        }
        Long userId = current.getUserId();
        if (deptIds.isEmpty()) {
            wrapper.eq(SysUser::getUserId, userId);
        } else {
            List<Long> ids = new ArrayList<>(deptIds);
            wrapper.and(query -> query.eq(SysUser::getUserId, userId).or().in(SysUser::getDeptId, ids));
        }
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