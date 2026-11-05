/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.task.openmetadata;

import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.AbstractTask;
import org.apache.dolphinscheduler.plugin.task.api.TaskCallBack;
import org.apache.dolphinscheduler.plugin.task.api.TaskConstants;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * Reports a table-level lineage edge to OpenMetadata.
 * <p>
 * The task resolves table FQNs to entity IDs through the OM REST API, then calls
 * {@code PUT /api/v1/lineage}. It is intended to run as a downstream task after an
 * ETL/SQL/SeaTunnel task succeeds, so a failed ETL does not publish a false edge.
 */
@Slf4j
public class OpenMetadataTask extends AbstractTask {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private final TaskExecutionContext taskExecutionContext;
    private OpenMetadataTaskParameters parameters;
    private String resolvedToken;

    public OpenMetadataTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        parameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), OpenMetadataTaskParameters.class);
        if (parameters == null || !parameters.checkParameters()) {
            throw new TaskException("openmetadata task params is not valid (openMetadataUrl/fromFqn/toFqn required)");
        }
        resolvedToken = StringUtils.defaultIfBlank(parameters.getToken(), System.getenv("OM_TOKEN"));
        if (StringUtils.isBlank(resolvedToken)) {
            throw new TaskException("openmetadata token is required: set task parameter token or OM_TOKEN env");
        }
        if (!parameters.getOpenMetadataUrl().startsWith("http")) {
            throw new TaskException("openMetadataUrl must start with http/https");
        }
        log.info("Initialize OpenMetadata task: {} -> {}, url={}", parameters.getFromFqn(),
                parameters.getToFqn(), parameters.getOpenMetadataUrl());
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        String base = StringUtils.removeEnd(parameters.getOpenMetadataUrl(), "/");
        try {
            String fromId = resolveId(base, parameters.getFromType(), parameters.getFromFqn());
            String toId = resolveId(base, parameters.getToType(), parameters.getToFqn());

            Map<String, Object> from = new LinkedHashMap<>();
            from.put("id", fromId);
            from.put("type", payloadType(parameters.getFromType()));
            Map<String, Object> to = new LinkedHashMap<>();
            to.put("id", toId);
            to.put("type", payloadType(parameters.getToType()));

            String sql = StringUtils.defaultIfBlank(parameters.getSql(),
                    SeatunnelSqlExtractor.extract(parameters.getSeatunnelConfig()));
            java.util.List<ColumnLineageParam> columns = parameters.getColumnsLineage();
            if ((columns == null || columns.isEmpty()) && parameters.isParseSql() && StringUtils.isNotBlank(sql)) {
                columns = SqlLineageParser.parse(sql, parameters.getFromFqn(), parameters.getToFqn()).getColumnLineage();
            }

            Map<String, Object> lineageDetails = new LinkedHashMap<>();
            if (StringUtils.isNotBlank(sql)) {
                lineageDetails.put("sqlQuery", sql);
            }
            if (columns != null && !columns.isEmpty()) {
                java.util.List<Map<String, Object>> columnMaps = new java.util.ArrayList<>();
                for (ColumnLineageParam column : columns) {
                    Map<String, Object> columnMap = new LinkedHashMap<>();
                    columnMap.put("fromColumns", column.getFromColumns());
                    columnMap.put("toColumn", column.getToColumn());
                    if (StringUtils.isNotBlank(column.getFunction())) {
                        columnMap.put("function", column.getFunction());
                    }
                    columnMaps.add(columnMap);
                }
                lineageDetails.put("columnsLineage", columnMaps);
            }
            if (StringUtils.isNotBlank(parameters.getDescription())) {
                lineageDetails.put("description", parameters.getDescription());
            }
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("fromEntity", from);
            edge.put("toEntity", to);
            if (!lineageDetails.isEmpty()) {
                edge.put("lineageDetails", lineageDetails);
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("edge", edge);

            int code = putJson(base + "/api/v1/lineage", JSONUtils.toJsonString(payload));
            if (code < 200 || code >= 300) {
                exitStatusCode = TaskConstants.EXIT_CODE_FAILURE;
                throw new TaskException("OpenMetadata lineage API returned HTTP " + code);
            }
            exitStatusCode = TaskConstants.EXIT_CODE_SUCCESS;
            log.info("OpenMetadata lineage pushed: {} -> {}", parameters.getFromFqn(), parameters.getToFqn());
        } catch (TaskException e) {
            exitStatusCode = TaskConstants.EXIT_CODE_FAILURE;
            throw e;
        } catch (Exception e) {
            exitStatusCode = TaskConstants.EXIT_CODE_FAILURE;
            throw new TaskException("Failed to push OpenMetadata lineage: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancel() throws TaskException {
        // HTTP request is short-lived; nothing to cancel.
    }

    @Override
    public AbstractParameters getParameters() {
        return parameters;
    }

    private String resolveId(String base, String type, String fqn) throws IOException {
        String path = "/api/v1/" + entityPath(type) + "/name/"
                + URLEncoder.encode(fqn, StandardCharsets.UTF_8.name());
        HttpURLConnection connection = open(base + path, "GET");
        int code = connection.getResponseCode();
        String body = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
        connection.disconnect();
        if (code != 200) {
            throw new IOException("cannot resolve " + type + " FQN " + fqn + ": HTTP " + code + " " + body);
        }
        Matcher matcher = ID_PATTERN.matcher(body);
        if (!matcher.find()) {
            throw new IOException("cannot find id in OpenMetadata response for " + fqn + ": " + body);
        }
        return matcher.group(1);
    }

    private int putJson(String url, String payload) throws IOException {
        HttpURLConnection connection = open(url, "PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
        int code = connection.getResponseCode();
        String body = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
        connection.disconnect();
        log.info("OpenMetadata lineage response: HTTP {} {}", code, body);
        return code;
    }

    private HttpURLConnection open(String url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(parameters.getConnectTimeout());
        connection.setReadTimeout(parameters.getReadTimeout());
        connection.setRequestProperty("Authorization", "Bearer " + resolvedToken);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private static String readBody(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private static String normalizeType(String type) {
        return StringUtils.defaultIfBlank(type, "table").toLowerCase(Locale.ROOT);
    }

    /** OM entity-type casing differs from the REST path segment casing. */
    private static String payloadType(String type) {
        String normalized = normalizeType(type);
        switch (normalized) {
            case "apicollection":
                return "apiCollection";
            case "apiendpoint":
                return "apiEndpoint";
            case "databaseschema":
                return "databaseSchema";
            default:
                return normalized;
        }
    }

    private static String entityPath(String type) {
        String normalized = normalizeType(type);
        switch (normalized) {
            case "apicollection":
                return "apiCollections";
            case "apiendpoint":
                return "apiEndpoints";
            case "databaseschema":
                return "databaseSchemas";
            case "dashboard":
                return "dashboards";
            case "pipeline":
                return "pipelines";
            default:
                return normalized + "s";
        }
    }
}
