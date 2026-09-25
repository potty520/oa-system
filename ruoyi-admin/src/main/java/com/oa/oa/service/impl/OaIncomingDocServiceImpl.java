package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.DocActionRequest;
import com.oa.oa.domain.OaIncomingDoc;
import com.oa.oa.mapper.OaIncomingDocMapper;
import com.oa.oa.service.IOaDocProcessService;
import com.oa.oa.service.IOaIncomingDocService;
import com.oa.system.domain.SysUser;
import com.oa.system.mapper.SysUserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OaIncomingDocServiceImpl extends ServiceImpl<OaIncomingDocMapper, OaIncomingDoc> implements IOaIncomingDocService {

    private static final String DOC_INCOMING = "1";

    /** 待签收：只有这个状态允许直接编辑或删除 */
    private static final String STATUS_PENDING_RECEIVE = "0";

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private IOaDocProcessService docProcessService;

    @Override
    public List<OaIncomingDoc> selectOaIncomingDocList(OaIncomingDoc doc) {
        LambdaQueryWrapper<OaIncomingDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(doc.getDocNumber()), OaIncomingDoc::getDocNumber, doc.getDocNumber())
                .like(StringUtils.isNotBlank(doc.getDocTitle()), OaIncomingDoc::getDocTitle, doc.getDocTitle())
                .eq(doc.getDocTypeId() != null, OaIncomingDoc::getDocTypeId, doc.getDocTypeId())
                .eq(StringUtils.isNotBlank(doc.getDocLevel()), OaIncomingDoc::getDocLevel, doc.getDocLevel())
                .eq(StringUtils.isNotBlank(doc.getStatus()), OaIncomingDoc::getStatus, doc.getStatus())
                .orderByDesc(OaIncomingDoc::getReceiveDate);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaIncomingDoc selectOaIncomingDocById(Long docId) {
        return baseMapper.selectById(docId);
    }

    @Override
    public boolean existsByDocNumber(String docNumber, Long excludeId) {
        if (StringUtils.isBlank(docNumber)) {
            return false;
        }
        LambdaQueryWrapper<OaIncomingDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaIncomingDoc::getDocNumber, docNumber)
                .ne(excludeId != null, OaIncomingDoc::getDocId, excludeId);
        return baseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public int insertOaIncomingDoc(OaIncomingDoc doc) {
        doc.setStatus(STATUS_PENDING_RECEIVE);
        // 流程运行时状态由服务端维护，不接受客户端预置（flowId 是用户选择的流程，保留）
        doc.setCurrentStepId(null);
        doc.setCurrentStepOrder(null);
        doc.setHandlerName(resolveUserName(doc.getHandlerId(), doc.getHandlerName()));
        return baseMapper.insert(doc);
    }

    @Override
    public int updateOaIncomingDoc(OaIncomingDoc doc) {
        OaIncomingDoc existing = requireEditable(doc == null ? null : doc.getDocId());
        // 只接受业务字段：状态、流程绑定、创建信息一律由服务端维护，
        // 否则客户端可以直接把 status 改成“已办结”绕过整个办理流程
        OaIncomingDoc update = new OaIncomingDoc();
        update.setDocId(existing.getDocId());
        update.setDocNumber(doc.getDocNumber());
        update.setDocTitle(doc.getDocTitle());
        update.setDocTypeId(doc.getDocTypeId());
        update.setTemplateId(doc.getTemplateId());
        update.setFlowId(doc.getFlowId());
        update.setSourceUnit(doc.getSourceUnit());
        update.setReceiveDate(doc.getReceiveDate());
        update.setDocLevel(doc.getDocLevel());
        update.setDocSecret(doc.getDocSecret());
        update.setSummary(doc.getSummary());
        update.setAttachmentUrl(doc.getAttachmentUrl());
        update.setHandlerId(doc.getHandlerId());
        update.setHandlerName(resolveUserName(doc.getHandlerId(), doc.getHandlerName()));
        update.setRemark(doc.getRemark());
        return baseMapper.updateById(update);
    }

    @Override
    public int deleteOaIncomingDocByIds(Long[] docIds) {
        int count = 0;
        for (Long docId : docIds) {
            requireEditable(docId);
            count += baseMapper.deleteById(docId);
        }
        return count;
    }

    @Override
    public int signOaIncomingDoc(Long docId) {
        return execute(docId, "receive");
    }

    @Override
    public int completeOaIncomingDoc(Long docId) {
        return execute(docId, "complete");
    }

    /**
     * 签收/办结与公文中心走同一条路径。
     *
     * <p>之前这里只改状态、不派发流程步骤，同一个转移在两个入口下行为不一致。</p>
     */
    private int execute(Long docId, String action) {
        requireExisting(docId);
        DocActionRequest request = new DocActionRequest();
        request.setAction(action);
        docProcessService.execute(DOC_INCOMING, docId, request);
        return 1;
    }

    private OaIncomingDoc requireEditable(Long docId) {
        OaIncomingDoc existing = requireExisting(docId);
        if (!STATUS_PENDING_RECEIVE.equals(existing.getStatus())) {
            throw new IllegalStateException("收文已进入办理流程，不能再直接修改或删除");
        }
        return existing;
    }

    private OaIncomingDoc requireExisting(Long docId) {
        OaIncomingDoc existing = docId == null ? null : baseMapper.selectById(docId);
        if (existing == null) {
            throw new IllegalArgumentException("收文不存在");
        }
        return existing;
    }

    /** 办理人姓名按 ID 回查，避免客户端把别人的名字写进公文 */
    private String resolveUserName(Long userId, String fallback) {
        if (userId == null) {
            return fallback;
        }
        SysUser target = userMapper.selectById(userId);
        if (target == null) {
            throw new IllegalArgumentException("办理人不存在: " + userId);
        }
        return StringUtils.defaultIfBlank(target.getNickname(), target.getUsername());
    }
}
