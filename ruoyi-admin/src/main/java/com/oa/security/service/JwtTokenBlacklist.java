package com.oa.security.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtTokenBlacklist {
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String token, long expiresAt) {
        if (token == null || token.trim().isEmpty() || expiresAt <= System.currentTimeMillis()) {
            return;
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
}
