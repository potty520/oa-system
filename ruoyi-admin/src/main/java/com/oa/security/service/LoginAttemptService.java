package com.oa.security.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数与锁定。
 *
 * <p>失败次数只在 {@link #WINDOW_MILLIS} 窗口内累计，窗口外的旧计数会被清掉。
 * 没有窗口时，随机用户名撞库会把 key 永久留在内存里（每次失败都新增一条）。</p>
 */
@Service
public class LoginAttemptService {
    private static final int USER_MAX_ATTEMPTS = 5;
    private static final int IP_MAX_ATTEMPTS = 20;
    private static final long LOCK_MILLIS = Duration.ofMinutes(15).toMillis();
    /** 失败计数的有效窗口：超过这个时间没有新的失败就从头开始计 */
    private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();
    /** 兼底容量：随机用户名撞库会不断新增 key，超限后丢弃最久未活动的记录 */
    private static final int MAX_TRACKED_KEYS = 20000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Object monitor = new Object();

    public boolean isBlocked(String username, String clientIp) {
        synchronized (monitor) {
            cleanupExpired();
            return isBlocked(userKey(username)) || isBlocked(ipKey(clientIp));
        }
    }

    public void recordFailure(String username, String clientIp) {
        synchronized (monitor) {
            cleanupExpired();
            recordFailure(userKey(username), USER_MAX_ATTEMPTS);
            recordFailure(ipKey(clientIp), IP_MAX_ATTEMPTS);
            enforceCapacity();
        }
    }

    public void recordSuccess(String username, String clientIp) {
        synchronized (monitor) {
            attempts.remove(userKey(username));
            attempts.remove(ipKey(clientIp));
        }
    }

    private boolean isBlocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.blockedUntil > System.currentTimeMillis()) {
            return true;
        }
        if (isStale(attempt)) {
            attempts.remove(key, attempt);
        }
        return false;
    }

    private void recordFailure(String key, int maxAttempts) {
        long now = System.currentTimeMillis();
        Attempt attempt = attempts.computeIfAbsent(key, ignored -> new Attempt());
        if (attempt.blockedUntil > now) {
            return;
        }
        if (isStale(attempt)) {
            attempt.failures = 0;
            attempt.blockedUntil = 0;
        }
        attempt.failures++;
        attempt.lastFailureAt = now;
        if (attempt.failures >= maxAttempts) {
            attempt.blockedUntil = now + LOCK_MILLIS;
        }
    }

    private void cleanupExpired() {
        attempts.entrySet().removeIf(entry -> isStale(entry.getValue()));
    }

    private boolean isStale(Attempt attempt) {
        long now = System.currentTimeMillis();
        if (attempt.blockedUntil > now) {
            return false;
        }
        if (attempt.blockedUntil > 0) {
            // 锁定期已过，记录没有保留价值
            return true;
        }
        return now - attempt.lastFailureAt > WINDOW_MILLIS;
    }

    private void enforceCapacity() {
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        List<Map.Entry<String, Attempt>> entries = new ArrayList<>(attempts.entrySet());
        entries.sort((left, right) -> Long.compare(lastActivity(left.getValue()), lastActivity(right.getValue())));
        int drop = entries.size() - MAX_TRACKED_KEYS / 2;
        for (int index = 0; index < drop; index++) {
            attempts.remove(entries.get(index).getKey(), entries.get(index).getValue());
        }
    }

    private long lastActivity(Attempt attempt) {
        return Math.max(attempt.lastFailureAt, attempt.blockedUntil);
    }

    private String userKey(String username) {
        return "user:" + normalize(username);
    }

    private String ipKey(String clientIp) {
        return "ip:" + normalize(clientIp);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static class Attempt {
        private int failures;
        private long lastFailureAt;
        private long blockedUntil;
    }
}
