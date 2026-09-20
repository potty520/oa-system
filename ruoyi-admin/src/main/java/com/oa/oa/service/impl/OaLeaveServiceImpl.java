package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaLeave;
import com.oa.oa.mapper.OaLeaveMapper;
import com.oa.oa.service.IOaLeaveService;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OaLeaveServiceImpl extends ServiceImpl<OaLeaveMapper, OaLeave> implements IOaLeaveService {

    @Override
    public List<OaLeave> selectOaLeaveList(OaLeave leave) {
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(leave.getLeaveType()), OaLeave::getLeaveType, leave.getLeaveType())
                .eq(StringUtils.isNotBlank(leave.getApprovalStatus()), OaLeave::getApprovalStatus, leave.getApprovalStatus())
                .eq(leave.getUserId() != null, OaLeave::getUserId, leave.getUserId())
                .orderByDesc(OaLeave::getApplyTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<OaLeave> selectMyLeaveList(Long userId) {
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaLeave::getUserId, userId)
                .orderByDesc(OaLeave::getApplyTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaLeave selectOaLeaveById(Long leaveId) {
        return baseMapper.selectById(leaveId);
    }

    @Override
    public int insertOaLeave(OaLeave leave) {
        leave.setUserId(SecurityUtils.getUserId());
        leave.setUserName(SecurityUtils.getUsername());
        leave.setApprovalStatus("0");
        leave.setCurrentStep(0);
        leave.setApplyTime(new Date());
        return baseMapper.insert(leave);
    }

    @Override
    public int updateOaLeave(OaLeave leave) {
        return baseMapper.updateById(leave);
    }

    @Override
    public int deleteOaLeaveByIds(Long[] leaveIds) {
        int count = 0;
        for (Long leaveId : leaveIds) {
            count += baseMapper.deleteById(leaveId);
        }
        return count;
    }

    @Override
    public int submitOaLeave(Long leaveId) {
        OaLeave leave = baseMapper.selectById(leaveId);
        if (leave != null && "0".equals(leave.getApprovalStatus())) {
            leave.setApprovalStatus("1");
            leave.setCurrentStep(1);
            return baseMapper.updateById(leave);
        }
        return 0;
    }

    @Override
    public int withdrawOaLeave(Long leaveId) {
        OaLeave leave = baseMapper.selectById(leaveId);
        if (leave != null && "1".equals(leave.getApprovalStatus())) {
            leave.setApprovalStatus("0");
            leave.setCurrentStep(0);
            return baseMapper.updateById(leave);
        }
        return 0;
    }

    @Override
    public int approveOaLeave(OaLeave leave) {
        OaLeave existLeave = baseMapper.selectById(leave.getLeaveId());
        if (existLeave != null && "1".equals(existLeave.getApprovalStatus())) {
            existLeave.setApprovalStatus(leave.getApprovalStatus());
            existLeave.setRemark(leave.getRemark());
            existLeave.setApproveBy(SecurityUtils.getUsername());
            existLeave.setApproveTime(new Date());
            return baseMapper.updateById(existLeave);
        }
        return 0;
    }
}