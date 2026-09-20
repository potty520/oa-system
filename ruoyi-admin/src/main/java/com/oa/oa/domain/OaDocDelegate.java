package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

@Data
@TableName("oa_doc_delegate")
public class OaDocDelegate extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long delegateId;
    private Long docId;
    private String docType;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private String delegateType;
    private String opinion;
    private String status;
}