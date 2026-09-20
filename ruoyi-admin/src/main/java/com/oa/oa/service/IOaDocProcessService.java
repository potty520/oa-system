package com.oa.oa.service;

import com.oa.oa.domain.DocActionRequest;
import com.oa.oa.domain.OaDocApproval;
import com.oa.oa.domain.OaDocArchive;
import com.oa.oa.domain.OaDocCirculation;
import com.oa.oa.domain.OaDocReminder;

import java.util.List;
import java.util.Map;

public interface IOaDocProcessService {
    Map<String, Object> getTimeline(String docType, Long docId);
    void execute(String docType, Long docId, DocActionRequest request);
    List<OaDocCirculation> getMyCirculation();
    int markCirculationRead(Long circulationId);
    List<OaDocArchive> getArchiveList(OaDocArchive archive);
    List<OaDocApproval> getMyDone();
    List<OaDocReminder> getReminderList(String docType, Long docId);
}