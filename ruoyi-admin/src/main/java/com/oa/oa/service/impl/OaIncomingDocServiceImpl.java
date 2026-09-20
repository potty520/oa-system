package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaIncomingDoc;
import com.oa.oa.mapper.OaIncomingDocMapper;
import com.oa.oa.service.IOaIncomingDocService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

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
        if (doc != null && "0".equals(doc.getStatus())) {
            doc.setStatus("1");
            return baseMapper.updateById(doc);
        }
        return 0;
    }

    @Override
    public int completeOaIncomingDoc(Long docId) {
        OaIncomingDoc doc = baseMapper.selectById(docId);
        if (doc != null && "1".equals(doc.getStatus())) {
            doc.setStatus("2");
            return baseMapper.updateById(doc);
        }
        return 0;
    }
}
