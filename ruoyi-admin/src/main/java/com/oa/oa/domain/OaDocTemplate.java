package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

@Data
@TableName("oa_doc_template")
public class OaDocTemplate extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long templateId;
    private String templateName;
    private String docType;
    private String redHeaderText;
    private String titleFont;
    private Integer titleSize;
    private String bodyFont;
    private Integer bodySize;
    private String templateContent;
    private String status;
}