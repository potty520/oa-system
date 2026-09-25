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

    /** 草稿 */
    private static final String STATUS_DRAFT = "0";

    /** 已发布 */
    private static final String STATUS_PUBLISHED = "1";

    /** 公开可见 */
    private static final String IS_PUBLIC_YES = "1";

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
        wrapper.eq(OaAnnouncement::getStatus, STATUS_PUBLISHED)
                .like(StringUtils.isNotBlank(announcement.getAnnTitle()), OaAnnouncement::getAnnTitle, announcement.getAnnTitle())
                .eq(StringUtils.isNotBlank(announcement.getAnnType()), OaAnnouncement::getAnnType, announcement.getAnnType())
                .orderByDesc(OaAnnouncement::getIsTop)
                .orderByDesc(OaAnnouncement::getCreateTime);
        applyEffectiveAndPublic(wrapper);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public long countEffectiveAnnouncements() {
        LambdaQueryWrapper<OaAnnouncement> wrapper = new LambdaQueryWrapper<>();
        applyEffectiveAndPublic(wrapper);
        Long count = baseMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    /**
     * 只保留当前生效且允许对外公开的公告。
     *
     * <p>该查询条件用于匿名可访问的 {@code /oa/announcement/published} 接口，
     * 因此必须叠加 is_public，否则内部公告会泄露给未登录访问者。</p>
     */
    private void applyEffectiveAndPublic(LambdaQueryWrapper<OaAnnouncement> wrapper) {
        Date now = new Date();
        wrapper.eq(OaAnnouncement::getIsPublic, IS_PUBLIC_YES)
                // 未填发布时间窗视为长期有效；直接用 <=/>= 会把 NULL 行永久过滤掉
                .and(query -> query.isNull(OaAnnouncement::getPublishStartTime)
                        .or().le(OaAnnouncement::getPublishStartTime, now))
                .and(query -> query.isNull(OaAnnouncement::getPublishEndTime)
                        .or().ge(OaAnnouncement::getPublishEndTime, now));
    }

    @Override
    public OaAnnouncement selectOaAnnouncementById(Long annId) {
        return baseMapper.selectById(annId);
    }

    @Override
    public int insertOaAnnouncement(OaAnnouncement announcement) {
        // 浏览量由服务端维护，状态缺省为草稿
        announcement.setViewCount(0);
        if (StringUtils.isBlank(announcement.getStatus())) {
            announcement.setStatus(STATUS_DRAFT);
        }
        requireKnownStatus(announcement.getStatus());
        return baseMapper.insert(announcement);
    }

    @Override
    public int updateOaAnnouncement(OaAnnouncement announcement) {
        OaAnnouncement existing = requireExisting(announcement == null ? null : announcement.getAnnId());
        requireKnownStatus(announcement.getStatus());
        // 只接受业务字段，浏览量与创建信息不允许被客户端改写
        OaAnnouncement update = new OaAnnouncement();
        update.setAnnId(existing.getAnnId());
        update.setAnnTitle(announcement.getAnnTitle());
        update.setAnnType(announcement.getAnnType());
        update.setAnnContent(announcement.getAnnContent());
        update.setAnnLevel(announcement.getAnnLevel());
        update.setAttachmentUrl(announcement.getAttachmentUrl());
        update.setPublishStartTime(announcement.getPublishStartTime());
        update.setPublishEndTime(announcement.getPublishEndTime());
        update.setIsTop(announcement.getIsTop());
        update.setIsPublic(announcement.getIsPublic());
        update.setStatus(announcement.getStatus());
        update.setRemark(announcement.getRemark());
        return baseMapper.updateById(update);
    }

    @Override
    public int deleteOaAnnouncementByIds(Long[] annIds) {
        int count = 0;
        for (Long annId : annIds) {
            requireExisting(annId);
            count += baseMapper.deleteById(annId);
        }
        return count;
    }

    @Override
    public int incrementViewCount(Long annId) {
        if (annId == null) {
            return 0;
        }
        // 用单条自增语句替代"读-改-写"，避免并发浏览时计数丢失
        return baseMapper.incrementViewCount(annId);
    }

    private OaAnnouncement requireExisting(Long annId) {
        OaAnnouncement existing = annId == null ? null : baseMapper.selectById(annId);
        if (existing == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        return existing;
    }

    private void requireKnownStatus(String status) {
        if (StringUtils.isNotBlank(status) && !STATUS_DRAFT.equals(status) && !STATUS_PUBLISHED.equals(status)) {
            throw new IllegalArgumentException("公告状态只能是草稿或已发布");
        }
    }
}