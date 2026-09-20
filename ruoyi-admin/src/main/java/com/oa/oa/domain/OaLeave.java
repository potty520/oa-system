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
@TableName("oa_leave")
public class OaLeave extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long leaveId;
    
    private String leaveType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    
    private BigDecimal leaveDays;
    
    private String leaveReason;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;
    
    private String approvalStatus;
    
    private Integer currentStep;
    
    private Long userId;
    
    private String userName;
    
    private Long deptId;
    
    private String deptName;

    private String approveBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;
}