package com.oa.oa.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class OaTodo {
    
    private Long todoId;
    
    private String todoType;
    
    private String todoTitle;
    
    private String todoContent;
    
    private String applyUser;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;
    
    private String status;
    
    private Long relatedId;
}