package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysDept;
import com.oa.system.service.ISysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/dept")
public class SysDeptController extends BaseController {

    @Autowired
    private ISysDeptService deptService;

    @GetMapping("/list")
    public AjaxResult list(SysDept dept) {
        return AjaxResult.success(deptService.selectDeptList(dept));
    }

    @GetMapping("/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId) {
        return AjaxResult.success(deptService.getById(deptId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysDept dept) {
        return toAjax(deptService.save(dept));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody SysDept dept) {
        return toAjax(deptService.updateById(dept));
    }

    @DeleteMapping("/{deptIds}")
    public AjaxResult remove(@PathVariable Long[] deptIds) {
        boolean success = true;
        for (Long deptId : deptIds) {
            success = deptService.removeById(deptId) && success;
        }
        return toAjax(success);
    }

    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysDept dept) {
        return toAjax(deptService.updateById(dept));
    }
}