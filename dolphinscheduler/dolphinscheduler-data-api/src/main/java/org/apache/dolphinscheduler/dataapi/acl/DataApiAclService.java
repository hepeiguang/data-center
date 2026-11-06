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

package org.apache.dolphinscheduler.dataapi.acl;

import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * CRUD + evaluation for {@code da_api_acl}.
 */
@Slf4j
@Service
public class DataApiAclService {

    private final DataSource metaDataSource;
    private final DataApiProperties properties;

    private volatile List<DataApiAclEntry> ruleCache;
    private volatile long ruleCacheAt;

    public DataApiAclService(DataSource metaDataSource, DataApiProperties properties) {
        this.metaDataSource = metaDataSource;
        this.properties = properties;
    }

    @PostConstruct
    public void ensureTable() {
        try (Connection conn = metaDataSource.getConnection(); Statement st = conn.createStatement()) {
            String product = conn.getMetaData().getDatabaseProductName();
            String normalized = product == null ? "" : product.toLowerCase(Locale.ROOT);
            st.execute(buildCreateTableDdl(normalized));
            if (!normalized.contains("mysql")) {
                // MySQL cannot express IF NOT EXISTS on CREATE INDEX; its DDL already carries the unique key.
                try {
                    st.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_da_acl ON da_api_acl"
                            + " (api_path, subject_type, subject_id)");
                    st.execute("CREATE INDEX IF NOT EXISTS idx_da_acl_subject ON da_api_acl"
                            + " (subject_type, subject_id)");
                } catch (SQLException e) {
                    log.debug("ensure da_api_acl indexes skipped: {}", e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.warn("ensure da_api_acl failed: {}", e.getMessage());
        }
    }

    static String buildCreateTableDdl(String productName) {
        String product = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
        boolean postgres = product.contains("postgres");
        boolean mysql = product.contains("mysql");
        String id = postgres ? "BIGSERIAL PRIMARY KEY" : "BIGINT AUTO_INCREMENT PRIMARY KEY";
        String smallInt = postgres ? "SMALLINT" : "TINYINT";
        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS da_api_acl (")
                .append("id ").append(id).append(',')
                .append("api_path VARCHAR(255) NOT NULL,")
                .append("subject_type ").append(smallInt).append(" NOT NULL,")
                .append("subject_id VARCHAR(128) NOT NULL,")
                .append("effect ").append(smallInt).append(" NOT NULL DEFAULT 1,")
                .append("remark VARCHAR(255),")
                .append("create_by INT,")
                .append("create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,")
                .append("update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        if (mysql) {
            ddl.append(",UNIQUE KEY uk_da_acl (api_path, subject_type, subject_id),")
                    .append("KEY idx_da_acl_subject (subject_type, subject_id)");
        }
        return ddl.append(')').toString();
    }

    public List<DataApiAclEntry> list(String apiPathFilter) {
        String sql = "SELECT id, api_path, subject_type, subject_id, effect, remark, create_by, create_time, update_time "
                + "FROM da_api_acl";
        if (StringUtils.isNotBlank(apiPathFilter)) {
            sql += " WHERE api_path LIKE ?";
        }
        sql += " ORDER BY api_path, subject_type, id";
        List<DataApiAclEntry> rows = new ArrayList<>();
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (StringUtils.isNotBlank(apiPathFilter)) {
                ps.setString(1, "%" + apiPathFilter.trim() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.warn("list acl failed: {}", e.getMessage());
        }
        return rows;
    }

    /**
     * Idempotent create: an existing (path, subjectType, subjectId) row is updated instead of duplicated.
     */
    public DataApiAclEntry create(String apiPath, AclSubjectType subjectType, String subjectId, int effect,
                                  String remark, Integer createBy) {
        String path = normalizePathPattern(apiPath);
        if (StringUtils.isBlank(path) || subjectType == null || StringUtils.isBlank(subjectId)) {
            throw new IllegalArgumentException("apiPath / subjectType / subjectId required");
        }
        int normalizedEffect = effect == 0 ? 0 : 1;
        DataApiAclEntry existing = findExisting(path, subjectType.getCode(), subjectId.trim());
        if (existing != null) {
            existing.setEffect(normalizedEffect);
            existing.setRemark(remark);
            existing.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            String updateSql = "UPDATE da_api_acl SET effect=?, remark=?, update_time=? WHERE id=?";
            try (Connection conn = metaDataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, normalizedEffect);
                if (StringUtils.isBlank(remark)) {
                    ps.setNull(2, Types.VARCHAR);
                } else {
                    ps.setString(2, remark);
                }
                ps.setTimestamp(3, existing.getUpdateTime());
                ps.setLong(4, existing.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException("update acl failed: " + e.getMessage(), e);
            }
            invalidateCache();
            return existing;
        }
        String sql = "INSERT INTO da_api_acl(api_path, subject_type, subject_id, effect, remark, create_by, create_time, update_time) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, path);
            ps.setInt(2, subjectType.getCode());
            ps.setString(3, subjectId.trim());
            ps.setInt(4, normalizedEffect);
            if (StringUtils.isBlank(remark)) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, remark);
            }
            if (createBy == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, createBy);
            }
            ps.setTimestamp(7, now);
            ps.setTimestamp(8, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                DataApiAclEntry entry = new DataApiAclEntry();
                entry.setApiPath(path);
                entry.setSubjectType(subjectType.getCode());
                entry.setSubjectId(subjectId.trim());
                entry.setEffect(normalizedEffect);
                entry.setRemark(remark);
                entry.setCreateBy(createBy);
                entry.setCreateTime(now);
                entry.setUpdateTime(now);
                if (keys.next()) {
                    entry.setId(keys.getLong(1));
                }
                invalidateCache();
                return entry;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("create acl failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update effect / remark of an existing rule.
     *
     * @param effect {@code null} keeps the current value
     * @param remark {@code null} keeps the current value
     */
    public boolean update(long id, Integer effect, String remark) {
        String sql = "UPDATE da_api_acl SET effect=COALESCE(?, effect), remark=COALESCE(?, remark), update_time=? WHERE id=?";
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (effect == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, effect == 0 ? 0 : 1);
            }
            if (remark == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, remark);
            }
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setLong(4, id);
            boolean changed = ps.executeUpdate() > 0;
            invalidateCache();
            return changed;
        } catch (SQLException e) {
            throw new IllegalStateException("update acl failed: " + e.getMessage(), e);
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM da_api_acl WHERE id=?";
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            boolean changed = ps.executeUpdate() > 0;
            invalidateCache();
            return changed;
        } catch (SQLException e) {
            throw new IllegalStateException("delete acl failed: " + e.getMessage(), e);
        }
    }

    private DataApiAclEntry findExisting(String path, int subjectType, String subjectId) {
        String sql = "SELECT id, api_path, subject_type, subject_id, effect, remark, create_by, create_time, update_time "
                + "FROM da_api_acl WHERE api_path=? AND subject_type=? AND subject_id=? ORDER BY id";
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setInt(2, subjectType);
            ps.setString(3, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.warn("lookup acl failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Rules are read on every gateway call; cache them briefly and invalidate on writes.
     */
    private List<DataApiAclEntry> cachedRules() {
        long ttl = properties.getAclCacheMs();
        if (ttl <= 0) {
            return list(null);
        }
        List<DataApiAclEntry> snapshot = ruleCache;
        if (snapshot != null && System.currentTimeMillis() - ruleCacheAt < ttl) {
            return snapshot;
        }
        synchronized (this) {
            List<DataApiAclEntry> current = ruleCache;
            if (current == null || System.currentTimeMillis() - ruleCacheAt >= ttl) {
                current = list(null);
                ruleCache = current;
                ruleCacheAt = System.currentTimeMillis();
            }
            return current;
        }
    }

    private void invalidateCache() {
        ruleCache = null;
        ruleCacheAt = 0L;
    }

    /**
     * Evaluate whether {@code user} (optionally carrying raw access token) may call {@code requestPath}.
     *
     * @param requestPath path relative to gateway, e.g. {@code /smoke/select1}
     * @param rawToken    original header token if it was an access token (may equal sessionId)
     */
    public boolean isAllowed(User user, String rawToken, String requestPath) {
        return isAllowed(user, rawToken, null, requestPath);
    }

    public boolean isAllowed(User user, String rawToken, String appId, String requestPath) {
        String mode = StringUtils.defaultIfBlank(properties.getAclMode(), "optional").toLowerCase(Locale.ROOT);
        if ("off".equals(mode)) {
            return true;
        }
        if (user == null) {
            return false;
        }
        if (user.getUserType() == UserType.ADMIN_USER) {
            return true;
        }

        String path = normalizeRequestPath(requestPath);
        List<DataApiAclEntry> matched = findMatchingRules(path);
        if (matched.isEmpty()) {
            // optional: no rules → allow authenticated; enforce: deny
            return !"enforce".equals(mode);
        }

        // DENY wins if any deny matches this subject
        for (DataApiAclEntry rule : matched) {
            if (rule.getEffect() == 0 && subjectMatches(rule, user, rawToken, appId)) {
                return false;
            }
        }
        for (DataApiAclEntry rule : matched) {
            if (rule.getEffect() == 1 && subjectMatches(rule, user, rawToken, appId)) {
                return true;
            }
        }
        // path has rules but none match this subject
        return false;
    }

    private List<DataApiAclEntry> findMatchingRules(String requestPath) {
        List<DataApiAclEntry> all = cachedRules();
        List<DataApiAclEntry> matched = new ArrayList<>();
        for (DataApiAclEntry e : all) {
            if (pathMatches(e.getApiPath(), requestPath)) {
                matched.add(e);
            }
        }
        return matched;
    }

    static boolean pathMatches(String pattern, String requestPath) {
        if (StringUtils.isBlank(pattern) || StringUtils.isBlank(requestPath)) {
            return false;
        }
        String p = normalizePathPattern(pattern);
        String r = normalizeRequestPath(requestPath);
        if (p.endsWith("/**")) {
            String prefix = p.substring(0, p.length() - 3);
            return r.equals(prefix) || r.startsWith(prefix.endsWith("/") ? prefix : prefix + "/");
        }
        if (p.endsWith("/*")) {
            String prefix = p.substring(0, p.length() - 2);
            if (!r.startsWith(prefix.endsWith("/") ? prefix : prefix + "/") && !r.equals(prefix)) {
                return false;
            }
            String rest = r.substring(Math.min(r.length(), prefix.length() + (prefix.endsWith("/") ? 0 : 1)));
            return !rest.contains("/");
        }
        return p.equals(r);
    }

    private static boolean subjectMatches(DataApiAclEntry rule, User user, String rawToken, String appId) {
        AclSubjectType type = AclSubjectType.fromCode(rule.getSubjectType());
        String sid = rule.getSubjectId();
        switch (type) {
            case USER:
                return String.valueOf(user.getId()).equals(sid)
                        || (user.getUserName() != null && user.getUserName().equalsIgnoreCase(sid));
            case TOKEN:
                return StringUtils.isNotBlank(rawToken) && rawToken.equals(sid);
            case APP:
                return StringUtils.isNotBlank(appId) && appId.equals(sid);
            default:
                return false;
        }
    }

    public static String normalizePathPattern(String path) {
        if (path == null) {
            return "";
        }
        String p = path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        // strip gateway prefix if pasted by mistake
        if (p.startsWith("/data-api/gateway")) {
            p = p.substring("/data-api/gateway".length());
            if (!p.startsWith("/")) {
                p = "/" + p;
            }
        }
        return p;
    }

    public static String normalizeRequestPath(String path) {
        return normalizePathPattern(path);
    }

    /**
     * Extract gateway-relative path from full request URI.
     */
    public static String extractGatewayRelativePath(String requestUri) {
        if (requestUri == null) {
            return "/";
        }
        String uri = requestUri;
        int idx = uri.indexOf("/data-api/gateway");
        if (idx >= 0) {
            uri = uri.substring(idx + "/data-api/gateway".length());
        }
        int q = uri.indexOf('?');
        if (q >= 0) {
            uri = uri.substring(0, q);
        }
        return normalizeRequestPath(uri);
    }

    private static DataApiAclEntry mapRow(ResultSet rs) throws SQLException {
        DataApiAclEntry e = new DataApiAclEntry();
        e.setId(rs.getLong("id"));
        e.setApiPath(rs.getString("api_path"));
        e.setSubjectType(rs.getInt("subject_type"));
        e.setSubjectId(rs.getString("subject_id"));
        e.setEffect(rs.getInt("effect"));
        e.setRemark(rs.getString("remark"));
        int createBy = rs.getInt("create_by");
        e.setCreateBy(rs.wasNull() ? null : createBy);
        e.setCreateTime(rs.getTimestamp("create_time"));
        e.setUpdateTime(rs.getTimestamp("update_time"));
        return e;
    }
}
