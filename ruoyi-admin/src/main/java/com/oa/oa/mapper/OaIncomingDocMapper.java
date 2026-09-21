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
}
