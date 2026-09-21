package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaIncomingDoc;
import com.oa.oa.mapper.OaIncomingDocMapper;
import com.oa.oa.service.IOaIncomingDocService;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class OaIncomingDocServiceImpl extends ServiceImpl<OaIncomingDocMapper, OaIncomingDoc> implements IOaIncomingDocService {

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
        doc.setStatus("0");
        return baseMapper.insert(doc);
    }

    @Override
    public int updateOaIncomingDoc(OaIncomingDoc doc) {
        return baseMapper.updateById(doc);
    }

    @Override
    public int deleteOaIncomingDocByIds(Long[] docIds) {
        int count = 0;
        for (Long docId : docIds) {
            count += baseMapper.deleteById(docId);
        }
        return count;
    }

    @Override
    public int signOaIncomingDoc(Long docId) {
        OaIncomingDoc doc = baseMapper.selectById(docId);
        requireHandler(doc);
        return baseMapper.updateStatusIfCurrent(docId, "0", "1", SecurityUtils.getUsername());
    }

    @Override
    public int completeOaIncomingDoc(Long docId) {
        OaIncomingDoc doc = baseMapper.selectById(docId);
        requireHandler(doc);
        return baseMapper.updateStatusIfCurrent(docId, "1", "2", SecurityUtils.getUsername());
    }

    private void requireHandler(OaIncomingDoc doc) {
        if (doc == null || SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        if (!Objects.equals(doc.getHandlerId(), userId) && !Objects.equals(doc.getCreateBy(), username)) {
            throw new AccessDeniedException("当前用户无权处理该收文");
        }
    }
}
