package com.oa.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("oa_announcement")
public class OaAnnouncement extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long annId;
    
    private String annTitle;
    
    private String annType;
    
    private String annContent;
    
    private String annLevel;
    
    private String attachmentUrl;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishEndTime;
    
    private String isTop;
    
    private String isPublic;
    
    private Integer viewCount;
    
    private String status;
}