package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@TableName("oa_reimbursement")
public class OaReimbursement extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long reimbId;
    
    private String reimbType;
    
    private BigDecimal totalAmount;
    
    private String reimbReason;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;
    
    private String approvalStatus;
    
    private Integer currentStep;
    
    private String paymentStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date paymentTime;
    
    private String approveBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;
    
    private String approveRemark;
    
    private Long userId;
    
    private String userName;
    
    private Long deptId;
    
    private String deptName;
    
    @TableField(exist = false)
    private List<OaReimbursementDetail> details;
    
    @TableField(exist = false)
    private List<OaReimbursementApproveLog> approveLogs;
}