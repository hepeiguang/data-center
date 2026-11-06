package com.lhyf.dataasset.lineage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persistence for key_mapping and row_switch (the lineage config tables owned by this service).
 */
@Repository
public class LineageConfigRepository {

    private static final String KEY_COLUMNS = "id, target_fqn, key_column, source_fqn, source_key_column, "
            + "maintained_by, created_at";
    private static final ColumnMapRowMapper MAPPER = new ColumnMapRowMapper();

    private final NamedParameterJdbcTemplate jdbc;

    public LineageConfigRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listKeys() {
        return jdbc.query("select " + KEY_COLUMNS + " from key_mapping order by target_fqn, key_column",
                Map.of(), MAPPER);
    }

    public List<Map<String, Object>> listKeysByTarget(String targetFqnLower) {
        return jdbc.query("select " + KEY_COLUMNS + " from key_mapping where lower(target_fqn) = :target"
                        + " order by target_fqn, key_column",
                new MapSqlParameterSource("target", targetFqnLower), MAPPER);
    }

    public List<Map<String, Object>> keyRows(String targetFqn, String keyColumn, String sourceFqn) {
        return jdbc.query("select " + KEY_COLUMNS + " from key_mapping where lower(target_fqn) = lower(:target) "
                        + "and key_column = :keyColumn and lower(source_fqn) = lower(:source)",
                new MapSqlParameterSource()
                        .addValue("target", targetFqn)
                        .addValue("keyColumn", keyColumn)
                        .addValue("source", sourceFqn),
                MAPPER);
    }

    public void insertKey(String target, String keyColumn, String source, String sourceKeyColumn, String user) {
        jdbc.update("insert into key_mapping (target_fqn, key_column, source_fqn, source_key_column, "
                        + "maintained_by, created_at) values (:target, :keyColumn, :source, :sourceKey, :user, now())",
                new MapSqlParameterSource()
                        .addValue("target", target)
                        .addValue("keyColumn", keyColumn)
                        .addValue("source", source)
                        .addValue("sourceKey", sourceKeyColumn)
                        .addValue("user", user));
    }

    public void updateKeySource(Long id, String sourceKeyColumn, String user) {
        jdbc.update("update key_mapping set source_key_column = :sourceKey, maintained_by = :user, "
                        + "created_at = now() where id = :id",
                new MapSqlParameterSource()
                        .addValue("sourceKey", sourceKeyColumn)
                        .addValue("user", user)
                        .addValue("id", id));
    }

    public boolean deleteKey(long id) {
        return jdbc.update("delete from key_mapping where id = :id",
                new MapSqlParameterSource("id", id)) > 0;
    }

    public Map<String, String> keyMapping(String targetFqnLower, String sourceFqnLower) {
        Map<String, String> result = new LinkedHashMap<>();
        RowCallbackHandler collect = rs -> result.put(rs.getString(1), rs.getString(2));
        jdbc.query("select key_column, source_key_column from key_mapping where lower(target_fqn) = :target "
                        + "and lower(source_fqn) = :source",
                new MapSqlParameterSource()
                        .addValue("target", targetFqnLower)
                        .addValue("source", sourceFqnLower),
                collect);
        return result;
    }

    public List<Map<String, Object>> listSwitches() {
        return jdbc.query("select table_fqn, enabled, updated_by, updated_at from row_switch "
                + "order by enabled desc, table_fqn", Map.of(), MAPPER);
    }

    public boolean switchExists(String tableFqnLower) {
        Integer count = jdbc.queryForObject("select count(*) from row_switch where lower(table_fqn) = :fqn",
                new MapSqlParameterSource("fqn", tableFqnLower), Integer.class);
        return count != null && count > 0;
    }

    public void updateSwitch(String tableFqnLower, boolean enabled, String user) {
        jdbc.update("update row_switch set enabled = :enabled, updated_by = :user, updated_at = now() "
                        + "where lower(table_fqn) = :fqn",
                new MapSqlParameterSource()
                        .addValue("enabled", enabled)
                        .addValue("user", user)
                        .addValue("fqn", tableFqnLower));
    }

    public void insertSwitch(String tableFqnLower, boolean enabled, String user) {
        jdbc.update("insert into row_switch (table_fqn, enabled, updated_by, updated_at) "
                        + "values (:fqn, :enabled, :user, now())",
                new MapSqlParameterSource()
                        .addValue("fqn", tableFqnLower)
                        .addValue("enabled", enabled)
                        .addValue("user", user));
    }

    public boolean rowLineageEnabled(String tableFqnLower) {
        List<Boolean> rows = jdbc.queryForList("select enabled from row_switch where lower(table_fqn) = :fqn",
                new MapSqlParameterSource("fqn", tableFqnLower), Boolean.class);
        return !rows.isEmpty() && rows.get(0);
    }
}
