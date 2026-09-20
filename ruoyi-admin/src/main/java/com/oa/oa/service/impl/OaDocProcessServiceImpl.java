package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oa.oa.domain.*;
import com.oa.oa.mapper.*;
import com.oa.oa.service.IOaDocProcessService;
import com.oa.security.model.LoginUser;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OaDocProcessServiceImpl implements IOaDocProcessService {

    public static final String DOC_INCOMING = "1";
    public static final String DOC_OUTGOING = "2";

    @Autowired
    private OaIncomingDocMapper incomingDocMapper;
    @Autowired
    private OaOutgoingDocMapper outgoingDocMapper;
    @Autowired
    private OaDocApprovalMapper approvalMapper;
    @Autowired
    private OaDocCirculationMapper circulationMapper;
    @Autowired
    private OaDocReminderMapper reminderMapper;
    @Autowired
    private OaDocArchiveMapper archiveMapper;

    @Override
    public Map<String, Object> getTimeline(String docType, Long docId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvals", approvalMapper.selectList(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getDocType, docType)
                .eq(OaDocApproval::getDocId, docId)
                .orderByDesc(OaDocApproval::getApproveTime)));
        result.put("circulations", circulationMapper.selectList(new LambdaQueryWrapper<OaDocCirculation>()
                .eq(OaDocCirculation::getDocType, docType)
                .eq(OaDocCirculation::getDocId, docId)
                .orderByDesc(OaDocCirculation::getCreateTime)));
        result.put("reminders", getReminderList(docType, docId));
        result.put("archives", archiveMapper.selectList(new LambdaQueryWrapper<OaDocArchive>()
                .eq(OaDocArchive::getDocType, docType)
                .eq(OaDocArchive::getDocId, docId)
                .orderByDesc(OaDocArchive::getArchiveTime)));
        return result;
    }

    @Override
    @Transactional
    public void execute(String docType, Long docId, DocActionRequest request) {
        LoginUser user = requireUser();
        String action = request.getAction();
        if (StringUtils.isBlank(action)) {
            throw new IllegalArgumentException("操作类型不能为空");
        }
        String currentStatus = getStatus(docType, docId);
        if (currentStatus == null) {
            throw new IllegalArgumentException("公文不存在");
        }

        if ("receive".equals(action)) {
            requireStatus(docType, DOC_INCOMING, currentStatus, "0", "只有待签收收文可以签收");
            setStatus(docType, docId, "1");
            return;
        }
        if ("submit".equals(action)) {
            requireStatus(docType, DOC_OUTGOING, currentStatus, "0", "只有草稿发文可以提交");
            setStatus(docType, docId, "1");
            return;
        }
        if ("complete".equals(action)) {
            requireStatus(docType, DOC_INCOMING, currentStatus, "1", "只有办理中的收文可以办结");
            setStatus(docType, docId, "2");
            return;
        }
        if ("sign".equals(action)) {
            requireStatus(docType, DOC_OUTGOING, currentStatus, "1", "只有审核中的发文可以签发");
            setStatus(docType, docId, "2");
        }

        if (Arrays.asList("approve", "countersign", "return", "withdraw", "transfer", "circulate", "urge", "archive").contains(action)) {
            if ("approve".equals(action)) {
                addApproval(docType, docId, "1", request, user);
                if (DOC_OUTGOING.equals(docType) && "1".equals(currentStatus)) {
                    setStatus(docType, docId, "2");
                } else if (DOC_INCOMING.equals(docType) && "1".equals(currentStatus)) {
                    setStatus(docType, docId, "2");
                }
            } else if ("countersign".equals(action)) {
                addApproval(docType, docId, "5", request, user);
            } else if ("return".equals(action)) {
                addApproval(docType, docId, "3", request, user);
                setStatus(docType, docId, DOC_OUTGOING.equals(docType) ? "0" : "1");
            } else if ("withdraw".equals(action)) {
                addApproval(docType, docId, "4", request, user);
                setStatus(docType, docId, "0");
            } else if ("transfer".equals(action)) {
                if (request.getTargetUserId() == null) {
                    throw new IllegalArgumentException("转办需要选择目标用户");
                }
                addApproval(docType, docId, "6", request, user);
                addCirculation(docType, docId, request.getTargetUserId(), request.getTargetUserName(), "0");
            } else if ("circulate".equals(action)) {
                List<Long> ids = request.getTargetUserIds();
                if (ids == null || ids.isEmpty()) {
                    throw new IllegalArgumentException("传阅需要选择接收人");
                }
                for (Long targetUserId : ids) {
                    addCirculation(docType, docId, targetUserId, null, "0");
                }
            } else if ("urge".equals(action)) {
                OaDocReminder reminder = new OaDocReminder();
                reminder.setDocId(docId);
                reminder.setDocType(docType);
                reminder.setFromUserId(user.getUserId());
                reminder.setFromUserName(user.getUsername());
                reminder.setToUserId(request.getTargetUserId());
                reminder.setToUserName(request.getTargetUserName());
                reminder.setReminderContent(StringUtils.defaultIfBlank(request.getOpinion(), "请及时办理"));
                reminder.setReminderTime(new Date());
                reminder.setStatus("0");
                reminderMapper.insert(reminder);
            } else if ("archive".equals(action)) {
                OaDocArchive archive = new OaDocArchive();
                archive.setDocId(docId);
                archive.setDocType(docType);
                archive.setArchiveCode(StringUtils.defaultIfBlank(request.getArchiveCode(), "ARCH-" + docType + "-" + docId));
                archive.setArchiveTime(new Date());
                archive.setArchiveUserId(user.getUserId());
                archive.setArchiveUserName(user.getUsername());
                archive.setArchiveLocation(request.getArchiveLocation());
                archiveMapper.insert(archive);
                setStatus(docType, docId, "3");
            }
            return;
        }
        throw new IllegalArgumentException("不支持的公文操作: " + action);
    }

    @Override
    public List<OaDocCirculation> getMyCirculation() {
        LoginUser user = requireUser();
        List<OaDocCirculation> list = circulationMapper.selectList(new LambdaQueryWrapper<OaDocCirculation>()
                .eq(OaDocCirculation::getUserId, user.getUserId())
                .orderByAsc(OaDocCirculation::getReadStatus)
                .orderByDesc(OaDocCirculation::getCreateTime));
        for (OaDocCirculation item : list) {
            enrichCirculation(item);
        }
        return list;
    }

    @Override
    public int markCirculationRead(Long circulationId) {
        OaDocCirculation circulation = circulationMapper.selectById(circulationId);
        LoginUser user = requireUser();
        if (circulation == null || !Objects.equals(circulation.getUserId(), user.getUserId())) {
            return 0;
        }
        circulation.setReadStatus("1");
        circulation.setReadTime(new Date());
        return circulationMapper.updateById(circulation);
    }

    @Override
    public List<OaDocArchive> getArchiveList(OaDocArchive archive) {
        LambdaQueryWrapper<OaDocArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(archive.getDocId() != null, OaDocArchive::getDocId, archive.getDocId())
                .eq(StringUtils.isNotBlank(archive.getDocType()), OaDocArchive::getDocType, archive.getDocType())
                .like(StringUtils.isNotBlank(archive.getArchiveCode()), OaDocArchive::getArchiveCode, archive.getArchiveCode())
                .orderByDesc(OaDocArchive::getArchiveTime);
        return archiveMapper.selectList(wrapper);
    }

    @Override
    public List<OaDocApproval> getMyDone() {
        LoginUser user = requireUser();
        return approvalMapper.selectList(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getUserId, user.getUserId())
                .orderByDesc(OaDocApproval::getApproveTime));
    }

    @Override
    public List<OaDocReminder> getReminderList(String docType, Long docId) {
        return reminderMapper.selectList(new LambdaQueryWrapper<OaDocReminder>()
                .eq(docType != null && !docType.isEmpty(), OaDocReminder::getDocType, docType)
                .eq(docId != null, OaDocReminder::getDocId, docId)
                .orderByDesc(OaDocReminder::getReminderTime));
    }

    private LoginUser requireUser() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) {
            throw new IllegalStateException("未登录");
        }
        return user;
    }

    private void requireStatus(String actualType, String expectedType, String current, String expectedStatus, String message) {
        if (!expectedType.equals(actualType) || !expectedStatus.equals(current)) {
            throw new IllegalStateException(message);
        }
    }

    private String getStatus(String docType, Long docId) {
        if (DOC_INCOMING.equals(docType)) {
            OaIncomingDoc doc = incomingDocMapper.selectById(docId);
            return doc == null ? null : doc.getStatus();
        }
        OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
        return doc == null ? null : doc.getStatus();
    }

    private void setStatus(String docType, Long docId, String status) {
        if (DOC_INCOMING.equals(docType)) {
            OaIncomingDoc doc = incomingDocMapper.selectById(docId);
            doc.setStatus(status);
            incomingDocMapper.updateById(doc);
        } else {
            OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
            doc.setStatus(status);
            outgoingDocMapper.updateById(doc);
        }
    }

    private void addApproval(String docType, Long docId, String result, DocActionRequest request, LoginUser user) {
        OaDocApproval approval = new OaDocApproval();
        approval.setDocId(docId);
        approval.setDocType(docType);
        approval.setFlowId(request.getFlowId());
        approval.setStepId(request.getStepId());
        approval.setUserId(user.getUserId());
        approval.setUserName(user.getUsername());
        approval.setApproveResult(result);
        approval.setApproveRemark(request.getOpinion());
        approval.setApproveTime(new Date());
        approvalMapper.insert(approval);
    }

    private void addCirculation(String docType, Long docId, Long userId, String userName, String readStatus) {
        OaDocCirculation circulation = new OaDocCirculation();
        circulation.setDocId(docId);
        circulation.setDocType(docType);
        circulation.setUserId(userId);
        circulation.setUserName(userName);
        circulation.setReadStatus(readStatus);
        circulationMapper.insert(circulation);
    }

    private void enrichCirculation(OaDocCirculation item) {
        if (DOC_INCOMING.equals(item.getDocType())) {
            OaIncomingDoc doc = incomingDocMapper.selectById(item.getDocId());
            if (doc != null) {
                item.setDocTitle(doc.getDocTitle());
                item.setDocNumber(doc.getDocNumber());
                item.setStatus(doc.getStatus());
            }
        } else {
            OaOutgoingDoc doc = outgoingDocMapper.selectById(item.getDocId());
            if (doc != null) {
                item.setDocTitle(doc.getDocTitle());
                item.setDocNumber(doc.getDocNumber());
                item.setStatus(doc.getStatus());
            }
        }
    }
}