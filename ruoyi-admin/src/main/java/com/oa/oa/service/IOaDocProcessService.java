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

    /**
     * 当前登录用户与指定公文是否有办理关系：起草人、办理人、签发人，
     * 或签批/传阅/催办/委托环节的参与人。
     *
     * <p>收发文列表的数据范围、单份公文的读取与变更校验共用这一个口径，
     * 避免列表看得见、详情却 403（或反过来）的两套判断。</p>
     */
    boolean isDocumentEntitled(String docType, Long docId);

    /**
     * 当前登录用户在签批/传阅/催办/委托环节参与过的公文 ID 集合。
     *
     * <p>列表的数据范围 = 首页统计口径（办理人/签发人、起草人）并上这份 ID 集合，
     * 保证流程派发给本人的公文不会因为不是本人起草而不可见。</p>
     */
    List<Long> selectParticipantDocIds(String docType);
}