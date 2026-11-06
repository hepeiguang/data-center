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

package org.apache.dolphinscheduler.dataapi.console;

import org.apache.dolphinscheduler.dataapi.auth.DsTokenAuthorizationInterceptor;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.meta.Flag;
import org.apache.dolphinscheduler.dataapi.meta.User;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business query console REST surface (plan §7.1). Mappings only exist when
 * {@code data-api.console.enabled=true}, so disabled instances answer 404 (AC-6).
 * Identity comes from the same {@code token} / {@code sessionId} headers as the rest of data-api.
 */
@Slf4j
@RestController
@RequestMapping("/data-api/console")
@ConditionalOnProperty(prefix = "data-api.console", name = "enabled", havingValue = "true")
public class ConsoleController {

    private final ConsoleService consoleService;
    private final ConsoleAuditService auditService;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;
    private final DataApiProperties properties;

    public ConsoleController(ConsoleService consoleService,
                             ConsoleAuditService auditService,
                             DsTokenAuthorizationInterceptor authorizationInterceptor,
                             DataApiProperties properties) {
        this.consoleService = consoleService;
        this.auditService = auditService;
        this.authorizationInterceptor = authorizationInterceptor;
        this.properties = properties;
    }

    @GetMapping("/datasources")
    public ResponseEntity<?> datasources(@RequestHeader(value = "token", required = false) String token,
                                         @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = requireUser(token, sessionId);
        Map<String, Object> body = new HashMap<>();
        body.put("items", consoleService.listDatasources(user));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestHeader(value = "token", required = false) String token,
                                     @RequestHeader(value = "sessionId", required = false) String sessionId,
                                     @RequestParam(value = "limit", required = false) Integer limit) {
        User user = requireUser(token, sessionId);
        int size = limit == null ? properties.getConsole().getHistoryPageSize() : limit;
        Map<String, Object> body = new HashMap<>();
        body.put("items", auditService.history(String.valueOf(user.getId()), size));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestHeader(value = "token", required = false) String token,
                                     @RequestHeader(value = "sessionId", required = false) String sessionId,
                                     @RequestBody Map<String, Object> request) {
        User user = requireUser(token, sessionId);
        try {
            Integer datasourceId = toInt(request.get("datasourceId"));
            String sql = StringUtils.trimToEmpty(String.valueOf(request.getOrDefault("sql", "")));
            if (datasourceId == null) {
                return error(HttpStatus.BAD_REQUEST, "datasourceId is required");
            }
            ConsoleService.QueryResult result = consoleService.execute(user, datasourceId, sql);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("queryId", result.queryId);
            body.put("columns", result.columns);
            body.put("rows", result.rows);
            body.put("rowCount", result.rowCount);
            body.put("truncated", result.truncated);
            body.put("elapsedMs", result.elapsedMs);
            return ResponseEntity.ok(body);
        } catch (ConsoleService.ConsoleException e) {
            return error(HttpStatus.valueOf(e.getStatus()), e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@RequestHeader(value = "token", required = false) String token,
                                    @RequestHeader(value = "sessionId", required = false) String sessionId,
                                    @RequestBody Map<String, Object> request) {
        User user = requireUser(token, sessionId);
        String queryId = StringUtils.trimToNull(String.valueOf(request.getOrDefault("queryId", "")));
        boolean cancelled = queryId != null && consoleService.cancel(user, queryId);
        Map<String, Object> body = new HashMap<>();
        body.put("cancelled", cancelled);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/export")
    public void export(@RequestHeader(value = "token", required = false) String token,
                       @RequestHeader(value = "sessionId", required = false) String sessionId,
                       @RequestBody Map<String, Object> request,
                       HttpServletResponse response) throws java.io.IOException {
        User user;
        try {
            user = requireUserEntity(token, sessionId);
        } catch (ConsoleService.ConsoleException e) {
            writePlainError(response, e.getStatus(), e.getMessage());
            return;
        }
        Integer datasourceId = toInt(request.get("datasourceId"));
        String sql = StringUtils.trimToEmpty(String.valueOf(request.getOrDefault("sql", "")));
        if (datasourceId == null) {
            writePlainError(response, 400, "datasourceId is required");
            return;
        }
        try {
            response.setStatus(200);
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"data-api-query-" + System.currentTimeMillis() + ".csv\"");
            // UTF-8 BOM so Excel opens Chinese columns correctly
            response.getOutputStream().write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
            try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
                consoleService.exportCsv(user, datasourceId, sql, writer);
            }
        } catch (ConsoleService.ConsoleException e) {
            if (response.isCommitted()) {
                log.warn("console export aborted mid-stream: {}", e.getMessage());
                return;
            }
            writePlainError(response, e.getStatus(), e.getMessage());
        }
    }

    private User requireUser(String token, String sessionId) {
        try {
            return requireUserEntity(token, sessionId);
        } catch (ConsoleService.ConsoleException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.valueOf(e.getStatus()), e.getMessage(), e);
        }
    }

    private User requireUserEntity(String token, String sessionId) {
        String auth = StringUtils.isNotBlank(token) ? token : sessionId;
        if (StringUtils.isBlank(auth)) {
            throw new ConsoleService.ConsoleException(401, "token required");
        }
        User user = authorizationInterceptor.resolveDsUser(auth);
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            throw new ConsoleService.ConsoleException(401, "token invalid or user disabled");
        }
        return user;
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private static void writePlainError(HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        if (response.isCommitted()) {
            return;
        }
        response.reset();
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\""
                + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
