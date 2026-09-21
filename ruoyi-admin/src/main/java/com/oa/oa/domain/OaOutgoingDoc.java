package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("oa_outgoing_doc")
public class OaOutgoingDoc extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long docId;
    
    private String docNumber;
    
    private String docTitle;
    
    private Long docTypeId;

    private Long templateId;

    private Long flowId;

    private Long currentStepId;

    private Integer currentStepOrder;
    
    private String draftUnit;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date draftDate;
    
    private String docLevel;
    
    private String docSecret;
    
    private String mainSend;
    
    private String copySend;
    
    private String summary;
    
    private String attachmentUrl;
    
    private Long signUserId;
    
    private String signUserName;
    
    private String status;
}