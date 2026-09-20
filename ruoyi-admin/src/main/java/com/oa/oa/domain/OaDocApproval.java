package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("oa_doc_approval")
public class OaDocApproval extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long approvalId;
    private Long docId;
    private String docType;
    private Long flowId;
    private Long stepId;
    private Long userId;
    private String userName;
    private String approveResult;
    private String approveRemark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;
}