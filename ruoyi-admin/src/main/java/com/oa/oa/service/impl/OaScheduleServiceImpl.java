package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaSchedule;
import com.oa.oa.mapper.OaScheduleMapper;
import com.oa.oa.service.IOaScheduleService;
import com.oa.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OaScheduleServiceImpl extends ServiceImpl<OaScheduleMapper, OaSchedule> implements IOaScheduleService {

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
        schedule.setUserId(SecurityUtils.getUserId());
        return baseMapper.insert(schedule);
    }

    @Override
    public int updateOaSchedule(OaSchedule schedule) {
        return baseMapper.updateById(schedule);
    }

    @Override
    public int deleteOaScheduleByIds(Long[] scheduleIds) {
        int count = 0;
        for (Long scheduleId : scheduleIds) {
            count += baseMapper.deleteById(scheduleId);
        }
        return count;
    }

    @Override
    public int cancelOaSchedule(Long scheduleId) {
        OaSchedule schedule = baseMapper.selectById(scheduleId);
        if (schedule != null) {
            schedule.setStatus("3");
            return baseMapper.updateById(schedule);
        }
        return 0;
    }
}