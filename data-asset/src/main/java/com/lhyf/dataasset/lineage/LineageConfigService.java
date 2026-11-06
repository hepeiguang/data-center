package com.lhyf.dataasset.lineage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Key mappings and row-lineage switches: the only lineage data this service owns.
 * Both fill in what column-level lineage in OpenMetadata cannot express (key semantics, opt-in scope).
 * SQL lives in LineageConfigRepository.
 */
@Service
public class LineageConfigService {

    private final LineageConfigRepository repository;

    public LineageConfigService(LineageConfigRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listKeys(String targetFqn) {
        if (StringUtils.hasText(targetFqn)) {
            return repository.listKeysByTarget(targetFqn.toLowerCase());
        }
        return repository.listKeys();
    }

    public Map<String, Object> addKey(String targetFqn, String keyColumn, String sourceFqn,
                                      String sourceKeyColumn, String user) {
        requireTable(targetFqn);
        requireTable(sourceFqn);
        requireColumn(keyColumn, "key column");
        requireColumn(sourceKeyColumn, "source key column");
        String target = targetFqn.toLowerCase();
        String source = sourceFqn.toLowerCase();
        List<Map<String, Object>> existing = repository.keyRows(target, keyColumn, source);
        if (existing.isEmpty()) {
            repository.insertKey(target, keyColumn, source, sourceKeyColumn, user);
        } else {
            repository.updateKeySource(((Number) existing.get(0).get("id")).longValue(), sourceKeyColumn, user);
        }
        List<Map<String, Object>> rows = repository.keyRows(target, keyColumn, source);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
    }

    public boolean removeKey(long id) {
        return repository.deleteKey(id);
    }

    /** key_column -> source_key_column for one edge; empty means the edge has no registered key. */
    public Map<String, String> keyMapping(String targetFqn, String sourceFqn) {
        return repository.keyMapping(targetFqn.toLowerCase(), sourceFqn.toLowerCase());
    }

    public List<Map<String, Object>> listSwitches() {
        return repository.listSwitches();
    }

    public Map<String, Object> setSwitch(String tableFqn, boolean enabled, String user) {
        requireTable(tableFqn);
        String fqn = tableFqn.toLowerCase();
        if (repository.switchExists(fqn)) {
            repository.updateSwitch(fqn, enabled, user);
        } else {
            repository.insertSwitch(fqn, enabled, user);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tableFqn", fqn);
        result.put("enabled", enabled);
        result.put("updatedBy", user);
        return result;
    }

    public boolean rowLineageEnabled(String tableFqn) {
        return repository.rowLineageEnabled(tableFqn.toLowerCase());
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
