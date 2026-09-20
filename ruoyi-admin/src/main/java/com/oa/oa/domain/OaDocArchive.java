package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("oa_doc_archive")
public class OaDocArchive extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long archiveId;
    private Long docId;
    private String docType;
    private String archiveCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date archiveTime;
    private Long archiveUserId;
    private String archiveUserName;
    private String archiveLocation;
}