package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.system.domain.SysRole;
import com.oa.system.mapper.SysRoleMapper;
import com.oa.system.service.ISysRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Override
    public List<SysRole> selectRoleList(SysRole role) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(role.getRoleName()), SysRole::getRoleName, role.getRoleName())
                .like(StringUtils.isNotBlank(role.getRoleKey()), SysRole::getRoleKey, role.getRoleKey())
                .eq(StringUtils.isNotBlank(role.getStatus()), SysRole::getStatus, role.getStatus())
                .orderByAsc(SysRole::getRoleSort);
        return baseMapper.selectList(wrapper);
    }
}