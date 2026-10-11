package com.oa.common.utils;

import javax.servlet.http.HttpServletRequest;

/** 解析客户端 IP：只有显式信任反向代理时才采用 X-Forwarded-For，否则客户端可以伪造审计 IP。 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String resolve(HttpServletRequest request, boolean trustForwardedHeaders) {
        if (request == null) {
            return null;
        }
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
                String clientIp = forwardedFor.split(",")[0].trim();
                if (!clientIp.isEmpty()) {
                    return clientIp.length() > 128 ? clientIp.substring(0, 128) : clientIp;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
