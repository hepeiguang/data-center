package com.lhyf.dataasset.lineage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhyf.dataasset.audit.AuditService;
import com.lhyf.dataasset.auth.AuthInterceptor;
import com.lhyf.dataasset.om.OmClient;
import com.lhyf.dataasset.om.OmTableRegistrar;

/**
 * Lineage facade. Table and column lineage stay in OpenMetadata; this service adds only the key mappings
 * and row-level switches that column lineage cannot express.
 */
@RestController
@RequestMapping("/data-asset/lineage")
public class LineageController {

    private final OmClient omClient;
    private final OmTableRegistrar registrar;
    private final LineageConfigService lineageConfig;
    private final RowLineageService rowLineage;
    private final AuditService audit;

    public LineageController(OmClient omClient, OmTableRegistrar registrar, LineageConfigService lineageConfig,
                             RowLineageService rowLineage, AuditService audit) {
        this.omClient = omClient;
        this.registrar = registrar;
        this.lineageConfig = lineageConfig;
        this.rowLineage = rowLineage;
        this.audit = audit;
    }

    @GetMapping("/table")
    public Map<String, Object> table(@RequestParam String fqn,
                                     @RequestParam(defaultValue = "2") int upstreamDepth,
                                     @RequestParam(defaultValue = "2") int downstreamDepth) {
        JsonNode response = omClient.lineage(fqn, Math.min(Math.max(upstreamDepth, 1), 5),
                Math.min(Math.max(downstreamDepth, 1), 5));
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        collectNodes(response.path("nodes"), nodes);
        List<Map<String, Object>> edges = new ArrayList<>();
        collectEdges(response.path("upstreamEdges"), nodes, edges);
        collectEdges(response.path("downstreamEdges"), nodes, edges);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("root", fqn);
        result.put("nodes", new ArrayList<>(nodes.values()));
        result.put("edges", edges);
        return result;
    }

    /** Create or replace a manually maintained table edge, with column mappings, inside OpenMetadata. */
    @PostMapping("/manual")
    public Map<String, Object> manual(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                      @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                      @RequestBody ManualLineageRequest request) {
        requireAdmin(admin);
        long start = System.currentTimeMillis();
        if (!StringUtils.hasText(request.getFromFqn()) || !StringUtils.hasText(request.getToFqn())) {
            throw new IllegalArgumentException("fromFqn 与 toFqn 必填");
        }
        if (request.getFromFqn().equalsIgnoreCase(request.getToFqn())) {
            throw new IllegalArgumentException("上游与下游表不能相同");
        }
        List<String> registered = new ArrayList<>();
        JsonNode from = resolveTable(request.getFromFqn(), registered);
        JsonNode to = resolveTable(request.getToFqn(), registered);
        List<Map<String, Object>> columnsLineage = new ArrayList<>();
        for (ManualLineageRequest.ColumnMapping mapping : request.getColumns()) {
            String source = qualified(mapping.getFrom(), request.getFromFqn(), "上游");
            String target = qualified(mapping.getTo(), request.getToFqn(), "下游");
            requireColumn(from, mapping.getFrom());
            requireColumn(to, mapping.getTo());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fromColumns", List.of(source));
            item.put("toColumn", target);
            if (StringUtils.hasText(mapping.getFunction())) {
                item.put("function", mapping.getFunction().trim());
            }
            columnsLineage.add(item);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("sqlQuery", request.getSqlQuery() == null ? "" : request.getSqlQuery());
        details.put("columnsLineage", columnsLineage);
        details.put("source", "Manual");
        if (StringUtils.hasText(request.getDescription())) {
            details.put("description", request.getDescription());
        }
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("fromEntity", reference(from, request.getFromFqn()));
        edge.put("toEntity", reference(to, request.getToFqn()));
        edge.put("lineageDetails", details);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("edge", edge);

        try {
            omClient.put(omClient.baseUrl() + "/api/v1/lineage", payload);
        } catch (Exception e) {
            audit.record(user, "LINEAGE_WRITE", edgeKey(request), Map.of("error", String.valueOf(e.getMessage())),
                    502, System.currentTimeMillis() - start, null);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "写入 OpenMetadata 失败：" + rootMessage(e));
        }
        audit.record(user, "LINEAGE_WRITE", edgeKey(request),
                Map.of("columns", columnsLineage.size(), "registered", registered),
                200, System.currentTimeMillis() - start, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("from", request.getFromFqn());
        result.put("to", request.getToFqn());
        result.put("columns", columnsLineage.size());
        result.put("registered", registered);
        return result;
    }

    @DeleteMapping("/manual")
    public Map<String, Object> removeManual(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                            @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                            @RequestParam String fromFqn, @RequestParam String toFqn) {
        requireAdmin(admin);
        long start = System.currentTimeMillis();
        try {
            omClient.deleteLineageEdge(fromFqn, toFqn);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "删除血缘失败：" + rootMessage(e));
        }
        audit.record(user, "LINEAGE_DELETE", fromFqn + "--->" + toFqn, Map.of(), 200,
                System.currentTimeMillis() - start, null);
        return Map.of("status", "ok");
    }

    @GetMapping("/row/resolve")
    public Map<String, Object> rowResolve(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                          @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                          @RequestParam String fqn,
                                          @RequestParam(required = false) List<String> key,
                                          @RequestParam(defaultValue = "3") int depth) {
        Map<String, String> keys = new LinkedHashMap<>();
        for (String pair : key == null ? List.<String>of() : key) {
            int index = pair.indexOf('=');
            if (index > 0) {
                keys.put(pair.substring(0, index).trim(), pair.substring(index + 1).trim());
            }
        }
        Map<String, Object> result = rowLineage.resolve(fqn, keys, depth, user, admin);
        audit.record(user, "ROW_LINEAGE", fqn,
                Map.of("keys", keys, "depth", depth, "status", result.getOrDefault("status", ""),
                        "nodes", result.getOrDefault("nodeCount", 0)),
                200, ((Number) result.getOrDefault("durationMs", 0)).longValue(), null);
        return result;
    }

    @GetMapping("/keys")
    public List<Map<String, Object>> keys(@RequestParam(required = false) String targetFqn) {
        return lineageConfig.listKeys(targetFqn);
    }

    @PostMapping("/keys")
    public Map<String, Object> addKey(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                      @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                      @RequestParam String targetFqn, @RequestParam String keyColumn,
                                      @RequestParam String sourceFqn, @RequestParam String sourceKeyColumn) {
        requireAdmin(admin);
        Map<String, Object> saved;
        try {
            saved = lineageConfig.addKey(targetFqn, keyColumn, sourceFqn, sourceKeyColumn, user);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "键映射已存在或写入失败：" + rootMessage(e));
        }
        audit.record(user, "KEY_MAPPING_WRITE", targetFqn,
                Map.of("keyColumn", keyColumn, "sourceFqn", sourceFqn, "sourceKeyColumn", sourceKeyColumn),
                200, 0, null);
        return saved;
    }

    @DeleteMapping("/keys/{id}")
    public Map<String, Object> removeKey(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                         @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                         @PathVariable long id) {
        requireAdmin(admin);
        boolean removed = lineageConfig.removeKey(id);
        audit.record(user, "KEY_MAPPING_DELETE", String.valueOf(id), Map.of("removed", removed), 200, 0, null);
        return Map.of("status", removed ? "ok" : "not_found");
    }

    @GetMapping("/row-switch")
    public List<Map<String, Object>> rowSwitch() {
        return lineageConfig.listSwitches();
    }

    @PutMapping("/row-switch")
    public Map<String, Object> setRowSwitch(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                            @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                            @RequestParam String tableFqn, @RequestParam boolean enabled) {
        requireAdmin(admin);
        Map<String, Object> result = lineageConfig.setSwitch(tableFqn, enabled, user);
        audit.record(user, "ROW_SWITCH", tableFqn, Map.of("enabled", enabled), 200, 0, null);
        return result;
    }

    private JsonNode resolveTable(String fqn, List<String> registered) {
        JsonNode table = null;
        try {
            table = omClient.getOrNull(omClient.baseUrl() + "/api/v1/tables/name/" + encode(fqn));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenMetadata 不可用：" + e.getMessage());
        }
        if (table == null || !StringUtils.hasText(table.path("id").asText(""))) {
            registrar.ensure(fqn);
            registered.add(fqn);
            table = omClient.tableRef(fqn);
        }
        if (!StringUtils.hasText(table.path("id").asText(""))) {
            throw new IllegalArgumentException("OM 未返回表 id：" + fqn);
        }
        return table;
    }

    private static String encode(String fqn) {
        return java.net.URLEncoder.encode(fqn, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void requireAdmin(boolean admin) {
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员可以维护血缘");
        }
    }

    private static String edgeKey(ManualLineageRequest request) {
        return request.getFromFqn() + "--->" + request.getToFqn();
    }

    private static Map<String, Object> reference(JsonNode entity, String fqn) {
        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("id", entity.path("id").asText(""));
        reference.put("type", "table");
        reference.put("name", entity.path("name").asText(lastSegment(fqn)));
        reference.put("fullyQualifiedName", entity.path("fullyQualifiedName").asText(fqn));
        if (StringUtils.hasText(entity.path("displayName").asText(""))) {
            reference.put("displayName", entity.path("displayName").asText(""));
        }
        return reference;
    }

    private static String qualified(String column, String tableFqn, String label) {
        if (!StringUtils.hasText(column)) {
            throw new IllegalArgumentException(label + "列名不能为空");
        }
        return tableFqn + "." + column.trim();
    }

    private static void requireColumn(JsonNode table, String column) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode node : table.path("columns")) {
            names.add(node.path("name").asText(""));
        }
        if (!names.isEmpty() && !names.contains(column.trim())) {
            throw new IllegalArgumentException("列 " + column + " 不属于表 "
                    + table.path("fullyQualifiedName").asText(""));
        }
    }

    private static String rootMessage(Exception e) {
        Throwable cursor = e;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        if (message == null) {
            return cursor.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 300));
    }

    private void collectNodes(JsonNode raw, Map<String, Map<String, Object>> nodes) {
        if (raw == null || !raw.isObject()) {
            return;
        }
        raw.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            JsonNode entity = value.has("entity") ? value.path("entity") : value;
            String fqn = entity.path("fullyQualifiedName").asText(entry.getKey());
            if (fqn.isEmpty() || nodes.containsKey(fqn)) {
                return;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("fqn", fqn);
            node.put("name", entity.path("name").asText(lastSegment(fqn)));
            node.put("displayName", entity.path("displayName").asText(""));
            node.put("type", entity.path("type").asText("table"));
            node.put("database", entity.path("database").path("name").asText(""));
            node.put("service", entity.path("service").path("name").asText(""));
            node.put("columnCount", entity.path("columns").size());
            node.put("certification", entity.path("certification").path("tagLabel").asText(""));
            List<String> tags = new ArrayList<>();
            for (JsonNode tag : entity.path("tags")) {
                tags.add(tag.path("tagFQN").asText(""));
            }
            node.put("tags", tags);
            nodes.put(fqn, node);
        });
    }

    private void collectEdges(JsonNode raw, Map<String, Map<String, Object>> nodes,
                              List<Map<String, Object>> edges) {
        if (raw == null || !raw.isObject()) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        raw.elements().forEachRemaining(edge -> {
            String from = fqnOf(edge.path("fromEntity"));
            String to = fqnOf(edge.path("toEntity"));
            if (from.isEmpty() || to.isEmpty() || !seen.add(from + "--->" + to)) {
                return;
            }
            ensureNode(nodes, from);
            ensureNode(nodes, to);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("from", from);
            item.put("to", to);
            item.put("sqlQuery", edge.path("sqlQuery").asText(""));
            item.put("source", edge.path("source").asText(""));
            List<Map<String, String>> columns = new ArrayList<>();
            JsonNode columnArray = edge.has("columns") ? edge.path("columns")
                    : edge.path("lineageDetails").path("columnsLineage");
            if (columnArray != null && columnArray.isArray()) {
                for (JsonNode column : columnArray) {
                    String target = shortName(column.path("toColumn").asText(""));
                    for (JsonNode source : column.path("fromColumns")) {
                        Map<String, String> mapping = new LinkedHashMap<>();
                        mapping.put("from", shortName(source.asText("")));
                        mapping.put("fromTable", tableOf(source.asText("")));
                        mapping.put("to", target);
                        mapping.put("function", column.path("function").asText(""));
                        columns.add(mapping);
                    }
                }
            }
            item.put("columns", columns);
            edges.add(item);
        });
    }

    private static void ensureNode(Map<String, Map<String, Object>> nodes, String fqn) {
        if (!nodes.containsKey(fqn)) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("fqn", fqn);
            node.put("name", lastSegment(fqn));
            node.put("displayName", "");
            node.put("type", "table");
            nodes.put(fqn, node);
        }
    }

    private static String fqnOf(JsonNode entity) {
        String fqn = entity.path("fullyQualifiedName").asText("");
        return fqn.isEmpty() ? entity.path("id").asText("") : fqn;
    }

    private static String lastSegment(String fqn) {
        int index = fqn == null ? -1 : fqn.lastIndexOf('.');
        return index < 0 ? fqn : fqn.substring(index + 1);
    }

    private static String shortName(String fqn) {
        return fqn == null || fqn.isEmpty() ? "" : lastSegment(fqn);
    }

    private static String tableOf(String columnFqn) {
        if (columnFqn == null) {
            return "";
        }
        int index = columnFqn.lastIndexOf('.');
        return index < 0 ? "" : columnFqn.substring(0, index);
    }
}
