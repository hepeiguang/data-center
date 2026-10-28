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

package org.apache.dolphinscheduler.dataapi.datasource;

import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.PasswordUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Loads DolphinScheduler datasource center entries (Doris/MySQL by default) into magic-api dynamic datasources.
 * <p>
 * Magic key prefers sanitized DS {@code name} for readability (e.g. {@code osint_mysql});
 * console display name keeps original name + type/id/database for mapping back to 源中心.
 */
@Slf4j
@Component
public class DsMagicDataSourceLoader {

    private final DataSourceMapper dataSourceMapper;
    private final DataApiProperties properties;
    private final MagicDynamicDataSource magicDynamicDataSource;

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final Map<String, String> fingerprints = new ConcurrentHashMap<>();
    /** magic key → DS center id (for permission / UI mapping) */
    private final Map<String, Integer> keyToDatasourceId = new ConcurrentHashMap<>();
    /** magic key → snapshot for admin UI */
    private final Map<String, LoadedDatasourceView> loadedViews = new ConcurrentHashMap<>();

    public DsMagicDataSourceLoader(DataSourceMapper dataSourceMapper,
                                   DataApiProperties properties,
                                   MagicDynamicDataSource magicDynamicDataSource) {
        this.dataSourceMapper = dataSourceMapper;
        this.properties = properties;
        this.magicDynamicDataSource = magicDynamicDataSource;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    @Scheduled(fixedDelayString = "${data-api.datasource-refresh-ms:60000}")
    public void scheduledReload() {
        reload();
    }

    public synchronized void reload() {
        Set<String> desiredKeys = new HashSet<>();
        List<DataSource> sources = listTargetSources();
        // first pass: assign unique keys from names
        Map<Integer, String> idToKey = assignKeys(sources);

        for (DataSource source : sources) {
            if (source.getType() == null || StringUtils.isBlank(source.getConnectionParams())) {
                continue;
            }
            String key = idToKey.get(source.getId());
            if (key == null) {
                continue;
            }
            desiredKeys.add(key);
            String fingerprint =
                    source.getId() + "|" + source.getName() + "|" + source.getUpdateTime() + "|"
                            + source.getConnectionParams();
            if (fingerprint.equals(fingerprints.get(key))) {
                continue;
            }
            try {
                register(source, key, fingerprint);
            } catch (Exception e) {
                log.error("Failed to register datasource key={} id={} name={}", key, source.getId(), source.getName(),
                        e);
            }
        }
        // remove stale
        List<String> toRemove = new ArrayList<>();
        for (String existing : pools.keySet()) {
            if (!desiredKeys.contains(existing)) {
                toRemove.add(existing);
            }
        }
        for (String key : toRemove) {
            unregister(key);
        }
        log.info("Magic datasources refreshed, active={}", pools.keySet());
    }

    public Set<String> loadedKeys() {
        return Collections.unmodifiableSet(pools.keySet());
    }

    public Integer resolveDatasourceId(String magicKey) {
        if (StringUtils.isBlank(magicKey)) {
            return null;
        }
        return keyToDatasourceId.get(magicKey);
    }

    public List<LoadedDatasourceView> loadedViews() {
        return new ArrayList<>(loadedViews.values());
    }

    private List<DataSource> listTargetSources() {
        List<DataSource> all = dataSourceMapper.selectList(new QueryWrapper<>());
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        List<DataSource> filtered = new ArrayList<>();
        for (DataSource source : all) {
            if (source.getType() == null) {
                continue;
            }
            String typeName = source.getType().name().toUpperCase(Locale.ROOT);
            if (properties.getLoadDbTypes().contains(typeName)) {
                filtered.add(source);
            }
        }
        return filtered;
    }

    /**
     * Prefer sanitized DS name as magic key; on collision / invalid name append {@code _id}.
     */
    static Map<Integer, String> assignKeys(List<DataSource> sources) {
        Map<Integer, String> idToKey = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();
        for (DataSource source : sources) {
            if (source.getType() == null || source.getId() == null) {
                continue;
            }
            String candidate = sanitizeKeyCandidate(source.getName());
            if (StringUtils.isBlank(candidate)) {
                candidate = fallbackTypeIdKey(source);
            }
            String key = candidate;
            if (used.contains(key) || !isValidIdentifier(key)) {
                key = candidate + "_" + source.getId();
                if (!isValidIdentifier(key) || used.contains(key)) {
                    key = fallbackTypeIdKey(source);
                }
            }
            used.add(key);
            idToKey.put(source.getId(), key);
        }
        return idToKey;
    }

    static String sanitizeKeyCandidate(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        String base = name.trim()
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_");
        base = StringUtils.strip(base, "_");
        return base;
    }

    static boolean isValidIdentifier(String key) {
        return StringUtils.isNotBlank(key) && key.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }

    static String fallbackTypeIdKey(DataSource source) {
        DbType type = source.getType();
        String prefix = type == null ? "ds" : type.getName().toLowerCase(Locale.ROOT);
        return prefix + "_" + source.getId();
    }

    /** @deprecated use {@link #assignKeys(List)} ; kept for tests/compat */
    public static String buildKey(DataSource source) {
        String candidate = sanitizeKeyCandidate(source.getName());
        if (isValidIdentifier(candidate)) {
            return candidate;
        }
        return fallbackTypeIdKey(source);
    }

    private void register(DataSource source, String key, String fingerprint) {
        ConnectionParam connectionParam =
                DataSourceUtils.buildConnectionParams(source.getType(), source.getConnectionParams());
        if (!(connectionParam instanceof BaseConnectionParam)) {
            throw new IllegalStateException("unsupported connection param for " + source.getType());
        }
        BaseConnectionParam base = (BaseConnectionParam) connectionParam;
        String jdbcUrl = DataSourceUtils.getJdbcUrl(source.getType(), connectionParam);
        String driver = DataSourceUtils.getDatasourceDriver(source.getType());
        String password = PasswordUtils.decodePassword(base.getPassword());
        String database = StringUtils.defaultString(base.getDatabase(), "");

        HikariConfig config = new HikariConfig();
        config.setPoolName("da-" + key);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(base.getUser());
        config.setPassword(password);
        config.setDriverClassName(StringUtils.defaultIfBlank(base.getDriverClassName(), driver));
        config.setMaximumPoolSize(properties.getPoolMaximumPoolSize());
        config.setMinimumIdle(properties.getPoolMinimumIdle());
        config.setConnectionTimeout(10000);
        // do not fail service boot/reload when remote FE/DB is temporarily down
        config.setInitializationFailTimeout(-1);
        config.setConnectionTestQuery(
                StringUtils.defaultIfBlank(base.getValidationQuery(), "select 1"));
        String initSql = resolveSessionInitSql(source.getType());
        if (StringUtils.isNotBlank(initSql)) {
            config.setConnectionInitSql(initSql);
        }
        config.setReadOnly(properties.isReadOnlyConnections());

        HikariDataSource newPool = new HikariDataSource(config);
        HikariDataSource old = pools.put(key, newPool);

        String displayName = buildDisplayName(source, database);
        // id = DS center id — magic will drop old key when same id re-registers under new key
        magicDynamicDataSource.put(String.valueOf(source.getId()), key, displayName, newPool,
                properties.getMaxRows());

        keyToDatasourceId.put(key, source.getId());
        String safeUrl = maskJdbcUrl(jdbcUrl);
        loadedViews.put(key, new LoadedDatasourceView(
                key,
                source.getName(),
                displayName,
                source.getType() == null ? null : source.getType().name(),
                source.getId(),
                database,
                safeUrl));
        fingerprints.put(key, fingerprint);
        if (old != null) {
            old.close();
        }
        log.info("Registered magic datasource key={} name={} type={} db={} url={} initSql={}",
                key, source.getName(), source.getType(), database, safeUrl,
                StringUtils.defaultIfBlank(initSql, "-"));
    }

    /**
     * Session-level query timeout per dialect; {@code data-api.session-init-sql.<TYPE>} overrides,
     * an empty override disables it.
     */
    String resolveSessionInitSql(DbType type) {
        if (type == null) {
            return null;
        }
        String name = type.name();
        for (Map.Entry<String, String> entry : properties.getSessionInitSql().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                String override = entry.getValue();
                return StringUtils.isBlank(override) ? null : applyTimeout(override);
            }
        }
        return defaultSessionInitSql(type);
    }

    private String defaultSessionInitSql(DbType type) {
        int seconds = properties.getQueryTimeoutSeconds();
        if (seconds <= 0) {
            return null;
        }
        switch (type) {
            case MYSQL:
                return "SET SESSION max_execution_time = " + (seconds * 1000L);
            case DORIS:
                return "SET query_timeout = " + seconds;
            case POSTGRESQL:
                return "SET statement_timeout = " + (seconds * 1000L);
            default:
                return null;
        }
    }

    private String applyTimeout(String sql) {
        return sql.replace("{timeoutSeconds}", String.valueOf(properties.getQueryTimeoutSeconds()));
    }

    /** Hide credentials that occasionally travel inside JDBC URLs before logging / exposing them. */
    static String maskJdbcUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        return url.replaceAll("(?i)(password|pwd|user|username)=([^&;]*)", "$1=***");
    }

    @PreDestroy
    public synchronized void shutdown() {
        for (HikariDataSource pool : pools.values()) {
            try {
                pool.close();
            } catch (Exception e) {
                log.warn("close datasource pool failed: {}", e.getMessage());
            }
        }
        pools.clear();
        fingerprints.clear();
        keyToDatasourceId.clear();
        loadedViews.clear();
    }

    static String buildDisplayName(DataSource source, String database) {
        String name = StringUtils.defaultIfBlank(source.getName(), "ds");
        String type = source.getType() == null ? "?" : source.getType().name();
        if (StringUtils.isNotBlank(database)) {
            return String.format("%s [%s#%d / %s]", name, type, source.getId(), database);
        }
        return String.format("%s [%s#%d]", name, type, source.getId());
    }

    private void unregister(String key) {
        fingerprints.remove(key);
        keyToDatasourceId.remove(key);
        loadedViews.remove(key);
        HikariDataSource old = pools.remove(key);
        removeFromMagic(key);
        if (old != null) {
            old.close();
        }
        log.info("Unregistered magic datasource key={}", key);
    }

    private void removeFromMagic(String key) {
        try {
            magicDynamicDataSource.delete(key);
        } catch (Exception e) {
            log.warn("magicDynamicDataSource.delete({}) failed: {}", key, e.getMessage());
        }
    }

    /**
     * Admin / UI view of a loaded magic datasource and its DS-center mapping.
     */
    public static final class LoadedDatasourceView {
        private final String key;
        private final String name;
        private final String displayName;
        private final String type;
        private final Integer id;
        private final String database;
        private final String jdbcUrl;

        public LoadedDatasourceView(String key, String name, String displayName, String type, Integer id,
                                    String database, String jdbcUrl) {
            this.key = key;
            this.name = name;
            this.displayName = displayName;
            this.type = type;
            this.id = id;
            this.database = database;
            this.jdbcUrl = jdbcUrl;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getType() {
            return type;
        }

        public Integer getId() {
            return id;
        }

        public String getDatabase() {
            return database;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("key", key);
            m.put("name", name);
            m.put("displayName", displayName);
            m.put("type", type);
            m.put("id", id);
            m.put("database", database);
            m.put("jdbcUrl", jdbcUrl);
            m.put("scriptHint", "db." + key + ".select('select 1')");
            return m;
        }
    }
}
