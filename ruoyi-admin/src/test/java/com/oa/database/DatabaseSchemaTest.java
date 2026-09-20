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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSchemaTest {

    @Test
    void allDialectScriptsShouldBePackaged() throws Exception {
        String[] dialects = {"mysql", "mariadb", "postgresql", "sqlite", "h2", "sqlserver", "oracle", "dm", "kingbase"};
        for (String dialect : dialects) {
            ClassPathResource resource = new ClassPathResource("db/" + dialect + "/schema.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("CREATE TABLE"), dialect + " is missing CREATE TABLE");
            assertTrue(sql.contains("sys_user"), dialect + " is missing sys_user");
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

    @Test
    void sqliteSchemaShouldInitializeAndSeedAdmin() throws Exception {
        Path database = Files.createTempFile("oa-schema-", ".db");
        try {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.sqlite.JDBC");
            dataSource.setUrl("jdbc:sqlite:" + database.toAbsolutePath().toString().replace('\\', '/') +
                    "?date_class=text&date_string_format=yyyy-MM-dd HH:mm:ss");
            initialize(dataSource, "db/sqlite/schema.sql");
            assertEquals(3, new JdbcTemplate(dataSource).queryForObject("select count(*) from sys_user", Integer.class));
        } finally {
            Files.deleteIfExists(database);
        }
    }

    private void initializeH2(String databaseName, String mode, String schemaLocation) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + databaseName + ";" + mode + ";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        initialize(dataSource, schemaLocation);
        assertEquals(3, new JdbcTemplate(dataSource).queryForObject("select count(*) from sys_user", Integer.class));
    }

    private void initialize(DataSource dataSource, String schemaLocation) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(schemaLocation));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}