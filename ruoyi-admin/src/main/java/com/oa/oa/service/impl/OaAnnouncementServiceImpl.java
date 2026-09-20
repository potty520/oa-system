package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaAnnouncement;
import com.oa.oa.mapper.OaAnnouncementMapper;
import com.oa.oa.service.IOaAnnouncementService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OaAnnouncementServiceImpl extends ServiceImpl<OaAnnouncementMapper, OaAnnouncement> implements IOaAnnouncementService {

    @Override
    public List<OaAnnouncement> selectOaAnnouncementList(OaAnnouncement announcement) {
        LambdaQueryWrapper<OaAnnouncement> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(announcement.getAnnTitle()), OaAnnouncement::getAnnTitle, announcement.getAnnTitle())
                .eq(StringUtils.isNotBlank(announcement.getAnnType()), OaAnnouncement::getAnnType, announcement.getAnnType())
                .eq(StringUtils.isNotBlank(announcement.getStatus()), OaAnnouncement::getStatus, announcement.getStatus())
                .orderByDesc(OaAnnouncement::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<OaAnnouncement> selectPublishedAnnouncementList(OaAnnouncement announcement) {
        LambdaQueryWrapper<OaAnnouncement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaAnnouncement::getStatus, "1")
                .le(OaAnnouncement::getPublishStartTime, new Date())
                .ge(OaAnnouncement::getPublishEndTime, new Date())
                .like(StringUtils.isNotBlank(announcement.getAnnTitle()), OaAnnouncement::getAnnTitle, announcement.getAnnTitle())
                .eq(StringUtils.isNotBlank(announcement.getAnnType()), OaAnnouncement::getAnnType, announcement.getAnnType())
                .orderByDesc(OaAnnouncement::getIsTop)
                .orderByDesc(OaAnnouncement::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaAnnouncement selectOaAnnouncementById(Long annId) {
        return baseMapper.selectById(annId);
    }

    @Override
    public int insertOaAnnouncement(OaAnnouncement announcement) {
        return baseMapper.insert(announcement);
    }

    @Override
    public int updateOaAnnouncement(OaAnnouncement announcement) {
        return baseMapper.updateById(announcement);
    }

    @Override
    public int deleteOaAnnouncementByIds(Long[] annIds) {
        int count = 0;
        for (Long annId : annIds) {
            count += baseMapper.deleteById(annId);
        }
        return count;
    }

    @Override
    public int incrementViewCount(Long annId) {
        OaAnnouncement announcement = baseMapper.selectById(annId);
        if (announcement != null) {
            announcement.setViewCount(announcement.getViewCount() == null ? 1 : announcement.getViewCount() + 1);
            return baseMapper.updateById(announcement);
        }
        return 0;
    }
}