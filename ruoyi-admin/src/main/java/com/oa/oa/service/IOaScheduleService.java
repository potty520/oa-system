package com.oa.oa.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.oa.domain.OaSchedule;

import java.util.List;

public interface IOaScheduleService extends IService<OaSchedule> {
    List<OaSchedule> selectOaScheduleList(OaSchedule schedule);
    List<OaSchedule> selectUserScheduleList(Long userId, OaSchedule schedule);
    OaSchedule selectOaScheduleById(Long scheduleId);
    int insertOaSchedule(OaSchedule schedule);
    int updateOaSchedule(OaSchedule schedule);
    int deleteOaScheduleByIds(Long[] scheduleIds);
    int cancelOaSchedule(Long scheduleId);
}