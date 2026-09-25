package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oa.oa.domain.*;
import com.oa.oa.mapper.*;
import com.oa.oa.service.IOaDocProcessService;
import com.oa.security.model.LoginUser;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.mapper.SysUserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private OaDocDelegateMapper delegateMapper;
    @Autowired
    private OaDocFlowMapper flowMapper;
    @Autowired
    private OaDocFlowStepMapper flowStepMapper;
    @Autowired
    private SysUserMapper userMapper;

    @Override
    public Map<String, Object> getTimeline(String docType, Long docId) {
        requireDocumentAccess(docType, docId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvals", approvalMapper.selectList(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getDocType, docType)
                .eq(OaDocApproval::getDocId, docId)
                .orderByDesc(OaDocApproval::getApproveTime)));
        result.put("circulations", circulationMapper.selectList(new LambdaQueryWrapper<OaDocCirculation>()
                .eq(OaDocCirculation::getDocType, docType)
                .eq(OaDocCirculation::getDocId, docId)
                .orderByDesc(OaDocCirculation::getCreateTime)));
        result.put("reminders", queryReminders(docType, docId));
        result.put("delegates", delegateMapper.selectList(new LambdaQueryWrapper<OaDocDelegate>()
                .eq(OaDocDelegate::getDocType, docType)
                .eq(OaDocDelegate::getDocId, docId)
                .orderByDesc(OaDocDelegate::getCreateTime)));
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
        requireActionPermission(docType, docId, action, request, user);

        if ("receive".equals(action)) {
            requireStatus(docType, DOC_INCOMING, currentStatus, "0", "只有待签收收文可以签收");
            setStatus(docType, docId, currentStatus, "1");
            startDocumentFlow(docType, docId, user);
            return;
        }
        if ("submit".equals(action)) {
            requireStatus(docType, DOC_OUTGOING, currentStatus, "0", "只有草稿发文可以提交");
            requireSignerAssigned(docType, docId);
            setStatus(docType, docId, currentStatus, "1");
            startDocumentFlow(docType, docId, user);
            return;
        }
        if ("complete".equals(action)) {
            requireStatus(docType, DOC_INCOMING, currentStatus, "1", "只有办理中的收文可以办结");
            setStatus(docType, docId, currentStatus, "2");
            return;
        }
        if ("sign".equals(action)) {
            requireStatus(docType, DOC_OUTGOING, currentStatus, "1", "只有审核中的发文可以签发");
            if (hasFlowBinding(docType, docId)) {
                completeDocumentStep(docType, docId, currentStatus, user, request, true);
                return;
            }
            int rows = outgoingDocMapper.signIfCurrent(docId, currentStatus, "2", user.getUserId(), user.getUsername());
            if (rows != 1) {
                throw new IllegalStateException("公文状态已变化，请刷新后重试");
            }
            return;
        }

        validateActionState(docType, currentStatus, action);

        if (Arrays.asList("approve", "countersign", "add_sign", "cancel_sign", "return", "withdraw", "transfer", "delegate", "agent", "circulate", "urge", "archive").contains(action)) {
            if ("approve".equals(action)) {
                if (hasFlowBinding(docType, docId)) {
                    completeDocumentStep(docType, docId, currentStatus, user, request, false);
                } else {
                    addApproval(docType, docId, "1", request, user);
                    if (DOC_INCOMING.equals(docType) && "1".equals(currentStatus)) {
                        setStatus(docType, docId, currentStatus, "2");
                    }
                }
            } else if ("countersign".equals(action)) {
                if (request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
                    for (Long targetUserId : request.getTargetUserIds()) {
                        addApprovalForUser(docType, docId, "5", request, user, targetUserId, request.getTargetUserName());
                    }
                } else {
                    addApproval(docType, docId, "5", request, user);
                }
            } else if ("add_sign".equals(action)) {
                requireTargetUsers(request, "加签需要选择用户");
                for (Long targetUserId : request.getTargetUserIds()) {
                    addApprovalForUser(docType, docId, "7", request, user, targetUserId, request.getTargetUserName());
                }
            } else if ("cancel_sign".equals(action)) {
                if (request.getApprovalId() == null) {
                    throw new IllegalArgumentException("撤签需要指定签批记录");
                }
                OaDocApproval approval = approvalMapper.selectById(request.getApprovalId());
                if (approval == null || !Objects.equals(approval.getDocId(), docId) || !Objects.equals(approval.getDocType(), docType)) {
                    throw new IllegalArgumentException("签批记录不存在");
                }
                // 只能撤分配给自己的待办签批，否则任何参与人都能把别人的签批改成“已撤签”
                if (!SecurityUtils.isAdmin(user.getUserId()) && !Objects.equals(approval.getUserId(), user.getUserId())) {
                    throw new AccessDeniedException("只能撤回分配给自己的签批");
                }
                if (approvalMapper.approveIfPending(approval.getApprovalId(), "8",
                        StringUtils.defaultIfBlank(request.getOpinion(), "已撤签"), user.getUsername()) != 1) {
                    throw new IllegalStateException("该签批已经办理，无法撤签");
                }
            } else if ("delegate".equals(action) || "agent".equals(action)) {
                if (request.getTargetUserId() == null) {
                    throw new IllegalArgumentException("委托或代理需要选择目标用户");
                }
                OaDocDelegate delegate = new OaDocDelegate();
                delegate.setDocId(docId);
                delegate.setDocType(docType);
                delegate.setFromUserId(user.getUserId());
                delegate.setFromUserName(user.getUsername());
                delegate.setToUserId(request.getTargetUserId());
                delegate.setToUserName(resolveUserName(request.getTargetUserId(), request.getTargetUserName()));
                delegate.setDelegateType("agent".equals(action) ? "2" : "1");
                delegate.setOpinion(request.getOpinion());
                delegate.setStatus("0");
                delegateMapper.insert(delegate);
            } else if ("return".equals(action)) {
                addApproval(docType, docId, "3", request, user);
                cancelPendingAssignments(docType, docId, StringUtils.defaultIfBlank(request.getOpinion(), "已退回"));
                clearFlowState(docType, docId);
                setStatus(docType, docId, currentStatus, DOC_OUTGOING.equals(docType) ? "0" : "1");
            } else if ("withdraw".equals(action)) {
                addApproval(docType, docId, "4", request, user);
                cancelPendingAssignments(docType, docId, StringUtils.defaultIfBlank(request.getOpinion(), "已撤回"));
                clearFlowState(docType, docId);
                setStatus(docType, docId, currentStatus, "0");
            } else if ("transfer".equals(action)) {
                if (request.getTargetUserId() == null) {
                    throw new IllegalArgumentException("转办需要选择目标用户");
                }
                addApproval(docType, docId, "6", request, user);
                addCirculation(docType, docId, request.getTargetUserId(),
                        resolveUserName(request.getTargetUserId(), request.getTargetUserName()), "0");
            } else if ("circulate".equals(action)) {
                List<Long> ids = request.getTargetUserIds();
                if (ids == null || ids.isEmpty()) {
                    throw new IllegalArgumentException("传阅需要选择接收人");
                }
                for (Long targetUserId : ids) {
                    addCirculation(docType, docId, targetUserId, resolveUserName(targetUserId, null), "0");
                }
            } else if ("urge".equals(action)) {
                OaDocReminder reminder = new OaDocReminder();
                reminder.setDocId(docId);
                reminder.setDocType(docType);
                reminder.setFromUserId(user.getUserId());
                reminder.setFromUserName(user.getUsername());
                reminder.setToUserId(request.getTargetUserId());
                reminder.setToUserName(resolveUserName(request.getTargetUserId(), request.getTargetUserName()));
                reminder.setReminderContent(StringUtils.defaultIfBlank(request.getOpinion(), "请及时办理"));
                reminder.setReminderTime(new Date());
                reminder.setStatus("0");
                reminderMapper.insert(reminder);
            } else if ("archive".equals(action)) {
                if (!"2".equals(currentStatus)) {
                    throw new IllegalStateException("只有已办结或已签发的公文可以归档");
                }
                OaDocArchive archive = new OaDocArchive();
                archive.setDocId(docId);
                archive.setDocType(docType);
                archive.setArchiveCode(StringUtils.defaultIfBlank(request.getArchiveCode(), "ARCH-" + docType + "-" + docId));
                archive.setArchiveTime(new Date());
                archive.setArchiveUserId(user.getUserId());
                archive.setArchiveUserName(user.getUsername());
                archive.setArchiveLocation(request.getArchiveLocation());
                archiveMapper.insert(archive);
                setStatus(docType, docId, currentStatus, "3");
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
        enrichCirculations(list);
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
        requireDocumentAccess(docType, docId);
        return queryReminders(docType, docId);
    }

    private List<OaDocReminder> queryReminders(String docType, Long docId) {
        return reminderMapper.selectList(new LambdaQueryWrapper<OaDocReminder>()
                .eq(docType != null && !docType.isEmpty(), OaDocReminder::getDocType, docType)
                .eq(docId != null, OaDocReminder::getDocId, docId)
                .orderByDesc(OaDocReminder::getReminderTime));
    }

    private void startDocumentFlow(String docType, Long docId, LoginUser user) {
        Long flowId = getDocumentFlowId(docType, docId);
        if (flowId == null) {
            flowId = findSingleActiveFlow(docType);
            if (flowId == null) {
                return;
            }
            updateDocumentFlowState(docType, docId, flowId, null, 0, user.getUsername());
        }
        OaDocFlow flow = flowMapper.selectById(flowId);
        if (flow == null || !"0".equals(flow.getStatus()) || !docType.equals(flow.getFlowType())) {
            throw new IllegalStateException("公文绑定的流程不存在或已停用");
        }
        List<OaDocFlowStep> steps = flowStepMapper.selectList(new LambdaQueryWrapper<OaDocFlowStep>()
                .eq(OaDocFlowStep::getFlowId, flowId)
                .orderByAsc(OaDocFlowStep::getStepOrder)
                .orderByAsc(OaDocFlowStep::getStepId));
        if (steps.isEmpty()) {
            throw new IllegalStateException("流程未配置处理步骤");
        }
        OaDocFlowStep firstStep = steps.get(0);
        assignStepToRole(docType, docId, flowId, firstStep, user.getUsername());
        updateDocumentFlowState(docType, docId, flowId, firstStep.getStepId(), firstStep.getStepOrder(), user.getUsername());
    }

    private void completeDocumentStep(String docType, Long docId, String currentStatus, LoginUser user,
                                      DocActionRequest request, boolean signing) {
        Long flowId = getDocumentFlowId(docType, docId);
        Long stepId = getDocumentCurrentStepId(docType, docId);
        Integer stepOrder = getDocumentCurrentStepOrder(docType, docId);
        if (flowId == null || stepId == null || stepOrder == null) {
            throw new IllegalStateException("公文未进入有效流程");
        }
        List<OaDocApproval> assignments = approvalMapper.selectList(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getDocType, docType)
                .eq(OaDocApproval::getDocId, docId)
                .eq(OaDocApproval::getFlowId, flowId)
                .eq(OaDocApproval::getStepId, stepId)
                .eq(OaDocApproval::getUserId, user.getUserId())
                .eq(OaDocApproval::getApproveResult, "0")
                .orderByDesc(OaDocApproval::getApprovalId));
        OaDocApproval assignment = assignments.isEmpty() ? null : assignments.get(0);
        if (assignment == null) {
            throw new AccessDeniedException("当前流程步骤未分配给该用户");
        }
        // 条件更新：同一步骤的其他处理人可能刚刚已经办完，必须确认记录仍是待办状态
        if (approvalMapper.approveIfPending(assignment.getApprovalId(), signing ? "2" : "1",
                request == null ? null : request.getOpinion(), user.getUsername()) != 1) {
            throw new IllegalStateException("该步骤已被其他处理人办理，请刷新后重试");
        }

        List<OaDocApproval> peers = approvalMapper.selectList(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getDocType, docType)
                .eq(OaDocApproval::getDocId, docId)
                .eq(OaDocApproval::getFlowId, flowId)
                .eq(OaDocApproval::getStepId, stepId)
                .eq(OaDocApproval::getApproveResult, "0")
                .ne(OaDocApproval::getApprovalId, assignment.getApprovalId()));
        for (OaDocApproval peer : peers) {
            // 并发下 peer 可能已经被本人办理，条件更新失败直接忽略
            approvalMapper.approveIfPending(peer.getApprovalId(), "9", "该步骤已由其他处理人办理", user.getUsername());
        }

        OaDocFlowStep nextStep = findNextStep(flowId, stepOrder);
        if (nextStep != null) {
            advanceFlowState(docType, docId, stepId, flowId, nextStep.getStepId(), nextStep.getStepOrder(), user.getUsername());
            assignStepToRole(docType, docId, flowId, nextStep, user.getUsername());
            return;
        }

        advanceFlowState(docType, docId, stepId, flowId, null, stepOrder, user.getUsername());
        if (DOC_OUTGOING.equals(docType)) {
            int rows = outgoingDocMapper.signIfCurrent(docId, currentStatus, "2", user.getUserId(), user.getUsername());
            if (rows != 1) {
                throw new IllegalStateException("公文状态已变化，请刷新后重试");
            }
        } else {
            setStatus(docType, docId, currentStatus, "2");
        }
    }

    private void assignStepToRole(String docType, Long docId, Long flowId, OaDocFlowStep step, String operator) {
        if (step.getRoleId() == null) {
            throw new IllegalStateException("流程步骤未配置角色");
        }
        List<com.oa.system.domain.SysUser> users = userMapper.selectUsersByRoleId(step.getRoleId());
        if (users == null || users.isEmpty()) {
            throw new IllegalStateException("流程步骤角色没有可用用户");
        }
        for (com.oa.system.domain.SysUser target : users) {
            OaDocApproval approval = new OaDocApproval();
            approval.setDocId(docId);
            approval.setDocType(docType);
            approval.setFlowId(flowId);
            approval.setStepId(step.getStepId());
            approval.setUserId(target.getUserId());
            approval.setUserName(target.getUsername());
            approval.setApproveResult("0");
            approval.setApproveRemark("待" + (StringUtils.isBlank(step.getStepName()) ? "处理" : step.getStepName()));
            approval.setApproveTime(new Date());
            approvalMapper.insert(approval);
        }
    }

    private OaDocFlowStep findNextStep(Long flowId, Integer currentOrder) {
        List<OaDocFlowStep> steps = flowStepMapper.selectList(new LambdaQueryWrapper<OaDocFlowStep>()
                .eq(OaDocFlowStep::getFlowId, flowId)
                .gt(OaDocFlowStep::getStepOrder, currentOrder)
                .orderByAsc(OaDocFlowStep::getStepOrder)
                .orderByAsc(OaDocFlowStep::getStepId));
        return steps.isEmpty() ? null : steps.get(0);
    }

    private Long findSingleActiveFlow(String docType) {
        List<OaDocFlow> flows = flowMapper.selectList(new LambdaQueryWrapper<OaDocFlow>()
                .eq(OaDocFlow::getFlowType, docType)
                .eq(OaDocFlow::getStatus, "0")
                .orderByAsc(OaDocFlow::getFlowId));
        if (flows == null || flows.isEmpty()) {
            // 未配置流程时退化为自由签批，由 requireActionPermission 控制谁能办理
            return null;
        }
        if (flows.size() > 1) {
            // 之前这里静默返回 null，会让公文在“明明配了流程”的情况下仍然走自由签批，审批口径完全不可预期
            throw new IllegalStateException("该文书方向配置了多个启用中的流程，请先停用多余流程或在公文上指定流程");
        }
        return flows.get(0).getFlowId();
    }

    private boolean hasFlowBinding(String docType, Long docId) {
        return getDocumentFlowId(docType, docId) != null;
    }

    private Long getDocumentFlowId(String docType, Long docId) {
        if (DOC_INCOMING.equals(docType)) {
            OaIncomingDoc doc = incomingDocMapper.selectById(docId);
            return doc == null ? null : doc.getFlowId();
        }
        OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
        return doc == null ? null : doc.getFlowId();
    }

    private Long getDocumentCurrentStepId(String docType, Long docId) {
        if (DOC_INCOMING.equals(docType)) {
            OaIncomingDoc doc = incomingDocMapper.selectById(docId);
            return doc == null ? null : doc.getCurrentStepId();
        }
        OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
        return doc == null ? null : doc.getCurrentStepId();
    }

    private Integer getDocumentCurrentStepOrder(String docType, Long docId) {
        if (DOC_INCOMING.equals(docType)) {
            OaIncomingDoc doc = incomingDocMapper.selectById(docId);
            return doc == null ? null : doc.getCurrentStepOrder();
        }
        OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
        return doc == null ? null : doc.getCurrentStepOrder();
    }

    private void updateDocumentFlowState(String docType, Long docId, Long flowId, Long stepId,
                                         Integer stepOrder, String username) {
        int rows;
        if (DOC_INCOMING.equals(docType)) {
            rows = incomingDocMapper.updateFlowState(docId, flowId, stepId, stepOrder, username);
        } else {
            rows = outgoingDocMapper.updateFlowState(docId, flowId, stepId, stepOrder, username);
        }
        if (rows != 1) {
            throw new IllegalStateException("公文流程状态更新失败");
        }
    }

    private void clearFlowState(String docType, Long docId) {
        Long flowId = getDocumentFlowId(docType, docId);
        updateDocumentFlowState(docType, docId, flowId, null, 0, SecurityUtils.getUsername());
    }

    private void cancelPendingAssignments(String docType, Long docId, String remark) {
        List<OaDocApproval> pending = approvalMapper.selectList(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getDocType, docType)
                .eq(OaDocApproval::getDocId, docId)
                .eq(OaDocApproval::getApproveResult, "0"));
        for (OaDocApproval approval : pending) {
            approval.setApproveResult("9");
            approval.setApproveRemark(remark);
            approval.setApproveTime(new Date());
            approvalMapper.updateById(approval);
        }
    }

    private void requireActionPermission(String docType, Long docId, String action,
                                         DocActionRequest request, LoginUser user) {
        if (SecurityUtils.isAdmin(user.getUserId())) {
            return;
        }
        if ("submit".equals(action) && isCreator(docType, docId, user.getUsername())) {
            return;
        }
        if ("receive".equals(action)
                && (isCreator(docType, docId, user.getUsername()) || isAssignedHandler(docType, docId, user.getUserId()))) {
            return;
        }
        if ("sign".equals(action)
                && (isAssignedSigner(docType, docId, user.getUserId())
                || isPendingApprover(docType, docId, user.getUserId()))) {
            return;
        }
        if ("approve".equals(action)
                && (isAssignedHandler(docType, docId, user.getUserId())
                || isPendingApprover(docType, docId, user.getUserId()))) {
            return;
        }
        if (Arrays.asList("complete", "countersign", "add_sign", "cancel_sign", "return", "transfer",
                "delegate", "agent", "circulate", "urge", "archive").contains(action)
                && isDocumentOwnerOrActiveDelegate(docType, docId, user)) {
            return;
        }
        if (Arrays.asList("countersign", "add_sign", "cancel_sign", "return").contains(action)
                && isPendingApprover(docType, docId, user.getUserId())) {
            return;
        }
        throw new AccessDeniedException("当前用户无权处理该公文");
    }

    private boolean isCreator(String docType, Long docId, String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        if (DOC_INCOMING.equals(docType)) {
            OaIncomingDoc doc = incomingDocMapper.selectById(docId);
            return doc != null && username.equals(doc.getCreateBy());
        }
        OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
        return doc != null && username.equals(doc.getCreateBy());
    }

    private boolean isAssignedHandler(String docType, Long docId, Long userId) {
        if (!DOC_INCOMING.equals(docType) || userId == null) {
            return false;
        }
        OaIncomingDoc doc = incomingDocMapper.selectById(docId);
        return doc != null && Objects.equals(doc.getHandlerId(), userId);
    }

    private boolean isAssignedSigner(String docType, Long docId, Long userId) {
        if (!DOC_OUTGOING.equals(docType) || userId == null) {
            return false;
        }
        OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
        // 只认公文上登记的签发人：允许未指定签发人时由起草人自签，等于起草、审核、签发三权合一
        return doc != null && Objects.equals(doc.getSignUserId(), userId);
    }

    private void requireSignerAssigned(String docType, Long docId) {
        if (!DOC_OUTGOING.equals(docType)) {
            return;
        }
        OaOutgoingDoc doc = outgoingDocMapper.selectById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("发文不存在");
        }
        if (doc.getSignUserId() == null) {
            throw new IllegalStateException("请先指定发文签发人，起草人不能自签");
        }
    }

    private boolean isPendingApprover(String docType, Long docId, Long userId) {
        if (userId == null) {
            return false;
        }
        // 只有 '0' 才是待办；'5'(会签)/'7'(加签) 是已经写下的办理意见，不能当成待办反复使用
        // stepId 也不再采信请求体：真正的步骤校验在 completeDocumentStep 里按公文当前步骤做
        return approvalMapper.selectCount(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getDocType, docType)
                .eq(OaDocApproval::getDocId, docId)
                .eq(OaDocApproval::getUserId, userId)
                .eq(OaDocApproval::getApproveResult, "0")) > 0;
    }

    private boolean isDocumentOwnerOrActiveDelegate(String docType, Long docId, LoginUser user) {
        if (isCreator(docType, docId, user.getUsername())
                || isAssignedHandler(docType, docId, user.getUserId())
                || isAssignedSigner(docType, docId, user.getUserId())) {
            return true;
        }
        return delegateMapper.selectCount(new LambdaQueryWrapper<OaDocDelegate>()
                .eq(OaDocDelegate::getDocType, docType)
                .eq(OaDocDelegate::getDocId, docId)
                .eq(OaDocDelegate::getToUserId, user.getUserId())
                .eq(OaDocDelegate::getStatus, "0")) > 0;
    }

    private void requireTargetUsers(DocActionRequest request, String message) {
        if (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateActionState(String docType, String status, String action) {
        Set<String> allowed = new HashSet<>();
        if (DOC_INCOMING.equals(docType)) {
            if ("0".equals(status)) allowed.addAll(Arrays.asList("receive"));
            if ("1".equals(status)) allowed.addAll(Arrays.asList("approve", "countersign", "add_sign", "cancel_sign", "return", "withdraw", "transfer", "delegate", "agent", "circulate", "urge", "complete"));
            if ("2".equals(status)) allowed.addAll(Arrays.asList("archive", "circulate", "urge"));
        } else if (DOC_OUTGOING.equals(docType)) {
            if ("0".equals(status)) allowed.addAll(Arrays.asList("submit"));
            if ("1".equals(status)) allowed.addAll(Arrays.asList("approve", "countersign", "add_sign", "cancel_sign", "return", "withdraw", "transfer", "delegate", "agent", "circulate", "urge", "sign"));
            if ("2".equals(status)) allowed.addAll(Arrays.asList("archive", "circulate", "urge"));
        }
        if (!allowed.contains(action)) {
            throw new IllegalStateException("当前状态不允许执行该操作: " + action);
        }
    }

    private void addApprovalForUser(String docType, Long docId, String result, DocActionRequest request,
                                    LoginUser operator, Long targetUserId, String targetUserName) {
        insertApproval(docType, docId, result, request, targetUserId, resolveUserName(targetUserId, targetUserName));
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

    private void setStatus(String docType, Long docId, String expectedStatus, String status) {
        String username = SecurityUtils.getUsername();
        int rows;
        if (DOC_INCOMING.equals(docType)) {
            rows = incomingDocMapper.updateStatusIfCurrent(docId, expectedStatus, status, username);
        } else {
            rows = outgoingDocMapper.updateStatusIfCurrent(docId, expectedStatus, status, username);
        }
        if (rows != 1) {
            throw new IllegalStateException("公文状态已变化，请刷新后重试");
        }
    }

    private void addApproval(String docType, Long docId, String result, DocActionRequest request, LoginUser user) {
        insertApproval(docType, docId, result, request, user.getUserId(), user.getUsername());
    }

    /**
     * 写入一条签批记录。
     *
     * <p>flowId/stepId 一律取公文当前的流程状态，不接受请求体里的值：
     * 客户端可以随意伪造这两个字段，把签批记录挂到别的步骤上污染办理轨迹。</p>
     */
    private void insertApproval(String docType, Long docId, String result, DocActionRequest request,
                                Long userId, String userName) {
        OaDocApproval approval = new OaDocApproval();
        approval.setDocId(docId);
        approval.setDocType(docType);
        approval.setFlowId(getDocumentFlowId(docType, docId));
        approval.setStepId(getDocumentCurrentStepId(docType, docId));
        approval.setUserId(userId);
        approval.setUserName(userName);
        approval.setApproveResult(result);
        approval.setApproveRemark(request == null ? null : request.getOpinion());
        approval.setApproveTime(new Date());
        approvalMapper.insert(approval);
    }

    /** 目标用户名一律按 ID 回查数据库，避免客户端把别人的名字写进办理轨迹 */
    private String resolveUserName(Long userId, String fallback) {
        if (userId == null) {
            return fallback;
        }
        com.oa.system.domain.SysUser target = userMapper.selectById(userId);
        if (target == null) {
            throw new IllegalArgumentException("目标用户不存在: " + userId);
        }
        return target.getUsername();
    }

    /** 带乐观锁的流程推进：公文已经不在 expectedStepId 说明有人抢先办理，直接失败回滚 */
    private void advanceFlowState(String docType, Long docId, Long expectedStepId, Long flowId,
                                  Long stepId, Integer stepOrder, String username) {
        int rows;
        if (DOC_INCOMING.equals(docType)) {
            rows = incomingDocMapper.advanceFlowStateIfCurrent(docId, expectedStepId, flowId, stepId, stepOrder, username);
        } else {
            rows = outgoingDocMapper.advanceFlowStateIfCurrent(docId, expectedStepId, flowId, stepId, stepOrder, username);
        }
        if (rows != 1) {
            throw new IllegalStateException("公文流程已被其他处理人推进，请刷新后重试");
        }
    }

    /**
     * 单份公文的读取权限。
     *
     * <p>公文中心对普通用户也是开放的，不能只靠 oa:doc:list 就把任意公文的
     * 审批意见、传阅名单、委托关系全部拿到。</p>
     */
    private void requireDocumentAccess(String docType, Long docId) {
        LoginUser user = requireUser();
        if (SecurityUtils.isAdmin(user.getUserId())) {
            return;
        }
        if (hasPermission(user, DOC_INCOMING.equals(docType) ? "oa:incomingDoc:list" : "oa:outgoingDoc:list")) {
            return;
        }
        if (isDocumentOwnerOrActiveDelegate(docType, docId, user) || isParticipant(docType, docId, user.getUserId())) {
            return;
        }
        throw new AccessDeniedException("无权查看该公文的办理过程");
    }

    /** 是否在该公文的办理过程里出现过：签批、传阅、催办、委托任一环节都算参与人 */
    private boolean isParticipant(String docType, Long docId, Long userId) {
        if (userId == null) {
            return false;
        }
        if (approvalMapper.selectCount(new LambdaQueryWrapper<OaDocApproval>()
                .eq(OaDocApproval::getDocType, docType)
                .eq(OaDocApproval::getDocId, docId)
                .eq(OaDocApproval::getUserId, userId)) > 0) {
            return true;
        }
        if (circulationMapper.selectCount(new LambdaQueryWrapper<OaDocCirculation>()
                .eq(OaDocCirculation::getDocType, docType)
                .eq(OaDocCirculation::getDocId, docId)
                .eq(OaDocCirculation::getUserId, userId)) > 0) {
            return true;
        }
        if (reminderMapper.selectCount(new LambdaQueryWrapper<OaDocReminder>()
                .eq(OaDocReminder::getDocType, docType)
                .eq(OaDocReminder::getDocId, docId)
                .and(query -> query.eq(OaDocReminder::getFromUserId, userId)
                        .or().eq(OaDocReminder::getToUserId, userId))) > 0) {
            return true;
        }
        return delegateMapper.selectCount(new LambdaQueryWrapper<OaDocDelegate>()
                .eq(OaDocDelegate::getDocType, docType)
                .eq(OaDocDelegate::getDocId, docId)
                .and(query -> query.eq(OaDocDelegate::getFromUserId, userId)
                        .or().eq(OaDocDelegate::getToUserId, userId))) > 0;
    }

    private boolean hasPermission(LoginUser user, String permission) {
        return user.getPermissions() != null && user.getPermissions().contains(permission);
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

    private void enrichCirculations(List<OaDocCirculation> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> incomingIds = items.stream()
                .filter(item -> DOC_INCOMING.equals(item.getDocType()))
                .map(OaDocCirculation::getDocId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> outgoingIds = items.stream()
                .filter(item -> DOC_OUTGOING.equals(item.getDocType()))
                .map(OaDocCirculation::getDocId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, OaIncomingDoc> incomingDocs = incomingIds.isEmpty()
                ? Collections.emptyMap()
                : incomingDocMapper.selectBatchIds(incomingIds).stream()
                .collect(Collectors.toMap(OaIncomingDoc::getDocId, doc -> doc));
        Map<Long, OaOutgoingDoc> outgoingDocs = outgoingIds.isEmpty()
                ? Collections.emptyMap()
                : outgoingDocMapper.selectBatchIds(outgoingIds).stream()
                .collect(Collectors.toMap(OaOutgoingDoc::getDocId, doc -> doc));

        for (OaDocCirculation item : items) {
            if (DOC_INCOMING.equals(item.getDocType())) {
                OaIncomingDoc doc = incomingDocs.get(item.getDocId());
                if (doc != null) {
                    item.setDocTitle(doc.getDocTitle());
                    item.setDocNumber(doc.getDocNumber());
                    item.setStatus(doc.getStatus());
                }
            } else {
                OaOutgoingDoc doc = outgoingDocs.get(item.getDocId());
                if (doc != null) {
                    item.setDocTitle(doc.getDocTitle());
                    item.setDocNumber(doc.getDocNumber());
                    item.setStatus(doc.getStatus());
                }
            }
        }
    }
}