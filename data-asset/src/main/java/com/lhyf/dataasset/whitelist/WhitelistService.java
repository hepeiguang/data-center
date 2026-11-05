package com.lhyf.dataasset.whitelist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhyf.dataasset.util.TtlCache;

@Service
public class WhitelistService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final TtlCache<String, List<BrowseEntry>> activeCache = new TtlCache<>(10_000);

    public WhitelistService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<BrowseEntry> list() {
        return jdbc.query("select * from browse_entry order by updated_at desc", this::map);
    }

    public BrowseEntry find(String id) {
        List<BrowseEntry> rows = jdbc.query("select * from browse_entry where id = ?", this::map, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public BrowseEntry create(BrowseEntry entry, String applicant) {
        validate(entry);
        entry.setId(UUID.randomUUID().toString().replace("-", ""));
        entry.setApplicant(applicant);
        entry.setStatus(BrowseEntry.PENDING);
        jdbc.update("insert into browse_entry (id, datasource, database_name, schema_name, tables_json, "
                        + "include_columns_json, exclude_columns_json, mask_columns_json, row_policy, subjects_json, "
                        + "page_size, max_rows, export_enabled, status, valid_until, applicant, approved_by, "
                        + "created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),now())",
                entry.getId(), entry.getDatasource(), entry.getDatabase(), entry.getSchema(),
                json(entry.getTables()), json(entry.getIncludeColumns()), json(entry.getExcludeColumns()),
                json(entry.getMaskColumns()), entry.getRowPolicy(), json(entry.getSubjects()),
                entry.getPageSize(), entry.getMaxRows(), entry.isExportEnabled(), entry.getStatus(),
                entry.getValidUntil() == null ? null : java.sql.Date.valueOf(entry.getValidUntil()),
                entry.getApplicant(), null);
        invalidate();
        return entry;
    }

    public void approve(String id, String approver) {
        BrowseEntry entry = find(id);
        if (entry == null) {
            throw new IllegalArgumentException("entry not found: " + id);
        }
        if (approver != null && approver.equals(entry.getApplicant())) {
            throw new IllegalArgumentException("applicant cannot approve own entry");
        }
        jdbc.update("update browse_entry set status = ?, approved_by = ?, updated_at = now() where id = ?",
                BrowseEntry.ACTIVE, approver, id);
        jdbc.update("insert into approval_event (entry_id, action, actor, comment, created_at) values (?,?,?,?,now())",
                id, "APPROVE", approver, null);
        invalidate();
    }

    public void reject(String id, String approver, String comment) {
        if (!StringUtils.hasText(comment)) {
            throw new IllegalArgumentException("reject reason is required");
        }
        jdbc.update("update browse_entry set status = ?, approved_by = ?, updated_at = now() where id = ?",
                BrowseEntry.REJECTED, approver, id);
        jdbc.update("insert into approval_event (entry_id, action, actor, comment, created_at) values (?,?,?,?,now())",
                id, "REJECT", approver, comment);
        invalidate();
    }

    public void disable(String id, String actor) {
        jdbc.update("update browse_entry set status = ?, updated_at = now() where id = ?", BrowseEntry.DISABLED, id);
        jdbc.update("insert into approval_event (entry_id, action, actor, comment, created_at) values (?,?,?,?,now())",
                id, "DISABLE", actor, null);
        invalidate();
    }

    public List<String> approvals(String id) {
        return jdbc.query("select action || ' by ' || actor || coalesce(' (' || comment || ')', '') "
                        + "from approval_event where entry_id = ? order by created_at",
                (rs, i) -> rs.getString(1), id);
    }

    /** Finds an active entry granting this user access to the table fqn (service.database.schema.table). */
    public BrowseEntry findActiveFor(String user, boolean admin, String fqn) {
        String[] parts = fqn.split("\\.");
        if (parts.length < 4) {
            return null;
        }
        for (BrowseEntry entry : activeEntries()) {
            if (entry.getValidUntil() != null && entry.getValidUntil().isBefore(LocalDate.now())) {
                continue;
            }
            if (!eq(entry.getDatasource(), parts[0]) || !eq(entry.getDatabase(), parts[1])
                    || !eq(entry.getSchema(), parts[2])) {
                continue;
            }
            if (!matchesTable(entry, fqn, parts[3])) {
                continue;
            }
            if (matchesSubject(entry, user, admin)) {
                return entry;
            }
        }
        return null;
    }

    private List<BrowseEntry> activeEntries() {
        return activeCache.get("active",
                () -> jdbc.query("select * from browse_entry where status = ? order by updated_at desc",
                        this::map, BrowseEntry.ACTIVE));
    }

    private static boolean matchesTable(BrowseEntry entry, String fqn, String table) {
        for (String configured : entry.getTables()) {
            if (fqn.equalsIgnoreCase(configured) || table.equalsIgnoreCase(configured)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesSubject(BrowseEntry entry, String user, boolean admin) {
        BrowseEntry.Subjects subjects = entry.getSubjects();
        if (subjects.isEmpty()) {
            return admin;
        }
        for (String candidate : subjects.getUsers()) {
            if (candidate.equalsIgnoreCase(user)) {
                return true;
            }
        }
        for (String role : subjects.getRoles()) {
            if ("ADMIN".equalsIgnoreCase(role) && admin) {
                return true;
            }
        }
        return false;
    }

    private static boolean eq(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private static void validate(BrowseEntry entry) {
        if (!StringUtils.hasText(entry.getDatasource()) || !StringUtils.hasText(entry.getDatabase())
                || !StringUtils.hasText(entry.getSchema()) || entry.getTables().isEmpty()) {
            throw new IllegalArgumentException("datasource, database, schema and tables are required");
        }
        for (String table : entry.getTables()) {
            if ("*".equals(table) || table.contains("*")) {
                throw new IllegalArgumentException("wildcard tables are not allowed: " + table);
            }
        }
        if (entry.getPageSize() <= 0 || entry.getPageSize() > 1000) {
            throw new IllegalArgumentException("pageSize must be between 1 and 1000");
        }
        if (entry.getMaxRows() <= 0 || entry.getMaxRows() > 100_000) {
            throw new IllegalArgumentException("maxRows must be between 1 and 100000");
        }
    }

    private void invalidate() {
        activeCache.clear();
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
