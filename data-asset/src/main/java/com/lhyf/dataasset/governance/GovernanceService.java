package com.lhyf.dataasset.governance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.lhyf.dataasset.om.OmClient;

/** Governance proxy: domains, data products, classifications/tags, glossary and layer/level assignment. */
@Service
public class GovernanceService {

    private final OmClient om;

    public GovernanceService(OmClient om) {
        this.om = om;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("domains", summaries(om.get(api("/domains?limit=100"))));
        result.put("dataProducts", summaries(om.get(api("/dataProducts?limit=100"))));
        result.put("glossaryTerms", summaries(om.get(api("/glossaryTerms?limit=200"))));
        result.put("classifications", summaries(om.get(api("/classifications?limit=100"))));

        Map<String, List<Map<String, Object>>> tagsByClassification = new LinkedHashMap<>();
        for (JsonNode tag : om.get(api("/tags?limit=500")).path("data")) {
            String fqn = tag.path("fullyQualifiedName").asText("");
            int dot = fqn.indexOf('.');
            if (dot <= 0) {
                continue;
            }
            String classification = fqn.substring(0, dot);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fqn", fqn);
            item.put("name", tag.path("name").asText(""));
            item.put("description", tag.path("description").asText(""));
            tagsByClassification.computeIfAbsent(classification, k -> new ArrayList<>()).add(item);
        }
        result.put("tagsByClassification", tagsByClassification);
        return result;
    }

    public List<Map<String, Object>> assets() {
        JsonNode search = om.searchTables("*", 100, 0);
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode hit : search.path("hits").path("hits")) {
            JsonNode source = hit.path("_source");
            String fqn = source.path("fullyQualifiedName").asText("");
            if (fqn.isEmpty()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fqn", fqn);
            item.put("name", source.path("name").asText(""));
            item.put("displayName", source.path("displayName").asText(""));
            try {
                JsonNode entity = tableGovernance(fqn);
                item.put("tags", refs(entity.path("tags"), "tagFQN"));
                item.put("domains", refs(entity.path("domains"), "fullyQualifiedName"));
                item.put("dataProducts", refs(entity.path("dataProducts"), "fullyQualifiedName"));
                item.put("certification", entity.path("certification").path("tagLabel").path("tagFQN").asText(""));
            } catch (Exception e) {
                item.put("tags", List.of());
                item.put("domains", List.of());
                item.put("dataProducts", List.of());
                item.put("certification", "");
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> asset(String fqn) {
        JsonNode entity = tableGovernance(fqn);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fqn", fqn);
        result.put("tags", refs(entity.path("tags"), "tagFQN"));
        result.put("domains", refs(entity.path("domains"), "fullyQualifiedName"));
        result.put("inheritedDomains", inheritedRefs(entity.path("domains"), "fullyQualifiedName"));
        result.put("dataProducts", refs(entity.path("dataProducts"), "fullyQualifiedName"));
        result.put("inheritedDataProducts", inheritedRefs(entity.path("dataProducts"), "fullyQualifiedName"));
        result.put("owners", refs(entity.path("owners"), "name"));
        result.put("certification", entity.path("certification").path("tagLabel").path("tagFQN").asText(""));
        return result;
    }

    /** Adds or removes a single tag. Body: {fqn, tagFqn, action: add|remove}. */
    public Map<String, Object> tag(Map<String, String> body) {
        String fqn = required(body, "fqn");
        String tagFqn = required(body, "tagFqn");
        String action = "remove".equalsIgnoreCase(body.get("action")) ? "remove" : "add";
        JsonNode entity = tableGovernance(fqn);
        if (tagFqn.startsWith("Certification.")) {
            patchCertification(fqn, "remove".equals(action) ? "" : tagFqn, entity);
            return Map.of("fqn", fqn, "tagFqn", tagFqn, "action", action);
        }
        List<Map<String, Object>> ops = new ArrayList<>();
        if ("remove".equals(action)) {
            int index = indexOfTag(entity.path("tags"), tagFqn);
            if (index >= 0) {
                ops.add(Map.of("op", "remove", "path", "/tags/" + index));
            }
        } else if (indexOfTag(entity.path("tags"), tagFqn) < 0) {
            ops.add(addTagOp(tagFqn));
        }
        patchTags(fqn, ops);
        return Map.of("fqn", fqn, "tagFqn", tagFqn, "action", action);
    }

    /** Replaces all tags of one classification. Body: {fqn, classification, tagFqn} (tagFqn empty clears). */
    public Map<String, Object> tagSet(Map<String, String> body) {
        String fqn = required(body, "fqn");
        String classification = required(body, "classification");
        String tagFqn = body.getOrDefault("tagFqn", "");
        JsonNode entity = tableGovernance(fqn);
        if ("Certification".equalsIgnoreCase(classification)) {
            patchCertification(fqn, tagFqn, entity);
            return Map.of("fqn", fqn, "classification", classification, "tagFqn", tagFqn);
        }
        List<Integer> indexes = new ArrayList<>();
        JsonNode tags = entity.path("tags");
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).path("tagFQN").asText("").startsWith(classification + ".")) {
                indexes.add(i);
            }
        }
        indexes.sort(Comparator.reverseOrder());
        List<Map<String, Object>> ops = new ArrayList<>();
        for (int index : indexes) {
            ops.add(Map.of("op", "remove", "path", "/tags/" + index));
        }
        if (!tagFqn.isBlank()) {
            ops.add(addTagOp(tagFqn));
        }
        patchTags(fqn, ops);
        return Map.of("fqn", fqn, "classification", classification, "tagFqn", tagFqn);
    }

    public Map<String, Object> domain(Map<String, String> body) {
        return attachRef(body, "domainFqn", "domains", "/domains/name/{name}", "domain");
    }

    public Map<String, Object> dataProduct(Map<String, String> body) {
        return attachRef(body, "dataProductFqn", "dataProducts", "/dataProducts/name/{name}", "dataProduct");
    }

    public Map<String, Object> glossaryTerm(Map<String, String> body) {
        String fqn = required(body, "fqn");
        String termFqn = required(body, "termFqn");
        String action = "remove".equalsIgnoreCase(body.get("action")) ? "remove" : "add";
        JsonNode entity = tableGovernance(fqn);
        int index = indexOfTag(entity.path("tags"), termFqn);
        List<Map<String, Object>> ops = new ArrayList<>();
        if ("remove".equals(action)) {
            if (index >= 0) {
                ops.add(Map.of("op", "remove", "path", "/tags/" + index));
            }
        } else if (index < 0) {
            ops.add(addTagOp(termFqn, "Glossary"));
        }
        patchTags(fqn, ops);
        return Map.of("fqn", fqn, "termFqn", termFqn, "action", action);
    }

    public JsonNode createClassification(Map<String, String> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", required(body, "name"));
        payload.put("description", body.getOrDefault("description", ""));
        payload.put("provider", "user");
        payload.put("mutuallyExclusive", false);
        return idempotent(() -> om.post(api("/classifications"), payload));
    }

    public JsonNode createTag(Map<String, String> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("classification", required(body, "classification"));
        payload.put("name", required(body, "name"));
        payload.put("description", body.getOrDefault("description", ""));
        return idempotent(() -> om.post(api("/tags"), payload));
    }

    public JsonNode createDomain(Map<String, String> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", required(body, "name"));
        payload.put("description", body.getOrDefault("description", ""));
        payload.put("domainType", body.getOrDefault("domainType", "Aggregate"));
        return idempotent(() -> om.post(api("/domains"), payload));
    }

    public JsonNode createDataProduct(Map<String, String> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", required(body, "name"));
        payload.put("description", body.getOrDefault("description", ""));
        payload.put("domains", List.of(required(body, "domainFqn")));
        return idempotent(() -> om.post(api("/dataProducts"), payload));
    }

    public JsonNode createGlossaryTerm(Map<String, String> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("glossary", required(body, "glossary"));
        payload.put("name", required(body, "name"));
        payload.put("description", body.getOrDefault("description", ""));
        return idempotent(() -> om.post(api("/glossaryTerms"), payload));
    }

    private JsonNode idempotent(Supplier<JsonNode> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException.Conflict e) {
            return JsonNodeFactory.instance.objectNode().put("status", "exists");
        }
    }

    private int indexOfTag(JsonNode tags, String tagFqn) {
        for (int i = 0; i < tags.size(); i++) {
            if (tagFqn.equals(tags.get(i).path("tagFQN").asText(""))) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Object> addTagOp(String tagFqn) {
        return addTagOp(tagFqn, "Classification");
    }

    private Map<String, Object> addTagOp(String tagFqn, String source) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("tagFQN", tagFqn);
        value.put("labelType", "Manual");
        value.put("state", "Confirmed");
        value.put("source", source);
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("op", "add");
        op.put("path", "/tags/-");
        op.put("value", value);
        return op;
    }

    private void patchTags(String fqn, List<Map<String, Object>> ops) {
        if (ops.isEmpty()) {
            return;
        }
        om.patch(api("/tables/{id}", tableId(fqn)), ops);
    }

    private void patchCertification(String fqn, String tagFqn, JsonNode entity) {
        JsonNode current = entity.path("certification");
        boolean has = !current.isMissingNode() && !current.isNull()
                && !current.path("tagLabel").path("tagFQN").asText("").isEmpty();
        List<Map<String, Object>> ops = new ArrayList<>();
        if (tagFqn == null || tagFqn.isBlank()) {
            if (has) {
                ops.add(Map.of("op", "remove", "path", "/certification"));
            }
        } else {
            Map<String, Object> label = new LinkedHashMap<>();
            label.put("tagFQN", tagFqn);
            label.put("labelType", "Manual");
            label.put("state", "Confirmed");
            label.put("source", "Classification");
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("tagLabel", label);
            value.put("appliedDate", System.currentTimeMillis());
            ops.add(Map.of("op", has ? "replace" : "add", "path", "/certification", "value", value));
        }
        if (!ops.isEmpty()) {
            om.patch(api("/tables/{id}", tableId(fqn)), ops);
        }
    }

    private Map<String, Object> attachRef(Map<String, String> body, String refKey, String field,
                                          String lookupTemplate, String type) {
        String fqn = required(body, "fqn");
        String ref = required(body, refKey);
        String action = "remove".equalsIgnoreCase(body.get("action")) ? "remove" : "add";
        JsonNode entity = tableGovernance(fqn);
        JsonNode array = entity.path(field);
        int index = -1;
        boolean inherited = false;
        for (int i = 0; i < array.size(); i++) {
            if (ref.equals(array.get(i).path("fullyQualifiedName").asText(""))) {
                index = i;
                inherited = array.get(i).path("inherited").asBoolean(false);
                break;
            }
        }
        List<Map<String, Object>> ops = new ArrayList<>();
        String status;
        if ("add".equals(action)) {
            if (index >= 0) {
                status = inherited ? "inherited" : "exists";
            } else {
                String refId = om.get(UriComponentsBuilder.fromHttpUrl(base() + "/api/v1" + lookupTemplate)
                        .buildAndExpand(ref).encode().toUriString()).path("id").asText("");
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("id", refId);
                value.put("type", type);
                ops.add(Map.of("op", "add", "path", "/" + field + "/-", "value", value));
                status = "added";
            }
        } else if (index < 0) {
            status = "absent";
        } else if (inherited) {
            status = "inherited";
        } else {
            ops.add(Map.of("op", "remove", "path", "/" + field + "/" + index));
            status = "removed";
        }
        if (!ops.isEmpty()) {
            om.patch(api("/tables/{id}", tableId(fqn)), ops);
        }
        return Map.of("fqn", fqn, refKey, ref, "action", action, "status", status);
    }

    private JsonNode tableGovernance(String fqn) {
        String url = UriComponentsBuilder.fromHttpUrl(base() + "/api/v1/tables/name/{fqn}")
                .queryParam("fields", "tags,domains,dataProducts,owners,certification")
                .buildAndExpand(fqn)
                .encode()
                .toUriString();
        return om.get(url);
    }

    private String tableId(String fqn) {
        String url = UriComponentsBuilder.fromHttpUrl(base() + "/api/v1/tables/name/{fqn}")
                .buildAndExpand(fqn)
                .encode()
                .toUriString();
        return om.get(url).path("id").asText("");
    }

    private List<Map<String, Object>> summaries(JsonNode response) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode item : response.path("data")) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("fqn", item.path("fullyQualifiedName").asText(""));
            summary.put("name", item.path("name").asText(""));
            summary.put("displayName", item.path("displayName").asText(""));
            summary.put("description", item.path("description").asText(""));
            result.add(summary);
        }
        return result;
    }

    private List<String> refs(JsonNode array, String field) {
        List<String> result = new ArrayList<>();
        for (JsonNode item : array) {
            String value = item.path(field).asText("");
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private List<String> inheritedRefs(JsonNode array, String field) {
        List<String> result = new ArrayList<>();
        for (JsonNode item : array) {
            if (item.path("inherited").asBoolean(false)) {
                String value = item.path(field).asText("");
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private String api(String path) {
        return base() + "/api/v1" + path;
    }

    private String api(String template, String id) {
        return UriComponentsBuilder.fromHttpUrl(base() + "/api/v1" + template)
                .buildAndExpand(id)
                .encode()
                .toUriString();
    }

    private String base() {
        return om.baseUrl();
    }

    private static String required(Map<String, String> body, String key) {
        String value = body.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }
}
