package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.common.constant.Constants;
import com.oa.security.model.LoginUser;
import com.oa.security.service.DataScopeResolver;
import com.oa.security.service.JwtTokenBlacklist;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    /** 内置超级管理员账号：不允许被停用或删除，否则没人能再进系统管理 */
    private static final long BUILT_IN_ADMIN_USER_ID = 1L;

    /** 内置超级管理员角色：只有超管本人能授予 */
    private static final long BUILT_IN_ADMIN_ROLE_ID = 1L;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private LoginUserCache loginUserCache;

    @Autowired
    private DataScopeResolver dataScopeResolver;

    @Autowired
    private JwtTokenBlacklist jwtTokenBlacklist;

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
    public List<SysUser> selectUserOptions() {
        // 公文办理人、签发人、传阅对象都需要跨部门选人，所以不套数据范围；
        // 但只查必要列，不把手机号、邮箱之类的个人信息一并暴露出去
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getUserId, SysUser::getUsername, SysUser::getNickname, SysUser::getDeptId)
                .eq(SysUser::getStatus, Constants.STATUS_NORMAL)
                .orderByAsc(SysUser::getUserId);
        return baseMapper.selectList(wrapper);
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
        invalidateSessions(user.getUsername(), false);
        return rows;
    }

    @Override
    @Transactional
    public int updateUser(SysUser user) {
        SysUser existing = baseMapper.selectById(user.getUserId());
        if (existing == null) {
            return 0;
        }
        requireCanManage(existing.getUserId(), user.getRoleIds());
        boolean passwordChanged = StringUtils.isNotBlank(user.getPassword());
        if (!passwordChanged) {
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
        invalidateSessions(existing.getUsername(), passwordChanged);
        return rows;
    }

    @Override
    @Transactional
    public int deleteUserByIds(Long[] userIds) {
        Long currentUserId = SecurityUtils.getUserId();
        int count = 0;
        for (Long userId : userIds) {
            SysUser existing = userId == null ? null : baseMapper.selectById(userId);
            if (existing == null) {
                continue;
            }
            if (userId == BUILT_IN_ADMIN_USER_ID) {
                throw new IllegalArgumentException("内置超级管理员账号不允许删除");
            }
            if (currentUserId != null && currentUserId.equals(userId)) {
                throw new IllegalArgumentException("不能删除当前登录账号");
            }
            userMapper.deleteUserRoleByUserId(userId);
            userMapper.deleteUserPostByUserId(userId);
            count += baseMapper.deleteById(userId);
            invalidateSessions(existing.getUsername(), true);
        }
        return count;
    }

    @Override
    public int changeStatus(SysUser user) {
        SysUser existing = user == null || user.getUserId() == null ? null : baseMapper.selectById(user.getUserId());
        if (existing == null) {
            return 0;
        }
        String status = user.getStatus();
        if (!Constants.STATUS_NORMAL.equals(status) && !Constants.STATUS_DISABLE.equals(status)) {
            throw new IllegalArgumentException("账号状态只能是正常或停用");
        }
        boolean disabling = Constants.STATUS_DISABLE.equals(status);
        if (disabling) {
            if (existing.getUserId() == BUILT_IN_ADMIN_USER_ID) {
                throw new IllegalArgumentException("内置超级管理员账号不允许停用");
            }
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId != null && currentUserId.equals(existing.getUserId())) {
                throw new IllegalArgumentException("不能停用当前登录账号");
            }
        }
        // 只接受状态字段，避免 changeStatus 成为改写任意用户属性的入口
        SysUser update = new SysUser();
        update.setUserId(existing.getUserId());
        update.setStatus(status);
        int rows = baseMapper.updateById(update);
        invalidateSessions(existing.getUsername(), disabling);
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
        // 与 updateUser/changeStatus/deleteUserByIds 保持同一道越权兼底：
        // 缺了这层校验，任何持有 system:user:resetPwd 的人都能重置内置超管口令并接管系统
        requireCanManage(user.getUserId(), null);
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        // 管理员重置后同样要求本人首次登录改密
        update.setMustChangePwd(Constants.MUST_CHANGE_PWD_YES);
        int rows = baseMapper.updateById(update);
        if (existing != null) {
            // 重置口令后旧 Token 必须立即作废，否则失窃的 Token 在有效期内仍然可用
            invalidateSessions(existing.getUsername(), true);
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
        // 本人改密同样要吊销已签发的 Token，前端改密成功后会引导重新登录
        invalidateSessions(existing.getUsername(), true);
        return null;
    }

    /**
     * 用户管理操作的越权兼底：非超管既不能动内置超管账号，
     * 也不能给自己或别人授予内置超管角色。
     */
    private void requireCanManage(Long targetUserId, List<Long> roleIds) {
        Long currentUserId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin(currentUserId)) {
            return;
        }
        if (targetUserId != null && targetUserId == BUILT_IN_ADMIN_USER_ID) {
            throw new AccessDeniedException("不能操作内置超级管理员账号");
        }
        if (roleIds != null && roleIds.contains(BUILT_IN_ADMIN_ROLE_ID)) {
            throw new AccessDeniedException("不能授予内置超级管理员角色");
        }
    }

    /**
     * 让缓存里的旧权限/旧口令立刻失效。
     *
     * @param revokeTokens 为真时把该用户已签发的 Token 一并作废（口令变更、账号停用或删除）
     */
    private void invalidateSessions(String username, boolean revokeTokens) {
        loginUserCache.evict(username);
        if (revokeTokens) {
            jwtTokenBlacklist.revokeUser(username);
        }
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