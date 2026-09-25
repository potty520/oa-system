package com.oa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(1)
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    /** 历史版本内置的演示口令 admin123 的 BCrypt 值，属于公开已知弱口令。 */
    private static final String LEGACY_DEMO_PASSWORD_HASH =
            "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

    private final DataSource dataSource;

    public DatabaseMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ensureColumn(connection, "sys_user", "must_change_pwd", "CHAR(1)");
            migrateMustChangePwd(connection);
            ensureColumn(connection, "oa_doc_template", "doc_type_id", "BIGINT");
            ensureColumn(connection, "oa_incoming_doc", "flow_id", "BIGINT");
            ensureColumn(connection, "oa_incoming_doc", "current_step_id", "BIGINT");
            ensureColumn(connection, "oa_incoming_doc", "current_step_order", "INTEGER");
            ensureColumn(connection, "oa_outgoing_doc", "flow_id", "BIGINT");
            ensureColumn(connection, "oa_outgoing_doc", "current_step_id", "BIGINT");
            ensureColumn(connection, "oa_outgoing_doc", "current_step_order", "INTEGER");
            seedDocumentTypes(connection);
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String type) throws SQLException {
        if (tableHasColumn(connection, tableName, columnName)) {
            return;
        }
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
        String databaseType = "BIGINT".equals(type) && (product.contains("oracle") || product.equals("dm dbms") || product.contains("dameng"))
                ? "NUMBER(19)"
                : ("INTEGER".equals(type) && (product.contains("oracle") || product.equals("dm dbms") || product.contains("dameng"))
                ? "NUMBER(10)" : type);
        String sql;
        if (product.contains("oracle") || product.equals("dm dbms") || product.contains("dameng")) {
            sql = "ALTER TABLE " + tableName + " ADD (" + columnName + " " + databaseType + ")";
        } else if (product.contains("sql server")) {
            sql = "ALTER TABLE " + tableName + " ADD " + columnName + " " + databaseType;
        } else {
            sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + databaseType;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            log.info("Added {}.{} column", tableName, columnName);
        }
    }

    /**
     * 补齐老库的 must_change_pwd 取值：新增列后历史行为 NULL，统一回填 '0'；
     * 仍在使用内置演示口令 admin123 的账号则强制首次登录改密。
     */
    private void migrateMustChangePwd(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE sys_user SET must_change_pwd = '0' WHERE must_change_pwd IS NULL");
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE sys_user SET must_change_pwd = '1' WHERE password = ?")) {
            update.setString(1, LEGACY_DEMO_PASSWORD_HASH);
            int rows = update.executeUpdate();
            if (rows > 0) {
                log.warn("检测到 {} 个账号仍在使用内置演示口令，已标记为首次登录必须修改密码", rows);
            }
        }
    }

    private boolean tableHasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE 1 = 0")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int index = 1; index <= metaData.getColumnCount(); index++) {
                if (columnName.equalsIgnoreCase(metaData.getColumnName(index))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void seedDocumentTypes(Connection connection) throws SQLException {
        Map<String, Object[]> types = new LinkedHashMap<>();
        types.put("TZ", new Object[]{"通知", 1});
        types.put("TB", new Object[]{"通报", 2});
        types.put("JD", new Object[]{"决定", 3});
        types.put("BG", new Object[]{"报告", 4});
        types.put("QS", new Object[]{"请示", 5});
        types.put("PF", new Object[]{"批复", 6});
        types.put("HAN", new Object[]{"函", 7});
        types.put("JY", new Object[]{"纪要", 8});

        try (Statement countStatement = connection.createStatement();
             ResultSet countResult = countStatement.executeQuery("SELECT COUNT(*) FROM oa_doc_type")) {
            if (countResult.next() && countResult.getInt(1) > 0) {
                return;
            }
        }
        String insertSql = "INSERT INTO oa_doc_type(type_name, type_code, parent_id, sort_order, status) VALUES(?, ?, 0, ?, '0')";
        for (Map.Entry<String, Object[]> entry : types.entrySet()) {
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setString(1, (String) entry.getValue()[0]);
                insert.setString(2, entry.getKey());
                insert.setInt(3, (Integer) entry.getValue()[1]);
                insert.executeUpdate();
            }
        }
    }
}
