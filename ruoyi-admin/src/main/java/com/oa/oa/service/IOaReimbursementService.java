package com.oa.oa.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.oa.domain.OaReimbursement;
import com.oa.oa.domain.OaReimbursementApproveLog;
import com.oa.oa.domain.OaReimbursementDetail;

import java.util.List;

public interface IOaReimbursementService extends IService<OaReimbursement> {
    List<OaReimbursement> selectOaReimbursementList(OaReimbursement reimbursement);
    List<OaReimbursement> selectMyReimbursementList(Long userId);
    OaReimbursement selectOaReimbursementById(Long reimbId);
    int saveWithDetails(OaReimbursement reimbursement);
    int updateOaReimbursement(OaReimbursement reimbursement);
    int deleteOaReimbursementByIds(Long[] reimbIds);
    int submitOaReimbursement(Long reimbId);
    int withdrawOaReimbursement(Long reimbId);
    int approveOaReimbursement(OaReimbursement reimbursement);
    List<OaReimbursementDetail> getReimbursementDetails(Long reimbId);
    List<OaReimbursementApproveLog> getApproveLogs(Long reimbId);
}