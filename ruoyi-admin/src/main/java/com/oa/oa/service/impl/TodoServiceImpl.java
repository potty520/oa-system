package com.oa.oa.service.impl;

import com.oa.oa.domain.OaLeave;
import com.oa.oa.domain.OaReimbursement;
import com.oa.oa.domain.OaTodo;
import com.oa.oa.mapper.OaLeaveMapper;
import com.oa.oa.mapper.OaReimbursementMapper;
import com.oa.oa.mapper.OaTodoMapper;
import com.oa.oa.service.ITodoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoServiceImpl implements ITodoService {

    @Autowired
    private OaTodoMapper todoMapper;

    @Autowired
    private OaLeaveMapper leaveMapper;

    @Autowired
    private OaReimbursementMapper reimbursementMapper;

    @Override
    public List<OaTodo> selectTodoList(Long userId, String todoTitle, String status) {
        String todoTitleLike = org.apache.commons.lang3.StringUtils.isBlank(todoTitle) ? null : "%" + todoTitle + "%";
        return todoMapper.selectTodoList(userId, todoTitleLike, status);
    }

    @Override
    public int selectTodoCount(Long userId) {
        return todoMapper.selectTodoCount(userId);
    }

    @Override
    public void completeTodo(String todoType, Long relatedId) {
        if ("leave".equals(todoType)) {
            OaLeave leave = leaveMapper.selectById(relatedId);
            if (leave != null) {
                leave.setApprovalStatus("2");
                leaveMapper.updateById(leave);
            }
        } else if ("reimbursement".equals(todoType)) {
            OaReimbursement reimbursement = reimbursementMapper.selectById(relatedId);
            if (reimbursement != null) {
                reimbursement.setApprovalStatus("2");
                reimbursementMapper.updateById(reimbursement);
            }
        }
    }
}