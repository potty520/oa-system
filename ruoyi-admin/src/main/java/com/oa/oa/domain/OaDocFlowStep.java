package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

@Data
@TableName("oa_doc_flow_step")
public class OaDocFlowStep extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long stepId;
    
    private Long flowId;
    
    private String stepName;
    
    private Integer stepOrder;
    
    private Long roleId;
    
    private String isSign;
}