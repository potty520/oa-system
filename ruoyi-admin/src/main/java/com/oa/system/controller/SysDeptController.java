package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysDept;
import com.oa.system.service.ISysDeptService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/dept")
public class SysDeptController extends BaseController {

    @Autowired
    private ISysDeptService deptService;

    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public AjaxResult list(SysDept dept) {
        return AjaxResult.success(deptService.selectDeptList(dept));
    }

    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping("/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId) {
        return AjaxResult.success(deptService.getById(deptId));
    }

    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public AjaxResult add(@RequestBody SysDept dept) {
        if (StringUtils.isBlank(dept.getDeptName())) {
            return AjaxResult.error(400, "部门名称不能为空");
        }
        return toAjax(deptService.save(dept));
    }

    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody SysDept dept) {
        return toAjax(deptService.updateById(dept));
    }

    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptIds}")
    public AjaxResult remove(@PathVariable Long[] deptIds) {
        boolean success = true;
        for (Long deptId : deptIds) {
            success = deptService.removeById(deptId) && success;
        }
        return toAjax(success);
    }

    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysDept dept) {
        return toAjax(deptService.changeStatus(dept.getDeptId(), dept.getStatus()));
    }
}