package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaOutgoingDoc;
import com.oa.oa.mapper.OaOutgoingDocMapper;
import com.oa.oa.service.IOaOutgoingDocService;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OaOutgoingDocServiceImpl extends ServiceImpl<OaOutgoingDocMapper, OaOutgoingDoc> implements IOaOutgoingDocService {

    @Override
    public List<OaOutgoingDoc> selectOaOutgoingDocList(OaOutgoingDoc doc) {
        LambdaQueryWrapper<OaOutgoingDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(doc.getDocNumber()), OaOutgoingDoc::getDocNumber, doc.getDocNumber())
                .like(StringUtils.isNotBlank(doc.getDocTitle()), OaOutgoingDoc::getDocTitle, doc.getDocTitle())
                .eq(doc.getDocTypeId() != null, OaOutgoingDoc::getDocTypeId, doc.getDocTypeId())
                .eq(StringUtils.isNotBlank(doc.getDocLevel()), OaOutgoingDoc::getDocLevel, doc.getDocLevel())
                .eq(StringUtils.isNotBlank(doc.getStatus()), OaOutgoingDoc::getStatus, doc.getStatus())
                .orderByDesc(OaOutgoingDoc::getDraftDate);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaOutgoingDoc selectOaOutgoingDocById(Long docId) {
        return baseMapper.selectById(docId);
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
        doc.setStatus("0");
        return baseMapper.insert(doc);
    }

    @Override
    public int updateOaOutgoingDoc(OaOutgoingDoc doc) {
        return baseMapper.updateById(doc);
    }

    @Override
    public int deleteOaOutgoingDocByIds(Long[] docIds) {
        int count = 0;
        for (Long docId : docIds) {
            count += baseMapper.deleteById(docId);
        }
        return count;
    }

    @Override
    public int submitOaOutgoingDoc(Long docId) {
        OaOutgoingDoc doc = baseMapper.selectById(docId);
        if (doc != null && "0".equals(doc.getStatus())) {
            doc.setStatus("1");
            return baseMapper.updateById(doc);
        }
        return 0;
    }
    @Override
    public int signOaOutgoingDoc(Long docId) {
        OaOutgoingDoc doc = baseMapper.selectById(docId);
        if (doc != null && "1".equals(doc.getStatus())) {
            doc.setStatus("2");
            doc.setSignUserId(SecurityUtils.getUserId());
            doc.setSignUserName(SecurityUtils.getUsername());
            return baseMapper.updateById(doc);
        }
        return 0;
    }
}
