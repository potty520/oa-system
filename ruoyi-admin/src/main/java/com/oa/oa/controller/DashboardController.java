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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** 待登记 / 办理中 */
    private static final List<String> STATUS_IN_PROGRESS = Arrays.asList("0", "1");

    /** 已归档 */
    private static final String STATUS_ARCHIVED = "3";

    @GetMapping("/stats")
    public AjaxResult stats() {
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        boolean unrestricted = SecurityUtils.isAdmin(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todoCount", todoService.selectTodoCount(userId, username));
        result.put("incomingPending", incomingDocMapper.selectCount(incomingWrapper(unrestricted, userId, username)
                .in(OaIncomingDoc::getStatus, STATUS_IN_PROGRESS)));
        result.put("outgoingPending", outgoingDocMapper.selectCount(outgoingWrapper(unrestricted, userId, username)
                .in(OaOutgoingDoc::getStatus, STATUS_IN_PROGRESS)));
        result.put("unreadCirculation", circulationMapper.selectCount(new LambdaQueryWrapper<OaDocCirculation>()
                .eq(OaDocCirculation::getUserId, userId)
                .eq(OaDocCirculation::getReadStatus, "0")));
        result.put("archivedIncoming", incomingDocMapper.selectCount(incomingWrapper(unrestricted, userId, username)
                .eq(OaIncomingDoc::getStatus, STATUS_ARCHIVED)));
        result.put("archivedOutgoing", outgoingDocMapper.selectCount(outgoingWrapper(unrestricted, userId, username)
                .eq(OaOutgoingDoc::getStatus, STATUS_ARCHIVED)));
        return AjaxResult.success(result);
    }

    /**
     * 收文统计口径。非管理员只统计与自己相关的公文，
     * 否则首页会把全单位的公文数量暴露给普通员工。
     */
    private LambdaQueryWrapper<OaIncomingDoc> incomingWrapper(boolean unrestricted, Long userId, String username) {
        LambdaQueryWrapper<OaIncomingDoc> wrapper = new LambdaQueryWrapper<>();
        if (!unrestricted) {
            wrapper.and(query -> query.eq(OaIncomingDoc::getHandlerId, userId)
                    .or().eq(OaIncomingDoc::getCreateBy, username));
        }
        return wrapper;
    }

    /** 发文统计口径，同 {@link #incomingWrapper}。 */
    private LambdaQueryWrapper<OaOutgoingDoc> outgoingWrapper(boolean unrestricted, Long userId, String username) {
        LambdaQueryWrapper<OaOutgoingDoc> wrapper = new LambdaQueryWrapper<>();
        if (!unrestricted) {
            wrapper.and(query -> query.eq(OaOutgoingDoc::getSignUserId, userId)
                    .or().eq(OaOutgoingDoc::getCreateBy, username));
        }
        return wrapper;
    }
}