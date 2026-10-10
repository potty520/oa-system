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

    /**
     * 固定的一次性 BCrypt 串（Spring Security 文档中的公开示例，cost 与默认强度一致，不是任何账号的口令）。
     * 只在“账号不存在”或“口令仍是占位符”时用来替代真实口令做一次完整比对，
     * 保证无论账号是否存在都恰好执行一次 BCrypt 运算，响应时间不再泄露账号是否存在。
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

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
            // 锁定时间随连续失败次数逐级增长（30 秒起步，最长 15 分钟），提示里不再写死固定时长
            long blockedSeconds = loginAttemptService.blockedSeconds(username, clientIp);
            return AjaxResult.error(429, blockedSeconds > 0
                    ? "登录失败次数过多，请 " + blockedSeconds + " 秒后再试"
                    : "登录失败次数过多，请稍后再试");
        }

        LoginUser loginUser = userService.selectUserByUsername(username);
        // 账号不存在或口令仍是占位符时改用固定的 dummy 哈希：
        // 无论哪种情况都恰好做一次 BCrypt 比对，避免“未知用户名返回更快”被用来枚举账号。
        // 占位符不是合法 BCrypt 串，直接拿去比对会抛异常，所以必须先替换掉。
        boolean pendingPassword = loginUser == null
                || Constants.PENDING_PASSWORD.equals(loginUser.getPassword());
        String passwordHash = pendingPassword ? DUMMY_PASSWORD_HASH : loginUser.getPassword();
        boolean passwordMatched = SecurityUtils.matchesPassword(password, passwordHash);
        if (pendingPassword || !passwordMatched) {
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