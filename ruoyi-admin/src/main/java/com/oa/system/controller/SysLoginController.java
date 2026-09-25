package com.oa.system.controller;

import com.oa.common.constant.Constants;
import com.oa.common.core.result.AjaxResult;
import com.oa.common.utils.StringUtils;
import com.oa.security.model.LoginUser;
import com.oa.security.service.LoginAttemptService;
import com.oa.security.utils.JwtUtils;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysUser;
import com.oa.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SysLoginController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> loginBody, HttpServletRequest request) {
        String username = loginBody.get("username");
        String password = loginBody.get("password");
        String clientIp = resolveClientIp(request);

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return AjaxResult.error(400, "用户名或密码不能为空");
        }
        if (loginAttemptService.isBlocked(username, clientIp)) {
            return AjaxResult.error(429, "登录失败次数过多，请15分钟后再试");
        }

        LoginUser loginUser = userService.selectUserByUsername(username);
        // 占位口令不是合法 BCrypt 串，直接短路，既避免无意义的哈希比对也不暴露账号状态
        if (loginUser == null || Constants.PENDING_PASSWORD.equals(loginUser.getPassword())
                || !SecurityUtils.matchesPassword(password, loginUser.getPassword())) {
            loginAttemptService.recordFailure(username, clientIp);
            return AjaxResult.error(400, "用户名或密码错误");
        }
        if (!"0".equals(loginUser.getStatus())) {
            return AjaxResult.error(403, "账号已被停用");
        }
        loginAttemptService.recordSuccess(username, clientIp);

        String token = jwtUtils.createToken(loginUser.getUsername(), loginUser.getUserId());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", loginUser.getUserId());
        userInfo.put("userName", loginUser.getUsername());
        userInfo.put("nickName", loginUser.getNickname());
        userInfo.put("avatar", loginUser.getAvatar());
        userInfo.put("mustChangePwd", mustChangePwd(loginUser));

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return AjaxResult.success("登录成功", result);
    }

    private boolean mustChangePwd(LoginUser loginUser) {
        return Constants.MUST_CHANGE_PWD_YES.equals(loginUser.getMustChangePwd());
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.isNotEmpty(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
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
        user.put("mustChangePwd", mustChangePwd(loginUser));

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("permissions", loginUser.getPermissions());
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('system:user:add')")
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