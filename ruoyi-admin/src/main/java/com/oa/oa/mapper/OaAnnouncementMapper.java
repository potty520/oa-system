package com.oa.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.oa.domain.OaAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OaAnnouncementMapper extends BaseMapper<OaAnnouncement> {

    @Update("UPDATE oa_announcement SET view_count = CASE WHEN view_count IS NULL THEN 1 ELSE view_count + 1 END " +
            "WHERE ann_id = #{annId} AND del_flag = '0'")
    int incrementViewCount(@Param("annId") Long annId);
}