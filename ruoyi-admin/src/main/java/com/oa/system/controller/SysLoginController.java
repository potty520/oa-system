package com.oa.system.controller;

import com.oa.common.core.result.AjaxResult;
import com.oa.common.utils.StringUtils;
import com.oa.security.model.LoginUser;
import com.oa.security.utils.JwtUtils;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysUser;
import com.oa.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SysLoginController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> loginBody) {
        String username = loginBody.get("username");
        String password = loginBody.get("password");

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return AjaxResult.error("用户名或密码不能为空");
        }

        LoginUser loginUser = userService.selectUserByUsername(username);
        if (loginUser == null) {
            return AjaxResult.error("用户不存在");
        }
        if (!SecurityUtils.matchesPassword(password, loginUser.getPassword())) {
            return AjaxResult.error("密码错误");
        }
        if (!"0".equals(loginUser.getStatus())) {
            return AjaxResult.error("账号已被停用");
        }

        String token = jwtUtils.createToken(loginUser.getUsername(), loginUser.getUserId());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", loginUser.getUserId());
        userInfo.put("userName", loginUser.getUsername());
        userInfo.put("nickName", loginUser.getNickname());
        userInfo.put("avatar", loginUser.getAvatar());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return AjaxResult.success("登录成功", result);
    }

    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return AjaxResult.error("未登录");
        }

        Map<String, Object> user = new HashMap<>();
        user.put("userId", loginUser.getUserId());
        user.put("userName", loginUser.getUsername());
        user.put("nickName", loginUser.getNickname());
        user.put("avatar", loginUser.getAvatar());

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("permissions", loginUser.getPermissions());
        return AjaxResult.success(result);
    }

    @PostMapping("/register")
    public AjaxResult register(@RequestBody SysUser user) {
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return AjaxResult.error("用户名和密码不能为空");
        }
        if (userService.selectSysUserByUsername(user.getUsername()) != null) {
            return AjaxResult.error("用户名已存在");
        }
        user.setStatus("0");
        if (user.getDeptId() == null) {
            user.setDeptId(100L);
        }
        user.setRoleIds(java.util.Collections.singletonList(2L));
        return AjaxResult.toAjax(userService.insertUser(user));
    }

    @PostMapping("/logout")
    public AjaxResult logout() {
        return AjaxResult.success("退出成功");
    }
}