package com.oa.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.oa.domain.OaDocFlow;
import com.oa.oa.domain.OaDocFlowStep;
import com.oa.oa.mapper.OaDocFlowMapper;
import com.oa.oa.mapper.OaDocFlowStepMapper;
import com.oa.oa.service.IOaDocFlowService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OaDocFlowServiceImpl extends ServiceImpl<OaDocFlowMapper, OaDocFlow> implements IOaDocFlowService {

    @Autowired
    private OaDocFlowMapper docFlowMapper;

    @Autowired
    private OaDocFlowStepMapper stepMapper;

    @Override
    public List<OaDocFlow> selectDocFlowList(OaDocFlow docFlow) {
        LambdaQueryWrapper<OaDocFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(docFlow.getFlowName()), OaDocFlow::getFlowName, docFlow.getFlowName())
                .eq(StringUtils.isNotBlank(docFlow.getFlowType()), OaDocFlow::getFlowType, docFlow.getFlowType())
                .eq(StringUtils.isNotBlank(docFlow.getStatus()), OaDocFlow::getStatus, docFlow.getStatus())
                .orderByDesc(OaDocFlow::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public OaDocFlow selectDocFlowById(Long flowId) {
        return docFlowMapper.selectDocFlowById(flowId);
    }

    @Override
    @Transactional
    public int insertDocFlow(OaDocFlow docFlow) {
        int result = baseMapper.insert(docFlow);
        if (docFlow.getSteps() != null && !docFlow.getSteps().isEmpty()) {
            for (OaDocFlowStep step : docFlow.getSteps()) {
                step.setStepId(null);
                step.setFlowId(docFlow.getFlowId());
                stepMapper.insert(step);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int updateDocFlow(OaDocFlow docFlow) {
        LambdaQueryWrapper<OaDocFlowStep> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(OaDocFlowStep::getFlowId, docFlow.getFlowId());
        stepMapper.delete(deleteWrapper);

        if (docFlow.getSteps() != null && !docFlow.getSteps().isEmpty()) {
            for (OaDocFlowStep step : docFlow.getSteps()) {
                step.setStepId(null);
                step.setFlowId(docFlow.getFlowId());
                stepMapper.insert(step);
            }
        }
        return baseMapper.updateById(docFlow);
    }

    @Override
    public int deleteDocFlowByIds(Long[] flowIds) {
        int count = 0;
        for (Long flowId : flowIds) {
            LambdaQueryWrapper<OaDocFlowStep> stepWrapper = new LambdaQueryWrapper<>();
            stepWrapper.eq(OaDocFlowStep::getFlowId, flowId);
            stepMapper.delete(stepWrapper);
            count += baseMapper.deleteById(flowId);
        }
        return count;
    }
}