package com.oa.config;

import com.oa.common.constant.Constants;
import com.oa.security.utils.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * 初始账号口令初始化。
 *
 * <p>数据库脚本里不再内置任何可用口令，初始账号的 password 是占位符
 * {@link Constants#PENDING_PASSWORD}，任何密码都无法与之匹配。应用启动时本类负责把这些账号
 * 落地成真实口令：优先读取环境变量 {@code OA_INIT_<用户名大写>_PASSWORD}（例如
 * {@code OA_INIT_ADMIN_PASSWORD}）；未配置时生成一次性随机密码。</p>
 *
 * <p>生成的一次性口令绝不写入日志：日志会被集中收集并长期保留，任何能看日志的人都等于拿到
 * 一个可用凭据。生成的密码只写入服务器本地文件（{@code app.init-password-file}），日志里只出现
 * 文件路径和"用后立即删除"的提示。该文件不是任何 HTTP 资源的映射目录，也没有任何接口可以读取
 * 它，只能通过服务器本机文件系统访问。口令来自环境变量时为了不在磁盘上多留一份副本，不写文件
 * （来源方本来就知道口令）。无论口令来源如何，账号都会被标记为首次登录必须修改密码。</p>
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
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 生成的一次性初始口令的落地文件，默认写在进程工作目录下 */
    @Value("${app.init-password-file:./oa-init-passwords.txt}")
    private String initPasswordFile;

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
        List<String> generatedEntries = new ArrayList<>();
        for (String username : usernames) {
            String entry = initialize(username);
            if (entry != null) {
                generatedEntries.add(entry);
            }
        }
        if (!generatedEntries.isEmpty()) {
            writeGeneratedPasswords(generatedEntries);
        }
    }

    /**
     * 初始化单个账号。
     *
     * @return 生成的一次性口令对应的 {@code 用户名=口令} 行（交给 {@link #writeGeneratedPasswords} 落盘）；
     *         口令来自环境变量、或账号已被其它实例初始化过时返回 {@code null}
     */
    private String initialize(String username) {
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
            return null;
        }
        if (!generated) {
            // 口令由环境变量提供，来源方已经掌握口令，磁盘上没必要再留副本
            log.info("初始账号 {} 已按环境变量 {} 完成初始化，登录后必须修改密码", username, envKey);
            return null;
        }
        return username + "=" + password;
    }

    /**
     * 把生成的一次性口令写入本地文件，日志里只记录文件路径和删除提示，绝不记录口令本身。
     *
     * <p>权限按平台收紧：Linux/macOS 等 POSIX 文件系统设为 {@code rw-------}（仅属主可读写）；
     * Windows 没有 POSIX 权限位，改用 ACL 只给属主授权（但系统管理员仍可能绕过，所以同样要求
     * 用后立即删除）；两种方式都不可用时只打印告警，要求管理员手工限制访问权限。</p>
     */
    private void writeGeneratedPasswords(List<String> entries) {
        Path file = Paths.get(initPasswordFile).toAbsolutePath().normalize();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder content = new StringBuilder();
            content.append("# OA 初始账号一次性口令，首次登录后必须修改密码\n");
            content.append("# 生成时间：").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append('\n');
            content.append("# 请立即保存到密码管理器，然后删除本文件\n");
            for (String entry : entries) {
                content.append(entry).append('\n');
            }
            // 追加写入：多次启动产生的口令都不会丢，也避免覆盖上一次尚未取走的口令
            Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("写入初始口令文件失败（{}），本次生成的口令没有其它留存位置："
                            + "请把相关账号的 password 重置为占位符 {} 后重启应用，"
                            + "或改用环境变量 OA_INIT_<用户名大写>_PASSWORD 提供口令",
                    file, Constants.PENDING_PASSWORD, e);
            return;
        }
        restrictToOwner(file);
        log.warn("已生成 {} 个初始账号的一次性口令并写入本地文件 {}：日志中不会出现口令本身，"
                        + "请立即取走内容并在使用后删除该文件",
                entries.size(), file);
    }

    /** 尽可能把文件权限收紧成"只有属主可读写"，失败时只告警，绝不把口令退回日志。 */
    private void restrictToOwner(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            return;
        } catch (UnsupportedOperationException e) {
            // Windows 等没有 POSIX 权限位的文件系统，继续尝试 ACL
        } catch (IOException e) {
            log.warn("设置初始口令文件权限失败（POSIX），请手工限制 {} 的访问权限，用完立即删除", file, e);
            return;
        }
        try {
            AclFileAttributeView view = Files.getFileAttributeView(file, AclFileAttributeView.class);
            if (view == null) {
                log.warn("当前平台不支持设置文件权限，请手工限制 {} 的访问权限，用完立即删除", file);
                return;
            }
            UserPrincipal owner = view.getOwner();
            AclEntry ownerOnly = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(owner)
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            view.setAcl(java.util.Collections.singletonList(ownerOnly));
        } catch (IOException e) {
            log.warn("设置初始口令文件权限失败（ACL），请手工限制 {} 的访问权限，用完立即删除", file, e);
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
