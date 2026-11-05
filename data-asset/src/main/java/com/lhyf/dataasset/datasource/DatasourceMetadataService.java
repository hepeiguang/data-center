package com.lhyf.dataasset.datasource;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lhyf.dataasset.config.BusinessDataSources;
import com.lhyf.dataasset.config.DataAssetProperties;

/**
 * Schema introspection over the read-only business pools. Metadata only - no row data is read here,
 * so this is not gated by the browse whitelist; the whitelist still governs /data-asset/rows.
 */
@Service
public class DatasourceMetadataService {

    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,127}$");
    private static final List<String> SYSTEM_SCHEMAS = List.of(
            "information_schema", "pg_catalog", "pg_toast", "pg_temp_1", "pg_toast_temp_1",
            "sys", "mysql", "performance_schema", "dbms", "ltidb");

    private final DataAssetProperties properties;
    private final BusinessDataSources dataSources;

    public DatasourceMetadataService(DataAssetProperties properties, BusinessDataSources dataSources) {
        this.properties = properties;
        this.dataSources = dataSources;
    }

    public List<Map<String, Object>> datasources() {
        List<Map<String, Object>> result = new ArrayList<>();
        properties.getDatasources().forEach((name, config) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("database", databaseOf(config.getUrl()));
            result.add(item);
        });
        return result;
    }

    public List<String> schemas(String datasource) {
        String sql = "select schema_name from information_schema.schemata order by schema_name";
        List<String> all = jdbc(datasource).queryForList(sql, String.class);
        List<String> result = new ArrayList<>();
        for (String schema : all) {
            if (!SYSTEM_SCHEMAS.contains(schema.toLowerCase())) {
                result.add(schema);
            }
        }
        return result;
    }

    public List<Map<String, Object>> tables(String datasource, String schema, String query, int limit) {
        requireExposedSchema(datasource, schema);
        String like = "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
        String sql = "select lower(table_name) as table_name, table_type"
                + " from information_schema.tables where table_schema = ? and lower(table_name) like ?"
                + " order by table_name limit ?";
        List<Map<String, Object>> rows = jdbc(datasource).queryForList(sql, schema, like,
                Math.max(1, Math.min(limit, 500)));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("schema", schema);
            item.put("table", row.get("table_name"));
            item.put("type", "BASE TABLE".equalsIgnoreCase(String.valueOf(row.get("table_type")))
                    ? "table" : String.valueOf(row.get("table_type")));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> columns(String datasource, String schema, String table) {
        requireExposedSchema(datasource, schema);
        requireIdentifier(table, "table");
        String sql = "select column_name, data_type, is_nullable, ordinal_position,"
                + " character_maximum_length, numeric_precision, numeric_scale"
                + " from information_schema.columns where table_schema = ? and table_name = ?"
                + " order by ordinal_position";
        List<Map<String, Object>> rows = jdbc(datasource).queryForList(sql, schema, table);
        List<String> primaryKeys = primaryKeys(datasource, schema, table);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = String.valueOf(row.get("column_name"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("dataType", row.get("data_type"));
            item.put("nullable", "YES".equals(String.valueOf(row.get("is_nullable"))));
            item.put("ordinalPosition", row.get("ordinal_position"));
            item.put("characterMaximumLength", row.get("character_maximum_length"));
            item.put("numericPrecision", row.get("numeric_precision"));
            item.put("numericScale", row.get("numeric_scale"));
            item.put("constraint", primaryKeys.contains(name) ? "PRIMARY_KEY" : "NULL");
            result.add(item);
        }
        return result;
    }

    public List<String> primaryKeys(String datasource, String schema, String table) {
        requireIdentifier(schema, "schema");
        requireIdentifier(table, "table");
        String sql = "select kcu.column_name from information_schema.table_constraints tc"
                + " join information_schema.key_column_usage kcu"
                + " on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema"
                + " where tc.constraint_type = 'PRIMARY KEY' and tc.table_schema = ? and tc.table_name = ?"
                + " order by kcu.ordinal_position";
        try {
            List<String> keys = jdbc(datasource).queryForList(sql, String.class, schema, table);
            if (!keys.isEmpty()) {
                return keys;
            }
            // Postgres hides key_column_usage rows for constraints the current role does not own
            String pgSql = "select a.attname from pg_catalog.pg_constraint c"
                    + " join pg_catalog.pg_attribute a on a.attrelid = c.conrelid and a.attnum = any(c.conkey)"
                    + " where c.contype = 'p' and c.conrelid = ?::regclass"
                    + " order by array_position(c.conkey, a.attnum)";
            return jdbc(datasource).queryForList(pgSql, String.class, schema + "." + table);
        } catch (Exception e) {
            return List.of();
        }
    }

    private JdbcTemplate jdbc(String datasource) {
        if (!properties.getDatasources().containsKey(datasource)) {
            throw new IllegalArgumentException("unknown datasource: " + datasource);
        }
        return dataSources.get(datasource);
    }

    /** Only schemas the picker exposes may be introspected, so system catalogs stay out of reach. */
    private void requireExposedSchema(String datasource, String schema) {
        requireIdentifier(schema, "schema");
        if (!schemas(datasource).contains(schema)) {
            throw new IllegalArgumentException("schema is not exposed: " + schema);
        }
    }

    private static void requireIdentifier(String value, String label) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("illegal " + label + ": " + value);
        }
    }

    /** jdbc:postgresql://host:5432/agri_sat?... -> agri_sat */
    private static String databaseOf(String url) {
        if (url == null) {
            return "";
        }
        try {
            String path = URI.create(url.substring(url.indexOf("://") + 3)).getPath();
            return path == null || path.length() < 2 ? "" : path.substring(1);
        } catch (Exception e) {
            int slash = url.lastIndexOf('/');
            int question = url.indexOf('?', slash);
            return slash < 0 ? "" : url.substring(slash + 1, question < 0 ? url.length() : question);
        }
    }
}
