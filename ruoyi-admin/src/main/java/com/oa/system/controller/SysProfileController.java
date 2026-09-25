package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.result.AjaxResult;
import com.oa.security.model.LoginUser;
import com.oa.security.utils.PasswordPolicy;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 当前登录用户的自助操作。任何已认证用户都可访问，不需要 system:user:* 权限，
 * 这样被强制改密的初始账号也能在只有基础菜单时完成改密。
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {

    @Autowired
    private ISysUserService userService;

    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> body) {
        LoginUser current = SecurityUtils.getLoginUser();
        if (current == null) {
            return AjaxResult.error(401, "未登录");
        }
        String oldPassword = body == null ? null : body.get("oldPassword");
        String newPassword = body == null ? null : body.get("newPassword");
        if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)) {
            return AjaxResult.error(400, "原密码和新密码不能为空");
        }
        if (oldPassword.equals(newPassword)) {
            return AjaxResult.error(400, "新密码不能与原密码相同");
        }
        String violation = PasswordPolicy.validate(newPassword);
        if (violation != null) {
            return AjaxResult.error(400, violation);
        }
        String error = userService.updateOwnPassword(current.getUserId(), oldPassword, newPassword);
        return error == null ? AjaxResult.success("密码修改成功") : AjaxResult.error(400, error);
    }
}
