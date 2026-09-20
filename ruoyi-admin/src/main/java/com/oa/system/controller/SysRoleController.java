package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysRole;
import com.oa.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/role")
public class SysRoleController extends BaseController {

    @Autowired
    private ISysRoleService roleService;

    @GetMapping("/list")
    public TableDataInfo list(SysRole role) {
        startPage();
        List<SysRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    @GetMapping("/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId) {
        return AjaxResult.success(roleService.getById(roleId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysRole role) {
        return toAjax(roleService.save(role));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody SysRole role) {
        return toAjax(roleService.updateById(role));
    }

    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds) {
        boolean success = true;
        for (Long roleId : roleIds) {
            success = roleService.removeById(roleId) && success;
        }
        return toAjax(success);
    }

    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysRole role) {
        return toAjax(roleService.updateById(role));
    }
}