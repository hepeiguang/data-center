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

package org.apache.dolphinscheduler.dataapi.security;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Application credentials ({@code da_app_credential}).
 * Only the SHA-256 hash of the secret is stored; the plaintext is returned once on create/rotate.
 */
@Slf4j
@Service
public class AppCredentialService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DataSource metaDataSource;

    public AppCredentialService(DataSource metaDataSource) {
        this.metaDataSource = metaDataSource;
    }

    public AppCredential create(String appId, int userId, String scopes, Timestamp expireTime) {
        String normalized = normalizeAppId(appId);
        String secret = newSecret();
        String sql = "INSERT INTO da_app_credential(app_id, app_key_hash, user_id, scopes, expire_time, create_time) "
                + "VALUES (?,?,?,?,?,?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, normalized);
            ps.setString(2, sha256(secret));
            ps.setInt(3, userId);
            if (StringUtils.isBlank(scopes)) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, scopes.trim());
            }
            if (expireTime == null) {
                ps.setNull(5, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(5, expireTime);
            }
            ps.setTimestamp(6, now);
            ps.executeUpdate();
            Long id = null;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }
            return new AppCredential(id, normalized, secret, userId, scopes, expireTime, now);
        } catch (SQLException e) {
            throw new IllegalStateException("create app credential failed: " + e.getMessage(), e);
        }
    }

    /**
     * @return principal when app id / secret / expiry all match, otherwise {@code null}
     */
    public AppPrincipal authenticate(String appId, String secret) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(secret)) {
            return null;
        }
        String sql = "SELECT app_id, app_key_hash, user_id, scopes, expire_time FROM da_app_credential WHERE app_id=?";
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp expire = rs.getTimestamp("expire_time");
                if (expire != null && expire.getTime() < System.currentTimeMillis()) {
                    return null;
                }
                if (!MessageDigest.isEqual(sha256(secret).getBytes(StandardCharsets.UTF_8),
                        rs.getString("app_key_hash").getBytes(StandardCharsets.UTF_8))) {
                    return null;
                }
                return new AppPrincipal(rs.getString("app_id"), rs.getInt("user_id"), parseScopes(rs.getString("scopes")));
            }
        } catch (SQLException e) {
            log.warn("authenticate app failed: {}", e.getMessage());
            return null;
        }
    }

    public String rotate(long id) {
        String secret = newSecret();
        String sql = "UPDATE da_app_credential SET app_key_hash=? WHERE id=?";
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sha256(secret));
            ps.setLong(2, id);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("app not found: " + id);
            }
            return secret;
        } catch (SQLException e) {
            throw new IllegalStateException("rotate app credential failed: " + e.getMessage(), e);
        }
    }

    public List<AppCredential> list() {
        String sql = "SELECT id, app_id, user_id, scopes, expire_time, create_time FROM da_app_credential"
                + " ORDER BY id";
        List<AppCredential> result = new ArrayList<>();
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new AppCredential(rs.getLong("id"), rs.getString("app_id"), null,
                        rs.getInt("user_id"), rs.getString("scopes"), rs.getTimestamp("expire_time"),
                        rs.getTimestamp("create_time")));
            }
        } catch (SQLException e) {
            log.warn("list app credentials failed: {}", e.getMessage());
        }
        return result;
    }

    public boolean delete(long id) {
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM da_app_credential WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("delete app credential failed: " + e.getMessage(), e);
        }
    }

    private static String normalizeAppId(String appId) {
        if (StringUtils.isBlank(appId) || !appId.trim().matches("^[A-Za-z][A-Za-z0-9_-]{2,63}$")) {
            throw new IllegalArgumentException("appId must match [A-Za-z][A-Za-z0-9_-]{2,63}");
        }
        return appId.trim();
    }

    private static String newSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Set<String> parseScopes(String scopes) {
        if (StringUtils.isBlank(scopes)) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String scope : scopes.split(",")) {
            String value = scope.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class AppPrincipal {

        private final String appId;
        private final int userId;
        private final Set<String> scopes;

        AppPrincipal(String appId, int userId, Set<String> scopes) {
            this.appId = appId;
            this.userId = userId;
            this.scopes = scopes;
        }

        public String getAppId() {
            return appId;
        }

        public int getUserId() {
            return userId;
        }

        public boolean allows(String apiPath) {
            if (scopes.isEmpty()) {
                return true;
            }
            for (String scope : scopes) {
                if (scope.endsWith("/**")) {
                    String prefix = scope.substring(0, scope.length() - 3);
                    if (apiPath.equals(prefix) || apiPath.startsWith(prefix + "/")) {
                        return true;
                    }
                } else if (scope.equals(apiPath)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class AppCredential {

        private final Long id;
        private final String appId;
        private final String secret;
        private final int userId;
        private final String scopes;
        private final Timestamp expireTime;
        private final Timestamp createTime;

        private AppCredential(String appId, String secret, int userId, String scopes, Timestamp expireTime,
                              Timestamp createTime) {
            this(null, appId, secret, userId, scopes, expireTime, createTime);
        }

        private AppCredential(Long id, String appId, String secret, int userId, String scopes, Timestamp expireTime,
                              Timestamp createTime) {
            this.id = id;
            this.appId = appId;
            this.secret = secret;
            this.userId = userId;
            this.scopes = scopes;
            this.expireTime = expireTime;
            this.createTime = createTime;
        }

        public Long getId() {
            return id;
        }

        public String getAppId() {
            return appId;
        }

        public String getSecret() {
            return secret;
        }

        public int getUserId() {
            return userId;
        }

        public String getScopes() {
            return scopes;
        }

        public Timestamp getExpireTime() {
            return expireTime;
        }

        public Timestamp getCreateTime() {
            return createTime;
        }
    }
}
