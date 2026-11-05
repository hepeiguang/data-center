package com.lhyf.dataasset.om;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhyf.dataasset.config.DataAssetProperties;

/** Read-only OpenMetadata client. Token is held server-side only. */
@Component
public class OmClient {

    private final DataAssetProperties.Om config;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private volatile String token;
    private volatile long tokenExpiresAt;

    public OmClient(DataAssetProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getOm();
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(config.getTimeoutSeconds()).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(config.getTimeoutSeconds()).toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    public JsonNode searchTables(String query, int size, int from) {
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl() + "/api/v1/search/query")
                .queryParam("q", query)
                .queryParam("index", "table_search_index")
                .queryParam("size", size)
                .queryParam("from", from)
                .encode()
                .toUriString();
        return get(url);
    }

    public JsonNode tableByFqn(String fqn) {
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl() + "/api/v1/tables/name/{fqn}")
                .queryParam("fields", "columns,tags,owners")
                .buildAndExpand(fqn)
                .encode()
                .toUriString();
        return get(url);
    }

    public JsonNode lineage(String fqn, int upstreamDepth, int downstreamDepth) {
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl() + "/api/v1/lineage/getLineage")
                .queryParam("fqn", fqn)
                .queryParam("type", "table")
                .queryParam("upstreamDepth", upstreamDepth)
                .queryParam("downstreamDepth", downstreamDepth)
                .encode()
                .toUriString();
        return get(url);
    }

    /** Edges carry {@code lineageDetails.columnsLineage} here, which getLineage flattens away. */
    public JsonNode lineageEdges(String fqn, int upstreamDepth, int downstreamDepth) {
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl()
                + "/api/v1/lineage/table/name/{fqn}")
                .queryParam("upstreamDepth", upstreamDepth)
                .queryParam("downstreamDepth", downstreamDepth)
                .buildAndExpand(fqn)
                .encode()
                .toUriString();
        return get(url);
    }

    public JsonNode tableRef(String fqn) {
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl() + "/api/v1/tables/name/{fqn}")
                .queryParam("fields", "columns")
                .buildAndExpand(fqn)
                .encode()
                .toUriString();
        return get(url);
    }

    public JsonNode get(String url) {
        HttpHeaders headers = authHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        String body = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        return parse(body);
    }

    /** Null when OM answers 404, so callers can probe existence without an exception. */
    public JsonNode getOrNull(String url) {
        try {
            return get(url);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public JsonNode post(String url, Object payload) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        String json = toJson(payload);
        String body = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(json, headers), String.class)
                .getBody();
        return parse(body);
    }

    public JsonNode put(String url, Object payload) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        String json = toJson(payload);
        String body = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(json, headers), String.class)
                .getBody();
        return parse(body);
    }

    public JsonNode delete(String url) {
        HttpHeaders headers = authHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        String body = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class)
                .getBody();
        return parse(body);
    }

    public JsonNode deleteLineageEdge(String fromFqn, String toFqn) {
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl()
                + "/api/v1/lineage/table/name/{from}/table/name/{to}")
                .buildAndExpand(fromFqn, toFqn)
                .encode()
                .toUriString();
        return delete(url);
    }

    public JsonNode patch(String url, Object payload) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + token())
                    .header("Content-Type", "application/json-patch+json")
                    .header("Accept", "application/json")
                    .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(toJson(payload),
                            StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .build();
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("OM PATCH " + url + " failed: " + response.statusCode() + " "
                        + response.body());
            }
            return parse(response.body());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("OM PATCH failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OM PATCH interrupted", e);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token());
        return headers;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("cannot serialize OM payload", e);
        }
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (Exception e) {
            throw new IllegalStateException("invalid OM response", e);
        }
    }

    public JsonNode read(String json) {
        try {
            return json == null || json.isEmpty() ? null : objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    public String version() {
        return get(config.getBaseUrl() + "/api/v1/system/version").path("version").asText("");
    }

    public String baseUrl() {
        return config.getBaseUrl();
    }

    private String token() {
        if (StringUtils.hasText(config.getToken())) {
            return config.getToken();
        }
        if (token != null && System.currentTimeMillis() < tokenExpiresAt) {
            return token;
        }
        synchronized (this) {
            if (token != null && System.currentTimeMillis() < tokenExpiresAt) {
                return token;
            }
            String url = config.getBaseUrl() + "/api/v1/users/login";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String payload = "{\"email\":\"" + config.getAdminEmail() + "\",\"password\":\""
                    + Base64.getEncoder().encodeToString(
                            config.getAdminPassword().getBytes(StandardCharsets.UTF_8))
                    + "\"}";
            String body = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class)
                    .getBody();
            try {
                JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
                token = root.path("accessToken").asText("");
            } catch (Exception e) {
                throw new IllegalStateException("cannot parse OM login response", e);
            }
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("OM login failed: empty access token");
            }
            tokenExpiresAt = System.currentTimeMillis() + Duration.ofMinutes(50).toMillis();
            return token;
        }
    }
}
