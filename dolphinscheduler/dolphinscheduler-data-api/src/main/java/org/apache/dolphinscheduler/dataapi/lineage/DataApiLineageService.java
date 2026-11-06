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

package org.apache.dolphinscheduler.dataapi.lineage;

import org.apache.dolphinscheduler.dataapi.acl.DataApiAclService;
import org.apache.dolphinscheduler.dataapi.auth.DataApiRequestInterceptor;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.service.DataApiCatalogService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * Derives {@code api_path → table} references from published scripts and persists them for
 * OpenMetadata lineage ingestion.
 */
@Slf4j
@Service
public class DataApiLineageService {

    private final MagicResourceService magicResourceService;
    private final DataSource metaDataSource;
    private final DataApiProperties properties;
    private final Map<String, ApiLineage> cache = new LinkedHashMap<>();

    public DataApiLineageService(MagicResourceService magicResourceService,
                                 DataSource metaDataSource,
                                 DataApiProperties properties) {
        this.magicResourceService = magicResourceService;
        this.metaDataSource = metaDataSource;
        this.properties = properties;
    }

    @Scheduled(initialDelay = 30000L, fixedDelay = 600000L)
    public void scheduledRefresh() {
        if (properties.isLineageEnabled()) {
            refresh();
        }
    }

    public synchronized void refresh() {
        List<ApiLineage> items = new ArrayList<>();
        for (MagicEntity entity : magicResourceService.files("api")) {
            if (!(entity instanceof ApiInfo)) {
                continue;
            }
            ApiInfo api = (ApiInfo) entity;
            String groupPath = magicResourceService.getGroupPath(api.getGroupId());
            String fullPath = DataApiAclService.normalizePathPattern(
                    DataApiCatalogService.joinPath(groupPath, api.getPath()));
            Set<String> tables = SqlTableExtractor.extractTables(api.getScript());
            Set<String> datasources = DataApiRequestInterceptor.extractDatasourceKeys(api.getScript());
            items.add(new ApiLineage(fullPath, api.getMethod(), api.getName(), tables, datasources));
        }
        cache.clear();
        for (ApiLineage item : items) {
            cache.put(item.apiPath, item);
        }
        persist(items);
    }

    public List<Map<String, Object>> views() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ApiLineage item : cache.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("apiPath", item.apiPath);
            row.put("method", item.method);
            row.put("name", item.name);
            row.put("tables", new ArrayList<>(item.tables));
            row.put("datasources", new ArrayList<>(item.datasources));
            result.add(row);
        }
        return result;
    }

    private void persist(List<ApiLineage> items) {
        String deleteSql = "DELETE FROM da_api_table_ref WHERE api_path=?";
        String insertSql = "INSERT INTO da_api_table_ref(api_path, datasource_key, table_name, update_time)"
                + " VALUES (?,?,?,?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = metaDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(deleteSql);
                    PreparedStatement insert = conn.prepareStatement(insertSql)) {
                for (ApiLineage item : items) {
                    delete.setString(1, item.apiPath);
                    delete.addBatch();
                }
                delete.executeBatch();
                for (ApiLineage item : items) {
                    String datasourceKey = item.datasources.isEmpty() ? "" : item.datasources.iterator().next();
                    for (String table : item.tables) {
                        insert.setString(1, item.apiPath);
                        insert.setString(2, datasourceKey);
                        insert.setString(3, table);
                        insert.setTimestamp(4, now);
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.warn("persist lineage failed: {}", e.getMessage());
        }
    }

    private static final class ApiLineage {

        private final String apiPath;
        private final String method;
        private final String name;
        private final Set<String> tables;
        private final Set<String> datasources;

        private ApiLineage(String apiPath, String method, String name, Set<String> tables,
                           Set<String> datasources) {
            this.apiPath = apiPath;
            this.method = method;
            this.name = name;
            this.tables = new LinkedHashSet<>(tables);
            this.datasources = new LinkedHashSet<>(datasources);
        }
    }
}
