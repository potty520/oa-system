package com.oa.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 吊销表。
 *
 * <p>JWT 是无状态的，签发后在有效期内一直可用。退出登录按单个 Token 吊销；
 * 改密 / 重置密码 / 停用账号则按用户吊销，让该用户此前拿到的所有 Token 立刻失效。</p>
 *
 * <p>纯内存实现，仅适用于单实例部署；多实例需要换成 Redis 之类的共享存储。</p>
 */
@Service
public class JwtTokenBlacklist {

    /** 单 Token 黑名单容量上限，超出后按到期时间丢弃最早失效的一批，保证内存有界 */
    private static final int MAX_REVOKED_TOKENS = 10000;

    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> revokedUsers = new ConcurrentHashMap<>();

    /** Token 有效期（毫秒）：用户级吊销记录超过这个时长就不可能再命中任何有效 Token */
    @Value("${jwt.expiration:86400000}")
    private long tokenTtlMillis;

    public void revoke(String token, long expiresAt) {
        if (token == null || token.trim().isEmpty() || expiresAt <= System.currentTimeMillis()) {
            return;
        }
        if (revokedTokens.size() >= MAX_REVOKED_TOKENS) {
            evictTokens();
        }
        revokedTokens.put(token, expiresAt);
    }

    public boolean isRevoked(String token) {
        if (token == null) {
            return false;
        }
        Long expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            revokedTokens.remove(token, expiresAt);
            return false;
        }
        return true;
    }

    /** 作废该用户此前签发的全部 Token。 */
    public void revokeUser(String username) {
        String key = normalize(username);
        if (key == null) {
            return;
        }
        long now = System.currentTimeMillis();
        cleanupUsers(now);
        // JWT 的 iat 只精确到秒，这里向下取整，避免把同一秒内重新登录签发的 Token 一起废掉
        revokedUsers.put(key, now / 1000L * 1000L);
    }

    /** 该 Token 是否签发于用户级吊销点之前。缺少 iat 的 Token 一律视为已吊销。 */
    public boolean isUserRevoked(String username, Date issuedAt) {
        String key = normalize(username);
        if (key == null) {
            return false;
        }
        Long revokedAt = revokedUsers.get(key);
        if (revokedAt == null) {
            return false;
        }
        if (revokedAt + tokenTtlMillis <= System.currentTimeMillis()) {
            revokedUsers.remove(key, revokedAt);
            return false;
        }
        return issuedAt == null || issuedAt.getTime() < revokedAt;
    }

    private void evictTokens() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (revokedTokens.size() < MAX_REVOKED_TOKENS) {
            return;
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<>(revokedTokens.entrySet());
        entries.sort((left, right) -> Long.compare(left.getValue(), right.getValue()));
        int drop = entries.size() - MAX_REVOKED_TOKENS / 2;
        for (int index = 0; index < drop; index++) {
            revokedTokens.remove(entries.get(index).getKey(), entries.get(index).getValue());
        }
    }

    private void cleanupUsers(long now) {
        revokedUsers.entrySet().removeIf(entry -> entry.getValue() + tokenTtlMillis <= now);
    }

    private String normalize(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }
}
