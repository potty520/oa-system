package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.common.constant.ApprovalStatus;
import com.oa.oa.domain.OaLeave;
import com.oa.oa.mapper.OaLeaveMapper;
import com.oa.oa.service.IOaLeaveService;
import com.oa.security.model.LoginUser;
import com.oa.security.utils.AccessGuards;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
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
        // 申请人身份一律取当前登录用户，不接受客户端传入，避免冒名提交
        LoginUser current = SecurityUtils.getLoginUser();
        leave.setUserId(current.getUserId());
        leave.setUserName(current.getUsername());
        leave.setDeptId(current.getDeptId());
        leave.setDeptName(current.getDeptName());
        leave.setApprovalStatus(ApprovalStatus.DRAFT);
        leave.setCurrentStep(0);
        leave.setApplyTime(new Date());
        leave.setApproveBy(null);
        leave.setApproveTime(null);
        return baseMapper.insert(leave);
    }

    @Override
    public int updateOaLeave(OaLeave leave) {
        OaLeave existing = requireExisting(leave == null ? null : leave.getLeaveId());
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "请假单");
        requireDraft(existing);
        // 只接受业务字段，申请人、审批状态、审批人等由服务端维护
        OaLeave update = new OaLeave();
        update.setLeaveId(existing.getLeaveId());
        update.setLeaveType(leave.getLeaveType());
        update.setStartTime(leave.getStartTime());
        update.setEndTime(leave.getEndTime());
        update.setLeaveDays(leave.getLeaveDays());
        update.setLeaveReason(leave.getLeaveReason());
        update.setRemark(leave.getRemark());
        return baseMapper.updateById(update);
    }

    @Override
    public int deleteOaLeaveByIds(Long[] leaveIds) {
        int count = 0;
        for (Long leaveId : leaveIds) {
            OaLeave existing = requireExisting(leaveId);
            AccessGuards.requireSelfOrAdmin(existing.getUserId(), "请假单");
            requireDraft(existing);
            count += baseMapper.deleteById(leaveId);
        }
        return count;
    }

    @Override
    public int submitOaLeave(Long leaveId) {
        OaLeave existing = requireExisting(leaveId);
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "请假单");
        requireDraft(existing);
        OaLeave update = new OaLeave();
        update.setLeaveId(existing.getLeaveId());
        update.setApprovalStatus(ApprovalStatus.PENDING);
        update.setCurrentStep(1);
        return baseMapper.updateById(update);
    }

    @Override
    public int withdrawOaLeave(Long leaveId) {
        OaLeave existing = requireExisting(leaveId);
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "请假单");
        if (!ApprovalStatus.PENDING.equals(existing.getApprovalStatus())) {
            throw new IllegalArgumentException("只有审批中的请假单才能撤回");
        }
        OaLeave update = new OaLeave();
        update.setLeaveId(existing.getLeaveId());
        update.setApprovalStatus(ApprovalStatus.DRAFT);
        update.setCurrentStep(0);
        return baseMapper.updateById(update);
    }

    @Override
    public int approveOaLeave(OaLeave leave) {
        if (leave == null || leave.getLeaveId() == null) {
            throw new IllegalArgumentException("请假单ID不能为空");
        }
        if (!ApprovalStatus.isFinalDecision(leave.getApprovalStatus())) {
            throw new IllegalArgumentException("审批结果只能是通过或拒绝");
        }
        OaLeave existing = requireExisting(leave.getLeaveId());
        if (!ApprovalStatus.PENDING.equals(existing.getApprovalStatus())) {
            throw new IllegalArgumentException("该请假单不在审批中，无法审批");
        }
        Long current = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin(current) && current != null && current.equals(existing.getUserId())) {
            throw new AccessDeniedException("不能审批自己提交的请假单");
        }
        OaLeave update = new OaLeave();
        update.setLeaveId(existing.getLeaveId());
        update.setApprovalStatus(leave.getApprovalStatus());
        update.setRemark(leave.getRemark());
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(new Date());
        return baseMapper.updateById(update);
    }

    private OaLeave requireExisting(Long leaveId) {
        OaLeave existing = leaveId == null ? null : baseMapper.selectById(leaveId);
        if (existing == null) {
            throw new IllegalArgumentException("请假单不存在");
        }
        return existing;
    }

    private void requireDraft(OaLeave leave) {
        if (!ApprovalStatus.DRAFT.equals(leave.getApprovalStatus())) {
            throw new IllegalArgumentException("请假单已提交审批，不能再修改或删除");
        }
    }
}