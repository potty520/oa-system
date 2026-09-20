package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("oa_doc_reminder")
public class OaDocReminder extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long reminderId;
    private Long docId;
    private String docType;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private String reminderContent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reminderTime;
    private String status;
}