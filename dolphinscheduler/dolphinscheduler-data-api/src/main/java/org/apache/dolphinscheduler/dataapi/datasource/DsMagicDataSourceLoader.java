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

import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.meta.ConnectionParams;
import org.apache.dolphinscheduler.dataapi.meta.DataSourceRepository;
import org.apache.dolphinscheduler.dataapi.meta.DbType;
import org.apache.dolphinscheduler.dataapi.meta.DsDataSource;
import org.apache.dolphinscheduler.dataapi.meta.DsPasswordCodec;
import org.apache.dolphinscheduler.dataapi.registry.DatasourceTypeDescriptor;
import org.apache.dolphinscheduler.dataapi.registry.DatasourceTypeRegistry;

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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Loads DolphinScheduler datasource center entries into magic-api dynamic datasources.
 * <p>
 * Pools are built from the type descriptor registry (datasource-types.yaml) instead of the DS
 * plugin manager, so the Boot 3 module no longer depends on dolphinscheduler-datasource-*.
 * <p>
 * Magic key prefers sanitized DS {@code name} for readability (e.g. {@code osint_mysql});
 * console display name keeps original name + type/id/database for mapping back to 源中心.
 */
@Slf4j
@Component
public class DsMagicDataSourceLoader {

    private final DataSourceRepository dataSourceRepository;
    private final DatasourceTypeRegistry typeRegistry;
    private final DataApiProperties properties;
    private final MagicDynamicDataSource magicDynamicDataSource;

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final Map<String, String> fingerprints = new ConcurrentHashMap<>();
    /** magic key → DS center id (for permission / UI mapping) */
    private final Map<String, Integer> keyToDatasourceId = new ConcurrentHashMap<>();
    /** DS center id → magic key (reverse index for the query console) */
    private final Map<Integer, String> datasourceIdToKey = new ConcurrentHashMap<>();
    /** magic key → snapshot for admin UI */
    private final Map<String, LoadedDatasourceView> loadedViews = new ConcurrentHashMap<>();

    public DsMagicDataSourceLoader(DataSourceRepository dataSourceRepository,
                                   DatasourceTypeRegistry typeRegistry,
                                   DataApiProperties properties,
                                   MagicDynamicDataSource magicDynamicDataSource) {
        this.dataSourceRepository = dataSourceRepository;
        this.typeRegistry = typeRegistry;
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
        List<DsDataSource> sources = listTargetSources();
        // first pass: assign unique keys from names
        Map<Integer, String> idToKey = assignKeys(sources);

        for (DsDataSource source : sources) {
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

    /** Console lookup: the live pool for a DS-center datasource id, or null when not loaded. */
    public HikariDataSource poolByDatasourceId(int datasourceId) {
        String key = datasourceIdToKey.get(datasourceId);
        return key == null ? null : pools.get(key);
    }

    public String magicKeyOf(int datasourceId) {
        return datasourceIdToKey.get(datasourceId);
    }

    public List<LoadedDatasourceView> loadedViews() {
        return new ArrayList<>(loadedViews.values());
    }

    private List<DsDataSource> listTargetSources() {
        List<DsDataSource> all = dataSourceRepository.selectAll();
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        List<DsDataSource> filtered = new ArrayList<>();
        for (DsDataSource source : all) {
            if (source.getType() == null) {
                continue;
            }
            String typeName = source.getType().name().toUpperCase(Locale.ROOT);
            if (!properties.getLoadDbTypes().contains(typeName)) {
                continue;
            }
            if (typeRegistry.find(typeName) == null) {
                log.warn("skip datasource id={} type={}: no descriptor in datasource-types.yaml",
                        source.getId(), typeName);
                continue;
            }
            filtered.add(source);
        }
        return filtered;
    }

    /**
     * Prefer sanitized DS name as magic key; on collision / invalid name append {@code _id}.
     */
    static Map<Integer, String> assignKeys(List<DsDataSource> sources) {
        Map<Integer, String> idToKey = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();
        for (DsDataSource source : sources) {
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

    static String fallbackTypeIdKey(DsDataSource source) {
        DbType type = source.getType();
        String prefix = type == null ? "ds" : type.getName().toLowerCase(Locale.ROOT);
        return prefix + "_" + source.getId();
    }

    /** @deprecated use {@link #assignKeys(List)} ; kept for tests/compat */
    public static String buildKey(DsDataSource source) {
        String candidate = sanitizeKeyCandidate(source.getName());
        if (isValidIdentifier(candidate)) {
            return candidate;
        }
        return fallbackTypeIdKey(source);
    }

    private void register(DsDataSource source, String key, String fingerprint) {
        DatasourceTypeDescriptor descriptor = typeRegistry.find(source.getType().name());
        ConnectionParams params = ConnectionParams.parse(source.getConnectionParams());
        if (params == null) {
            throw new IllegalStateException("unsupported connection params for " + source.getType());
        }
        String jdbcUrl = params.effectiveJdbcUrl();
        String password = DsPasswordCodec.decodePassword(params.getPassword());
        String database = StringUtils.defaultString(params.getDatabase(), "");

        HikariConfig config = new HikariConfig();
        config.setPoolName("da-" + key);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(params.getUser());
        config.setPassword(password);
        config.setDriverClassName(StringUtils.defaultIfBlank(descriptor.getDriver(),
                StringUtils.defaultString(params.getDriverClassName())));
        config.setMaximumPoolSize(properties.getPoolMaximumPoolSize());
        config.setMinimumIdle(properties.getPoolMinimumIdle());
        config.setConnectionTimeout(10000);
        // do not fail service boot/reload when remote FE/DB is temporarily down
        config.setInitializationFailTimeout(-1);
        config.setConnectionTestQuery(
                StringUtils.defaultIfBlank(params.getValidationQuery(), descriptor.getValidationQuery()));
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
        datasourceIdToKey.put(source.getId(), key);
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
     * an empty override disables it. Defaults come from the type descriptor.
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
        DatasourceTypeDescriptor descriptor = typeRegistry.find(name);
        if (descriptor == null || StringUtils.isBlank(descriptor.getSessionInitSql())) {
            return null;
        }
        return applyTimeout(descriptor.getSessionInitSql());
    }

    private String applyTimeout(String sql) {
        int seconds = properties.getQueryTimeoutSeconds();
        return sql.replace("{timeoutSeconds}", String.valueOf(seconds))
                .replace("{timeoutMillis}", String.valueOf(seconds * 1000L));
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
        datasourceIdToKey.clear();
        loadedViews.clear();
    }

    static String buildDisplayName(DsDataSource source, String database) {
        String name = StringUtils.defaultIfBlank(source.getName(), "ds");
        String type = source.getType() == null ? "?" : source.getType().name();
        if (StringUtils.isNotBlank(database)) {
            return String.format("%s [%s#%d / %s]", name, type, source.getId(), database);
        }
        return String.format("%s [%s#%d]", name, type, source.getId());
    }

    private void unregister(String key) {
        fingerprints.remove(key);
        Integer dsId = keyToDatasourceId.remove(key);
        if (dsId != null) {
            datasourceIdToKey.remove(dsId);
        }
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
