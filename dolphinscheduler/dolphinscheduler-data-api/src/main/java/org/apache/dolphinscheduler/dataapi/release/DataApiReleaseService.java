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

package org.apache.dolphinscheduler.dataapi.release;

import org.apache.dolphinscheduler.dataapi.acl.DataApiAclService;
import org.apache.dolphinscheduler.dataapi.service.DataApiCatalogService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * Publish history for magic APIs: snapshot, rollback and offline.
 * A snapshot stores the full {@link ApiInfo} JSON plus the script for quick inspection.
 */
@Slf4j
@Service
public class DataApiReleaseService {

    private final MagicResourceService magicResourceService;
    private final DataSource metaDataSource;
    private final ObjectMapper objectMapper;

    public DataApiReleaseService(MagicResourceService magicResourceService,
                                 DataSource metaDataSource,
                                 ObjectMapper objectMapper) {
        this.magicResourceService = magicResourceService;
        this.metaDataSource = metaDataSource;
        this.objectMapper = objectMapper;
    }

    public synchronized int snapshot(Integer userId, String remark) {
        int count = 0;
        for (MagicEntity entity : magicResourceService.files("api")) {
            if (entity instanceof ApiInfo) {
                insertRelease((ApiInfo) entity, "PUBLISHED", userId, remark);
                count++;
            }
        }
        return count;
    }

    public List<Map<String, Object>> list(String apiPath) {
        StringBuilder sql = new StringBuilder("SELECT id, api_path, version, method, state, remark, create_by,"
                + " create_time FROM da_api_release");
        if (apiPath != null && !apiPath.trim().isEmpty()) {
            sql.append(" WHERE api_path=?");
        }
        sql.append(" ORDER BY id DESC");
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (apiPath != null && !apiPath.trim().isEmpty()) {
                ps.setString(1, DataApiAclService.normalizePathPattern(apiPath));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("apiPath", rs.getString("api_path"));
                    row.put("version", rs.getInt("version"));
                    row.put("method", rs.getString("method"));
                    row.put("state", rs.getString("state"));
                    row.put("remark", rs.getString("remark"));
                    row.put("createBy", rs.getInt("create_by"));
                    row.put("createTime", rs.getTimestamp("create_time"));
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            log.warn("list releases failed: {}", e.getMessage());
        }
        return result;
    }

    public long rollback(long releaseId, Integer userId) {
        ReleaseRow row = getRelease(releaseId);
        ApiInfo api = deserialize(row.entityJson);
        if (!magicResourceService.saveFile(api)) {
            throw new IllegalStateException("magic-api rejected rollback for " + row.apiPath);
        }
        return insertRelease(api, "ROLLBACK", userId, "rollback from release #" + releaseId);
    }

    public long offline(long releaseId, Integer userId) {
        ReleaseRow row = getRelease(releaseId);
        ApiInfo api = deserialize(row.entityJson);
        boolean deleted = magicResourceService.delete(api.getId());
        if (!deleted) {
            throw new IllegalStateException("magic-api could not delete " + row.apiPath);
        }
        return insertRelease(api, "OFFLINE", userId, "offline from release #" + releaseId);
    }

    private long insertRelease(ApiInfo api, String state, Integer userId, String remark) {
        String groupPath = magicResourceService.getGroupPath(api.getGroupId());
        String fullPath = DataApiAclService.normalizePathPattern(
                DataApiCatalogService.joinPath(groupPath, api.getPath()));
        int version = nextVersion(fullPath);
        String entityJson;
        try {
            entityJson = objectMapper.writeValueAsString(api);
        } catch (Exception e) {
            throw new IllegalStateException("serialize api failed: " + e.getMessage(), e);
        }
        String sql = "INSERT INTO da_api_release"
                + "(api_path, version, method, script, entity_json, state, remark, create_by, create_time)"
                + " VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullPath);
            ps.setInt(2, version);
            ps.setString(3, api.getMethod());
            ps.setString(4, api.getScript());
            ps.setString(5, entityJson);
            ps.setString(6, state);
            if (remark == null) {
                ps.setNull(7, Types.VARCHAR);
            } else {
                ps.setString(7, remark);
            }
            if (userId == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, userId);
            }
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("snapshot failed for " + fullPath + ": " + e.getMessage(), e);
        }
    }

    private int nextVersion(String fullPath) {
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT COALESCE(MAX(version),0) FROM da_api_release WHERE api_path=?")) {
            ps.setString(1, fullPath);
            try (ResultSet rs = ps.executeQuery()) {
                return (rs.next() ? rs.getInt(1) : 0) + 1;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("read release version failed: " + e.getMessage(), e);
        }
    }

    private ReleaseRow getRelease(long releaseId) {
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT api_path, entity_json FROM da_api_release WHERE id=?")) {
            ps.setLong(1, releaseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("release not found: " + releaseId);
                }
                return new ReleaseRow(rs.getString("api_path"), rs.getString("entity_json"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("read release failed: " + e.getMessage(), e);
        }
    }

    private ApiInfo deserialize(String entityJson) {
        try {
            return objectMapper.readValue(entityJson, ApiInfo.class);
        } catch (Exception e) {
            throw new IllegalStateException("release payload is not a valid ApiInfo: " + e.getMessage(), e);
        }
    }

    private static final class ReleaseRow {

        private final String apiPath;
        private final String entityJson;

        private ReleaseRow(String apiPath, String entityJson) {
            this.apiPath = apiPath;
            this.entityJson = entityJson;
        }
    }
}
