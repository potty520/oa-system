package com.oa.security.utils;

import com.oa.common.exception.BusinessAccessDeniedException;
import org.springframework.security.access.AccessDeniedException;

/**
 * 数据归属校验：@PreAuthorize 只能判断"有没有这个功能权限"，
 * 具体某条记录能不能动还要看它是不是本人的，统一在这里收口。
 */
public final class AccessGuards {

    private AccessGuards() {
    }

    /** 当前登录用户是否为记录归属人；超级管理员视为归属人。 */
    public static boolean isSelfOrAdmin(Long ownerUserId) {
        Long current = SecurityUtils.getUserId();
        if (current == null) {
            return false;
        }
        return SecurityUtils.isAdmin(current) || current.equals(ownerUserId);
    }

    /** 归属校验失败时抛出 403，由 GlobalExceptionHandler 统一转成响应。 */
    public static void requireSelfOrAdmin(Long ownerUserId, String resource) {
        if (!isSelfOrAdmin(ownerUserId)) {
            throw new BusinessAccessDeniedException("无权操作他人的" + resource);
        }
    }
}
