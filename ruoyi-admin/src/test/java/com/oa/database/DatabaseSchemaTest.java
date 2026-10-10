package com.oa.database;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSchemaTest {

    private static final String[] DIALECTS = {
            "mysql", "mariadb", "postgresql", "sqlite", "h2", "sqlserver", "oracle", "dm", "kingbase"};

    /** 初始化脚本只允许建对象、补种子数据，不允许 DROP/TRUNCATE/DELETE 破坏既有数据。 */
    private static final Pattern DESTRUCTIVE = Pattern.compile("(?i)\\b(DROP|TRUNCATE|DELETE)\\b");

    /** BCrypt 口令形如 $2a$10$...，种子里出现即说明内置了可用口令。 */
    private static final Pattern BCRYPT = Pattern.compile("\\$\\d[abxy]?\\$");

    @Test
    void allDialectScriptsShouldBePackaged() throws Exception {
        for (String dialect : DIALECTS) {
            String sql = readScript(dialect);
            assertTrue(sql.contains("CREATE TABLE"), dialect + " is missing CREATE TABLE");
            assertTrue(sql.contains("sys_user"), dialect + " is missing sys_user");
        }
    }

    /**
     * 9 个方言的脚本都必须只有占位口令账号：Oracle/SQL Server/DM 无法在测试里真正执行，
     * 之前只检查了 CREATE TABLE 与 sys_user 两个字符串，默认口令哈希能直接溜过去。
     */
    @Test
    void everyDialectScriptShouldSeedPendingPasswordAccountsOnly() throws Exception {
        for (String dialect : DIALECTS) {
            String sql = readScript(dialect);
            assertFalse(BCRYPT.matcher(sql).find(), dialect + " 的种子数据里出现了 BCrypt 口令");
            assertEquals(3, countOccurrences(sql, "PENDING_INITIAL_PASSWORD"),
                    dialect + " 应有 3 个占位口令账号");
            for (String account : new String[]{"admin", "zhangsan", "lisi"}) {
                assertTrue(sql.contains("'" + account + "'"), dialect + " 缺少种子账号 " + account);
            }
            assertTrue(sql.contains("must_change_pwd"), dialect + " 缺少首次登录强制改密标记");
        }
    }

    @Test
    void noDialectScriptShouldDropOrTruncateObjects() throws Exception {
        for (String dialect : DIALECTS) {
            assertFalse(DESTRUCTIVE.matcher(readScript(dialect)).find(),
                    dialect + " 的脚本包含 DROP/TRUNCATE/DELETE 等破坏性语句");
        }
    }

    @Test
    void mysqlAndMariaDbScriptsShouldInitializeInMysqlCompatibilityMode() {
        initializeH2("mysql_script_test", "MODE=MySQL", "db/mysql/schema.sql");
        initializeH2("mariadb_script_test", "MODE=MySQL", "db/mariadb/schema.sql");
    }

    @Test
    void postgresAndKingbaseScriptsShouldInitializeInPostgresCompatibilityMode() {
        initializeH2("postgres_script_test", "MODE=PostgreSQL", "db/postgresql/schema.sql");
        initializeH2("kingbase_script_test", "MODE=PostgreSQL", "db/kingbase/schema.sql");
    }

    @Test
    void h2SchemaShouldInitializeAndSeedAdmin() {
        initializeH2("oa_schema_test", "MODE=PostgreSQL", "db/h2/schema.sql");
    }

    /**
     * 种子数据用显式主键写入（sys_dept 100-103、sys_menu 1-214、oa_doc_type 1000-1007），
     * 自增主键的起点必须越过这些值，否则运行期第一条不带主键的插入就主键冲突。
     */
    @Test
    void seededIdentityShouldAdvancePastSeedIdsAtRuntime() {
        String[][] cases = {
                {"h2", "MODE=PostgreSQL", "db/h2/schema.sql"},
                {"postgresql", "MODE=PostgreSQL", "db/postgresql/schema.sql"},
                {"kingbase", "MODE=PostgreSQL", "db/kingbase/schema.sql"},
                {"mysql", "MODE=MySQL", "db/mysql/schema.sql"},
                {"mariadb", "MODE=MySQL", "db/mariadb/schema.sql"},
        };
        for (String[] testCase : cases) {
            JdbcTemplate jdbc = initializeH2Template("identity_" + testCase[0] + "_test", testCase[1], testCase[2]);
            assertRuntimeInsertAdvancesIdentity(jdbc, testCase[0]);
        }
    }

    @Test
    void sqliteSchemaShouldInitializeAndSeedAdmin() throws Exception {
        Path database = Files.createTempFile("oa-schema-", ".db");
        try {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.sqlite.JDBC");
            dataSource.setUrl("jdbc:sqlite:" + database.toAbsolutePath().toString().replace('\\', '/') +
                    "?date_class=text&date_string_format=yyyy-MM-dd HH:mm:ss");
            initialize(dataSource, "db/sqlite/schema.sql");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertSeededAccounts(jdbc);
            assertRuntimeInsertAdvancesIdentity(jdbc, "sqlite");
        } finally {
            Files.deleteIfExists(database);
        }
    }

    private void initializeH2(String databaseName, String mode, String schemaLocation) {
        assertSeededAccounts(initializeH2Template(databaseName, mode, schemaLocation));
    }

    private JdbcTemplate initializeH2Template(String databaseName, String mode, String schemaLocation) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + databaseName + ";" + mode + ";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        initialize(dataSource, schemaLocation);
        return new JdbcTemplate(dataSource);
    }

    /** 初始化脚本不得内置任何可用口令：账号一律是占位口令 + 首次登录必须改密。 */
    private void assertSeededAccounts(JdbcTemplate jdbc) {
        assertEquals(3, jdbc.queryForObject("select count(*) from sys_user", Integer.class));
        assertEquals(3, jdbc.queryForObject(
                "select count(*) from sys_user where password = 'PENDING_INITIAL_PASSWORD' and must_change_pwd = '1'",
                Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from sys_user where password like '$2%'", Integer.class));
    }

    private void assertRuntimeInsertAdvancesIdentity(JdbcTemplate jdbc, String dialect) {
        jdbc.update("INSERT INTO oa_doc_type (type_name, type_code, parent_id, sort_order, status) "
                + "VALUES ('运行期新增', ?, 0, 9, '0')", "RT" + dialect);
        jdbc.update("INSERT INTO sys_dept (parent_id, ancestors, dept_name, order_num, leader, phone, email, status) "
                + "VALUES (0, '0', '运行期部门', 9, 'x', '13800000009', 'x@example.com', '0')");
        assertEquals(1008, jdbc.queryForObject("select max(doc_type_id) from oa_doc_type", Integer.class),
                dialect + " 的公文类型自增主键未越过种子主键 1007");
        assertEquals(104, jdbc.queryForObject("select max(dept_id) from sys_dept", Integer.class),
                dialect + " 的部门自增主键未越过种子主键 103");
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

    private void initialize(DataSource dataSource, String schemaLocation) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(schemaLocation));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}
