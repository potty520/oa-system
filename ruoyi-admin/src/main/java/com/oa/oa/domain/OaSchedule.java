package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("oa_schedule")
public class OaSchedule extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long scheduleId;
    
    private String scheduleTitle;
    
    private String scheduleType;
    
    private String scheduleColor;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    
    private String scheduleContent;
    
    private String location;
    
    private String remindFlag;
    
    private String remindTime;
    
    private String status;
    
    private Long userId;
}