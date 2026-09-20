package com.oa.oa.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.oa.domain.OaAnnouncement;

import java.util.List;

public interface IOaAnnouncementService extends IService<OaAnnouncement> {
    List<OaAnnouncement> selectOaAnnouncementList(OaAnnouncement announcement);
    List<OaAnnouncement> selectPublishedAnnouncementList(OaAnnouncement announcement);
    OaAnnouncement selectOaAnnouncementById(Long annId);
    int insertOaAnnouncement(OaAnnouncement announcement);
    int updateOaAnnouncement(OaAnnouncement announcement);
    int deleteOaAnnouncementByIds(Long[] annIds);
    int incrementViewCount(Long annId);
}