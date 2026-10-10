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
 *
 * <p>锁定时长采用**递增退避**而不是固定的 15 分钟：固定锁意味着任何人都能用 5 个请求
 * 把指定账号锁死 15 分钟（针对性的可用性攻击）。改为 30 秒起步、逐级加长、上限 15 分钟，
 * 第一次只能造成 30 秒影响，而持续爆破的代价迅速上升。一次成功登录会清空该账号的
 * 全部失败记录与退避档位。</p>
 *
 * <p>用户名维度（{@link #USER_MAX_ATTEMPTS} = 5）的精确序列：
 * 第 5 次失败 → 锁 30 秒 → 第 6 次失败 → 锁 1 分钟 → 第 7 次失败 → 锁 2 分钟 →
 * 第 8 次失败 → 锁 5 分钟 → 第 9 次及以后 → 锁 15 分钟（上限）。锁定期间不再累加失败次数，
 * 也不会延长锁定，所以攻击者无法靠持续刷请求把锁无限拖长；距最后一次失败超过
 * {@link #WINDOW_MILLIS} 没有新失败会清空记录，退避重新从 30 秒开始。
 * IP 维度（{@link #IP_MAX_ATTEMPTS} = 20）共用同一条阶梯，同样封顶 15 分钟。</p>
 */
@Service
public class LoginAttemptService {
    private static final int USER_MAX_ATTEMPTS = 5;
    private static final int IP_MAX_ATTEMPTS = 20;
    /**
     * 递增退避阶梯（毫秒）：30 秒 → 1 分钟 → 2 分钟 → 5 分钟 → 15 分钟。
     * 以用户名维度为例：第 5 次失败取第 1 档（30 秒），第 6/7/8 次分别取 1/2/5 分钟，
     * 第 9 次及以后固定取最后一档 15 分钟（上限）。
     */
    private static final long[] LOCK_BACKOFF_MILLIS = {
            Duration.ofSeconds(30).toMillis(),
            Duration.ofMinutes(1).toMillis(),
            Duration.ofMinutes(2).toMillis(),
            Duration.ofMinutes(5).toMillis(),
            Duration.ofMinutes(15).toMillis()
    };
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

    /** 距离解除锁定还剩多少秒，用于把提示写准确。未锁定时返回 0。 */
    public long blockedSeconds(String username, String clientIp) {
        synchronized (monitor) {
            long remaining = Math.max(remainingMillis(userKey(username)), remainingMillis(ipKey(clientIp)));
            return remaining <= 0 ? 0 : (remaining + 999) / 1000;
        }
    }

    private long remainingMillis(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return 0;
        }
        return Math.max(0, attempt.blockedUntil - System.currentTimeMillis());
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
            attempt.lockCount = 0;
        }
        attempt.failures++;
        attempt.lastFailureAt = now;
        if (attempt.failures >= maxAttempts) {
            long lock = LOCK_BACKOFF_MILLIS[Math.min(attempt.lockCount, LOCK_BACKOFF_MILLIS.length - 1)];
            attempt.lockCount++;
            attempt.blockedUntil = now + lock;
        }
    }

    private void cleanupExpired() {
        attempts.entrySet().removeIf(entry -> isStale(entry.getValue()));
    }

    /**
     * 记录是否已经可以丢弃。
     *
     * <p>按"最后一次活动时间 + 窗口"判断，而不是"锁定一结束就丢弃"：递增退避需要跨轮次
     * 记住已经触发过几次锁定，过早清掉记录会让退避永远停在第一档。窗口仍是 15 分钟，
     * 随机用户名撞库产生的 key 依然会被回收。</p>
     */
    private boolean isStale(Attempt attempt) {
        return System.currentTimeMillis() - lastActivity(attempt) > WINDOW_MILLIS;
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
        /** 已经触发过几次锁定，用于递增退避 */
        private int lockCount;
    }
}
