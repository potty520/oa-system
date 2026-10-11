package com.oa.common.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * 业务规则导致的拒绝访问。
 *
 * <p>与 Spring Security 因缺少权限而抛出的 {@link AccessDeniedException} 区分开：
 * 前者携带我们自己写的中文业务提示（例如"签发人不能是起草人本人"），必须原样回给用户；
 * 后者的 message 取自 Spring Security 的本地化资源包，会随 Accept-Language 在
 * "不允许访问"与"Access is denied"之间漂移，因此统一换成固定文案。</p>
 */
public class BusinessAccessDeniedException extends AccessDeniedException {

    private static final long serialVersionUID = 1L;

    public BusinessAccessDeniedException(String message) {
        super(message);
    }
}
