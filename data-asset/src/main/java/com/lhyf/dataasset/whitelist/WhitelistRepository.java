package com.lhyf.dataasset.whitelist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Owns all persistence for the browse_entry / approval_event config tables.
 * Row mapping and column serialization (JSON lists, subjects) live here so the
 * service layer keeps only business rules.
 */
@Repository
public class WhitelistRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public WhitelistRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<BrowseEntry> findAll() {
        return jdbc.query("select * from browse_entry order by updated_at desc", Map.of(), this::map);
    }

    public BrowseEntry findById(String id) {
        List<BrowseEntry> rows = jdbc.query("select * from browse_entry where id = :id",
                params("id", id), this::map);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<BrowseEntry> findActive() {
        return jdbc.query("select * from browse_entry where status = :status order by updated_at desc",
                params("status", BrowseEntry.ACTIVE), this::map);
    }

    public void insert(BrowseEntry entry) {
        jdbc.update("insert into browse_entry (id, datasource, database_name, schema_name, tables_json, "
                        + "include_columns_json, exclude_columns_json, mask_columns_json, row_policy, "
                        + "subjects_json, page_size, max_rows, export_enabled, status, valid_until, applicant, "
                        + "approved_by, created_at, updated_at) values (:id, :datasource, :database, :schema, "
                        + ":tables, :include, :exclude, :mask, :rowPolicy, :subjects, :pageSize, :maxRows, "
                        + ":export, :status, :validUntil, :applicant, null, now(), now())",
                params("id", entry.getId(),
                        "datasource", entry.getDatasource(),
                        "database", entry.getDatabase(),
                        "schema", entry.getSchema(),
                        "tables", json(entry.getTables()),
                        "include", json(entry.getIncludeColumns()),
                        "exclude", json(entry.getExcludeColumns()),
                        "mask", json(entry.getMaskColumns()),
                        "rowPolicy", entry.getRowPolicy(),
                        "subjects", json(entry.getSubjects()),
                        "pageSize", entry.getPageSize(),
                        "maxRows", entry.getMaxRows(),
                        "export", entry.isExportEnabled(),
                        "status", entry.getStatus(),
                        "validUntil", entry.getValidUntil() == null
                                ? null : java.sql.Date.valueOf(entry.getValidUntil()),
                        "applicant", entry.getApplicant()));
    }

    public void updateStatus(String id, String status, String approver) {
        jdbc.update("update browse_entry set status = :status, approved_by = :approver, updated_at = now() "
                        + "where id = :id",
                params("status", status, "approver", approver, "id", id));
    }

    public void updateStatusOnly(String id, String status) {
        jdbc.update("update browse_entry set status = :status, updated_at = now() where id = :id",
                params("status", status, "id", id));
    }

    public void insertApprovalEvent(String entryId, String action, String actor, String comment) {
        jdbc.update("insert into approval_event (entry_id, action, actor, comment, created_at) "
                        + "values (:entryId, :action, :actor, :comment, now())",
                params("entryId", entryId, "action", action, "actor", actor, "comment", comment));
    }

    public List<String> approvalLines(String entryId) {
        return jdbc.query("select action || ' by ' || actor || coalesce(' (' || comment || ')', '') "
                        + "from approval_event where entry_id = :entryId order by created_at",
                params("entryId", entryId), (rs, i) -> rs.getString(1));
    }

    /** Null-tolerant parameter map (Map.of rejects nulls). */
    private static Map<String, Object> params(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("cannot serialize whitelist entry", e);
        }
    }

    private BrowseEntry map(ResultSet rs, int rowNum) throws SQLException {
        BrowseEntry entry = new BrowseEntry();
        entry.setId(rs.getString("id"));
        entry.setDatasource(rs.getString("datasource"));
        entry.setDatabase(rs.getString("database_name"));
        entry.setSchema(rs.getString("schema_name"));
        entry.setTables(readList(rs.getString("tables_json")));
        entry.setIncludeColumns(readList(rs.getString("include_columns_json")));
        entry.setExcludeColumns(readList(rs.getString("exclude_columns_json")));
        entry.setMaskColumns(readList(rs.getString("mask_columns_json")));
        entry.setRowPolicy(rs.getString("row_policy"));
        entry.setSubjects(readSubjects(rs.getString("subjects_json")));
        entry.setPageSize(rs.getInt("page_size"));
        entry.setMaxRows(rs.getInt("max_rows"));
        entry.setExportEnabled(rs.getBoolean("export_enabled"));
        entry.setStatus(rs.getString("status"));
        java.sql.Date validUntil = rs.getDate("valid_until");
        entry.setValidUntil(validUntil == null ? null : validUntil.toLocalDate());
        entry.setApplicant(rs.getString("applicant"));
        entry.setApprovedBy(rs.getString("approved_by"));
        return entry;
    }

    private List<String> readList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private BrowseEntry.Subjects readSubjects(String json) {
        if (!StringUtils.hasText(json)) {
            return new BrowseEntry.Subjects();
        }
        try {
            return objectMapper.readValue(json, BrowseEntry.Subjects.class);
        } catch (Exception e) {
            return new BrowseEntry.Subjects();
        }
    }
}
