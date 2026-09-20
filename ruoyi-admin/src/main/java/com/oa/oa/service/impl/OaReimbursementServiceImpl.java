package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaReimbursement;
import com.oa.oa.domain.OaReimbursementApproveLog;
import com.oa.oa.domain.OaReimbursementDetail;
import com.oa.oa.mapper.OaReimbursementApproveLogMapper;
import com.oa.oa.mapper.OaReimbursementDetailMapper;
import com.oa.oa.mapper.OaReimbursementMapper;
import com.oa.oa.service.IOaReimbursementService;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class OaReimbursementServiceImpl extends ServiceImpl<OaReimbursementMapper, OaReimbursement> implements IOaReimbursementService {

    @Autowired
    private OaReimbursementDetailMapper detailMapper;

    @Autowired
    private OaReimbursementApproveLogMapper approveLogMapper;

    @Override
    public List<OaReimbursement> selectOaReimbursementList(OaReimbursement reimbursement) {
        LambdaQueryWrapper<OaReimbursement> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(reimbursement.getReimbType()), OaReimbursement::getReimbType, reimbursement.getReimbType())
                .eq(StringUtils.isNotBlank(reimbursement.getApprovalStatus()), OaReimbursement::getApprovalStatus, reimbursement.getApprovalStatus())
                .eq(reimbursement.getUserId() != null, OaReimbursement::getUserId, reimbursement.getUserId())
                .orderByDesc(OaReimbursement::getApplyTime);
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
        reimbursement.setUserId(SecurityUtils.getUserId());
        reimbursement.setUserName(SecurityUtils.getUsername());
        reimbursement.setApprovalStatus("0");
        reimbursement.setCurrentStep(0);
        reimbursement.setApplyTime(new Date());
        int result = baseMapper.insert(reimbursement);

        if (reimbursement.getDetails() != null && !reimbursement.getDetails().isEmpty()) {
            for (OaReimbursementDetail detail : reimbursement.getDetails()) {
                detail.setReimbId(reimbursement.getReimbId());
                detailMapper.insert(detail);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int updateOaReimbursement(OaReimbursement reimbursement) {
        int result = baseMapper.updateById(reimbursement);
        if (reimbursement.getDetails() != null) {
            LambdaQueryWrapper<OaReimbursementDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OaReimbursementDetail::getReimbId, reimbursement.getReimbId());
            detailMapper.delete(wrapper);
            for (OaReimbursementDetail detail : reimbursement.getDetails()) {
                detail.setDetailId(null);
                detail.setReimbId(reimbursement.getReimbId());
                detailMapper.insert(detail);
            }
        }
        return result;
    }

    @Override
    public int deleteOaReimbursementByIds(Long[] reimbIds) {
        int count = 0;
        for (Long reimbId : reimbIds) {
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
    public int submitOaReimbursement(Long reimbId) {
        OaReimbursement reimbursement = baseMapper.selectById(reimbId);
        if (reimbursement != null && "0".equals(reimbursement.getApprovalStatus())) {
            reimbursement.setApprovalStatus("1");
            reimbursement.setCurrentStep(1);
            return baseMapper.updateById(reimbursement);
        }
        return 0;
    }

    @Override
    public int withdrawOaReimbursement(Long reimbId) {
        OaReimbursement reimbursement = baseMapper.selectById(reimbId);
        if (reimbursement != null && "1".equals(reimbursement.getApprovalStatus())) {
            reimbursement.setApprovalStatus("0");
            reimbursement.setCurrentStep(0);
            return baseMapper.updateById(reimbursement);
        }
        return 0;
    }

    @Override
    @Transactional
    public int approveOaReimbursement(OaReimbursement reimbursement) {
        OaReimbursement existReimbursement = baseMapper.selectById(reimbursement.getReimbId());
        if (existReimbursement != null && "1".equals(existReimbursement.getApprovalStatus())) {
            existReimbursement.setApprovalStatus(reimbursement.getApprovalStatus());
            existReimbursement.setApproveBy(SecurityUtils.getUsername());
            existReimbursement.setApproveTime(new Date());
            existReimbursement.setApproveRemark(reimbursement.getApproveRemark());

            if ("2".equals(reimbursement.getApprovalStatus())) {
                existReimbursement.setPaymentStatus("0");
            }

            int result = baseMapper.updateById(existReimbursement);

            OaReimbursementApproveLog log = new OaReimbursementApproveLog();
            log.setReimbId(reimbursement.getReimbId());
            log.setApproveResult(reimbursement.getApprovalStatus());
            log.setApproveRemark(reimbursement.getApproveRemark());
            log.setApproveBy(SecurityUtils.getUserId());
            log.setApproveTime(new Date());
            approveLogMapper.insert(log);

            return result;
        }
        return 0;
    }

    @Override
    public List<OaReimbursementDetail> getReimbursementDetails(Long reimbId) {
        LambdaQueryWrapper<OaReimbursementDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaReimbursementDetail::getReimbId, reimbId);
        return detailMapper.selectList(wrapper);
    }

    @Override
    public List<OaReimbursementApproveLog> getApproveLogs(Long reimbId) {
        LambdaQueryWrapper<OaReimbursementApproveLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaReimbursementApproveLog::getReimbId, reimbId)
                .orderByDesc(OaReimbursementApproveLog::getApproveTime);
        return approveLogMapper.selectList(wrapper);
    }
}