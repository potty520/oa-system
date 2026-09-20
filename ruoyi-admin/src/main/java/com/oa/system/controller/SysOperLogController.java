package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysOperLog;
import com.oa.system.mapper.SysOperLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
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

    @PreAuthorize("@ss.hasPermi('system:log:list')")
    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> export() {
        List<SysOperLog> logs = operLogMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getOperTime));
        StringBuilder csv = new StringBuilder("logId,title,operator,method,url,status,time,error\n");
        for (SysOperLog log : logs) {
            csv.append(csv(log.getLogId())).append(',')
                    .append(csv(log.getTitle())).append(',')
                    .append(csv(log.getOperatorName())).append(',')
                    .append(csv(log.getMethod())).append(',')
                    .append(csv(log.getOperUrl())).append(',')
                    .append(csv(log.getStatus())).append(',')
                    .append(csv(log.getOperTime() == null ? "" : log.getOperTime().toString())).append(',')
                    .append(csv(log.getErrorMsg())).append('\n');
        }
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=oper-log.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @PreAuthorize("@ss.hasPermi('system:log:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean(@RequestParam(value = "days", defaultValue = "30") Integer days) {
        Date before = new Date(System.currentTimeMillis() - Math.max(days, 1) * 86400000L);
        int count = operLogMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getOperTime, before));
        return AjaxResult.success("清理完成", count);
    }

    private String csv(Object value) {
        if (value == null) return "";
        return '"' + String.valueOf(value).replace("\"", "\"\"") + '"';
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