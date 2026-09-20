package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("oa_doc_circulation")
public class OaDocCirculation extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long circulationId;
    private Long docId;
    private String docType;
    private Long userId;
    private String userName;
    private String readStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;

    @TableField(exist = false)
    private String docTitle;

    @TableField(exist = false)
    private String docNumber;

    @TableField(exist = false)
    private String status;
}