package com.oa.oa.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaDocCirculation;
import com.oa.oa.domain.OaIncomingDoc;
import com.oa.oa.domain.OaOutgoingDoc;
import com.oa.oa.mapper.OaDocCirculationMapper;
import com.oa.oa.mapper.OaIncomingDocMapper;
import com.oa.oa.mapper.OaOutgoingDocMapper;
import com.oa.oa.service.ITodoService;
import com.oa.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/oa/dashboard")
public class DashboardController {
    @Autowired
    private ITodoService todoService;
    @Autowired
    private OaIncomingDocMapper incomingDocMapper;
    @Autowired
    private OaOutgoingDocMapper outgoingDocMapper;
    @Autowired
    private OaDocCirculationMapper circulationMapper;

    @GetMapping("/stats")
    public AjaxResult stats() {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todoCount", todoService.selectTodoCount(userId));
        result.put("incomingPending", incomingDocMapper.selectCount(new LambdaQueryWrapper<OaIncomingDoc>().in(OaIncomingDoc::getStatus, "0", "1")));
        result.put("outgoingPending", outgoingDocMapper.selectCount(new LambdaQueryWrapper<OaOutgoingDoc>().in(OaOutgoingDoc::getStatus, "0", "1")));
        result.put("unreadCirculation", circulationMapper.selectCount(new LambdaQueryWrapper<OaDocCirculation>()
                .eq(OaDocCirculation::getUserId, userId)
                .eq(OaDocCirculation::getReadStatus, "0")));
        result.put("archivedIncoming", incomingDocMapper.selectCount(new LambdaQueryWrapper<OaIncomingDoc>().eq(OaIncomingDoc::getStatus, "3")));
        result.put("archivedOutgoing", outgoingDocMapper.selectCount(new LambdaQueryWrapper<OaOutgoingDoc>().eq(OaOutgoingDoc::getStatus, "3")));
        return AjaxResult.success(result);
    }
}