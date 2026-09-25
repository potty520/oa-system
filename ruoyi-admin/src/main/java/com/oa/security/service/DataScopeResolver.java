package com.oa.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oa.security.model.LoginUser;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysDept;
import com.oa.system.domain.SysRole;
import com.oa.system.mapper.SysDeptMapper;
import com.oa.system.mapper.SysRoleMapper;
import com.oa.system.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一解析并套用当前登录用户的数据范围，避免每个业务 Service 各写一份、口径不一致。
 *
 * <p>范围取值与 {@code sys_role.data_scope} 一致：1 全部、2 自定义部门、3 本部门、
 * 4 本部门及以下、5 仅本人。多个角色取并集（范围最宽者生效）。</p>
 */
@Component
public class DataScopeResolver {

    public static final String SCOPE_ALL = "1";
    public static final String SCOPE_CUSTOM = "2";
    public static final String SCOPE_DEPT = "3";
    public static final String SCOPE_DEPT_AND_CHILD = "4";
    public static final String SCOPE_SELF = "5";

    private static final String ROLE_ENABLED = "0";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public DataScopeResolver(SysUserMapper userMapper, SysRoleMapper roleMapper, SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
    }

    /** 当前用户是否不受数据范围限制：超级管理员，或任一启用角色被授予“全部数据”。 */
    public boolean isUnrestricted() {
        LoginUser current = SecurityUtils.getLoginUser();
        if (current == null) {
            return false;
        }
        return SecurityUtils.isAdmin(current.getUserId()) || scopesOf(current.getUserId()).contains(SCOPE_ALL);
    }

    /**
     * 把数据范围套到查询条件上。
     *
     * @param userColumn 归属人列，必填
     * @param deptColumn 归属部门列；表里没有该列时传 null，此时只按归属人过滤
     */
    public <T> void apply(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> userColumn, SFunction<T, ?> deptColumn) {
        LoginUser current = SecurityUtils.getLoginUser();
        if (current == null) {
            // 未登录不该看到任何按人隔离的数据
            wrapper.apply("1 = 0");
            return;
        }
        Long userId = current.getUserId();
        if (SecurityUtils.isAdmin(userId)) {
            return;
        }
        Set<String> scopes = scopesOf(userId);
        if (scopes.contains(SCOPE_ALL)) {
            return;
        }
        Set<Long> deptIds = deptIdsOf(userId, scopes, current.getDeptId());
        if (deptColumn == null || deptIds.isEmpty()) {
            wrapper.eq(userColumn, userId);
            return;
        }
        List<Long> visibleDeptIds = new ArrayList<>(deptIds);
        wrapper.and(query -> query.eq(userColumn, userId).or().in(deptColumn, visibleDeptIds));
    }

    private Set<String> scopesOf(Long userId) {
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.singleton(SCOPE_SELF);
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        Set<String> scopes = new HashSet<>();
        for (SysRole role : roles) {
            // 停用的角色不再贡献数据范围
            if (!ROLE_ENABLED.equals(role.getStatus())) {
                continue;
            }
            scopes.add(role.getDataScope() == null ? SCOPE_SELF : role.getDataScope());
        }
        return scopes.isEmpty() ? Collections.singleton(SCOPE_SELF) : scopes;
    }

    private Set<Long> deptIdsOf(Long userId, Set<String> scopes, Long currentDeptId) {
        Set<Long> deptIds = new HashSet<>();
        if (scopes.contains(SCOPE_CUSTOM)) {
            List<Long> custom = userMapper.selectDataScopeDeptIdsByUserId(userId);
            if (custom != null) {
                deptIds.addAll(custom);
            }
        }
        if (currentDeptId != null && (scopes.contains(SCOPE_DEPT) || scopes.contains(SCOPE_DEPT_AND_CHILD))) {
            deptIds.add(currentDeptId);
        }
        if (currentDeptId != null && scopes.contains(SCOPE_DEPT_AND_CHILD)) {
            deptIds.addAll(childDeptIds(currentDeptId));
        }
        return deptIds;
    }

    private Set<Long> childDeptIds(Long rootDeptId) {
        List<SysDept> all = deptMapper.selectList(null);
        Set<Long> result = new HashSet<>();
        result.add(rootDeptId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysDept dept : all) {
                if (result.contains(dept.getParentId()) && result.add(dept.getDeptId())) {
                    changed = true;
                }
            }
        }
        return result;
    }
}
