package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("oa_reimbursement_detail")
public class OaReimbursementDetail extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long detailId;
    
    private Long reimbId;
    
    private String expenseType;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expenseDate;
    
    private BigDecimal expenseAmount;
    
    private String expenseDesc;
    
    private String receiptUrl;
}