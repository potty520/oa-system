package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.List;

@Data
@TableName("oa_doc_flow")
public class OaDocFlow extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long flowId;
    
    private String flowName;
    
    private String flowType;
    
    private String description;
    
    private String status;
    
    @TableField(exist = false)
    private List<OaDocFlowStep> steps;
}