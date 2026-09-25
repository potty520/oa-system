package com.oa.config;

import com.oa.common.constant.Constants;
import com.oa.security.utils.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

/**
 * 初始账号口令初始化。
 *
 * <p>数据库脚本里不再内置任何可用口令，初始账号的 password 是占位符
 * {@link Constants#PENDING_PASSWORD}，任何密码都无法与之匹配。应用启动时本类负责把这些账号
 * 落地成真实口令：优先读取环境变量 {@code OA_INIT_<用户名大写>_PASSWORD}（例如
 * {@code OA_INIT_ADMIN_PASSWORD}）；未配置时生成一次性随机密码，只在本次启动日志里输出一次。
 * 无论口令来源如何，账号都会被标记为首次登录必须修改密码。</p>
 */
@Order(2)
@Component
public class InitialAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAccountInitializer.class);

    private static final String ENV_PREFIX = "OA_INIT_";
    private static final String ENV_SUFFIX = "_PASSWORD";
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALPHABET = LETTERS + DIGITS;
    private static final int GENERATED_LENGTH = 16;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public InitialAccountInitializer(DataSource dataSource, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> usernames;
        try {
            usernames = jdbcTemplate.queryForList(
                    "SELECT username FROM sys_user WHERE password = ? ORDER BY user_id",
                    String.class, Constants.PENDING_PASSWORD);
        } catch (RuntimeException e) {
            log.error("读取待初始化的账号失败，请检查 sys_user 表结构", e);
            return;
        }
        for (String username : usernames) {
            initialize(username);
        }
    }

    private void initialize(String username) {
        String envKey = envKeyFor(username);
        String configured = System.getenv(envKey);
        boolean generated = configured == null || configured.trim().isEmpty();
        String password = generated ? generatePassword() : configured;
        String violation = PasswordPolicy.validate(password);
        if (violation != null) {
            log.error("初始账号 {} 的环境变量 {} 口令不合规（{}），改为生成一次性随机密码", username, envKey, violation);
            password = generatePassword();
            generated = true;
        }
        int rows = jdbcTemplate.update(
                "UPDATE sys_user SET password = ?, must_change_pwd = ? WHERE username = ? AND password = ?",
                passwordEncoder.encode(password), Constants.MUST_CHANGE_PWD_YES, username, Constants.PENDING_PASSWORD);
        if (rows == 0) {
            return;
        }
        if (generated) {
            log.warn("初始账号 {} 未配置环境变量 {}，已生成一次性初始密码：{}（仅此一次输出，登录后必须修改）",
                    username, envKey, password);
        } else {
            log.info("初始账号 {} 已按环境变量 {} 完成初始化，登录后必须修改密码", username, envKey);
        }
    }

    static String envKeyFor(String username) {
        String normalized = username.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
        return ENV_PREFIX + normalized + ENV_SUFFIX;
    }

    /** 去掉易混淆字符，保证同时含字母与数字，满足 {@link PasswordPolicy}。 */
    private String generatePassword() {
        char[] buffer = new char[GENERATED_LENGTH];
        for (int index = 0; index < buffer.length; index++) {
            buffer[index] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        buffer[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
        buffer[buffer.length - 1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        return new String(buffer);
    }
}
