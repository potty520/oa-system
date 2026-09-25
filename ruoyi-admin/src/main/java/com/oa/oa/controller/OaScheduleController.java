package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaSchedule;
import com.oa.oa.service.IOaScheduleService;
import com.oa.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/schedule")
public class OaScheduleController extends BaseController {

    @Autowired
    private IOaScheduleService scheduleService;

    @PreAuthorize("@ss.hasPermi('oa:schedule:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaSchedule schedule) {
        startPage();
        List<OaSchedule> list = scheduleService.selectOaScheduleList(schedule);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:schedule:list')")
    @GetMapping("/myList")
    public TableDataInfo myList(OaSchedule schedule) {
        startPage();
        Long userId = SecurityUtils.getUserId();
        List<OaSchedule> list = scheduleService.selectUserScheduleList(userId, schedule);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:schedule:query')")
    @GetMapping("/{scheduleId}")
    public AjaxResult getInfo(@PathVariable Long scheduleId) {
        return AjaxResult.success(scheduleService.selectOaScheduleById(scheduleId));
    }

    @PreAuthorize("@ss.hasPermi('oa:schedule:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaSchedule schedule) {
        return toAjax(scheduleService.insertOaSchedule(schedule));
    }

    @PreAuthorize("@ss.hasPermi('oa:schedule:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OaSchedule schedule) {
        return toAjax(scheduleService.updateOaSchedule(schedule));
    }

    @PreAuthorize("@ss.hasPermi('oa:schedule:remove')")
    @DeleteMapping("/{scheduleIds}")
    public AjaxResult remove(@PathVariable Long[] scheduleIds) {
        return toAjax(scheduleService.deleteOaScheduleByIds(scheduleIds));
    }

    @PreAuthorize("@ss.hasPermi('oa:schedule:edit')")
    @PutMapping("/cancel/{scheduleId}")
    public AjaxResult cancel(@PathVariable Long scheduleId) {
        return toAjax(scheduleService.cancelOaSchedule(scheduleId));
    }
}