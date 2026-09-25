package com.oa.oa.mapper;

import com.oa.oa.domain.OaTodo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OaTodoMapper {
    List<OaTodo> selectTodoList(@Param("userId") Long userId,
                                @Param("userName") String userName,
                                @Param("todoTitleLike") String todoTitleLike,
                                @Param("status") String status);

    int selectTodoCount(@Param("userId") Long userId, @Param("userName") String userName);
}