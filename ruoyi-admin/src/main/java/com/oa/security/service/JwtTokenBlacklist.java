package com.oa.security.service;

import com.oa.system.mapper.SysTokenRevocationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 吊销表。
 *
 * <p>JWT 是无状态的，签发后在有效期内一直可用。退出登录按单个 Token 吊销；
 * 改密 / 重置密码 / 停用账号则按用户吊销，让该用户此前拿到的所有 Token 立刻失效。</p>
 *
 * <p>吊销记录持久化在 sys_token_revocation：Token 级键为 {@code token:<sha256(raw token)>}，
 * 用户级键为 {@code user:<用户名>}。落库后重启不再丢失，多实例部署也共用同一份判定结果
 * （同一个库），不再依赖单实例内存。</p>
 *
 * <p>读路径在过滤器里每个已认证请求都会走一次，因此只对“未吊销”的结论做短 TTL 负缓存
 * （{@link #NEGATIVE_CACHE_TTL_MILLIS} 毫秒），命中缓存时不查库。含义是：本实例执行吊销时
 * 会主动清掉对应键、立即生效；其他实例最长要等一个负缓存周期才看到新吊销记录。</p>
 */
@Service
public class JwtTokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenBlacklist.class);

    /** 单 Token 吊销键前缀，完整键形如 token:<sha256 hex> */
    private static final String TOKEN_KEY_PREFIX = "token:";

    /** 用户级吊销键前缀，完整键形如 user:<用户名小写> */
    private static final String USER_KEY_PREFIX = "user:";

    /**
     * 负缓存（只缓存“未吊销”这一结论）的有效期：3 秒。
     *
     * <p>这就是判定结果的最大陈旧窗口——多实例部署时，别的实例写入的吊销记录最多 3 秒后生效；
     * 本实例调用 {@link #revoke} / {@link #revokeUser} 时会直接清除对应键，立刻生效。</p>
     */
    private static final long NEGATIVE_CACHE_TTL_MILLIS = 3000L;

    /** 负缓存容量上限，超出后放弃缓存退化成每次查库，保证内存有界 */
    private static final int MAX_NEGATIVE_CACHE_ENTRIES = 10000;

    /** 过期记录清理的时间片：10 分钟最多清理一次，不给每个请求都挂一条 DELETE */
    private static final long PRUNE_INTERVAL_MILLIS = 10 * 60 * 1000L;

    /** 键 = token:<sha256> 或 user:<用户名>，值 = 该“未吊销”结论的有效期（毫秒时间戳） */
    private final Map<String, Long> negativeCache = new ConcurrentHashMap<>();

    /** 上次清理过期吊销记录的时间，用于把清理限流到 {@link #PRUNE_INTERVAL_MILLIS} */
    private final AtomicLong lastPruneAt = new AtomicLong(0L);

    @Autowired
    private SysTokenRevocationMapper tokenRevocationMapper;

    /** Token 有效期（毫秒）：用户级吊销记录超过这个时长就不可能再命中任何有效 Token */
    @Value("${jwt.expiration:86400000}")
    private long tokenTtlMillis;

    public void revoke(String token, long expiresAt) {
        if (token == null || token.trim().isEmpty() || expiresAt <= System.currentTimeMillis()) {
            return;
        }
        String key = TOKEN_KEY_PREFIX + sha256Hex(token);
        // 本实例内先清掉负缓存，退出登录后旧 Token 立刻不可用
        negativeCache.remove(key);
        long now = System.currentTimeMillis();
        try {
            // 表上只有主键，跨方言没有统一的 upsert 语法（MERGE / ON CONFLICT / ON DUPLICATE KEY 各不相同），
            // 所以先删后插；两条语句各自提交，避免这里的失败把调用方（改密等）的事务一起拖垮
            tokenRevocationMapper.deleteByKey(key);
            tokenRevocationMapper.insert(key, new Date(now), new Date(expiresAt));
        } catch (Exception e) {
            log.warn("写入单 Token 吊销记录失败，该 Token 在自身过期前可能仍然可用，key={}", key, e);
        }
    }

    public boolean isRevoked(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String key = TOKEN_KEY_PREFIX + sha256Hex(token);
        long now = System.currentTimeMillis();
        pruneExpiredIfDue(now);
        if (hasFreshNegative(key, now)) {
            return false;
        }
        try {
            Date expireTime = tokenRevocationMapper.selectLiveExpireTime(key, new Date(now));
            if (expireTime != null) {
                return true;
            }
        } catch (Exception e) {
            // 读库失败时退回缓存：缓存里只存“未吊销”，所以本次结论是不吊销，不误杀正常请求。
            // 紧接着过滤器还会用同一个库加载用户（LoginUserCache -> selectUserByUsername），
            // 真宕机时那一步必然失败、认证信息不会被置入 SecurityContext，请求照样进不来，不存在放行。
            log.warn("查询单 Token 吊销记录失败，本次按未吊销处理，key={}", key, e);
            return false;
        }
        cacheNegative(key, now);
        return false;
    }

    /** 作废该用户此前签发的全部 Token。 */
    public void revokeUser(String username) {
        String name = normalize(username);
        if (name == null) {
            return;
        }
        String key = USER_KEY_PREFIX + name;
        // 本实例内先清掉负缓存，改密/停用后旧 Token 立刻不可用
        negativeCache.remove(key);
        // JWT 的 iat 只精确到秒，这里向下取整，避免把同一秒内重新登录签发的 Token 一起废掉
        long revokedAt = System.currentTimeMillis() / 1000L * 1000L;
        try {
            // 用户级只需要保留“最近一次”吊销时间，先删后插让同一用户名始终只有一行
            tokenRevocationMapper.deleteByKey(key);
            tokenRevocationMapper.insert(key, new Date(revokedAt), new Date(revokedAt + tokenTtlMillis));
        } catch (Exception e) {
            log.warn("写入用户级吊销记录失败，该用户已签发的 Token 在过期前可能仍然可用，key={}", key, e);
        }
    }

    /** 该 Token 是否签发于用户级吊销点之前。缺少 iat 的 Token 一律视为已吊销。 */
    public boolean isUserRevoked(String username, Date issuedAt) {
        String name = normalize(username);
        if (name == null) {
            return false;
        }
        String key = USER_KEY_PREFIX + name;
        long now = System.currentTimeMillis();
        pruneExpiredIfDue(now);
        // 缺少 iat 的 Token 一律视为已吊销，这个兜底判断依赖吊销时间本身，
        // 所以 issuedAt 为 null 时不看负缓存，直接查库，避免被缓存窗口架空
        if (issuedAt != null && hasFreshNegative(key, now)) {
            return false;
        }
        Date revokedAt;
        try {
            revokedAt = tokenRevocationMapper.selectLiveRevokedAt(key, new Date(now));
        } catch (Exception e) {
            // 同 isRevoked：读库失败按缓存结论（未吊销）处理，真正的库故障会在随后加载用户时暴露
            log.warn("查询用户级吊销记录失败，本次按未吊销处理，key={}", key, e);
            return false;
        }
        if (revokedAt == null) {
            cacheNegative(key, now);
            return false;
        }
        if (issuedAt == null) {
            return true;
        }
        boolean revoked = issuedAt.getTime() < revokedAt.getTime();
        if (!revoked) {
            // 吊销之后重新登录签发的 Token：结论同样是“未吊销”，一并缓存，
            // 否则改密后在 TTL 内该用户的每个请求都要查一次库
            cacheNegative(key, now);
        }
        return revoked;
    }

    /** 命中“未吊销”负缓存返回 true；顺带清掉已过期的条目 */
    private boolean hasFreshNegative(String key, long now) {
        Long until = negativeCache.get(key);
        if (until == null) {
            return false;
        }
        if (until <= now) {
            negativeCache.remove(key, until);
            return false;
        }
        return true;
    }

    private void cacheNegative(String key, long now) {
        if (negativeCache.size() >= MAX_NEGATIVE_CACHE_ENTRIES) {
            negativeCache.entrySet().removeIf(entry -> entry.getValue() <= now);
            if (negativeCache.size() >= MAX_NEGATIVE_CACHE_ENTRIES) {
                // 打满说明 3 秒内在途 Token 超过上限：整体清空重来，
                // 只让随后一小段时间的判定回落到查库，正确性不受影响
                negativeCache.clear();
                log.warn("Token 吊销负缓存已达上限 {}，已整体清空", MAX_NEGATIVE_CACHE_ENTRIES);
            }
        }
        negativeCache.put(key, now + NEGATIVE_CACHE_TTL_MILLIS);
    }

    /**
     * 清理 expire_time 已过的吊销记录。
     *
     * <p>挂在读路径（{@link #isRevoked} / {@link #isUserRevoked}）上，即过滤器每请求都会经过的地方，
     * 但用 {@link #lastPruneAt} 限流到 {@link #PRUNE_INTERVAL_MILLIS} 最多一次，不会每个请求都删库；
     * 读路径在过滤器里、不在业务事务中，清理失败也不会污染任何业务事务。</p>
     */
    private void pruneExpiredIfDue(long now) {
        long last = lastPruneAt.get();
        if (now - last < PRUNE_INTERVAL_MILLIS || !lastPruneAt.compareAndSet(last, now)) {
            return;
        }
        try {
            int removed = tokenRevocationMapper.deleteExpired(new Date(now));
            if (removed > 0) {
                log.debug("清理过期 Token 吊销记录 {} 条", removed);
            }
        } catch (Exception e) {
            // 清理只是回收空间，失败不影响判定，等下一个时间片再试
            log.warn("清理过期 Token 吊销记录失败", e);
        }
    }

    /** 吊销键里只放摘要，不落原始 Token，避免库或备份泄露可直接使用的凭证 */
    private String sha256Hex(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0x0F, 16)).append(Character.forDigit(b & 0x0F, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JRE 必须提供的算法，走到这里说明运行环境异常
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    private String normalize(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }
}
