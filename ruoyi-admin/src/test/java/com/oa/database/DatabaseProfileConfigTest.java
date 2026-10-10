package com.oa.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseProfileConfigTest {

    /** 脚本自带重复执行守卫（IF NOT EXISTS / IF OBJECT_ID）的方言。 */
    private static final String[] GUARDED_DIALECTS = {
            "mysql", "mariadb", "postgresql", "sqlite", "h2", "sqlserver", "kingbase"};

    /** 没有任何守卫可用、只能靠容忍错误跑完的方言。 */
    private static final String[] TOLERANT_DIALECTS = {"oracle", "dm"};

    @Test
    void allDatabaseProfilesShouldBeValidYaml() {
        String[] profiles = {"mysql", "mariadb", "postgresql", "sqlite", "h2", "sqlserver", "oracle", "dm", "kingbase"};
        for (String profile : profiles) {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(new ClassPathResource("application-" + profile + ".yml"));
            Properties properties = factory.getObject();
            assertNotNull(properties, profile + " profile did not parse");
            assertTrue(properties.containsKey("spring.datasource.url"), profile + " is missing spring.datasource.url");
            assertTrue(properties.containsKey("spring.sql.init.schema-locations"), profile + " is missing schema location");
        }
    }

    /**
     * 初始化脚本每次启动都会重跑（mode 默认 always）。带守卫的方言必须让错误直接暴露，
     * 否则一个半成品 schema 会静默地绿着启动。
     */
    @Test
    void guardedDialectsShouldNotTolerateInitErrors() throws Exception {
        for (String dialect : GUARDED_DIALECTS) {
            assertFalse(continueOnError(dialect), dialect + " 的脚本自带重复执行守卫，不应再容忍初始化错误");
            String sql = readScript(dialect);
            boolean sqlServer = "sqlserver".equals(dialect);
            assertEquals(countOccurrences(sql, "CREATE TABLE"),
                    countOccurrences(sql, sqlServer ? "IF OBJECT_ID(" : "CREATE TABLE IF NOT EXISTS"),
                    dialect + " 存在没有 IF NOT EXISTS/IF OBJECT_ID 守卫的建表语句");
            assertEquals(countOccurrences(sql, "CREATE INDEX"),
                    countOccurrences(sql, sqlServer
                            ? "IF NOT EXISTS (SELECT 1 FROM sys.indexes" : "CREATE INDEX IF NOT EXISTS"),
                    dialect + " 存在没有守卫的建索引语句");
        }
    }

    /**
     * Oracle/DM 的 DDL 没有 IF NOT EXISTS / IF OBJECT_ID 这种守卫写法（脚本里 28 条 CREATE TABLE、
     * 22 条 CREATE INDEX 都是无条件的），第二次启动必然报"对象已存在"。
     * 因此这两个 profile 只能保留 continue-on-error: true；改成 false 会直接让重复启动失败。
     * 代价是真正的错误也被吞掉，所以这里同时钉住"脚本里没有破坏性语句"这条底线。
     */
    @Test
    void unguardedOracleAndDmScriptsMustTolerateExistingObjects() throws Exception {
        for (String dialect : TOLERANT_DIALECTS) {
            assertTrue(continueOnError(dialect), dialect + " 的脚本没有重复执行守卫，必须容忍\"对象已存在\"错误");
            String sql = readScript(dialect);
            assertFalse(sql.contains("IF NOT EXISTS"), dialect + " 不应依赖 Oracle/DM 不支持的 IF NOT EXISTS 语法");
            assertFalse(Pattern.compile("(?i)\\b(DROP|TRUNCATE|DELETE)\\b").matcher(sql).find(),
                    dialect + " 的脚本不应包含破坏性语句");
        }
    }

    private boolean continueOnError(String profile) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-" + profile + ".yml"));
        Properties properties = factory.getObject();
        assertNotNull(properties, profile + " profile did not parse");
        return Boolean.parseBoolean(String.valueOf(
                properties.getProperty("spring.sql.init.continue-on-error", "false")));
    }

    private String readScript(String dialect) throws Exception {
        return StreamUtils.copyToString(new ClassPathResource("db/" + dialect + "/schema.sql").getInputStream(),
                StandardCharsets.UTF_8);
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
