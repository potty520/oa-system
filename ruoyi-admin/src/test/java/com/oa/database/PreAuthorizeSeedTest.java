package com.oa.database;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限字符串必须落在菜单种子（sys_menu.perms）里：@PreAuthorize 上写了种子里没有的权限，
 * 就等于该接口除超级管理员外永久不可用。这里不启动 Spring 上下文，只读源码与种子脚本。
 */
class PreAuthorizeSeedTest {

    private static final Pattern PRE_AUTHORIZE = Pattern.compile("@PreAuthorize\\s*\\(\\s*\"([^\"]*)\"\\s*\\)");
    private static final Pattern QUOTED = Pattern.compile("'([^']*)'");

    /** 超级管理员通配权限，不属于菜单种子里的具体权限。 */
    private static final String WILDCARD = "*:*:*";

    @Test
    void everyPreAuthorizePermissionShouldExistInTheMenuSeed() throws Exception {
        Set<String> seeded = seededPermissions();
        assertFalse(seeded.isEmpty(), "菜单种子里没有解析到任何权限字符串");

        Set<String> required = new TreeSet<>();
        int annotations = 0;
        for (Path source : javaSources()) {
            String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            Matcher matcher = PRE_AUTHORIZE.matcher(text);
            while (matcher.find()) {
                annotations++;
                Matcher quoted = QUOTED.matcher(matcher.group(1));
                while (quoted.find()) {
                    for (String candidate : quoted.group(1).split(",")) {
                        String permission = candidate.trim();
                        if (!permission.isEmpty() && !WILDCARD.equals(permission)) {
                            required.add(permission);
                        }
                    }
                }
            }
        }
        assertTrue(annotations >= 40, "只解析到 " + annotations + " 个 @PreAuthorize，源码扫描范围可能不对");
        assertTrue(required.size() >= 40, "只解析到 " + required.size() + " 个权限字符串");

        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(seeded);
        assertTrue(missing.isEmpty(), "以下 @PreAuthorize 权限在菜单种子里不存在: " + missing);
    }

    /** 菜单种子里的权限字符串，直接查初始化脚本建出来的 sys_menu。 */
    private Set<String> seededPermissions() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:preauthorize_seed_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/h2/schema.sql"));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
        return new TreeSet<>(new JdbcTemplate(dataSource).queryForList(
                "SELECT perms FROM sys_menu WHERE perms IS NOT NULL AND perms <> ''", String.class));
    }

    /** 源码目录：Surefire 以模块目录为工作目录，同时也兼容从仓库根目录运行。 */
    private List<Path> javaSources() throws Exception {
        Path root = null;
        for (String candidate : new String[]{"src/main/java", "../src/main/java", "ruoyi-admin/src/main/java"}) {
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) {
                root = path;
                break;
            }
        }
        assertNotNull(root, "找不到 src/main/java 源码目录");
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }
}
