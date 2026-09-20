package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

@Data
@TableName("oa_doc_type")
public class OaDocType extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long docTypeId;
    private String typeName;
    private String typeCode;
    private Long parentId;
    private Integer sortOrder;
    private String status;
}