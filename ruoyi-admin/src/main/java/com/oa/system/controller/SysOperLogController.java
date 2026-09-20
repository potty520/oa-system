package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysOperLog;
import com.oa.system.mapper.SysOperLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/log")
public class SysOperLogController extends BaseController {
    @Autowired
    private SysOperLogMapper operLogMapper;

    @PreAuthorize("@ss.hasPermi('system:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysOperLog log) {
        startPage();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOperLog> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.like(log.getTitle() != null && !log.getTitle().isEmpty(), SysOperLog::getTitle, log.getTitle())
                .eq(log.getStatus() != null && !log.getStatus().isEmpty(), SysOperLog::getStatus, log.getStatus())
                .orderByDesc(SysOperLog::getOperTime);
        List<SysOperLog> list = operLogMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:log:remove')")
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds) {
        int count = 0;
        for (Long id : logIds) {
            count += operLogMapper.deleteById(id);
        }
        return toAjax(count);
    }
}