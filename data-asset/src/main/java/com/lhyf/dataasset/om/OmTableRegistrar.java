package com.lhyf.dataasset.om;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhyf.dataasset.datasource.DatasourceMetadataService;

/**
 * Registers a business table into OpenMetadata from live database metadata, so lineage can be wired up
 * before any ingestion job has collected it. Creates the database and schema when they are missing.
 */
@Component
public class OmTableRegistrar {

    private final OmClient om;
    private final DatasourceMetadataService metadata;

    public OmTableRegistrar(OmClient om, DatasourceMetadataService metadata) {
        this.om = om;
        this.metadata = metadata;
    }

    /** @return {@code {id, fqn, created, databaseCreated, schemaCreated, columns}} */
    public Map<String, Object> ensure(String fqn) {
        Map<String, Object> result = new LinkedHashMap<>();
        JsonNode existing = om.getOrNull(om.baseUrl() + "/api/v1/tables/name/" + encode(fqn));
        if (existing != null && existing.hasNonNull("id")) {
            result.put("id", existing.path("id").asText());
            result.put("fqn", fqn);
            result.put("created", false);
            result.put("columns", existing.path("columns").size());
            return result;
        }

        String[] parts = fqn.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("only table FQN service.database.schema.table: " + fqn);
        }
        String service = parts[0];
        String database = parts[1];
        String schema = parts[2];
        String table = parts[3];

        boolean databaseCreated = ensureDatabase(service, database);
        boolean schemaCreated = ensureSchema(service, database, schema);

        List<Map<String, Object>> columns = new ArrayList<>();
        for (Map<String, Object> column : metadata.columns(service, schema, table)) {
            Map<String, Object> item = new LinkedHashMap<>();
            String name = String.valueOf(column.get("name"));
            String dataType = omDataType(String.valueOf(column.get("dataType")));
            item.put("name", name);
            item.put("displayName", name);
            item.put("dataType", dataType);
            item.put("fullyQualifiedName", fqn + "." + name);
            item.put("constraint", "PRIMARY_KEY".equals(column.get("constraint")) ? "PRIMARY_KEY" : "NULL");
            if (column.get("ordinalPosition") != null) {
                item.put("ordinalPosition", column.get("ordinalPosition"));
            }
            // OM rejects varchar/char columns without a length
            if ("VARCHAR".equals(dataType) || "CHAR".equals(dataType)) {
                item.put("dataLength", column.get("characterMaximumLength") != null
                        ? column.get("characterMaximumLength") : 65535);
            }
            if ("NUMERIC".equals(dataType) || "DECIMAL".equals(dataType)) {
                if (column.get("numericPrecision") != null) {
                    item.put("precision", column.get("numericPrecision"));
                }
                if (column.get("numericScale") != null) {
                    item.put("scale", column.get("numericScale"));
                }
            }
            columns.add(item);
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("数据源中没有找到表 " + fqn + "（或该表没有可读列）");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", table);
        payload.put("displayName", table);
        // fullyQualifiedName is not part of OM's CreateTable shape; OM composes it from databaseSchema
        payload.put("databaseSchema", service + "." + database + "." + schema);
        payload.put("tableType", "Regular");
        payload.put("columns", columns);
        JsonNode created = post("/api/v1/tables", payload);

        result.put("id", created.path("id").asText());
        result.put("fqn", created.path("fullyQualifiedName").asText(fqn));
        result.put("created", true);
        result.put("databaseCreated", databaseCreated);
        result.put("schemaCreated", schemaCreated);
        result.put("columns", columns.size());
        return result;
    }

    private boolean ensureDatabase(String service, String database) {
        String fqn = service + "." + database;
        if (om.getOrNull(om.baseUrl() + "/api/v1/databases/name/" + encode(fqn)) != null) {
            return false;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", database);
        payload.put("displayName", database);
        payload.put("service", service);
        post("/api/v1/databases", payload);
        return true;
    }

    private boolean ensureSchema(String service, String database, String schema) {
        String fqn = service + "." + database + "." + schema;
        if (om.getOrNull(om.baseUrl() + "/api/v1/databaseSchemas/name/" + encode(fqn)) != null) {
            return false;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", schema);
        payload.put("displayName", schema);
        payload.put("database", service + "." + database);
        post("/api/v1/databaseSchemas", payload);
        return true;
    }

    private JsonNode post(String path, Map<String, Object> payload) {
        try {
            return om.post(om.baseUrl() + path, payload);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            // created concurrently by another caller; the entity exists either way
            JsonNode body = parseQuietly(e.getResponseBodyAsString());
            if (body != null && body.hasNonNull("id")) {
                return body;
            }
            throw new IllegalStateException("OM 已存在该实体：" + path);
        }
    }

    private JsonNode parseQuietly(String body) {
        return om.read(body);
    }

    private static String encode(String fqn) {
        return java.net.URLEncoder.encode(fqn, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** information_schema data types mapped onto the OpenMetadata ColumnDataType enum. */
    static String omDataType(String sqlType) {
        String type = sqlType == null ? "" : sqlType.trim().toLowerCase();
        switch (type) {
            case "character varying":
            case "varchar":
                return "VARCHAR";
            case "character":
            case "char":
                return "CHAR";
            case "text":
                return "TEXT";
            case "integer":
            case "int":
            case "int4":
                return "INT";
            case "smallint":
            case "int2":
                return "SMALLINT";
            case "bigint":
            case "int8":
                return "BIGINT";
            case "numeric":
            case "decimal":
                return "NUMERIC";
            case "real":
            case "float4":
                return "FLOAT";
            case "double precision":
            case "float8":
                return "DOUBLE";
            case "boolean":
            case "bool":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "timestamp without time zone":
            case "timestamp":
            case "datetime":
                return "TIMESTAMP";
            case "timestamp with time zone":
            case "timestamptz":
                return "TIMESTAMPZ";
            case "time without time zone":
            case "time":
                return "TIME";
            case "interval":
                return "INTERVAL";
            case "uuid":
                return "UUID";
            case "json":
            case "jsonb":
                return "JSON";
            case "bytea":
            case "binary":
            case "varbinary":
                return type.equals("bytea") ? "BYTEA" : "BINARY";
            case "array":
            case "_character varying":
            case "_text":
            case "_int4":
            case "_int8":
                return "ARRAY";
            default:
                return "VARCHAR";
        }
    }
}
