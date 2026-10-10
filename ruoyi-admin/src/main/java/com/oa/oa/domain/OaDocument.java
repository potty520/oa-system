package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

@Data
@TableName("oa_document")
public class OaDocument extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long documentId;
    private String documentName;
    private String documentType;
    /** 服务器绝对路径，只由后台上传逻辑写入，禁止下发到客户端（下载走 /oa/document/download/{documentId}） */
    @JsonIgnore
    private String filePath;
    private Long fileSize;
    private Long userId;
    private String userName;
    private String status;
}