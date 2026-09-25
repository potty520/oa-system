package com.oa.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.oa.domain.OaDocApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OaDocApprovalMapper extends BaseMapper<OaDocApproval> {

    /**
     * 条件办理：只有仍处于待办('0')的签批记录才会被更新。
     *
     * <p>同一流程步骤会分配给角色下的多个用户，先读后写会让两个人同时把流程往下推；
     * 靠这个原子更新保证只有第一个办理的人能成功。</p>
     */
    @Update("UPDATE oa_doc_approval SET approve_result = #{result}, approve_remark = #{remark}, "
            + "approve_time = CURRENT_TIMESTAMP, update_by = #{username}, update_time = CURRENT_TIMESTAMP "
            + "WHERE approval_id = #{approvalId} AND approve_result = '0' AND del_flag = '0'")
    int approveIfPending(@Param("approvalId") Long approvalId,
                         @Param("result") String result,
                         @Param("remark") String remark,
                         @Param("username") String username);
}
