package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController {

    @Autowired
    private ISysMenuService menuService;

    /** 角色授权对话框使用，因此复用角色查询权限。 */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/tree")
    public AjaxResult tree() {
        return AjaxResult.success(menuService.selectMenuTree());
    }
}
