package com.oa.oa.service.impl;

import com.oa.common.exception.BusinessAccessDeniedException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.DocActionRequest;
import com.oa.oa.domain.OaOutgoingDoc;
import com.oa.oa.mapper.OaOutgoingDocMapper;
import com.oa.oa.service.IOaDocProcessService;
import com.oa.oa.service.IOaOutgoingDocService;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysUser;
import com.oa.system.mapper.SysUserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OaOutgoingDocServiceImpl extends ServiceImpl<OaOutgoingDocMapper, OaOutgoingDoc> implements IOaOutgoingDocService {

    private static final String DOC_OUTGOING = "2";

    /** 草稿：只有这个状态允许直接编辑或删除 */
    private static final String STATUS_DRAFT = "0";

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private IOaDocProcessService docProcessService;

    @Override
    public List<OaOutgoingDoc> selectOaOutgoingDocList(OaOutgoingDoc doc) {
        LambdaQueryWrapper<OaOutgoingDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(doc.getDocNumber()), OaOutgoingDoc::getDocNumber, doc.getDocNumber())
                .like(StringUtils.isNotBlank(doc.getDocTitle()), OaOutgoingDoc::getDocTitle, doc.getDocTitle())
                .eq(doc.getDocTypeId() != null, OaOutgoingDoc::getDocTypeId, doc.getDocTypeId())
                .eq(StringUtils.isNotBlank(doc.getDocLevel()), OaOutgoingDoc::getDocLevel, doc.getDocLevel())
                .eq(StringUtils.isNotBlank(doc.getStatus()), OaOutgoingDoc::getStatus, doc.getStatus())
                .orderByDesc(OaOutgoingDoc::getDraftDate);
        applyDataScope(wrapper);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaOutgoingDoc selectOaOutgoingDocById(Long docId) {
        OaOutgoingDoc existing = docId == null ? null : baseMapper.selectById(docId);
        if (existing == null) {
            return null;
        }
        requireEntitled(docId);
        return existing;
    }

    @Override
    public boolean existsByDocNumber(String docNumber, Long excludeId) {
        if (StringUtils.isBlank(docNumber)) {
            return false;
        }
        LambdaQueryWrapper<OaOutgoingDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaOutgoingDoc::getDocNumber, docNumber)
                .ne(excludeId != null, OaOutgoingDoc::getDocId, excludeId);
        return baseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public int insertOaOutgoingDoc(OaOutgoingDoc doc) {
        doc.setStatus(STATUS_DRAFT);
        // 流程运行时状态由服务端维护，不接受客户端预置（flowId 是用户选择的流程，保留）
        doc.setCurrentStepId(null);
        doc.setCurrentStepOrder(null);
        // 起草人一律取当前登录用户，不接受客户端传入，否则数据归属和“起草人不能自签”都能被绕过
        doc.setCreateBy(SecurityUtils.getUsername());
        requireSignerNotCreator(doc.getSignUserId(), doc.getCreateBy());
        doc.setSignUserName(resolveUserName(doc.getSignUserId(), doc.getSignUserName()));
        return baseMapper.insert(doc);
    }

    @Override
    public int updateOaOutgoingDoc(OaOutgoingDoc doc) {
        OaOutgoingDoc existing = requireEditable(doc == null ? null : doc.getDocId());
        requireSignerNotCreator(doc.getSignUserId(), existing.getCreateBy());
        // 只接受业务字段：状态、流程绑定、签发结果、创建信息一律由服务端维护
        OaOutgoingDoc update = new OaOutgoingDoc();
        update.setDocId(existing.getDocId());
        update.setDocNumber(doc.getDocNumber());
        update.setDocTitle(doc.getDocTitle());
        update.setDocTypeId(doc.getDocTypeId());
        update.setTemplateId(doc.getTemplateId());
        update.setFlowId(doc.getFlowId());
        update.setDraftUnit(doc.getDraftUnit());
        update.setDraftDate(doc.getDraftDate());
        update.setDocLevel(doc.getDocLevel());
        update.setDocSecret(doc.getDocSecret());
        update.setMainSend(doc.getMainSend());
        update.setCopySend(doc.getCopySend());
        update.setSummary(doc.getSummary());
        update.setAttachmentUrl(doc.getAttachmentUrl());
        update.setSignUserId(doc.getSignUserId());
        update.setSignUserName(resolveUserName(doc.getSignUserId(), doc.getSignUserName()));
        update.setRemark(doc.getRemark());
        return baseMapper.updateById(update);
    }

    @Override
    public int deleteOaOutgoingDocByIds(Long[] docIds) {
        int count = 0;
        for (Long docId : docIds) {
            requireEditable(docId);
            count += baseMapper.deleteById(docId);
        }
        return count;
    }

    @Override
    public int submitOaOutgoingDoc(Long docId) {
        return execute(docId, "submit");
    }

    @Override
    public int signOaOutgoingDoc(Long docId) {
        return execute(docId, "sign");
    }

    /**
     * 送审/签发与公文中心走同一条路径。
     *
     * <p>签发人校验、状态机与流程派发都集中在 OaDocProcessServiceImpl，
     * 避免两个入口各写一套规则（旧实现允许未指定签发人时由起草人自签）。</p>
     */
    private int execute(Long docId, String action) {
        requireExisting(docId);
        DocActionRequest request = new DocActionRequest();
        request.setAction(action);
        docProcessService.execute(DOC_OUTGOING, docId, request);
        return 1;
    }

    private OaOutgoingDoc requireEditable(Long docId) {
        OaOutgoingDoc existing = requireExisting(docId);
        requireEntitled(existing.getDocId());
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            throw new IllegalStateException("发文已进入审核流程，不能再直接修改或删除");
        }
        return existing;
    }

    /**
     * 发文列表的数据范围：与首页统计（DashboardController）口径完全一致，
     * 再并上流程派发给本人的公文，避免审批人/传阅人看不到自己待办的那一份。
     */
    private void applyDataScope(LambdaQueryWrapper<OaOutgoingDoc> wrapper) {
        Long userId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin(userId)) {
            return;
        }
        String username = SecurityUtils.getUsername();
        List<Long> participantDocIds = docProcessService.selectParticipantDocIds(DOC_OUTGOING);
        wrapper.and(query -> {
            query.eq(OaOutgoingDoc::getSignUserId, userId).or().eq(OaOutgoingDoc::getCreateBy, username);
            if (!participantDocIds.isEmpty()) {
                query.or().in(OaOutgoingDoc::getDocId, participantDocIds);
            }
        });
    }

    /** 单份发文的读取与变更权限，口径与列表数据范围、办理过程读取一致 */
    private void requireEntitled(Long docId) {
        if (!docProcessService.isDocumentEntitled(DOC_OUTGOING, docId)) {
            throw new BusinessAccessDeniedException("无权查看或操作该发文");
        }
    }

    /** 起草人不能同时被指定为签发人，否则等于起草、审核、签发三权合一 */
    private void requireSignerNotCreator(Long signUserId, String creator) {
        if (signUserId == null || StringUtils.isBlank(creator)) {
            return;
        }
        SysUser signer = userMapper.selectById(signUserId);
        if (signer != null && creator.equals(signer.getUsername())) {
            throw new BusinessAccessDeniedException("签发人不能是发文起草人本人，请另行指定签发人");
        }
    }

    private OaOutgoingDoc requireExisting(Long docId) {
        OaOutgoingDoc existing = docId == null ? null : baseMapper.selectById(docId);
        if (existing == null) {
            throw new IllegalArgumentException("发文不存在");
        }
        return existing;
    }

    /** 签发人姓名按 ID 回查，避免客户端把别人的名字写进公文 */
    private String resolveUserName(Long userId, String fallback) {
        if (userId == null) {
            return fallback;
        }
        SysUser target = userMapper.selectById(userId);
        if (target == null) {
            throw new IllegalArgumentException("签发人不存在: " + userId);
        }
        return StringUtils.defaultIfBlank(target.getNickname(), target.getUsername());
    }
}
