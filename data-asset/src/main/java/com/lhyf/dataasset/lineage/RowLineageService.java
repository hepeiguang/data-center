package com.lhyf.dataasset.lineage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhyf.dataasset.browse.BrowseService;
import com.lhyf.dataasset.config.BusinessDataSources;
import com.lhyf.dataasset.om.OmClient;
import com.lhyf.dataasset.util.TtlCache;

/**
 * Row-level lineage without an edge store: walk column-level lineage in OpenMetadata upstream from the
 * target row, translate the key values into a read-only lookup on each source table, and recurse.
 * One node is one matched row, carrying the full row content allowed by the browse whitelist; {@code origin}
 * records how the hop above it was matched. Results reflect the current snapshot and are never persisted.
 */
@Service
public class RowLineageService {

    private static final Logger LOG = LoggerFactory.getLogger(RowLineageService.class);
    private static final int MAX_NODES = 300;
    private static final int MAX_ROWS_PER_EDGE = 20;

    private final OmClient omClient;
    private final LineageConfigService config;
    private final BusinessDataSources dataSources;
    private final BrowseService browse;
    private final TtlCache<String, Map<String, String>> typeCache = new TtlCache<>(60_000);
    private final TtlCache<String, Map<String, Object>> policyCache = new TtlCache<>(10_000);

    public RowLineageService(OmClient omClient, LineageConfigService config, BusinessDataSources dataSources,
                             BrowseService browse) {
        this.omClient = omClient;
        this.config = config;
        this.dataSources = dataSources;
        this.browse = browse;
    }

    public Map<String, Object> resolve(String fqn, Map<String, String> keys, int depth, String user,
                                       boolean admin) {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("root", fqn);
        result.put("keys", keys);
        result.put("resolvedAt", Instant.now().toString());
        result.put("note", "按当前数据解析，历史不保证可复现");
        if (keys.isEmpty()) {
            result.put("status", "NO_KEY");
            result.put("message", "需要至少一个键列及其取值");
            return result;
        }
        if (!config.rowLineageEnabled(fqn)) {
            result.put("status", "ROW_LINEAGE_DISABLED");
            result.put("message", "该表未开启行级追踪");
            return result;
        }
        Counter counter = new Counter();
        RowData root = fetch(fqn, keys, user, admin);
        Map<String, Object> tree = node(fqn, keys, Math.max(1, Math.min(depth, 5)), counter, new HashSet<>(), root,
                user, admin);
        if (root.rows.isEmpty()) {
            tree.put("status", "ROW_NOT_FOUND");
            tree.put("message", "目标表当前数据中没有匹配行，请核对键列取值");
        }
        result.put("status", tree.get("status"));
        result.put("tree", tree);
        result.put("nodeCount", counter.nodes);
        result.put("truncated", counter.nodes >= MAX_NODES);
        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }

    private Map<String, Object> node(String fqn, Map<String, String> keys, int depth, Counter counter,
                                     Set<String> visited, RowData data, String user, boolean admin) {
        counter.nodes++;
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("fqn", fqn);
        node.put("table", lastSegment(fqn));
        node.put("keys", keys);
        applyRow(node, data);
        node.put("status", "OK");
        List<Map<String, Object>> children = new ArrayList<>();
        node.put("children", children);

        if (!config.rowLineageEnabled(fqn)) {
            return fail(node, "ROW_LINEAGE_DISABLED", "该表未开启行级追踪");
        }
        if (depth <= 1) {
            return fail(node, "DEPTH_LIMIT", "已到展开层数上限");
        }
        if (!visited.add(fqn.toLowerCase() + "|" + new java.util.TreeMap<>(keys).toString())) {
            return fail(node, "CYCLE_SKIPPED", "同键节点已展开过，避免成环");
        }
        if (counter.nodes >= MAX_NODES) {
            return fail(node, "TRUNCATED", "已达单次解析节点上限 " + MAX_NODES);
        }

        JsonNode lineage = omClient.lineageEdges(fqn, 1, 1);
        Map<String, String> idToName = indexNodes(lineage);
        List<Edge> edges = new ArrayList<>();
        for (JsonNode edge : lineage.path("upstreamEdges")) {
            String from = idToName.get(edge.path("fromEntity").asText(""));
            if (from == null || from.equalsIgnoreCase(fqn)) {
                continue;
            }
            edges.add(new Edge(from, edge.path("lineageDetails")));
        }
        if (edges.isEmpty()) {
            return fail(node, "NO_UPSTREAM", "该表没有上游血缘（源层）");
        }
        for (Edge edge : edges) {
            children.addAll(expand(fqn, keys, edge.from, edge.details, depth, counter, visited, user, admin));
        }
        return node;
    }

    private List<Map<String, Object>> expand(String targetFqn, Map<String, String> keys, String sourceFqn,
                                             JsonNode details, int depth, Counter counter, Set<String> visited,
                                             String user, boolean admin) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> origin = new LinkedHashMap<>();
        origin.put("from", targetFqn);
        origin.put("sql", details.path("sqlQuery").asText(""));
        origin.put("edgeSource", details.path("source").asText(""));

        Map<String, String> byTargetKey = new LinkedHashMap<>();
        Set<String> functions = new LinkedHashSet<>();
        for (JsonNode mapping : details.path("columnsLineage")) {
            String toColumn = shortColumn(mapping.path("toColumn").asText(""));
            if (toColumn.isEmpty() || !keys.containsKey(toColumn)) {
                continue;
            }
            for (JsonNode source : mapping.path("fromColumns")) {
                String sourceColumn = source.asText("");
                if (!sameTable(sourceColumn, sourceFqn)) {
                    continue;
                }
                byTargetKey.putIfAbsent(toColumn, shortColumn(sourceColumn));
                String function = mapping.path("function").asText("");
                if (!function.isEmpty()) {
                    functions.add(function);
                }
            }
        }

        Map<String, String> registered = config.keyMapping(targetFqn, sourceFqn);
        List<String> fromRegistry = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();
        for (String keyColumn : keys.keySet()) {
            String sourceColumn = byTargetKey.get(keyColumn);
            if (sourceColumn == null && registered.containsKey(keyColumn)) {
                sourceColumn = registered.get(keyColumn);
                fromRegistry.add(keyColumn + "→" + sourceColumn);
            }
            if (sourceColumn == null) {
                unmapped.add(keyColumn);
                continue;
            }
            byTargetKey.putIfAbsent(keyColumn, sourceColumn);
        }
        if (byTargetKey.isEmpty()) {
            result.add(leaf(sourceFqn, "NO_KEY_LINEAGE", "键列 " + String.join("、", keys.keySet())
                    + " 在该边上既没有列级映射，也没有登记键映射", origin));
            return result;
        }
        if (!functions.isEmpty()) {
            origin.put("functions", new ArrayList<>(functions));
        }
        if (!fromRegistry.isEmpty()) {
            origin.put("viaRegistry", fromRegistry);
        }
        if (!unmapped.isEmpty()) {
            origin.put("partialKey", unmapped);
        }

        Map<String, String> conditions = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : byTargetKey.entrySet()) {
            conditions.put(entry.getValue(), keys.get(entry.getKey()));
        }
        RowData data;
        try {
            data = fetch(sourceFqn, conditions, user, admin);
        } catch (Exception e) {
            LOG.warn("row lineage lookup failed for {}", sourceFqn, e);
            result.add(leaf(sourceFqn, "QUERY_FAILED", safeMessage(e), origin));
            return result;
        }
        if (data.rows.isEmpty()) {
            result.add(leaf(sourceFqn, "ROW_NOT_FOUND", "在源表当前数据中未找到匹配行", origin));
            return result;
        }
        origin.put("matchedRows", data.rows.size());
        boolean aggregate = details.path("sqlQuery").asText("").toLowerCase().contains("group by");
        if (aggregate || data.rows.size() > 1) {
            origin.put("participatingSet", true);
        }

        for (Map<String, Object> row : data.rows) {
            Map<String, String> nextKeys = new LinkedHashMap<>();
            for (String column : conditions.keySet()) {
                nextKeys.put(column, row.get(column) == null ? "" : String.valueOf(row.get(column)));
            }
            if (counter.nodes >= MAX_NODES) {
                origin.put("truncated", true);
                break;
            }
            RowData single = new RowData(List.of(row), data.columns, data.note);
            Map<String, Object> child = node(sourceFqn, nextKeys, depth - 1, counter, visited, single, user, admin);
            child.put("origin", origin);
            result.add(child);
        }
        return result;
    }

    /** One read-only lookup: filter by the mapped key values, select whitelist-visible columns plus keys. */
    @SuppressWarnings("unchecked")
    private RowData fetch(String fqn, Map<String, String> conditions, String user, boolean admin) {
        TableRef table = TableRef.parse(fqn);
        Map<String, Object> policy = policy(fqn, user, admin);
        List<String> visible = (List<String>) policy.getOrDefault("visible", List.of());
        List<String> masked = (List<String>) policy.getOrDefault("masked", List.of());
        List<String> selected = new ArrayList<>(visible);
        for (String column : conditions.keySet()) {
            if (!selected.contains(column)) {
                selected.add(column);
            }
        }
        Lookup lookup = buildLookup(table, fqn, conditions, selected);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> raw : readOnly(dataSources.get(table.datasource))
                .queryForList(lookup.sql, lookup.params.toArray())) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String column : selected) {
                Object value = raw.get(column);
                row.put(column, masked.contains(column) ? "******" : value);
            }
            rows.add(row);
        }
        String note = Boolean.TRUE.equals(policy.get("available")) ? ""
                : String.valueOf(policy.getOrDefault("reason", "该表未开放数据浏览"));
        return new RowData(rows, visible, note);
    }

    private Map<String, Object> policy(String fqn, String user, boolean admin) {
        return policyCache.get(fqn.toLowerCase() + "|" + user + "|" + admin,
                () -> browse.columnPolicy(user, admin, fqn));
    }

    private static void applyRow(Map<String, Object> node, RowData data) {
        if (data == null) {
            return;
        }
        if (!data.rows.isEmpty()) {
            node.put("rows", data.rows);
        }
        if (!data.columns.isEmpty()) {
            node.put("rowColumns", data.columns);
        }
        if (data.note != null && !data.note.isEmpty()) {
            node.put("rowNote", data.note);
        }
    }

    private Map<String, Object> leaf(String fqn, String status, String message, Map<String, Object> origin) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("fqn", fqn);
        node.put("table", lastSegment(fqn));
        node.put("status", status);
        node.put("message", message);
        node.put("origin", origin);
        node.put("children", new ArrayList<>());
        return node;
    }

    private static Map<String, Object> fail(Map<String, Object> node, String status, String message) {
        node.put("status", status);
        node.put("message", message);
        return node;
    }

    private Lookup buildLookup(TableRef table, String fqn, Map<String, String> conditions, List<String> selected) {
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("no column to select for " + fqn);
        }
        com.lhyf.dataasset.browse.SqlDialect dialect = dataSources.dialect(table.datasource);
        Map<String, String> types = columnTypes(fqn);
        StringBuilder sql = new StringBuilder("select ");
        for (int i = 0; i < selected.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(dialect.quote(selected.get(i)));
        }
        sql.append(" from ").append(dialect.quote(table.schema)).append('.')
                .append(dialect.quote(table.table));
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            where.add(dialect.quote(entry.getKey()) + " = "
                    + placeholder(types.get(entry.getKey().toLowerCase())));
            params.add(entry.getValue());
        }
        sql.append(" where ").append(String.join(" and ", where)).append(dialect.limitOffset());
        params.add(MAX_ROWS_PER_EDGE);
        params.add(0);
        return new Lookup(sql.toString(), params);
    }

    /**
     * A raw string parameter makes Postgres reject {@code date = varchar}; casting the parameter keeps the
     * comparison on the indexed column type instead of casting the column itself.
     */
    private static String placeholder(String type) {
        return type == null || !type.matches("^[A-Za-z][A-Za-z0-9_]{0,30}$")
                ? "?" : "cast(? as " + type + ")";
    }

    private Map<String, String> columnTypes(String fqn) {
        String key = fqn.toLowerCase();
        return typeCache.get(key, () -> {
            Map<String, String> types = new LinkedHashMap<>();
            try {
                for (JsonNode column : omClient.tableRef(key).path("columns")) {
                    types.put(column.path("name").asText("").toLowerCase(), column.path("dataType").asText(""));
                }
            } catch (Exception e) {
                LOG.warn("column types unavailable for {}", key);
            }
            return types;
        });
    }

    private Map<String, String> indexNodes(JsonNode lineage) {
        Map<String, String> idToName = new LinkedHashMap<>();
        for (JsonNode node : lineage.path("nodes")) {
            put(idToName, node);
        }
        put(idToName, lineage.path("entity"));
        return idToName;
    }

    private static void put(Map<String, String> idToName, JsonNode ref) {
        String id = ref.path("id").asText("");
        String fqn = ref.path("fullyQualifiedName").asText("");
        if (!id.isEmpty() && !fqn.isEmpty()) {
            idToName.put(id, fqn);
        }
    }

    private static boolean sameTable(String columnFqn, String tableFqn) {
        int index = columnFqn.lastIndexOf('.');
        if (index <= 0) {
            return false;
        }
        return columnFqn.substring(0, index).equalsIgnoreCase(tableFqn);
    }

    private static String shortColumn(String columnFqn) {
        int index = columnFqn == null ? -1 : columnFqn.lastIndexOf('.');
        return index < 0 ? columnFqn : columnFqn.substring(index + 1);
    }

    private static String lastSegment(String fqn) {
        int index = fqn == null ? -1 : fqn.lastIndexOf('.');
        return index < 0 ? fqn : fqn.substring(index + 1);
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 200));
    }

    private static JdbcTemplate readOnly(JdbcTemplate shared) {
        JdbcTemplate jdbc = new JdbcTemplate(shared.getDataSource());
        jdbc.setQueryTimeout(5);
        jdbc.setMaxRows(MAX_ROWS_PER_EDGE);
        return jdbc;
    }

    /** Matched rows plus the columns the whitelist allows to display. */
    private static final class RowData {
        private final List<Map<String, Object>> rows;
        private final List<String> columns;
        private final String note;

        RowData(List<Map<String, Object>> rows, List<String> columns, String note) {
            this.rows = rows;
            this.columns = columns;
            this.note = note;
        }
    }

    static final class TableRef {
        final String datasource;
        final String schema;
        final String table;

        private TableRef(String datasource, String schema, String table) {
            this.datasource = datasource;
            this.schema = schema;
            this.table = table;
        }

        static TableRef parse(String fqn) {
            String[] parts = fqn == null ? new String[0] : fqn.split("\\.");
            if (parts.length != 4) {
                throw new IllegalArgumentException("only table FQN service.database.schema.table: " + fqn);
            }
            return new TableRef(parts[0], parts[2], parts[3]);
        }
    }

    private static final class Edge {
        private final String from;
        private final JsonNode details;

        Edge(String from, JsonNode details) {
            this.from = from;
            this.details = details;
        }
    }

    private static final class Lookup {
        final String sql;
        final List<Object> params;

        Lookup(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }

    private static final class Counter {
        private int nodes;
    }
}
