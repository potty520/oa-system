package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.security.service.LoginUserCache;
import com.oa.system.domain.SysRole;
import com.oa.system.mapper.SysRoleMapper;
import com.oa.system.service.ISysRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

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
        loginUserCache.evictAll();
        return rows;
    }

    @Override
    @Transactional
    public int updateRole(SysRole role) {
        int rows = baseMapper.updateById(role);
        baseMapper.deleteRoleDeptByRoleId(role.getRoleId());
        saveDepts(role.getRoleId(), role.getDeptIds());
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
    public boolean removeById(java.io.Serializable roleId) {
        boolean removed = super.removeById(roleId);
        if (removed) {
            loginUserCache.evictAll();
        }
        return removed;
    }

    private void saveDepts(Long roleId, List<Long> deptIds) {
        if (roleId == null || deptIds == null) {
            return;
        }
        for (Long deptId : deptIds) {
            if (deptId != null) {
                baseMapper.insertRoleDept(roleId, deptId);
            }
        }
    }

    private void enrichRole(SysRole role) {
        role.setDeptIds(baseMapper.selectDeptIdsByRoleId(role.getRoleId()));
    }
}