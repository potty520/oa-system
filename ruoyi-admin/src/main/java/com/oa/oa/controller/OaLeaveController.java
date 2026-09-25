package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaLeave;
import com.oa.oa.service.IOaLeaveService;
import com.oa.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/leave")
public class OaLeaveController extends BaseController {

    @Autowired
    private IOaLeaveService leaveService;

    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaLeave leave) {
        startPage();
        List<OaLeave> list = leaveService.selectOaLeaveList(leave);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/myList")
    public TableDataInfo myList() {
        startPage();
        Long userId = SecurityUtils.getUserId();
        List<OaLeave> list = leaveService.selectMyLeaveList(userId);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:query')")
    @GetMapping("/{leaveId}")
    public AjaxResult getInfo(@PathVariable Long leaveId) {
        return AjaxResult.success(leaveService.selectOaLeaveById(leaveId));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaLeave leave) {
        return toAjax(leaveService.insertOaLeave(leave));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OaLeave leave) {
        return toAjax(leaveService.updateOaLeave(leave));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:remove')")
    @DeleteMapping("/{leaveIds}")
    public AjaxResult remove(@PathVariable Long[] leaveIds) {
        return toAjax(leaveService.deleteOaLeaveByIds(leaveIds));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:submit')")
    @PutMapping("/submit/{leaveId}")
    public AjaxResult submit(@PathVariable Long leaveId) {
        return toAjax(leaveService.submitOaLeave(leaveId));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:withdraw')")
    @PutMapping("/withdraw/{leaveId}")
    public AjaxResult withdraw(@PathVariable Long leaveId) {
        return toAjax(leaveService.withdrawOaLeave(leaveId));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:approve')")
    @PutMapping("/approve")
    public AjaxResult approve(@RequestBody OaLeave leave) {
        return toAjax(leaveService.approveOaLeave(leave));
    }
}