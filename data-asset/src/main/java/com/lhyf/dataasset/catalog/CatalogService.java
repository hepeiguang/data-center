package com.lhyf.dataasset.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhyf.dataasset.om.OmClient;
import com.lhyf.dataasset.whitelist.BrowseEntry;
import com.lhyf.dataasset.whitelist.WhitelistService;

/** Metadata endpoints are read-only proxies to OpenMetadata, plus whitelist visibility flags. */
@Service
public class CatalogService {

    private final OmClient omClient;
    private final WhitelistService whitelist;

    public CatalogService(OmClient omClient, WhitelistService whitelist) {
        this.omClient = omClient;
        this.whitelist = whitelist;
    }

    public Map<String, Object> search(String q, int size, int from) {
        JsonNode response = omClient.searchTables(q, Math.min(Math.max(size, 1), 100), Math.max(from, 0));
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            items.add(toSummary(hit.path("_source")));
        }
        return Map.of("total", response.path("hits").path("total").path("value").asInt(items.size()),
                "items", items);
    }

    public List<Map<String, Object>> tree(int size) {
        JsonNode response = omClient.searchTables("*", Math.min(Math.max(size, 1), 1000), 0);
        Map<String, Map<String, Map<String, List<Map<String, Object>>>>> grouped = new LinkedHashMap<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            Map<String, Object> table = toSummary(hit.path("_source"));
            String service = String.valueOf(table.get("service"));
            String database = String.valueOf(table.get("database"));
            String schema = String.valueOf(table.get("schema"));
            grouped.computeIfAbsent(service, k -> new LinkedHashMap<>())
                    .computeIfAbsent(database, k -> new LinkedHashMap<>())
                    .computeIfAbsent(schema, k -> new ArrayList<>())
                    .add(table);
        }
        List<Map<String, Object>> services = new ArrayList<>();
        grouped.forEach((service, databases) -> {
            List<Map<String, Object>> databaseNodes = new ArrayList<>();
            databases.forEach((database, schemas) -> {
                List<Map<String, Object>> schemaNodes = new ArrayList<>();
                schemas.forEach((schema, tables) ->
                        schemaNodes.add(Map.of("schema", schema, "tables", tables)));
                databaseNodes.add(Map.of("database", database, "schemas", schemaNodes));
            });
            services.add(Map.of("service", service, "databases", databaseNodes));
        });
        return services;
    }

    public Map<String, Object> detail(String user, boolean admin, String fqn) {
        JsonNode entity = omClient.tableByFqn(fqn);
        BrowseEntry entry = whitelist.findActiveFor(user, admin, fqn);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entity", entity);
        result.put("browseAllowed", entry != null);
        result.put("entryId", entry == null ? null : entry.getId());
        return result;
    }

    public List<Map<String, Object>> columns(String user, boolean admin, String fqn) {
        JsonNode entity = omClient.tableByFqn(fqn);
        BrowseEntry entry = whitelist.findActiveFor(user, admin, fqn);
        List<Map<String, Object>> columns = new ArrayList<>();
        for (JsonNode column : entity.path("columns")) {
            String name = column.path("name").asText("");
            boolean included = entry == null || entry.getIncludeColumns().isEmpty()
                    || entry.getIncludeColumns().contains(name);
            boolean excluded = entry != null && entry.getExcludeColumns().contains(name);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("dataType", column.path("dataType").asText(""));
            item.put("constraint", column.path("constraint").asText(""));
            item.put("description", column.path("description").asText(""));
            item.put("visible", included && !excluded);
            item.put("masked", entry != null && entry.getMaskColumns().contains(name));
            columns.add(item);
        }
        return columns;
    }

    private static Map<String, Object> toSummary(JsonNode source) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fqn", source.path("fullyQualifiedName").asText(""));
        item.put("name", source.path("name").asText(""));
        item.put("displayName", source.path("displayName").asText(""));
        item.put("description", source.path("description").asText(""));
        item.put("service", source.path("service").path("name").asText(""));
        item.put("database", source.path("database").path("name").asText(""));
        item.put("schema", source.path("databaseSchema").path("name").asText(""));
        item.put("entityType", source.path("entityType").asText("table"));
        return item;
    }
}
