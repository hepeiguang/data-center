package org.apache.dolphinscheduler.plugin.datasource.sqlserver;

import org.apache.dolphinscheduler.common.utils.JSONUtils;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

public class Test1 {
    private static final Pattern SQL_KEYWORD_PATTERN = Pattern.compile("^(desc|key|order|group|select|from|where|having|by|and|or|not|as|on|join|inner|outer|left|right|union|intersect|except|create|drop|alter|insert|update|delete|table|column|index|primary|foreign|references|constraint|default|check|unique|clustered|nonclustered|with|option|values|set|into|all|any|some|exists|between|in|like|is|null|case|when|then|else|end|begin|try|catch|throw|goto|return|break|continue|declare|exec|execute|open|fetch|close|deallocate|print|raiserror|begin|commit|rollback|transaction|save|checkpoint|backup|restore|dbcc|kill|shutdown|reconfigure|configure|sp_|xp_|fn_|sys|information_schema|all|any|between|cross|except|foreign|group|having|in|inner|intersect|into|is|join|like|not|null|on|or|order|outer|select|some|union|where|add|all|alter|analyze|and|as|asc|asensitive|before|between|bigint|binary|blob|both|by|call|cascade|case|change|char|character|check|collate|column|condition|connection|constraint|continue|convert|create|cross|current_date|current_time|current_timestamp|current_user|cursor|database|databases|day_hour|day_microsecond|day_minute|day_second|dec|decimal|declare|default|delayed|delete|desc|describe|deterministic|distinct|distinctrow|div|double|drop|dual|each|else|elseif|enclosed|escaped|exists|exit|explain|false|fetch|float|float4|float8|for|force|foreign|from|fulltext|goto|grant|group|having|high_priority|hour_microsecond|hour_minute|hour_second|if|ignore|in|index|infile|inner|inout|insensitive|insert|int|int1|integer|interval|into|is|iterate|join|key|keys|kill|leading|leave|left|like|limit|lines|load|localtime|localtimestamp|lock|long|longblob|longtext|loop|low_priority|match|mediumblob|mediumint|mediumtext|middleint|minute_microsecond|minute_second|mod|modifies|natural|not|no_write_to_binlog|NULL|numeric|on|optimize|option|optionally|or|order|out|outer|outfile|precision|primary|procedure|purge|raid0|read|reads|real|references|regexp|release|rename|repeat|replace|require|restrict|return|revoke|right|rlike|schema|schemas|second_microsecond|select|sensitive|separator|set|show|smallint|soname|spatial|specific|sql|sql_big_result|sql_calc_found_rows|sql_small_result|sqlexception|sqlstate|sqlwarning|ssl|starting|straight_join|table|terminated|then|to|trailing|trigger|true|undo|union|unique|unlock|unsigned|update|usage|use|using|utc_date|utc_time|utc_timestamp|values|varbinary|varchar|varcharacter|varying|when|where|while|with|write|x509|xml|year_month|zerofill|action|admin|after|aggregate|allocation|analyse|array|asensitive|at|authors|auto_increment|avg_row_length|avg|backward|bit|bool|boolean|both|cascade|cascaded|catalog_name|chain|characteristics|character_set_catalog|character_set_name|character_set_schema|class_origin|client|clobber|code|collation_catalog|collation_name|collation_schema|column_name|command_class|commit|committed|condition_number|connection|const|constraint_catalog|constraint_name|constraint_schema|constructor|contains|continue|convert|copy|corr|corresponding|cpu|cube|cume_dist|current|current_schema|cursor_name|cycle|data|database|date|datetime|day|deallocate|dec|decimal|default|deferrable|deferred|defined|definer|degree|delay_key_write|delayed|delete|delimiter|delimiters|dense_rank|deterministic|diagnostics|directory|disable|disallow|disconnect|dispatch|distinct|do|document|domain|double|drop|dual|dynamic|dynamic_function|dynamic_function_code|each|element|else|elseif|enable|enclosed|encoding|encrypted|end|end-exec|enum|equals|error|escape|escaped|every|except|exception|exchange|exclude|exclusive|exec|execute|existing|expansion|expire|explain|export|expression|extension|external|extract|false|fetch|fields|file|first|first_value|fixed|following|follows|for|force|format|found|free|freeze|from|full|function|fusion|general|generated|geometry|geometrycollection|get|global|go|goto|grant|grants|greatest|group|grouping|groups|handler|hash|header|having|high_priority|hold|host|hosts|hour|identifier|identity|if|ignore|import|in|index|indicator|infile|inout|insensitive|insert|insert_method|install|instead|instr|integer|int|intersect|interval|into|invoker|is|isnull|isolation|issuer|iterate|join|json|key|keys|kill|label|lag|language|last|last_value|lateral|leading|least|leave|left|length|less|level|like|limit|linear|lines|linestring|listagg|listen|load|local|localtime|localtimestamp|location|locator|lock|locked|log|logfile|logs|longblob|longtext|loop|low_priority|master|match|materialized|max_rows|master_ssl_verify_server_cert|max|mediumblob|mediumint|mediumtext|memory|merge|message_length|message_octet_length|message_text|migrate|min_rows|min|minute|mod|mode|modifies|modify|multilinestring|multipoint|multipolygon|multiset|mumps|name|names|national|natural|nchar|nclob|nested|new|next|no|none|not|notnull|nowait|nullif|null|number|numeric|object|octet_length|of|off|offset|oids|old|on|one|only|open|optimize|option|options|or|order|ordinality|others|out|outer|outfile|output|over|overlaps|overlay|owner|pack_keys|pad|parameter|parameter_mode|parameter_name|parameter_ordinal_position|parameter_specific_catalog|parameter_specific_name|parameter_specific_schema|partial|partition|pascal|password|path|percent_rank|percentile_cont|percentile_disc|placing|plan|plugin|plugins|port|position|preceding|precision|prepare|preserve|primary|prior|privileges|procedure|process|processlist|profile|profiles|proxy|public|purge|quarter|query|quick|raise|random|range|rank|read|reads|real|reassign|rebuild|record|recursive|reduce|references|regexp|regr_avgx|regr_avgy|regr_count|regr_intercept|regr_r2|regr_slope|regr_sxx|regr_sxy|regr_syy|reindex|relative|relaylog|release|rename|repeat|repeatable|replace|replication|require|reset|resignal|restore|restrict|result|return|returned_cardinality|returned_length|returned_octet_length|returned_sqlstate|returns|revoke|right|role|routines|row_count|row_format|row_number|row|rows|rule|savepoint|schedule|schema|schema_name|schemas|scope|scope_catalog|scope_name|scope_schema|scroll|search|second|section|security|select|self|sensitive|separator|sequence|serializable|server_name|session|session_user|set|share|shared|show|shutdown|signal|similar|simple|size|smallint|snapshot|some|soname|soundex|source|space|spatial|specific|specifictype|sql|sql_big_result|sql_cache|sql_calc_found_rows|sql_no_cache|sql_small_result|sql_state|sql_warning|sqlexception|sqlstate|sqlvolume|sqrt|ssl|standalone|start|starting|starts|statement|static|statistics|status|stddev_pop|stddev_samp|stdin|stdout|storage|straight_join|strict|string|striped|structure|style|subclass_origin|subject|substring|successful|sum|superuser|table_name|table|tables|tablespace|temporary|temptable|terminated|text|than|then|ties|time|timestamp|timezone_hour|timezone_minute|tinyblob|tinyint|tinytext|to|trailing|transaction|translate|translation|treat|trigger|triggers|trim|true|truncate|type|uescape|unbounded|uncommitted|undefined|under|undo|unencrypted|union|unique|unknown|unlisten|unlock|unsigned|until|update|usage|use|user|user_resources|using|utc_date|utc_time|utc_timestamp|vacuum|valid|validate|validator|value|values|var_pop|var_samp|varbinary|varchar|varcharacter|variable|variables|varying|verbose|version|view|virtual|visible|volatile|wait|when|where|while|width_bucket|window|with|within|without|work|wrapper|write|xml|xmlattributes|xmlbinary|xmlcast|xmlcomment|xmlconcat|xmldocument|xmlelement|xmlexists|xmlforest|xmliterate|xmlnamespaces|xmlparse|xmlpi|xmlquery|xmlroot|xmlschema|xmlserialize|xmltable|xmltext|xmlvalidate|year|zone)$", Pattern.CASE_INSENSITIVE);
    private static String server = "172.41.133.84";
    private static String database = "UFDATA_027_2025";
    private static String user = "sa";
    private static String password = "admin@123";
    private static String connectionUrl = String.format(
            "jdbc:sqlserver://%s:1433;" +
                    "databaseName=%s;" +
                    "user=%s;" +
                    "password=%s;" +
                    "encrypt=false;" +
                    "trustServerCertificate=true;" +
                    "sslProtocol=TLSv1;" +
                    "loginTimeout=30;" +
                    "sendTimeAsDatetime=false;" +
                    "sendStringParametersAsUnicode=true;",
            server, database, user, password);

    // 添加失败表记录功能
    private static Set<String> failedTables = new HashSet<>();

    public static void main(String[] args) {
        List<String> notEmptyTableList = getNotEmptyTableList();
        System.out.println("需要同步的表个数: " + notEmptyTableList.size() + ", 列表：");
        Collections.sort(notEmptyTableList);
        System.out.println(notEmptyTableList);
        List<String> noPrimarkeyTableList = getNoPrimaryKeyTableList();
        System.out.println("主键为空需要处理的表,个数： " + noPrimarkeyTableList.size() + ", 列表：");
        Collections.sort(noPrimarkeyTableList);
        System.out.println(noPrimarkeyTableList);
//        Connection conn = null;
//        try {
//            conn = getConnection();
//            Map<String, String> tablePrimaryKeyMap = getTablePrimaryKeyMapWithFailedTables(noPrimarkeyTableList, conn);
//            System.out.println("成功处理的表， 个数: " + tablePrimaryKeyMap.size() + ", 列表：");
//            System.out.println(JSONUtils.toJsonString(tablePrimaryKeyMap));
//            System.out.println("未能处理的表， 个数：" + failedTables.size() + ", 列表：");
//            System.out.println(JSONUtils.toJsonString(getFailedTables()));
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } finally {
//            try {
//                if (conn != null)
//                    conn.close();
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }
    }

    private static Set<String> getFailedTables() {
        return failedTables;
    }

    private static Map<String, String> getTablePrimaryKeyMapWithFailedTables(List<String> noPrimaryKeyTableList, Connection conn) {
        Map<String, String> tablePrimaryKeyMap = new HashMap<>();
        failedTables.clear(); // 清空之前的失败记录

        for (String tableName : noPrimaryKeyTableList) {
            try {
                // 获取表的列信息
                List<String> columnNames = getTableColumns(conn, tableName);

                if (columnNames.isEmpty()) {
                    continue; // 如果没有列，跳过该表
                }

                // 尝试找出能保证数据唯一的列组合
                String uniqueColumns = findUniqueColumns(conn, tableName, columnNames);

                if (uniqueColumns != null && !uniqueColumns.isEmpty()) {
                    tablePrimaryKeyMap.put(tableName, uniqueColumns);
                }
            } catch (SQLException e) {
                System.err.println("处理表 " + tableName + " 时发生错误: " + e.getMessage());
                failedTables.add(tableName); // 记录失败的表
                // 继续处理下一个表
            }
        }

        return tablePrimaryKeyMap;
    }

    /**
     * 获取表的所有列名
     */
    private static List<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME"));
                }
            }
        }

        return columnNames;
    }

    /**
     * 尝试找出能保证数据唯一的列组合
     */
    private static String findUniqueColumns(Connection conn, String tableName, List<String> columnNames) throws SQLException {
        // 尝试不同的列组合来找到唯一性约束
        // 从单列开始，逐步增加列数

        // 首先尝试单列唯一性
        for (String column : columnNames) {
            if (isColumnUnique(conn, tableName, column)) {
                return column; // 找到单列唯一性，直接返回
            }
        }

        // 如果单列不唯一，尝试两列组合
        for (int i = 0; i < columnNames.size(); i++) {
            for (int j = i + 1; j < columnNames.size(); j++) {
                String col1 = columnNames.get(i);
                String col2 = columnNames.get(j);
                String combinedColumns = col1 + "," + col2;

                if (isColumnsUnique(conn, tableName, Arrays.asList(col1, col2))) {
                    return combinedColumns;
                }
            }
        }

        // 如果两列组合还不够，可以继续尝试更多列的组合
        // 为了效率考虑，这里只尝试到3列组合
        for (int i = 0; i < columnNames.size(); i++) {
            for (int j = i + 1; j < columnNames.size(); j++) {
                for (int k = j + 1; k < columnNames.size(); k++) {
                    String col1 = columnNames.get(i);
                    String col2 = columnNames.get(j);
                    String col3 = columnNames.get(k);
                    String combinedColumns = col1 + "," + col2 + "," + col3;

                    if (isColumnsUnique(conn, tableName, Arrays.asList(col1, col2, col3))) {
                        return combinedColumns;
                    }
                }
            }
        }

        // 如果没有找到唯一列组合，返回空字符串
        return "";
    }

    /**
     * 检查单列是否唯一
     */
    private static boolean isColumnUnique(Connection conn, String tableName, String columnName) throws SQLException {
        // 检查数据类型是否支持 GROUP BY
        if (!isColumnGroupable(conn, tableName, columnName)) {
            return false; // 如果列类型不支持分组，直接返回 false
        }

        // 转义 SQL 关键字
        String escapedColumnName = escapeSqlKeyword(columnName);
        String sql = "SELECT " + escapedColumnName + ", COUNT(1) AS ct FROM " + tableName +
                " GROUP BY " + escapedColumnName + " ORDER BY ct DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long count = rs.getLong("ct");
                // 如果最大计数大于1，说明该列不是唯一的
                return count == 1;
            }
        }

        return true; // 如果没有数据，认为是唯一的
    }

    /**
     * 检查多列组合是否唯一
     */
    private static boolean isColumnsUnique(Connection conn, String tableName, List<String> columnNames) throws SQLException {
        // 检查所有列是否都支持 GROUP BY
        for (String columnName : columnNames) {
            if (!isColumnGroupable(conn, tableName, columnName)) {
                return false; // 如果有任何一列不支持分组，返回 false
            }
        }

        // 转义 SQL 关键字
        List<String> escapedColumns = new ArrayList<>();
        for (String columnName : columnNames) {
            escapedColumns.add(escapeSqlKeyword(columnName));
        }
        String columnsStr = String.join(", ", escapedColumns);
        String sql = "SELECT " + columnsStr + ", COUNT(1) AS ct FROM " + tableName +
                " GROUP BY " + columnsStr + " ORDER BY ct DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long count = rs.getLong("ct");
                // 如果最大计数大于1，说明该列组合不是唯一的
                return count == 1;
            }
        }

        return true; // 如果没有数据，认为是唯一的
    }

    // 检查列是否支持 GROUP BY（即不是 text、ntext、image 等类型）
    private static boolean isColumnGroupable(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            pstmt.setString(2, columnName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dataType = rs.getString("DATA_TYPE").toLowerCase();
                    // 检查是否为不支持 GROUP BY 的数据类型
                    return !Arrays.asList("text", "ntext", "image", "xml", "varbinary", "binary").contains(dataType);
                }
            }
        }
        return false; // 如果无法获取数据类型，认为不支持
    }

    // 转义 SQL 关键字
    private static String escapeSqlKeyword(String columnName) {
        if (SQL_KEYWORD_PATTERN.matcher(columnName).matches()) {
            return "[" + columnName + "]"; // 使用方括号转义 SQL 关键字
        }
        return columnName;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl);
    }

    private static List<String> getNoPrimaryKeyTableList() {
        String sql = "SELECT \n" +
                "    SCHEMA_NAME(t.schema_id) AS [SchemaName],\n" +
                "    t.name AS [TableName],\n" +
                "    t.create_date AS [CreateDate],\n" +
                "    t.modify_date AS [LastModified],\n" +
                "    p.rows AS [RowCount],\n" +
                "    CAST(ROUND(SUM(a.total_pages) * 8.0 / 1024, 2) AS DECIMAL(10,2)) AS [TotalSize_MB]\n" +
                "FROM sys.tables t\n" +
                "INNER JOIN sys.indexes i ON t.object_id = i.object_id\n" +
                "INNER JOIN sys.partitions p ON i.object_id = p.object_id AND i.index_id = p.index_id\n" +
                "INNER JOIN sys.allocation_units a ON p.partition_id = a.container_id\n" +
                "WHERE t.is_ms_shipped = 0\n" +
                "    AND i.index_id <= 1\n" +
                "    AND NOT EXISTS (\n" +
                "        SELECT 1 \n" +
                "        FROM sys.indexes i2 \n" +
                "        WHERE i2.object_id = t.object_id \n" +
                "            AND i2.is_primary_key = 1\n" +
                "    )\n" +
                "    AND t.name IN (\n" +
                "      SELECT t.name\n" +
                "      FROM sys.tables t\n" +
                "      INNER JOIN sys.partitions p ON t.object_id = p.object_id\n" +
                "      WHERE p.index_id IN (0, 1)\n" +
                "          AND t.type = 'U'\n" +
                "          AND t.is_ms_shipped = 0\n" +
                "      --     AND t.name IN ()\n" +
                "      GROUP BY t.name\n" +
                "      HAVING SUM(p.rows) > 0\n" +
                "    )\n" +
                "GROUP BY SCHEMA_NAME(t.schema_id), t.name, t.create_date, t.modify_date, p.rows\n" +
                "ORDER BY [SchemaName], [TableName];";
        List<String> tableList = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String schemaName = rs.getString("SchemaName");
                String tableName = rs.getString("TableName");
                int rowCount = rs.getInt("RowCount");
//                System.out.println(schemaName + "." + tableName + ": " + rowCount);
                tableList.add(tableName);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tableList;
    }

    private static List<String> getNotEmptyTableList() {
        String sql = "SELECT \n" +
                "    SCHEMA_NAME(t.schema_id) AS [SchemaName],\n" +
                "    t.name AS [TableName],\n" +
                "    SUM(p.rows) AS [RowCount]\n" +
                "FROM sys.tables t\n" +
                "INNER JOIN sys.partitions p ON t.object_id = p.object_id\n" +
                "WHERE p.index_id IN (0, 1) \n" +
                "    AND t.type = 'U' \n" +
                "    AND t.is_ms_shipped = 0 \n" +
                "--     AND t.name IN ()\n" +
                "GROUP BY t.schema_id, t.name\n" +
                "HAVING SUM(p.rows) > 0\n" +
                "ORDER BY [RowCount] DESC, [SchemaName], [TableName];\n";
        List<String> tableList = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String schemaName = rs.getString("SchemaName");
                String tableName = rs.getString("TableName");
                int rowCount = rs.getInt("RowCount");
//                System.out.println(schemaName + "." + tableName + ": " + rowCount);
                tableList.add(tableName);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return tableList;
    }

}
