package com.oa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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

@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final DataSource dataSource;

    public DatabaseMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ensureTemplateDocTypeColumn(connection);
            seedDocumentTypes(connection);
        }
    }

    private void ensureTemplateDocTypeColumn(Connection connection) throws SQLException {
        if (tableHasColumn(connection, "oa_doc_template", "doc_type_id")) {
            return;
        }
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
        String sql;
        if (product.contains("oracle") || product.equals("dm dbms") || product.contains("dameng")) {
            sql = "ALTER TABLE oa_doc_template ADD (doc_type_id NUMBER(19))";
        } else if (product.contains("sql server")) {
            sql = "ALTER TABLE oa_doc_template ADD doc_type_id BIGINT";
        } else {
            sql = "ALTER TABLE oa_doc_template ADD COLUMN doc_type_id BIGINT";
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            log.info("Added oa_doc_template.doc_type_id column");
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
