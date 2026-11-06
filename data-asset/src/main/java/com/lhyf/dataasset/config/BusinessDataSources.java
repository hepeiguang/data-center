package com.lhyf.dataasset.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.lhyf.dataasset.browse.SqlDialect;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Creates one read-only pool per configured business datasource.
 * Credentials never leave this service; pools are lazily created and fail startup gracefully.
 */
@Component
public class BusinessDataSources {

    private final Map<String, HikariDataSource> pools = new LinkedHashMap<>();
    private final Map<String, JdbcTemplate> templates = new LinkedHashMap<>();
    private final Map<String, String> jdbcUrls = new LinkedHashMap<>();
    private final Map<String, SqlDialect> dialects = new LinkedHashMap<>();

    public BusinessDataSources(DataAssetProperties properties) {
        properties.getDatasources().forEach((name, cfg) -> {
            jdbcUrls.put(name, cfg.getUrl());
            HikariConfig hikari = new HikariConfig();
            hikari.setPoolName("asset-" + name);
            hikari.setJdbcUrl(cfg.getUrl());
            hikari.setUsername(cfg.getUsername());
            hikari.setPassword(cfg.getPassword());
            hikari.setReadOnly(true);
            hikari.setAutoCommit(true);
            hikari.setMaximumPoolSize(Math.max(1, cfg.getPoolSize()));
            hikari.setMinimumIdle(0);
            hikari.setConnectionTimeout(5_000);
            hikari.setValidationTimeout(3_000);
            hikari.setInitializationFailTimeout(-1);
            hikari.setConnectionTestQuery("select 1");
            HikariDataSource ds = new HikariDataSource(hikari);
            pools.put(name, ds);
            JdbcTemplate template = new JdbcTemplate(ds);
            template.setQueryTimeout(60);
            template.setMaxRows(10_000);
            templates.put(name, template);
        });
    }

    public JdbcTemplate get(String name) {
        JdbcTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("unknown datasource: " + name);
        }
        return template;
    }

    /** SQL dialect for one configured datasource, derived from its JDBC url. */
    public SqlDialect dialect(String name) {
        get(name);
        return dialects.computeIfAbsent(name, n -> SqlDialect.forJdbcUrl(jdbcUrls.get(n)));
    }

    public Map<String, String> health() {
        Map<String, String> result = new LinkedHashMap<>();
        pools.forEach((name, pool) -> {
            try (java.sql.Connection ignored = pool.getConnection()) {
                result.put(name, "ok");
            } catch (Exception e) {
                result.put(name, "error: " + e.getClass().getSimpleName());
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public void close() {
        pools.values().forEach(ds -> {
            try {
                ds.close();
            } catch (Exception ignored) {
                // best effort on shutdown
            }
        });
    }
}
