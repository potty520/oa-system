package com.oa.database;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseDialectDetectionTest {

    @Test
    void mybatisShouldDetectEverySupportedDatabase() {
        assertEquals(DbType.MYSQL, JdbcUtils.getDbType("jdbc:mysql://localhost:3306/oa_system"));
        assertEquals(DbType.MARIADB, JdbcUtils.getDbType("jdbc:mariadb://localhost:3306/oa_system"));
        assertEquals(DbType.POSTGRE_SQL, JdbcUtils.getDbType("jdbc:postgresql://localhost:5432/oa_system"));
        assertEquals(DbType.SQLITE, JdbcUtils.getDbType("jdbc:sqlite:oa-system.db"));
        assertEquals(DbType.H2, JdbcUtils.getDbType("jdbc:h2:mem:oa_system"));
        DbType sqlServerType = JdbcUtils.getDbType("jdbc:sqlserver://localhost:1433;databaseName=oa_system");
        assertTrue(sqlServerType == DbType.SQL_SERVER || sqlServerType == DbType.SQL_SERVER2005);
        assertEquals(DbType.ORACLE, JdbcUtils.getDbType("jdbc:oracle:thin:@//localhost:1521/FREEPDB1"));
        assertEquals(DbType.DM, JdbcUtils.getDbType("jdbc:dm://localhost:5236/OA_SYSTEM"));
        assertEquals(DbType.KINGBASE_ES, JdbcUtils.getDbType("jdbc:kingbase8://localhost:54321/oa_system"));
    }
}