package com.lhyf.dataasset.datasource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhyf.dataasset.audit.AuditService;
import com.lhyf.dataasset.auth.AuthInterceptor;
import com.lhyf.dataasset.om.OmClient;
import com.lhyf.dataasset.om.OmTableRegistrar;

/**
 * Table/column picker for the lineage editor. Names come from the business database itself, so tables
 * that OpenMetadata has not collected yet can still be wired up.
 */
@RestController
@RequestMapping("/data-asset/datasource")
public class DatasourceController {

    private final DatasourceMetadataService metadata;
    private final OmClient om;
    private final OmTableRegistrar registrar;
    private final AuditService audit;

    public DatasourceController(DatasourceMetadataService metadata, OmClient om, OmTableRegistrar registrar,
                                AuditService audit) {
        this.metadata = metadata;
        this.om = om;
        this.registrar = registrar;
        this.audit = audit;
    }

    @GetMapping("/datasources")
    public List<Map<String, Object>> datasources() {
        return metadata.datasources();
    }

    @GetMapping("/schemas")
    public List<String> schemas(@RequestParam String datasource) {
        return metadata.schemas(datasource);
    }

    @GetMapping("/tables")
    public List<Map<String, Object>> tables(@RequestParam String datasource, @RequestParam String schema,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "100") int limit) {
        String database = databaseOf(datasource);
        List<Map<String, Object>> tables = metadata.tables(datasource, schema, q, limit);
        for (Map<String, Object> table : tables) {
            table.put("datasource", datasource);
            table.put("database", database);
            table.put("fqn", String.join(".", datasource, database, schema,
                    String.valueOf(table.get("table"))));
        }
        return tables;
    }

    @GetMapping("/columns")
    public List<Map<String, Object>> columns(@RequestParam String datasource, @RequestParam String schema,
                                             @RequestParam String table) {
        return metadata.columns(datasource, schema, table);
    }

    /** Lets the editor flag which picks are not yet in OpenMetadata; saving the edge registers them. */
    @GetMapping("/exists")
    public Map<String, Object> exists(@RequestParam String fqn) {
        Map<String, Object> result = new LinkedHashMap<>();
        JsonNode table = om.getOrNull(om.baseUrl() + "/api/v1/tables/name/" + fqn);
        result.put("fqn", fqn);
        result.put("inOpenMetadata", table != null && table.hasNonNull("id"));
        if (table != null) {
            result.put("id", table.path("id").asText());
        }
        return result;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                        @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                        @RequestParam String fqn) {
        if (!admin) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "只有管理员可以登记资产");
        }
        Map<String, Object> result = registrar.ensure(fqn);
        audit.record(user, "ASSET_REGISTER", fqn, result, 200, 0, null);
        return result;
    }

    private String databaseOf(String datasource) {
        Map<String, Object> match = metadata.datasources().stream()
                .filter(item -> datasource.equals(item.get("name")))
                .findFirst()
                .orElse(new LinkedHashMap<>());
        return String.valueOf(match.getOrDefault("database", ""));
    }
}
