package com.oa.security.service;

import com.oa.security.model.LoginUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class LoginUserCache {
    private static final long TTL_MILLIS = 5 * 60 * 1000L;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public LoginUser get(String username, Supplier<LoginUser> loader) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        CacheEntry entry = cache.get(username);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAt > now) {
            return entry.user;
        }
        LoginUser user = loader.get();
        if (user != null) {
            cache.put(username, new CacheEntry(user, now + TTL_MILLIS));
        } else {
            cache.remove(username);
        }
        return user;
    }

    public void evict(String username) {
        if (username != null) {
            cache.remove(username);
        }
    }

    public void evictAll() {
        cache.clear();
    }

    private static class CacheEntry {
        private final LoginUser user;
        private final long expiresAt;

        private CacheEntry(LoginUser user, long expiresAt) {
            this.user = user;
            this.expiresAt = expiresAt;
        }
    }
}
