package com.oa.oa.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.oa.domain.OaIncomingDoc;

import java.util.List;

public interface IOaIncomingDocService extends IService<OaIncomingDoc> {
    List<OaIncomingDoc> selectOaIncomingDocList(OaIncomingDoc doc);
    OaIncomingDoc selectOaIncomingDocById(Long docId);
    int insertOaIncomingDoc(OaIncomingDoc doc);
    int updateOaIncomingDoc(OaIncomingDoc doc);
    int deleteOaIncomingDocByIds(Long[] docIds);
    int signOaIncomingDoc(Long docId);
    int completeOaIncomingDoc(Long docId);
}