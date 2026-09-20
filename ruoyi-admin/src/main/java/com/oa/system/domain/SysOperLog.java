package com.oa.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private String title;
    private String businessType;
    private String method;
    private String requestMethod;
    private String operatorName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private String status;
    private String errorMsg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operTime;
}