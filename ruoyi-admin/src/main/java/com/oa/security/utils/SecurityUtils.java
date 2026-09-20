package com.oa.security.utils;

import com.oa.security.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SecurityUtils {

    /**
     * 获取当前 Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUsername() : "";
    }

    /**
     * 获取当前用户真实姓名
     */
    public static String getNickname() {
        LoginUser user = getLoginUser();
        return user != null ? user.getNickname() : "";
    }

    /**
     * 密码加密
     */
    public static String encryptPassword(String password) {
        return new BCryptPasswordEncoder().encode(password);
    }

    /**
     * 密码匹配
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        return new BCryptPasswordEncoder().matches(rawPassword, encodedPassword);
    }

    /**
     * 判断是否为管理员
     */
    public static boolean isAdmin(Long userId) {
        return userId != null && userId == 1L;
    }
}
