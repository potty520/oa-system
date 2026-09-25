package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.common.constant.Constants;
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

    @Override
    public int changeStatus(Long deptId, String status) {
        if (deptId == null) {
            throw new IllegalArgumentException("部门ID不能为空");
        }
        if (!Constants.STATUS_NORMAL.equals(status) && !Constants.STATUS_DISABLE.equals(status)) {
            throw new IllegalArgumentException("部门状态只能是正常或停用");
        }
        if (baseMapper.selectById(deptId) == null) {
            return 0;
        }
        // 只更新状态列：直接把请求体丢给 updateById 会连带改掉 parentId/ancestors，把部门树改坏
        SysDept update = new SysDept();
        update.setDeptId(deptId);
        update.setStatus(status);
        return baseMapper.updateById(update);
    }
}