package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.common.constant.Constants;
import com.oa.security.service.LoginUserCache;
import com.oa.system.domain.SysRole;
import com.oa.system.mapper.SysRoleMapper;
import com.oa.system.service.ISysRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    /** 内置超级管理员角色，停用后没人能再进系统管理 */
    private static final long BUILT_IN_ADMIN_ROLE_ID = 1L;

    @org.springframework.beans.factory.annotation.Autowired
    private LoginUserCache loginUserCache;

    @Override
    public List<SysRole> selectRoleList(SysRole role) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(role.getRoleName()), SysRole::getRoleName, role.getRoleName())
                .like(StringUtils.isNotBlank(role.getRoleKey()), SysRole::getRoleKey, role.getRoleKey())
                .eq(StringUtils.isNotBlank(role.getStatus()), SysRole::getStatus, role.getStatus())
                .orderByAsc(SysRole::getRoleSort);
        List<SysRole> roles = baseMapper.selectList(wrapper);
        roles.forEach(this::enrichRole);
        return roles;
    }

    @Override
    public SysRole selectRoleById(Long roleId) {
        SysRole role = baseMapper.selectById(roleId);
        if (role != null) {
            enrichRole(role);
        }
        return role;
    }

    @Override
    @Transactional
    public int saveRole(SysRole role) {
        int rows = baseMapper.insert(role);
        saveDepts(role.getRoleId(), role.getDeptIds());
        saveMenus(role.getRoleId(), role.getMenuIds());
        loginUserCache.evictAll();
        return rows;
    }

    @Override
    @Transactional
    public int updateRole(SysRole role) {
        int rows = baseMapper.updateById(role);
        // deptIds/menuIds 为 null 表示请求未携带该字段，保留原有配置，避免误清空数据范围与菜单权限
        if (role.getDeptIds() != null) {
            baseMapper.deleteRoleDeptByRoleId(role.getRoleId());
        }
        saveDepts(role.getRoleId(), role.getDeptIds());
        if (role.getMenuIds() != null) {
            baseMapper.deleteRoleMenuByRoleId(role.getRoleId());
        }
        saveMenus(role.getRoleId(), role.getMenuIds());
        loginUserCache.evictAll();
        return rows;
    }

    @Override
    public int changeStatus(Long roleId, String status) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        if (!Constants.STATUS_NORMAL.equals(status) && !Constants.STATUS_DISABLE.equals(status)) {
            throw new IllegalArgumentException("角色状态只能是正常或停用");
        }
        SysRole existing = baseMapper.selectById(roleId);
        if (existing == null) {
            return 0;
        }
        if (roleId == BUILT_IN_ADMIN_ROLE_ID && Constants.STATUS_DISABLE.equals(status)) {
            throw new IllegalArgumentException("内置超级管理员角色不允许停用");
        }
        // 只更新状态列：直接把请求体丢给 updateById 会连带改掉 dataScope、roleKey 等敏感字段
        SysRole update = new SysRole();
        update.setRoleId(roleId);
        update.setStatus(status);
        int rows = baseMapper.updateById(update);
        loginUserCache.evictAll();
        return rows;
    }

    @Override
    public boolean updateById(SysRole role) {
        boolean updated = super.updateById(role);
        if (updated) {
            loginUserCache.evictAll();
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean removeById(java.io.Serializable roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        if (Long.valueOf(BUILT_IN_ADMIN_ROLE_ID).equals(roleId)) {
            throw new IllegalArgumentException("内置超级管理员角色不允许删除");
        }
        boolean removed = super.removeById(roleId);
        if (removed) {
            // 角色删除后必须一并清理 sys_role_dept / sys_role_menu，
            // 否则残留的部门数据范围与菜单授权会变成孤儿数据，被后续复用的 role_id 继承
            if (roleId instanceof Number) {
                Long id = ((Number) roleId).longValue();
                baseMapper.deleteRoleDeptByRoleId(id);
                baseMapper.deleteRoleMenuByRoleId(id);
            }
            loginUserCache.evictAll();
        }
        return removed;
    }

    private void saveDepts(Long roleId, List<Long> deptIds) {
        if (roleId == null || deptIds == null) {
            return;
        }
        // sys_role_dept 为 (role_id, dept_id) 复合主键，需去重后写入
        for (Long deptId : new LinkedHashSet<>(deptIds)) {
            if (deptId != null) {
                baseMapper.insertRoleDept(roleId, deptId);
            }
        }
    }

    private void saveMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null || menuIds == null) {
            return;
        }
        // sys_role_menu 为 (role_id, menu_id) 复合主键，需去重后写入
        for (Long menuId : new LinkedHashSet<>(menuIds)) {
            if (menuId != null) {
                baseMapper.insertRoleMenu(roleId, menuId);
            }
        }
    }

    private void enrichRole(SysRole role) {
        role.setDeptIds(baseMapper.selectDeptIdsByRoleId(role.getRoleId()));
        role.setMenuIds(baseMapper.selectMenuIdsByRoleId(role.getRoleId()));
    }
}