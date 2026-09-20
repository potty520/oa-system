package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.system.domain.SysDept;
import com.oa.system.mapper.SysDeptMapper;
import com.oa.system.service.ISysDeptService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(dept.getDeptName()), SysDept::getDeptName, dept.getDeptName())
                .eq(StringUtils.isNotBlank(dept.getStatus()), SysDept::getStatus, dept.getStatus())
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum);
        return baseMapper.selectList(wrapper);
    }
}