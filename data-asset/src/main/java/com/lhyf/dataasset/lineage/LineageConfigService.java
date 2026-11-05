package com.lhyf.dataasset.lineage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Key mappings and row-lineage switches: the only lineage data this service owns.
 * Both fill in what column-level lineage in OpenMetadata cannot express (key semantics, opt-in scope).
 */
@Service
public class LineageConfigService {

    private final JdbcTemplate jdbc;

    public LineageConfigService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listKeys(String targetFqn) {
        if (StringUtils.hasText(targetFqn)) {
            return jdbc.queryForList("select id, target_fqn, key_column, source_fqn, source_key_column, "
                    + "maintained_by, created_at from key_mapping where lower(target_fqn) = lower(?) "
                    + "order by target_fqn, key_column", targetFqn.toLowerCase());
        }
        return jdbc.queryForList("select id, target_fqn, key_column, source_fqn, source_key_column, "
                + "maintained_by, created_at from key_mapping order by target_fqn, key_column");
    }

    public Map<String, Object> addKey(String targetFqn, String keyColumn, String sourceFqn,
                                      String sourceKeyColumn, String user) {
        requireTable(targetFqn);
        requireTable(sourceFqn);
        requireColumn(keyColumn, "key column");
        requireColumn(sourceKeyColumn, "source key column");
        String target = targetFqn.toLowerCase();
        String source = sourceFqn.toLowerCase();
        List<Map<String, Object>> existing = keyRows(target, keyColumn, source);
        if (existing.isEmpty()) {
            jdbc.update("insert into key_mapping (target_fqn, key_column, source_fqn, source_key_column, "
                            + "maintained_by, created_at) values (?,?,?,?,?,now())",
                    target, keyColumn, source, sourceKeyColumn, user);
        } else {
            jdbc.update("update key_mapping set source_key_column = ?, maintained_by = ?, created_at = now() "
                    + "where id = ?", sourceKeyColumn, user, existing.get(0).get("id"));
        }
        List<Map<String, Object>> rows = keyRows(target, keyColumn, source);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
    }

    private List<Map<String, Object>> keyRows(String targetFqn, String keyColumn, String sourceFqn) {
        return jdbc.queryForList("select id, target_fqn, key_column, source_fqn, source_key_column, "
                + "maintained_by, created_at from key_mapping where lower(target_fqn) = lower(?) "
                + "and key_column = ? and lower(source_fqn) = lower(?)", targetFqn, keyColumn, sourceFqn);
    }

    public boolean removeKey(long id) {
        return jdbc.update("delete from key_mapping where id = ?", id) > 0;
    }

    /** key_column -> source_key_column for one edge; empty means the edge has no registered key. */
    public Map<String, String> keyMapping(String targetFqn, String sourceFqn) {
        Map<String, String> result = new LinkedHashMap<>();
        jdbc.query("select key_column, source_key_column from key_mapping where lower(target_fqn) = lower(?) "
                        + "and lower(source_fqn) = lower(?)",
                rs -> {
                    result.put(rs.getString(1), rs.getString(2));
                },
                targetFqn.toLowerCase(), sourceFqn.toLowerCase());
        return result;
    }

    public List<Map<String, Object>> listSwitches() {
        return jdbc.queryForList("select table_fqn, enabled, updated_by, updated_at from row_switch "
                + "order by enabled desc, table_fqn");
    }

    public Map<String, Object> setSwitch(String tableFqn, boolean enabled, String user) {
        requireTable(tableFqn);
        String fqn = tableFqn.toLowerCase();
        if (rowLineageEnabledAny(fqn)) {
            jdbc.update("update row_switch set enabled = ?, updated_by = ?, updated_at = now() "
                    + "where lower(table_fqn) = ?", enabled, user, fqn);
        } else {
            jdbc.update("insert into row_switch (table_fqn, enabled, updated_by, updated_at) values (?,?,?,now())",
                    fqn, enabled, user);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tableFqn", fqn);
        result.put("enabled", enabled);
        result.put("updatedBy", user);
        return result;
    }

    private boolean rowLineageEnabledAny(String tableFqn) {
        Integer count = jdbc.queryForObject("select count(*) from row_switch where lower(table_fqn) = ?",
                Integer.class, tableFqn);
        return count != null && count > 0;
    }

    public boolean rowLineageEnabled(String tableFqn) {
        List<Boolean> rows = jdbc.query("select enabled from row_switch where lower(table_fqn) = lower(?)",
                (rs, i) -> rs.getBoolean(1), tableFqn.toLowerCase());
        return !rows.isEmpty() && rows.get(0);
    }

    private static void requireTable(String fqn) {
        if (!StringUtils.hasText(fqn) || fqn.split("\\.").length != 4) {
            throw new IllegalArgumentException("only table FQN service.database.schema.table is supported: " + fqn);
        }
    }

    private static void requireColumn(String column, String label) {
        if (!StringUtils.hasText(column) || !column.matches("^[A-Za-z_][A-Za-z0-9_]{0,127}$")) {
            throw new IllegalArgumentException("illegal " + label + ": " + column);
        }
    }
}
