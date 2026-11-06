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

package org.apache.dolphinscheduler.dataapi.controller;

import org.apache.dolphinscheduler.common.enums.Flag;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dataapi.auth.DsTokenAuthorizationInterceptor;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.datasource.DatasourceHealthService;
import org.apache.dolphinscheduler.dataapi.datasource.DsMagicDataSourceLoader;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

@RestController
@RequestMapping("/data-api")
public class DataApiAdminController {

    private final DsMagicDataSourceLoader dataSourceLoader;
    private final DataApiProperties properties;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;
    private final MagicDynamicDataSource magicDynamicDataSource;
    private final DatasourceHealthService healthService;

    public DataApiAdminController(DsMagicDataSourceLoader dataSourceLoader,
                                  DataApiProperties properties,
                                  DsTokenAuthorizationInterceptor authorizationInterceptor,
                                  MagicDynamicDataSource magicDynamicDataSource,
                                  DatasourceHealthService healthService) {
        this.dataSourceLoader = dataSourceLoader;
        this.properties = properties;
        this.authorizationInterceptor = authorizationInterceptor;
        this.magicDynamicDataSource = magicDynamicDataSource;
        this.healthService = healthService;
    }

    @GetMapping("/datasources/health")
    public ResponseEntity<?> datasourceHealth(
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = authorizationInterceptor.resolveDsUser(firstNonBlank(token, sessionId));
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("token required");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", healthService.views());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        body.put("service", "dolphinscheduler-data-api");
        // datasource keys are internal names; keep them behind /data-api/datasources (token required)
        body.put("loadedDatasourceCount", dataSourceLoader.loadedKeys().size());
        return body;
    }

    @GetMapping("/datasources")
    public ResponseEntity<?> listDatasources(
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = authorizationInterceptor.resolveDsUser(firstNonBlank(token, sessionId));
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("token required");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("keys", dataSourceLoader.loadedKeys());
        body.put("items", dataSourceLoader.loadedViews().stream()
                .map(DsMagicDataSourceLoader.LoadedDatasourceView::toMap)
                .collect(java.util.stream.Collectors.toList()));
        body.put("loadDbTypes", properties.getLoadDbTypes());
        body.put("maxRows", properties.getMaxRows());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/datasources/reload")
    public ResponseEntity<?> reload(
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId) {
        String auth = firstNonBlank(token, sessionId);
        if (StringUtils.isBlank(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("token required");
        }
        User user = authorizationInterceptor.resolveDsUser(auth);
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("token invalid");
        }
        dataSourceLoader.reload();
        Map<String, Object> body = new HashMap<>();
        body.put("keys", dataSourceLoader.loadedKeys());
        body.put("items", dataSourceLoader.loadedViews().stream()
                .map(DsMagicDataSourceLoader.LoadedDatasourceView::toMap)
                .collect(java.util.stream.Collectors.toList()));
        return ResponseEntity.ok(body);
    }

    /**
     * Smoke: run {@code SELECT 1} against a loaded magic datasource key (e.g. mysql_1 / doris_2).
     */
    @GetMapping("/datasources/{key}/ping")
    public ResponseEntity<?> ping(@PathVariable("key") String key,
                                  @RequestHeader(value = "token", required = false) String token,
                                  @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = authorizationInterceptor.resolveDsUser(firstNonBlank(token, sessionId));
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("token required");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        try {
            MagicDynamicDataSource.DataSourceNode node =
                    "default".equalsIgnoreCase(key)
                            ? magicDynamicDataSource.getDataSource()
                            : magicDynamicDataSource.getDataSource(key);
            if (node == null || node.getDataSource() == null) {
                body.put("ok", false);
                body.put("error", "datasource not loaded");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
            }
            DataSource ds = node.getDataSource();
            try (Connection c = ds.getConnection();
                    Statement st = c.createStatement();
                    ResultSet rs = st.executeQuery("SELECT 1")) {
                rs.next();
                body.put("ok", true);
                body.put("result", rs.getInt(1));
                return ResponseEntity.ok(body);
            }
        } catch (Exception e) {
            body.put("ok", false);
            body.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.isNotBlank(a)) {
            return a;
        }
        return b;
    }
}
