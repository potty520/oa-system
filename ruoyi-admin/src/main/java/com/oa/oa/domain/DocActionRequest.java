package com.oa.oa.domain;

import lombok.Data;

import java.util.List;

@Data
public class DocActionRequest {
    private String docType;
    private String action;
    private Long approvalId;
    private String opinion;
    private Long targetUserId;
    private String targetUserName;
    private List<Long> targetUserIds;
    private Long flowId;
    private Long stepId;
    private String archiveCode;
    private String archiveLocation;
}