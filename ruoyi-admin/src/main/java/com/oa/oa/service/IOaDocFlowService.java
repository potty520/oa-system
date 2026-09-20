package com.oa.oa.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.oa.domain.OaDocFlow;

import java.util.List;

public interface IOaDocFlowService extends IService<OaDocFlow> {
    List<OaDocFlow> selectDocFlowList(OaDocFlow docFlow);
    OaDocFlow selectDocFlowById(Long flowId);
    int insertDocFlow(OaDocFlow docFlow);
    int updateDocFlow(OaDocFlow docFlow);
    int deleteDocFlowByIds(Long[] flowIds);
}