package com.oa.common.constant;

/**
 * 请假、报销等审批单据的通用审批状态。
 */
public final class ApprovalStatus {

    /** 草稿 */
    public static final String DRAFT = "0";

    /** 审批中 */
    public static final String PENDING = "1";

    /** 已通过 */
    public static final String APPROVED = "2";

    /** 已拒绝 */
    public static final String REJECTED = "3";

    private ApprovalStatus() {
    }

    /** 审批接口只允许把单据判成通过或拒绝。 */
    public static boolean isFinalDecision(String status) {
        return APPROVED.equals(status) || REJECTED.equals(status);
    }
}
