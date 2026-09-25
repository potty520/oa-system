package com.oa.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.oa.domain.OaIncomingDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OaIncomingDocMapper extends BaseMapper<OaIncomingDoc> {
    @Update("UPDATE oa_incoming_doc SET status = #{newStatus}, update_by = #{username}, update_time = CURRENT_TIMESTAMP "
            + "WHERE doc_id = #{docId} AND status = #{expectedStatus} AND del_flag = '0'")
    int updateStatusIfCurrent(@Param("docId") Long docId,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("newStatus") String newStatus,
                              @Param("username") String username);
    @Update("UPDATE oa_incoming_doc SET flow_id = #{flowId}, current_step_id = #{stepId}, current_step_order = #{stepOrder}, "
            + "update_by = #{username}, update_time = CURRENT_TIMESTAMP WHERE doc_id = #{docId} AND del_flag = '0'")
    int updateFlowState(@Param("docId") Long docId,
                         @Param("flowId") Long flowId,
                         @Param("stepId") Long stepId,
                         @Param("stepOrder") Integer stepOrder,
                         @Param("username") String username);
    /**
     * 推进流程时校验当前步骤：公文仍停在 expectedStepId 才允许写入新步骤。
     * 否则同一步骤的多个处理人并发办理时会把流程连推两步。
     */
    @Update("UPDATE oa_incoming_doc SET flow_id = #{flowId}, current_step_id = #{stepId}, current_step_order = #{stepOrder}, "
            + "update_by = #{username}, update_time = CURRENT_TIMESTAMP "
            + "WHERE doc_id = #{docId} AND del_flag = '0' AND current_step_id = #{expectedStepId}")
    int advanceFlowStateIfCurrent(@Param("docId") Long docId,
                                  @Param("expectedStepId") Long expectedStepId,
                                  @Param("flowId") Long flowId,
                                  @Param("stepId") Long stepId,
                                  @Param("stepOrder") Integer stepOrder,
                                  @Param("username") String username);
}
