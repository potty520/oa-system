package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaReimbursement;
import com.oa.oa.domain.OaReimbursementApproveLog;
import com.oa.oa.domain.OaReimbursementDetail;
import com.oa.oa.mapper.OaReimbursementApproveLogMapper;
import com.oa.oa.mapper.OaReimbursementDetailMapper;
import com.oa.oa.mapper.OaReimbursementMapper;
import com.oa.oa.service.IOaReimbursementService;
import com.oa.common.constant.ApprovalStatus;
import com.oa.security.model.LoginUser;
import com.oa.security.service.DataScopeResolver;
import com.oa.security.utils.AccessGuards;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class OaReimbursementServiceImpl extends ServiceImpl<OaReimbursementMapper, OaReimbursement> implements IOaReimbursementService {

    /** 审批步骤终态：0 草稿/已撤回、1 审批中、2 审批结束 */
    private static final int STEP_FINISHED = 2;

    /** 单条明细金额上限，用于拦截明显的录入错误（例如多打了几个 0） */
    private static final BigDecimal MAX_EXPENSE_AMOUNT = new BigDecimal("100000000");

    /** 支付状态：0 待支付、1 已支付（与 oa_reimbursement.payment_status 的注释一致） */
    private static final String PAYMENT_STATUS_PAID = "1";

    @Autowired
    private OaReimbursementDetailMapper detailMapper;

    @Autowired
    private OaReimbursementApproveLogMapper approveLogMapper;

    @Autowired
    private DataScopeResolver dataScopeResolver;

    @Override
    public List<OaReimbursement> selectOaReimbursementList(OaReimbursement reimbursement) {
        LambdaQueryWrapper<OaReimbursement> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(reimbursement.getReimbType()), OaReimbursement::getReimbType, reimbursement.getReimbType())
                .eq(StringUtils.isNotBlank(reimbursement.getApprovalStatus()), OaReimbursement::getApprovalStatus, reimbursement.getApprovalStatus())
                .eq(reimbursement.getUserId() != null, OaReimbursement::getUserId, reimbursement.getUserId())
                .orderByDesc(OaReimbursement::getApplyTime);
        dataScopeResolver.apply(wrapper, OaReimbursement::getUserId, OaReimbursement::getDeptId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<OaReimbursement> selectMyReimbursementList(Long userId) {
        LambdaQueryWrapper<OaReimbursement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaReimbursement::getUserId, userId)
                .orderByDesc(OaReimbursement::getApplyTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaReimbursement selectOaReimbursementById(Long reimbId) {
        OaReimbursement reimbursement = baseMapper.selectById(reimbId);
        if (reimbursement != null) {
            // 单条读取同样受数据范围约束，避免用 id 遍历绕过列表接口的过滤
            requireReadable(reimbursement);
            LambdaQueryWrapper<OaReimbursementDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(OaReimbursementDetail::getReimbId, reimbId);
            reimbursement.setDetails(detailMapper.selectList(detailWrapper));

            LambdaQueryWrapper<OaReimbursementApproveLog> logWrapper = new LambdaQueryWrapper<>();
            logWrapper.eq(OaReimbursementApproveLog::getReimbId, reimbId);
            reimbursement.setApproveLogs(approveLogMapper.selectList(logWrapper));
        }
        return reimbursement;
    }

    @Override
    @Transactional
    public int saveWithDetails(OaReimbursement reimbursement) {
        // 申请人身份一律取当前登录用户，不接受客户端传入
        LoginUser current = SecurityUtils.getLoginUser();
        reimbursement.setUserId(current.getUserId());
        reimbursement.setUserName(current.getUsername());
        reimbursement.setDeptId(current.getDeptId());
        reimbursement.setDeptName(current.getDeptName());
        reimbursement.setApprovalStatus(ApprovalStatus.DRAFT);
        reimbursement.setCurrentStep(0);
        reimbursement.setApplyTime(new Date());
        reimbursement.setPaymentStatus(null);
        reimbursement.setPaymentTime(null);
        reimbursement.setApproveBy(null);
        reimbursement.setApproveTime(null);
        reimbursement.setApproveRemark(null);
        // 金额以明细为准，避免客户端提交与明细不一致的总额
        requireValidDetails(reimbursement.getDetails());
        reimbursement.setTotalAmount(sumDetails(reimbursement.getDetails()));
        int result = baseMapper.insert(reimbursement);
        insertDetails(reimbursement.getReimbId(), reimbursement.getDetails());
        return result;
    }

    @Override
    @Transactional
    public int updateOaReimbursement(OaReimbursement reimbursement) {
        OaReimbursement existing = requireExisting(reimbursement == null ? null : reimbursement.getReimbId());
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "报销单");
        if (!ApprovalStatus.DRAFT.equals(existing.getApprovalStatus())) {
            throw new IllegalArgumentException("报销单已提交审批，不能再修改或删除");
        }
        // 只接受业务字段，申请人、审批状态、支付状态等由服务端维护
        OaReimbursement update = new OaReimbursement();
        update.setReimbId(existing.getReimbId());
        update.setReimbType(reimbursement.getReimbType());
        update.setReimbReason(reimbursement.getReimbReason());
        update.setRemark(reimbursement.getRemark());
        if (reimbursement.getDetails() != null) {
            requireValidDetails(reimbursement.getDetails());
            update.setTotalAmount(sumDetails(reimbursement.getDetails()));
            LambdaQueryWrapper<OaReimbursementDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OaReimbursementDetail::getReimbId, existing.getReimbId());
            detailMapper.delete(wrapper);
            insertDetails(existing.getReimbId(), reimbursement.getDetails());
        }
        return baseMapper.updateById(update);
    }

    @Override
    @Transactional
    public int deleteOaReimbursementByIds(Long[] reimbIds) {
        int count = 0;
        for (Long reimbId : reimbIds) {
            OaReimbursement existing = requireExisting(reimbId);
            AccessGuards.requireSelfOrAdmin(existing.getUserId(), "报销单");
            if (!ApprovalStatus.DRAFT.equals(existing.getApprovalStatus())) {
                throw new IllegalArgumentException("报销单已提交审批，不能再修改或删除");
            }
            LambdaQueryWrapper<OaReimbursementDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(OaReimbursementDetail::getReimbId, reimbId);
            detailMapper.delete(detailWrapper);

            LambdaQueryWrapper<OaReimbursementApproveLog> logWrapper = new LambdaQueryWrapper<>();
            logWrapper.eq(OaReimbursementApproveLog::getReimbId, reimbId);
            approveLogMapper.delete(logWrapper);

            count += baseMapper.deleteById(reimbId);
        }
        return count;
    }

    @Override
    @Transactional
    public int submitOaReimbursement(Long reimbId) {
        OaReimbursement existing = requireExisting(reimbId);
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "报销单");
        if (!ApprovalStatus.DRAFT.equals(existing.getApprovalStatus())) {
            throw new IllegalArgumentException("只有草稿状态的报销单才能提交审批");
        }
        OaReimbursement update = new OaReimbursement();
        update.setReimbId(existing.getReimbId());
        update.setApprovalStatus(ApprovalStatus.PENDING);
        update.setCurrentStep(1);
        return baseMapper.updateById(update);
    }

    @Override
    public int withdrawOaReimbursement(Long reimbId) {
        OaReimbursement existing = requireExisting(reimbId);
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "报销单");
        if (!ApprovalStatus.PENDING.equals(existing.getApprovalStatus())) {
            throw new IllegalArgumentException("只有审批中的报销单才能撤回");
        }
        OaReimbursement update = new OaReimbursement();
        update.setReimbId(existing.getReimbId());
        update.setApprovalStatus(ApprovalStatus.DRAFT);
        update.setCurrentStep(0);
        return baseMapper.updateById(update);
    }

    @Override
    @Transactional
    public int approveOaReimbursement(OaReimbursement reimbursement) {
        if (reimbursement == null || reimbursement.getReimbId() == null) {
            throw new IllegalArgumentException("报销单ID不能为空");
        }
        if (!ApprovalStatus.isFinalDecision(reimbursement.getApprovalStatus())) {
            throw new IllegalArgumentException("审批结果只能是通过或拒绝");
        }
        OaReimbursement existing = requireExisting(reimbursement.getReimbId());
        if (!ApprovalStatus.PENDING.equals(existing.getApprovalStatus())) {
            throw new IllegalArgumentException("该报销单不在审批中，无法审批");
        }
        Long current = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin(current) && current != null && current.equals(existing.getUserId())) {
            throw new AccessDeniedException("不能审批自己提交的报销单");
        }
        // 只能审批落在本人数据范围内的报销单，口径与列表接口完全一致
        if (!inDataScope(existing)) {
            throw new AccessDeniedException("无权审批该报销单");
        }

        Date now = new Date();
        OaReimbursement update = new OaReimbursement();
        update.setApprovalStatus(reimbursement.getApprovalStatus());
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(now);
        update.setApproveRemark(reimbursement.getApproveRemark());
        update.setCurrentStep(STEP_FINISHED);
        if (ApprovalStatus.APPROVED.equals(reimbursement.getApprovalStatus())) {
            update.setPaymentStatus("0");
        }
        // 条件更新（CAS）：只有仍处于“审批中”的记录才会被写入审批结果，
        // 并发重复审批时后到的一次不会覆盖先到的决定，影响 0 行即说明状态已被别人改过
        LambdaUpdateWrapper<OaReimbursement> pendingWrapper = new LambdaUpdateWrapper<>();
        pendingWrapper.eq(OaReimbursement::getReimbId, existing.getReimbId())
                .eq(OaReimbursement::getApprovalStatus, ApprovalStatus.PENDING);
        int result = baseMapper.update(update, pendingWrapper);
        if (result == 0) {
            throw new IllegalArgumentException("该报销单已被其他人审批，请刷新后重试");
        }

        OaReimbursementApproveLog log = new OaReimbursementApproveLog();
        log.setReimbId(existing.getReimbId());
        log.setApproveResult(reimbursement.getApprovalStatus());
        log.setApproveRemark(reimbursement.getApproveRemark());
        log.setApproveBy(current);
        log.setApproveTime(now);
        approveLogMapper.insert(log);

        return result;
    }

    @Override
    @Transactional
    public int payOaReimbursement(Long reimbId) {
        OaReimbursement existing = requireExisting(reimbId);
        if (!ApprovalStatus.APPROVED.equals(existing.getApprovalStatus())) {
            throw new IllegalArgumentException("只有已通过的报销单才能付款");
        }
        // 与审批同口径的权限判定：付款属于财务终点动作，同样受数据范围约束，
        // 并且保留“不能给自己提交的单据付款”的职责分离规则，管理员仍然豁免
        Long current = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin(current) && current != null && current.equals(existing.getUserId())) {
            throw new AccessDeniedException("不能给自己提交的报销单付款");
        }
        if (!inDataScope(existing)) {
            throw new AccessDeniedException("无权付款该报销单");
        }

        // 支付状态与支付时间只由服务端写入；接口不接收请求体（只有路径参数），
        // 客户端没有可以塞 paymentStatus / paymentTime 的入口
        OaReimbursement update = new OaReimbursement();
        update.setPaymentStatus(PAYMENT_STATUS_PAID);
        update.setPaymentTime(new Date());
        // 条件更新（CAS）：只有“已通过且尚未支付”的记录才会被写入，
        // 并发重复付款时后到的一次不会覆盖先到的支付时间，影响 0 行即说明已被别人付过
        LambdaUpdateWrapper<OaReimbursement> unpaidWrapper = new LambdaUpdateWrapper<>();
        unpaidWrapper.eq(OaReimbursement::getReimbId, existing.getReimbId())
                .eq(OaReimbursement::getApprovalStatus, ApprovalStatus.APPROVED)
                .and(query -> query.isNull(OaReimbursement::getPaymentStatus)
                        .or().ne(OaReimbursement::getPaymentStatus, PAYMENT_STATUS_PAID));
        int result = baseMapper.update(update, unpaidWrapper);
        if (result == 0) {
            throw new IllegalArgumentException("该报销单已付款或状态已变更，请刷新后重试");
        }
        return result;
    }

    /** 单条读取权限：本人、管理员，或通过数据范围（与列表接口同一口径）可见的记录。 */
    private void requireReadable(OaReimbursement reimbursement) {
        if (AccessGuards.isSelfOrAdmin(reimbursement.getUserId()) || inDataScope(reimbursement)) {
            return;
        }
        throw new AccessDeniedException("无权查看该报销单");
    }

    /** 该记录是否落在当前登录用户的数据范围内。 */
    private boolean inDataScope(OaReimbursement reimbursement) {
        LambdaQueryWrapper<OaReimbursement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaReimbursement::getReimbId, reimbursement.getReimbId());
        dataScopeResolver.apply(wrapper, OaReimbursement::getUserId, OaReimbursement::getDeptId);
        Long count = baseMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /** 明细与审批日志依附于主单：读取前先按主单的数据范围校验，主单不存在时保持原来的空结果。 */
    private void requireMasterReadable(Long reimbId) {
        OaReimbursement master = reimbId == null ? null : baseMapper.selectById(reimbId);
        if (master != null) {
            requireReadable(master);
        }
    }

    private OaReimbursement requireExisting(Long reimbId) {
        OaReimbursement existing = reimbId == null ? null : baseMapper.selectById(reimbId);
        if (existing == null) {
            throw new IllegalArgumentException("报销单不存在");
        }
        return existing;
    }

    private void insertDetails(Long reimbId, List<OaReimbursementDetail> details) {
        if (reimbId == null || details == null) {
            return;
        }
        for (OaReimbursementDetail detail : details) {
            if (detail == null) {
                continue;
            }
            // 明细归属由服务端决定，避免客户端把明细挂到别人的报销单上
            detail.setDetailId(null);
            detail.setReimbId(reimbId);
            detailMapper.insert(detail);
        }
    }

    /**
     * 明细金额一律由服务端校验：不能为空、不能为负、不能超过上限。
     *
     * <p>前端表单虽然有 {@code :min="0"}，但客户端校验只是提示，服务端才是唯一可信边界：
     * 直接调用接口提交负数明细会让合计变成负数。</p>
     */
    private void requireValidDetails(List<OaReimbursementDetail> details) {
        if (details == null) {
            return;
        }
        for (OaReimbursementDetail detail : details) {
            if (detail == null) {
                continue;
            }
            BigDecimal amount = detail.getExpenseAmount();
            if (amount == null) {
                throw new IllegalArgumentException("报销明细金额不能为空");
            }
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("报销明细金额不能为负数");
            }
            if (amount.compareTo(MAX_EXPENSE_AMOUNT) > 0) {
                throw new IllegalArgumentException("单条报销明细金额不能超过 " + MAX_EXPENSE_AMOUNT.toPlainString());
            }
        }
    }

    private BigDecimal sumDetails(List<OaReimbursementDetail> details) {
        BigDecimal total = BigDecimal.ZERO;
        if (details == null) {
            return total;
        }
        for (OaReimbursementDetail detail : details) {
            if (detail != null && detail.getExpenseAmount() != null) {
                total = total.add(detail.getExpenseAmount());
            }
        }
        return total;
    }

    @Override
    public List<OaReimbursementDetail> getReimbursementDetails(Long reimbId) {
        requireMasterReadable(reimbId);
        LambdaQueryWrapper<OaReimbursementDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaReimbursementDetail::getReimbId, reimbId);
        return detailMapper.selectList(wrapper);
    }

    @Override
    public List<OaReimbursementApproveLog> getApproveLogs(Long reimbId) {
        requireMasterReadable(reimbId);
        LambdaQueryWrapper<OaReimbursementApproveLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaReimbursementApproveLog::getReimbId, reimbId)
                .orderByDesc(OaReimbursementApproveLog::getApproveTime);
        return approveLogMapper.selectList(wrapper);
    }
}