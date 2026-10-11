package com.oa.oa.service.impl;

import com.oa.common.exception.BusinessAccessDeniedException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.common.constant.ApprovalStatus;
import com.oa.oa.domain.OaLeave;
import com.oa.oa.mapper.OaLeaveMapper;
import com.oa.oa.service.IOaLeaveService;
import com.oa.security.model.LoginUser;
import com.oa.security.service.DataScopeResolver;
import com.oa.security.utils.AccessGuards;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OaLeaveServiceImpl extends ServiceImpl<OaLeaveMapper, OaLeave> implements IOaLeaveService {

    /** 审批步骤终态：0 草稿/已撤回、1 审批中、2 审批结束 */
    private static final int STEP_FINISHED = 2;

    @Autowired
    private DataScopeResolver dataScopeResolver;

    @Override
    public List<OaLeave> selectOaLeaveList(OaLeave leave) {
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(leave.getLeaveType()), OaLeave::getLeaveType, leave.getLeaveType())
                .eq(StringUtils.isNotBlank(leave.getApprovalStatus()), OaLeave::getApprovalStatus, leave.getApprovalStatus())
                .eq(leave.getUserId() != null, OaLeave::getUserId, leave.getUserId())
                .orderByDesc(OaLeave::getApplyTime);
        dataScopeResolver.apply(wrapper, OaLeave::getUserId, OaLeave::getDeptId);
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
        OaLeave existing = leaveId == null ? null : baseMapper.selectById(leaveId);
        if (existing != null) {
            // 单条读取同样受数据范围约束，避免用 id 遍历绕过列表接口的过滤
            requireReadable(existing);
        }
        return existing;
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
            throw new BusinessAccessDeniedException("不能审批自己提交的请假单");
        }
        // 只能审批落在本人数据范围内的请假单，口径与列表接口完全一致
        if (!inDataScope(existing)) {
            throw new BusinessAccessDeniedException("无权审批该请假单");
        }
        OaLeave update = new OaLeave();
        update.setApprovalStatus(leave.getApprovalStatus());
        update.setRemark(leave.getRemark());
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(new Date());
        update.setCurrentStep(STEP_FINISHED);
        // 条件更新（CAS）：只有仍处于“审批中”的记录才会被写入审批结果，
        // 并发重复审批时后到的一次不会覆盖先到的决定，影响 0 行即说明状态已被别人改过
        LambdaUpdateWrapper<OaLeave> pendingWrapper = new LambdaUpdateWrapper<>();
        pendingWrapper.eq(OaLeave::getLeaveId, existing.getLeaveId())
                .eq(OaLeave::getApprovalStatus, ApprovalStatus.PENDING);
        int result = baseMapper.update(update, pendingWrapper);
        if (result == 0) {
            throw new IllegalArgumentException("该请假单已被其他人审批，请刷新后重试");
        }
        return result;
    }

    /** 单条读取权限：本人、管理员，或通过数据范围（与列表接口同一口径）可见的记录。 */
    private void requireReadable(OaLeave leave) {
        if (AccessGuards.isSelfOrAdmin(leave.getUserId()) || inDataScope(leave)) {
            return;
        }
        throw new BusinessAccessDeniedException("无权查看该请假单");
    }

    /** 该记录是否落在当前登录用户的数据范围内。 */
    private boolean inDataScope(OaLeave leave) {
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaLeave::getLeaveId, leave.getLeaveId());
        dataScopeResolver.apply(wrapper, OaLeave::getUserId, OaLeave::getDeptId);
        Long count = baseMapper.selectCount(wrapper);
        return count != null && count > 0;
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