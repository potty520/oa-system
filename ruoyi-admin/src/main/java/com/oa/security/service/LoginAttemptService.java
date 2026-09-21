package com.oa.security.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int USER_MAX_ATTEMPTS = 5;
    private static final int IP_MAX_ATTEMPTS = 20;
    private static final long LOCK_MILLIS = Duration.ofMinutes(15).toMillis();

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username, String clientIp) {
        return isBlocked("user:" + normalize(username), USER_MAX_ATTEMPTS)
                || isBlocked("ip:" + normalize(clientIp), IP_MAX_ATTEMPTS);
    }

    public void recordFailure(String username, String clientIp) {
        recordFailure("user:" + normalize(username));
        recordFailure("ip:" + normalize(clientIp));
    }

    public void recordSuccess(String username, String clientIp) {
        attempts.remove("user:" + normalize(username));
        attempts.remove("ip:" + normalize(clientIp));
    }

    private boolean isBlocked(String key, int maxAttempts) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        synchronized (attempt) {
            if (attempt.blockedUntil > System.currentTimeMillis()) {
                return true;
            }
            if (attempt.blockedUntil > 0) {
                attempts.remove(key, attempt);
            }
            return false;
        }
    }

    private void recordFailure(String key) {
        attempts.compute(key, (ignored, current) -> {
            long now = System.currentTimeMillis();
            Attempt attempt = current == null ? new Attempt() : current;
            synchronized (attempt) {
                if (attempt.blockedUntil > now) {
                    return attempt;
                }
                if (attempt.blockedUntil > 0) {
                    attempt.failures = 0;
                    attempt.blockedUntil = 0;
                }
                attempt.failures++;
                if (attempt.failures >= maxAttempts(key)) {
                    attempt.blockedUntil = now + LOCK_MILLIS;
                }
            }
            return attempt;
        });
    }

    private int maxAttempts(String key) {
        return key.startsWith("ip:") ? IP_MAX_ATTEMPTS : USER_MAX_ATTEMPTS;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static class Attempt {
        private int failures;
        private long blockedUntil;
    }
}
