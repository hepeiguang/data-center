package com.lhyf.dataasset.browse;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhyf.dataasset.audit.AuditService;
import com.lhyf.dataasset.config.BusinessDataSources;
import com.lhyf.dataasset.config.DataAssetProperties;
import com.lhyf.dataasset.om.OmClient;
import com.lhyf.dataasset.whitelist.BrowseEntry;
import com.lhyf.dataasset.whitelist.WhitelistService;

@Service
public class BrowseService {

    private final WhitelistService whitelist;
    private final OmClient omClient;
    private final BusinessDataSources dataSources;
    private final AuditService audit;
    private final DataAssetProperties properties;
    private final UserAttrRepository userAttrs;

    public BrowseService(WhitelistService whitelist, OmClient omClient, BusinessDataSources dataSources,
                         AuditService audit, DataAssetProperties properties, UserAttrRepository userAttrs) {
        this.whitelist = whitelist;
        this.omClient = omClient;
        this.dataSources = dataSources;
        this.audit = audit;
        this.properties = properties;
        this.userAttrs = userAttrs;
    }

    public Map<String, Object> browse(String user, boolean admin, String fqn, int page, int size,
                                      String orderBy, boolean ascending, List<QueryBuilder.Filter> filters,
                                      String clientIp) {
        long started = System.currentTimeMillis();
        BrowseEntry entry = whitelist.findActiveFor(user, admin, fqn);
        if (entry == null) {
            audit.record(user, "BROWSE", fqn, Map.of("reason", "not whitelisted"), 403,
                    System.currentTimeMillis() - started, clientIp);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "table is not open for browsing");
        }
        TableRef table = TableRef.parse(fqn);
        List<String> visible = visibleColumns(entry, table, fqn);
        int effectiveSize = Math.min(Math.max(size, 1), Math.min(entry.getPageSize(), 1000));
        int effectivePage = Math.max(page, 1);
        int offset = (effectivePage - 1) * effectiveSize;
        if (offset + effectiveSize > entry.getMaxRows()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "paging exceeds maxRows=" + entry.getMaxRows() + "; use a narrower filter");
        }
        QueryBuilder.SqlQuery query = QueryBuilder.build(dataSources.dialect(entry.getDatasource()),
                table.schema, table.table, visible, filters,
                orderBy, ascending, effectiveSize + 1, offset, entry.getRowPolicy(), userAttributes(user, admin));

        List<Object[]> rows = new ArrayList<>();
        dataSources.get(entry.getDatasource()).query(query.sql, query.params.toArray(), (ResultSet rs) -> {
            int count = visible.size();
            Object[] row = new Object[count];
            for (int i = 0; i < count; i++) {
                Object value = rs.getObject(i + 1);
                if (entry.getMaskColumns().contains(visible.get(i))) {
                    value = "******";
                }
                row[i] = value;
            }
            rows.add(row);
        });

        boolean hasMore = rows.size() > effectiveSize;
        if (hasMore) {
            rows.remove(rows.size() - 1);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entryId", entry.getId());
        result.put("fqn", fqn);
        result.put("columns", visible);
        result.put("maskedColumns", entry.getMaskColumns());
        result.put("rows", rows);
        result.put("page", effectivePage);
        result.put("size", effectiveSize);
        result.put("hasMore", hasMore);
        audit.record(user, "BROWSE", fqn,
                Map.of("entryId", entry.getId(), "columns", visible.size(), "rows", rows.size(),
                        "page", effectivePage), 200, System.currentTimeMillis() - started, clientIp);
        return result;
    }

    /**
     * Column visibility and masking for one table. Row-level lineage uses this so full rows follow the
     * same whitelist rules as browsing, without duplicating them.
     */
    public Map<String, Object> columnPolicy(String user, boolean admin, String fqn) {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("visible", List.of());
        policy.put("masked", List.of());
        BrowseEntry entry = whitelist.findActiveFor(user, admin, fqn);
        if (entry == null) {
            policy.put("available", false);
            policy.put("reason", "该表未开放数据浏览");
            return policy;
        }
        try {
            policy.put("available", true);
            policy.put("visible", visibleColumns(entry, TableRef.parse(fqn), fqn));
            policy.put("masked", entry.getMaskColumns());
        } catch (Exception e) {
            policy.put("available", false);
            policy.put("reason", e.getMessage());
        }
        return policy;
    }

    public String exportCsv(String user, boolean admin, String fqn, String clientIp) {
        long started = System.currentTimeMillis();
        BrowseEntry entry = whitelist.findActiveFor(user, admin, fqn);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "table is not open for browsing");
        }
        if (!entry.isExportEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "export is disabled for this entry");
        }
        TableRef table = TableRef.parse(fqn);
        List<String> visible = visibleColumns(entry, table, fqn);
        int limit = Math.min(entry.getMaxRows(), 10_000);
        QueryBuilder.SqlQuery query = QueryBuilder.build(dataSources.dialect(entry.getDatasource()),
                table.schema, table.table, visible, List.of(),
                null, true, limit, 0, entry.getRowPolicy(), userAttributes(user, admin));
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", visible)).append('\n');
        dataSources.get(entry.getDatasource()).query(query.sql, query.params.toArray(), (ResultSet rs) -> {
            for (int i = 0; i < visible.size(); i++) {
                if (i > 0) {
                    csv.append(',');
                }
                Object value = rs.getObject(i + 1);
                if (entry.getMaskColumns().contains(visible.get(i))) {
                    value = "******";
                }
                csv.append(escape(value));
            }
            csv.append('\n');
        });
        audit.record(user, "EXPORT", fqn, Map.of("entryId", entry.getId(), "limit", limit), 200,
                System.currentTimeMillis() - started, clientIp);
        return csv.toString();
    }

    private List<String> visibleColumns(BrowseEntry entry, TableRef table, String fqn) {
        JsonNode detail;
        try {
            detail = omClient.tableByFqn(fqn);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "cannot read columns from OM");
        }
        List<String> all = new ArrayList<>();
        for (JsonNode column : detail.path("columns")) {
            String name = column.path("name").asText("");
            if (!name.isEmpty()) {
                all.add(name);
            }
        }
        if (all.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OM returned no columns for " + fqn);
        }
        List<String> visible = new ArrayList<>();
        for (String column : all) {
            boolean included = entry.getIncludeColumns().isEmpty() || entry.getIncludeColumns().contains(column);
            if (included && !entry.getExcludeColumns().contains(column)) {
                visible.add(column);
            }
        }
        if (visible.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no visible columns for this user");
        }
        return visible;
    }

    private Map<String, String> userAttributes(String user, boolean admin) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("name", user);
        attrs.put("user", user);
        attrs.put("admin", Boolean.toString(admin));
        attrs.putAll(userAttrs.findAttributes(user));
        return attrs;
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    static final class TableRef {
        final String datasource;
        final String database;
        final String schema;
        final String table;

        private TableRef(String datasource, String database, String schema, String table) {
            this.datasource = datasource;
            this.database = database;
            this.schema = schema;
            this.table = table;
        }

        static TableRef parse(String fqn) {
            String[] parts = fqn == null ? new String[0] : fqn.split("\\.");
            if (parts.length != 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "only table FQN service.database.schema.table is supported");
            }
            return new TableRef(parts[0], parts[1], parts[2], parts[3]);
        }
    }
}
