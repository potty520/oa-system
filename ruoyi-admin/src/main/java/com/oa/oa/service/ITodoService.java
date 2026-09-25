package com.oa.oa.service;

import com.oa.oa.domain.OaTodo;

import java.util.List;

public interface ITodoService {
    List<OaTodo> selectTodoList(Long userId, String userName, String todoTitle, String status);

    int selectTodoCount(Long userId, String userName);

    void completeTodo(String todoType, Long relatedId);
}