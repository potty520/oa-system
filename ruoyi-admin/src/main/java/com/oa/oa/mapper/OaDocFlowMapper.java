package com.oa.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.oa.domain.OaDocFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OaDocFlowMapper extends BaseMapper<OaDocFlow> {

    OaDocFlow selectDocFlowById(@Param("flowId") Long flowId);
}