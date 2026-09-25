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
        insertSteps(docFlow.getFlowId(), docFlow.getSteps());
        return result;
    }

    @Override
    @Transactional
    public int updateDocFlow(OaDocFlow docFlow) {
        OaDocFlow existing = requireExisting(docFlow == null ? null : docFlow.getFlowId());
        // 只接受流程本身的配置字段，步骤仅在客户端显式提交时才整体替换，避免误清空
        OaDocFlow update = new OaDocFlow();
        update.setFlowId(existing.getFlowId());
        update.setFlowName(docFlow.getFlowName());
        update.setFlowType(docFlow.getFlowType());
        update.setDescription(docFlow.getDescription());
        update.setStatus(docFlow.getStatus());
        update.setRemark(docFlow.getRemark());

        if (docFlow.getSteps() != null) {
            LambdaQueryWrapper<OaDocFlowStep> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(OaDocFlowStep::getFlowId, existing.getFlowId());
            stepMapper.delete(deleteWrapper);
            insertSteps(existing.getFlowId(), docFlow.getSteps());
        }
        return baseMapper.updateById(update);
    }

    private void insertSteps(Long flowId, List<OaDocFlowStep> steps) {
        if (flowId == null || steps == null) {
            return;
        }
        for (OaDocFlowStep step : steps) {
            if (step == null) {
                continue;
            }
            // 步骤归属由服务端决定，避免客户端把步骤挂到别的流程上
            step.setStepId(null);
            step.setFlowId(flowId);
            stepMapper.insert(step);
        }
    }

    private OaDocFlow requireExisting(Long flowId) {
        OaDocFlow existing = flowId == null ? null : baseMapper.selectById(flowId);
        if (existing == null) {
            throw new IllegalArgumentException("流程配置不存在");
        }
        return existing;
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