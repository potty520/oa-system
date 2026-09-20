package com.oa.oa.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.oa.domain.OaLeave;

import java.util.List;

public interface IOaLeaveService extends IService<OaLeave> {
    List<OaLeave> selectOaLeaveList(OaLeave leave);
    List<OaLeave> selectMyLeaveList(Long userId);
    OaLeave selectOaLeaveById(Long leaveId);
    int insertOaLeave(OaLeave leave);
    int updateOaLeave(OaLeave leave);
    int deleteOaLeaveByIds(Long[] leaveIds);
    int submitOaLeave(Long leaveId);
    int withdrawOaLeave(Long leaveId);
    int approveOaLeave(OaLeave leave);
}