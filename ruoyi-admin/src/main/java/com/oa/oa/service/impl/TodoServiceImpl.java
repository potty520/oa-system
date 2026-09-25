package com.oa.oa.service.impl;

import com.oa.oa.domain.OaIncomingDoc;
import com.oa.oa.domain.OaOutgoingDoc;
import com.oa.oa.domain.DocActionRequest;
import com.oa.oa.domain.OaTodo;
import com.oa.oa.mapper.OaIncomingDocMapper;
import com.oa.oa.mapper.OaOutgoingDocMapper;
import com.oa.oa.mapper.OaTodoMapper;
import com.oa.oa.service.IOaDocProcessService;
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
    private OaIncomingDocMapper incomingDocMapper;

    @Autowired
    private OaOutgoingDocMapper outgoingDocMapper;

    @Autowired
    private IOaDocProcessService docProcessService;

    @Override
    public List<OaTodo> selectTodoList(Long userId, String userName, String todoTitle, String status) {
        String todoTitleLike = org.apache.commons.lang3.StringUtils.isBlank(todoTitle) ? null : "%" + todoTitle + "%";
        return todoMapper.selectTodoList(userId, userName, todoTitleLike, status);
    }

    @Override
    public int selectTodoCount(Long userId, String userName) {
        return todoMapper.selectTodoCount(userId, userName);
    }

    /**
     * 待办列表里的请假/报销条目是"我提交的单据"，把它们标记完成等同于审批通过。
     * 这里不再直接改写审批状态，必须走带归属校验与审批权限校验的审批接口，
     * 否则任何申请人都能审批自己的单据。
     */
    @Override
    public void completeTodo(String todoType, Long relatedId) {
        if ("incoming_doc".equals(todoType)) {
            OaIncomingDoc doc = incomingDocMapper.selectById(relatedId);
            if (doc == null) {
                throw new IllegalArgumentException("收文不存在");
            }
            executeDocAction("1", relatedId, "0".equals(doc.getStatus()) ? "receive" : "complete");
        } else if ("outgoing_doc".equals(todoType)) {
            OaOutgoingDoc doc = outgoingDocMapper.selectById(relatedId);
            if (doc == null) {
                throw new IllegalArgumentException("发文不存在");
            }
            executeDocAction("2", relatedId, "0".equals(doc.getStatus()) ? "submit" : "sign");
        } else {
            throw new IllegalArgumentException("请假、报销单据请在对应审批页面由审批人处理");
        }
    }

    private void executeDocAction(String docType, Long docId, String action) {
        DocActionRequest request = new DocActionRequest();
        request.setAction(action);
        docProcessService.execute(docType, docId, request);
    }
}