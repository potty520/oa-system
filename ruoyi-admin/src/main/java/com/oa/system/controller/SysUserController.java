package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysUser;
import com.oa.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {

    @Autowired
    private ISysUserService userService;

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(value = "userName", required = false) String userName,
                              @RequestParam(value = "phonenumber", required = false) String phonenumber,
                              @RequestParam(value = "status", required = false) String status) {
        SysUser user = new SysUser();
        user.setUsername(userName);
        user.setPhone(phonenumber);
        user.setStatus(status);
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    /**
     * 用户下拉选项。任何已登录用户都可读，但只返回选人所需的最小字段。
     *
     * <p>不能直接用 /list：那个接口带数据范围过滤，普通用户只能看到自己，
     * 没法把公文转办/传阅给其他部门的同事。</p>
     */
    @GetMapping("/options")
    public AjaxResult options() {
        return AjaxResult.success(userService.selectUserOptions());
    }

    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@PathVariable Long userId) {
        return AjaxResult.success(userService.selectUserById(userId));
    }

    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public AjaxResult add(@RequestBody SysUser user) {
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return AjaxResult.error(400, "用户名和密码不能为空");
        }
        if (userService.selectSysUserByUsername(user.getUsername()) != null) {
            return AjaxResult.error(409, "用户名已存在");
        }
        return toAjax(userService.insertUser(user));
    }

    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody SysUser user) {
        return toAjax(userService.updateUser(user));
    }

    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        return toAjax(userService.deleteUserByIds(userIds));
    }

    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user) {
        return toAjax(userService.changeStatus(user));
    }

    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @PutMapping("/resetPwd")
    public AjaxResult resetPassword(@RequestBody SysUser user) {
        return toAjax(userService.resetPassword(user));
    }
}