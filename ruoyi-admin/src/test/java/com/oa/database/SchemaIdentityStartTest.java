package com.oa.database;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缺陷 1 的回归测试：种子数据用显式主键写入，各数据库自增主键（身份列/序列）的起点必须严格大于
 * 该表种子里的主键最大值，否则运行期第一条不带主键的插入就会主键冲突。
 *
 * <p>这里只解析生成的 schema.sql 文本：Oracle/SQL Server/DM 无法在测试里真正执行，
 * 而 PostgreSQL/Kingbase 的起点由脚本末尾的 RESTART 语句决定，必须一并解析。</p>
 */
class SchemaIdentityStartTest {

    private static final List<String> DIALECTS = Arrays.asList(
            "mysql", "mariadb", "postgresql", "sqlite", "h2", "sqlserver", "oracle", "dm", "kingbase");

    /** 既有种子数据、又有自增主键的表；这个集合变化说明种子改了，需要同步复核期望值。 */
    private static final TreeSet<String> SEEDED_IDENTITY_TABLES = new TreeSet<>(Arrays.asList(
            "sys_dept", "sys_user", "sys_role", "sys_menu", "sys_post", "oa_doc_type"));

    private static final String SEED_MARKER = "-- System seed data";

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)^\\s*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?\\s*\\(");
    private static final Pattern IDENTITY_WITH_START = Pattern.compile(
            "(?i)^\\s*`?(\\w+)`?\\s+.*?\\bGENERATED\\s+BY\\s+DEFAULT\\s+AS\\s+IDENTITY\\s*\\(\\s*START\\s+WITH\\s+(\\d+)\\s*\\)");
    private static final Pattern IDENTITY_COMPACT = Pattern.compile(
            "(?i)^\\s*`?(\\w+)`?\\s+.*?\\bIDENTITY\\s*\\(\\s*(\\d+)\\s*,\\s*\\d+\\s*\\)");
    private static final Pattern SQLITE_AUTOINCREMENT = Pattern.compile(
            "(?i)^\\s*`?(\\w+)`?\\s+.*\\bAUTOINCREMENT\\b");
    private static final Pattern MYSQL_TABLE_OPTION = Pattern.compile("(?i)AUTO_INCREMENT\\s*=\\s*(\\d+)");
    private static final Pattern MYSQL_COLUMN = Pattern.compile(
            "(?i)^\\s*`?(\\w+)`?\\s+[A-Z]+\\(?[\\d,]*\\)?.*\\bAUTO_INCREMENT\\b");
    private static final Pattern RESTART_WITH = Pattern.compile(
            "(?i)^\\s*ALTER\\s+TABLE\\s+`?(\\w+)`?\\s+ALTER\\s+COLUMN\\s+`?(\\w+)`?\\s+RESTART\\s+WITH\\s+(\\d+)\\s*;");

    @Test
    void identityStartsShouldExceedSeededPrimaryKeysInEveryDialect() throws Exception {
        Map<String, Map<String, Integer>> seededByDialect = new LinkedHashMap<>();
        for (String dialect : DIALECTS) {
            String sql = readScript(dialect);
            IdentityInfo identities = parseIdentities(sql);
            assertFalse(identities.columns.isEmpty(), dialect + " 未解析到任何自增主键列");

            Map<String, Integer> seeded = parseSeededMaxIds(sql, dialect, identities.columns);
            assertEquals(SEEDED_IDENTITY_TABLES, new TreeSet<>(seeded.keySet()),
                    dialect + " 解析到的带种子数据的自增表与预期不一致");

            for (Map.Entry<String, Integer> entry : seeded.entrySet()) {
                String table = entry.getKey();
                Integer start = identities.starts.get(table);
                if (start == null) {
                    // SQLite 的 AUTOINCREMENT 天生取 MAX(rowid)+1，脚本里没有起点可写
                    assertEquals("sqlite", dialect, dialect + " 表 " + table + " 未解析到身份列起点");
                    continue;
                }
                assertTrue(start > entry.getValue(), dialect + " 表 " + table + " 的身份列起点 " + start
                        + " 未越过种子主键最大值 " + entry.getValue());
            }

            if ("sqlite".equals(dialect)) {
                assertTrue(sql.contains("INTEGER PRIMARY KEY AUTOINCREMENT"), "sqlite 缺少 AUTOINCREMENT 主键");
            } else {
                assertEquals(new TreeSet<>(identities.columns.keySet()), new TreeSet<>(identities.starts.keySet()),
                        dialect + " 存在没有起点的自增主键列");
            }

            if ("postgresql".equals(dialect) || "kingbase".equals(dialect)) {
                assertEquals(new TreeSet<>(identities.columns.keySet()), new TreeSet<>(identities.restartedColumns.keySet()),
                        dialect + " 的自增主键未在种子数据之后全部 RESTART");
                for (Map.Entry<String, String> entry : identities.restartedColumns.entrySet()) {
                    assertEquals(identities.columns.get(entry.getKey()), entry.getValue(),
                            dialect + " 表 " + entry.getKey() + " 的 RESTART 列与自增列不一致");
                }
            }
            seededByDialect.put(dialect, seeded);
        }

        // 同一份种子在 9 个方言里必须解析出相同的主键最大值，否则说明有方言漏了种子数据
        Map<String, Integer> reference = seededByDialect.get("mysql");
        for (String dialect : DIALECTS) {
            assertEquals(reference, seededByDialect.get(dialect),
                    dialect + " 解析到的种子主键最大值与其他方言不一致");
        }
    }

    /** 逐行扫描：CREATE TABLE 之后出现的自增列关键字即为该表的自增主键。 */
    private IdentityInfo parseIdentities(String sql) {
        IdentityInfo info = new IdentityInfo();
        String table = null;
        for (String line : sql.split("\n")) {
            Matcher create = CREATE_TABLE.matcher(line);
            if (create.find()) {
                table = create.group(1).toLowerCase();
                continue;
            }
            if (table == null) {
                continue;
            }
            Matcher restart = RESTART_WITH.matcher(line);
            if (restart.find()) {
                // 种子数据之后的 RESTART 才是 PostgreSQL/Kingbase 真正生效的起点
                info.starts.put(restart.group(1).toLowerCase(), Integer.valueOf(restart.group(3)));
                info.restartedColumns.put(restart.group(1).toLowerCase(), restart.group(2));
                continue;
            }
            Matcher withStart = IDENTITY_WITH_START.matcher(line);
            if (withStart.find()) {
                info.columns.put(table, withStart.group(1));
                info.starts.put(table, Integer.valueOf(withStart.group(2)));
                continue;
            }
            Matcher compact = IDENTITY_COMPACT.matcher(line);
            if (compact.find()) {
                info.columns.put(table, compact.group(1));
                info.starts.put(table, Integer.valueOf(compact.group(2)));
                continue;
            }
            Matcher sqlite = SQLITE_AUTOINCREMENT.matcher(line);
            if (sqlite.find()) {
                info.columns.put(table, sqlite.group(1));
                continue;
            }
            Matcher mysqlOption = MYSQL_TABLE_OPTION.matcher(line);
            if (mysqlOption.find()) {
                info.starts.put(table, Integer.valueOf(mysqlOption.group(1)));
                continue;
            }
            Matcher mysqlColumn = MYSQL_COLUMN.matcher(line);
            if (mysqlColumn.find()) {
                info.columns.put(table, mysqlColumn.group(1));
            }
        }
        return info;
    }

    /** 从种子数据段里取出每张自增表写入的最大主键。 */
    private Map<String, Integer> parseSeededMaxIds(String sql, String dialect, Map<String, String> identityColumns) {
        int marker = sql.indexOf(SEED_MARKER);
        assertTrue(marker >= 0, dialect + " 脚本里找不到 " + SEED_MARKER + " 分隔行");
        String seeds = sql.substring(marker);
        boolean mergeIntoUsing = "oracle".equals(dialect) || "dm".equals(dialect);

        Map<String, Integer> maxIds = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : identityColumns.entrySet()) {
            String table = entry.getKey();
            String keyColumn = entry.getValue();
            List<Integer> ids = new ArrayList<>();
            if (mergeIntoUsing) {
                Matcher statements = Pattern.compile("(?is)MERGE INTO " + table + " t .*?;").matcher(seeds);
                while (statements.find()) {
                    Matcher values = Pattern.compile("(?i)(\\d+)\\s+AS\\s+" + keyColumn + "\\b")
                            .matcher(statements.group());
                    while (values.find()) {
                        ids.add(Integer.valueOf(values.group(1)));
                    }
                }
            } else {
                Matcher statements = Pattern.compile(
                        "(?is)(?:MERGE INTO|INSERT(?: IGNORE)? INTO|INSERT OR IGNORE INTO)\\s+`?"
                                + table + "`?\\s*\\(([^)]*)\\)([^;]*);").matcher(seeds);
                while (statements.find()) {
                    List<String> columns = normalize(splitList(statements.group(1)));
                    int index = columns.indexOf(keyColumn);
                    if (index < 0) {
                        continue;
                    }
                    for (String tuple : valueTuples(statements.group(2))) {
                        List<String> values = splitList(tuple);
                        if (index < values.size()) {
                            ids.add(Integer.valueOf(values.get(index).trim()));
                        }
                    }
                }
            }
            if (!ids.isEmpty()) {
                maxIds.put(table, Collections.max(ids));
            }
        }
        return maxIds;
    }

    /** 取出 VALUES 关键字之后的各个括号元组。 */
    private List<String> valueTuples(String tail) {
        List<String> tuples = new ArrayList<>();
        Matcher values = Pattern.compile("(?i)\\bVALUES\\b").matcher(tail);
        if (!values.find()) {
            return tuples;
        }
        int index = values.end();
        while (index < tail.length()) {
            char current = tail.charAt(index);
            if (current == '(') {
                int depth = 0;
                int start = index;
                while (index < tail.length()) {
                    char inner = tail.charAt(index);
                    if (inner == '\'') {
                        index = skipQuoted(tail, index);
                        continue;
                    }
                    if (inner == '(') {
                        depth++;
                    } else if (inner == ')') {
                        depth--;
                        if (depth == 0) {
                            index++;
                            break;
                        }
                    }
                    index++;
                }
                if (depth != 0) {
                    return tuples;
                }
                tuples.add(tail.substring(start + 1, index - 1));
            } else if (current == ',' || Character.isWhitespace(current)) {
                index++;
            } else {
                break;
            }
        }
        return tuples;
    }

    /** 按逗号切分，忽略引号内的逗号（种子里的 '0,100' 等值）。 */
    private List<String> splitList(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            char character = text.charAt(index);
            if (character == '\'') {
                int next = skipQuoted(text, index);
                current.append(text, index, next);
                index = next;
                continue;
            }
            if (character == ',') {
                parts.add(current.toString());
                current.setLength(0);
                index++;
                continue;
            }
            current.append(character);
            index++;
        }
        parts.add(current.toString());
        return parts;
    }

    private List<String> normalize(List<String> raw) {
        List<String> normalized = new ArrayList<>();
        for (String item : raw) {
            normalized.add(item.trim().replace("`", "").replace("\"", ""));
        }
        return normalized;
    }

    private int skipQuoted(String text, int index) {
        int cursor = index + 1;
        while (cursor < text.length()) {
            if (text.charAt(cursor) == '\'') {
                if (cursor + 1 < text.length() && text.charAt(cursor + 1) == '\'') {
                    cursor += 2;
                    continue;
                }
                return cursor + 1;
            }
            cursor++;
        }
        return text.length();
    }

    private String readScript(String dialect) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource("db/" + dialect + "/schema.sql").getInputStream(), StandardCharsets.UTF_8);
    }

    private static final class IdentityInfo {
        private final Map<String, String> columns = new LinkedHashMap<>();
        private final Map<String, Integer> starts = new LinkedHashMap<>();
        private final Map<String, String> restartedColumns = new LinkedHashMap<>();
    }
}
