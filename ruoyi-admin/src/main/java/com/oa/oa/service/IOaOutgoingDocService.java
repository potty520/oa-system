package com.oa.oa.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.oa.domain.OaOutgoingDoc;

import java.util.List;

public interface IOaOutgoingDocService extends IService<OaOutgoingDoc> {
    List<OaOutgoingDoc> selectOaOutgoingDocList(OaOutgoingDoc doc);
    OaOutgoingDoc selectOaOutgoingDocById(Long docId);
    boolean existsByDocNumber(String docNumber, Long excludeId);
    int insertOaOutgoingDoc(OaOutgoingDoc doc);
    int updateOaOutgoingDoc(OaOutgoingDoc doc);
    int deleteOaOutgoingDocByIds(Long[] docIds);
    int submitOaOutgoingDoc(Long docId);
    int signOaOutgoingDoc(Long docId);
}
