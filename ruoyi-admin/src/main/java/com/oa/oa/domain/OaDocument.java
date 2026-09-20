package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

@Data
@TableName("oa_document")
public class OaDocument extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long documentId;
    private String documentName;
    private String documentType;
    private String filePath;
    private Long fileSize;
    private Long userId;
    private String userName;
    private String status;
}