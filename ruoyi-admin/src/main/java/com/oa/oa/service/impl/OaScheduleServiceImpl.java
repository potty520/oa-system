package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaSchedule;
import com.oa.oa.mapper.OaScheduleMapper;
import com.oa.oa.service.IOaScheduleService;
import com.oa.security.utils.AccessGuards;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OaScheduleServiceImpl extends ServiceImpl<OaScheduleMapper, OaSchedule> implements IOaScheduleService {

    /** 已取消 */
    private static final String STATUS_CANCELLED = "3";

    @Override
    public List<OaSchedule> selectOaScheduleList(OaSchedule schedule) {
        LambdaQueryWrapper<OaSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(schedule.getScheduleTitle()), OaSchedule::getScheduleTitle, schedule.getScheduleTitle())
                .eq(StringUtils.isNotBlank(schedule.getScheduleType()), OaSchedule::getScheduleType, schedule.getScheduleType())
                .eq(StringUtils.isNotBlank(schedule.getStatus()), OaSchedule::getStatus, schedule.getStatus())
                .orderByAsc(OaSchedule::getStartTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<OaSchedule> selectUserScheduleList(Long userId, OaSchedule schedule) {
        LambdaQueryWrapper<OaSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaSchedule::getUserId, userId)
                .like(StringUtils.isNotBlank(schedule.getScheduleTitle()), OaSchedule::getScheduleTitle, schedule.getScheduleTitle())
                .eq(StringUtils.isNotBlank(schedule.getScheduleType()), OaSchedule::getScheduleType, schedule.getScheduleType())
                .eq(StringUtils.isNotBlank(schedule.getStatus()), OaSchedule::getStatus, schedule.getStatus())
                .orderByAsc(OaSchedule::getStartTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaSchedule selectOaScheduleById(Long scheduleId) {
        return baseMapper.selectById(scheduleId);
    }

    @Override
    public int insertOaSchedule(OaSchedule schedule) {
        // 日程归属人一律取当前登录用户，不接受客户端传入
        schedule.setUserId(SecurityUtils.getUserId());
        return baseMapper.insert(schedule);
    }

    @Override
    public int updateOaSchedule(OaSchedule schedule) {
        OaSchedule existing = requireExisting(schedule == null ? null : schedule.getScheduleId());
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "日程");
        // 只接受业务字段，归属人不允许被改写
        OaSchedule update = new OaSchedule();
        update.setScheduleId(existing.getScheduleId());
        update.setScheduleTitle(schedule.getScheduleTitle());
        update.setScheduleType(schedule.getScheduleType());
        update.setScheduleColor(schedule.getScheduleColor());
        update.setStartTime(schedule.getStartTime());
        update.setEndTime(schedule.getEndTime());
        update.setScheduleContent(schedule.getScheduleContent());
        update.setLocation(schedule.getLocation());
        update.setRemindFlag(schedule.getRemindFlag());
        update.setRemindTime(schedule.getRemindTime());
        update.setStatus(schedule.getStatus());
        update.setRemark(schedule.getRemark());
        return baseMapper.updateById(update);
    }

    @Override
    public int deleteOaScheduleByIds(Long[] scheduleIds) {
        int count = 0;
        for (Long scheduleId : scheduleIds) {
            OaSchedule existing = requireExisting(scheduleId);
            AccessGuards.requireSelfOrAdmin(existing.getUserId(), "日程");
            count += baseMapper.deleteById(scheduleId);
        }
        return count;
    }

    @Override
    public int cancelOaSchedule(Long scheduleId) {
        OaSchedule existing = requireExisting(scheduleId);
        AccessGuards.requireSelfOrAdmin(existing.getUserId(), "日程");
        OaSchedule update = new OaSchedule();
        update.setScheduleId(existing.getScheduleId());
        update.setStatus(STATUS_CANCELLED);
        return baseMapper.updateById(update);
    }

    private OaSchedule requireExisting(Long scheduleId) {
        OaSchedule existing = scheduleId == null ? null : baseMapper.selectById(scheduleId);
        if (existing == null) {
            throw new IllegalArgumentException("日程不存在");
        }
        return existing;
    }
}