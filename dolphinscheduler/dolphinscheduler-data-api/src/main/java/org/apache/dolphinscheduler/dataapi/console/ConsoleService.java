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

import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.datasource.DsMagicDataSourceLoader;
import org.apache.dolphinscheduler.dataapi.meta.ConnectionParams;
import org.apache.dolphinscheduler.dataapi.meta.DataSourceRepository;
import org.apache.dolphinscheduler.dataapi.meta.DsDataSource;
import org.apache.dolphinscheduler.dataapi.meta.DsPasswordCodec;
import org.apache.dolphinscheduler.dataapi.meta.User;
import org.apache.dolphinscheduler.dataapi.registry.DatasourceExtRepository;
import org.apache.dolphinscheduler.dataapi.registry.DatasourceTypeDescriptor;
import org.apache.dolphinscheduler.dataapi.registry.DatasourceTypeRegistry;
import org.apache.dolphinscheduler.dataapi.security.RateLimitService;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Read-only business query console (plan §7): statement guard, read-only connection layer,
 * row/timeout/concurrency caps, streaming export and cancel. Every attempt — allowed or
 * rejected — is audited by {@link ConsoleAuditService}.
 */
@Slf4j
@Service
public class ConsoleService {

    /** Business error carrying the HTTP status the controller should surface. */
    public static class ConsoleException extends RuntimeException {

        private final int status;

        public ConsoleException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    public static class QueryResult {

        public String queryId;
        public List<Map<String, Object>> columns = new ArrayList<>();
        public List<List<Object>> rows = new ArrayList<>();
        public boolean truncated;
        public long elapsedMs;
        public int rowCount;
    }

    private static final class RunningQuery {

        private final String userId;
        private final Statement statement;

        private RunningQuery(String userId, Statement statement) {
            this.userId = userId;
            this.statement = statement;
        }
    }

    private static final class CachedPool {

        private final HikariDataSource pool;
        private final String fingerprint;

        private CachedPool(HikariDataSource pool, String fingerprint) {
            this.pool = pool;
            this.fingerprint = fingerprint;
        }
    }

    private final DataApiProperties properties;
    private final DataSourceRepository dataSourceRepository;
    private final DatasourceExtRepository extRepository;
    private final DsMagicDataSourceLoader dataSourceLoader;
    private final DatasourceTypeRegistry typeRegistry;
    private final RateLimitService rateLimitService;
    private final ConsoleAuditService auditService;

    private final Map<String, RunningQuery> running = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> perUserRunning = new ConcurrentHashMap<>();
    private final Map<Integer, CachedPool> readonlyPools = new ConcurrentHashMap<>();

    public ConsoleService(DataApiProperties properties,
                          DataSourceRepository dataSourceRepository,
                          DatasourceExtRepository extRepository,
                          DsMagicDataSourceLoader dataSourceLoader,
                          DatasourceTypeRegistry typeRegistry,
                          RateLimitService rateLimitService,
                          ConsoleAuditService auditService) {
        this.properties = properties;
        this.dataSourceRepository = dataSourceRepository;
        this.extRepository = extRepository;
        this.dataSourceLoader = dataSourceLoader;
        this.typeRegistry = typeRegistry;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
    }

    /** Console-visible datasources for the user: authorized in DS AND flagged in da_datasource_ext. */
    public List<Map<String, Object>> listDatasources(User user) {
        boolean admin = user.isAdmin();
        List<Map<String, Object>> items = new ArrayList<>();
        for (DsDataSource ds : dataSourceRepository.listAuthorized(user.getId(), admin)) {
            DatasourceExtRepository.DatasourceExt ext = extRepository.find(ds.getId()).orElse(null);
            if (ext == null || !ext.isConsoleEnabled()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ds.getId());
            item.put("name", ds.getName());
            item.put("type", ds.getType() == null ? null : ds.getType().name());
            item.put("database", ds.getType() == null ? null
                    : StringUtils.defaultString(ConnectionParams
                            .parse(ds.getConnectionParams()) == null ? null
                            : ConnectionParams.parse(ds.getConnectionParams()).getDatabase()));
            item.put("maxRows", Math.min(ext.getMaxRows() <= 0 ? properties.getConsole().getMaxDisplayRows()
                    : ext.getMaxRows(), properties.getConsole().getMaxDisplayRows()));
            items.add(item);
        }
        return items;
    }

    public QueryResult execute(User user, int datasourceId, String sql) {
        long start = System.currentTimeMillis();
        String userId = String.valueOf(user.getId());
        int maxRows = resolveMaxRows(user, datasourceId);
        try {
            authorize(user, datasourceId, sql);
            acquireSlot(userId);
            try (RunContext ctx = open(user, datasourceId, sql)) {
                String queryId = UUID.randomUUID().toString();
                ctx.statement.setQueryTimeout(properties.getConsole().getTimeoutSeconds());
                ctx.statement.setMaxRows(maxRows + 1);
                ctx.statement.setFetchSize(Math.min(maxRows + 1, 1000));
                running.put(queryId, new RunningQuery(userId, ctx.statement));
                try (ResultSet rs = ctx.statement.executeQuery(sql)) {
                    QueryResult result = drain(rs, maxRows);
                    result.queryId = queryId;
                    result.elapsedMs = System.currentTimeMillis() - start;
                    auditService.record(userId, datasourceId, sql, result.rowCount, result.elapsedMs, 200, null);
                    return result;
                } finally {
                    running.remove(queryId);
                }
            } finally {
                releaseSlot(userId);
            }
        } catch (ConsoleException e) {
            auditService.record(userId, datasourceId, sql, 0, System.currentTimeMillis() - start,
                    e.getStatus(), e.getMessage());
            throw e;
        } catch (SQLTimeoutException e) {
            auditService.record(userId, datasourceId, sql, 0, System.currentTimeMillis() - start,
                    408, "query timed out");
            throw new ConsoleException(408, "query timed out after "
                    + properties.getConsole().getTimeoutSeconds() + "s");
        } catch (SQLException e) {
            auditService.record(userId, datasourceId, sql, 0, System.currentTimeMillis() - start,
                    500, abbreviate(e.getMessage()));
            throw new ConsoleException(500, "query failed: " + abbreviate(e.getMessage()));
        }
    }

    /** Streaming CSV export, capped at console.maxExportRows (plan §7.3). */
    public void exportCsv(User user, int datasourceId, String sql, Writer writer) throws IOException {
        long start = System.currentTimeMillis();
        String userId = String.valueOf(user.getId());
        int maxRows = properties.getConsole().getMaxExportRows();
        try {
            authorize(user, datasourceId, sql);
            acquireSlot(userId);
            try (RunContext ctx = open(user, datasourceId, sql)) {
                ctx.statement.setQueryTimeout(properties.getConsole().getTimeoutSeconds());
                ctx.statement.setMaxRows(maxRows + 1);
                ctx.statement.setFetchSize(2000);
                try (ResultSet rs = ctx.statement.executeQuery(sql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columns = meta.getColumnCount();
                    StringBuilder header = new StringBuilder();
                    for (int i = 1; i <= columns; i++) {
                        if (i > 1) {
                            header.append(',');
                        }
                        header.append(csvCell(columnLabel(meta, i)));
                    }
                    writer.write(header.append('\n').toString());
                    int count = 0;
                    while (rs.next() && count < maxRows) {
                        StringBuilder line = new StringBuilder();
                        for (int i = 1; i <= columns; i++) {
                            if (i > 1) {
                                line.append(',');
                            }
                            line.append(csvCell(stringify(rs.getObject(i))));
                        }
                        writer.write(line.append('\n').toString());
                        count++;
                    }
                    writer.flush();
                    auditService.record(userId, datasourceId, sql, count,
                            System.currentTimeMillis() - start, 200, "export");
                }
            } finally {
                releaseSlot(userId);
            }
        } catch (ConsoleException e) {
            auditService.record(userId, datasourceId, sql, 0, System.currentTimeMillis() - start,
                    e.getStatus(), e.getMessage());
            throw e;
        } catch (SQLException e) {
            auditService.record(userId, datasourceId, sql, 0, System.currentTimeMillis() - start,
                    500, abbreviate(e.getMessage()));
            throw new ConsoleException(500, "export failed: " + abbreviate(e.getMessage()));
        }
    }

    public boolean cancel(User user, String queryId) {
        RunningQuery query = running.get(queryId);
        if (query == null || !query.userId.equals(String.valueOf(user.getId()))) {
            return false;
        }
        try {
            query.statement.cancel();
            auditService.record(query.userId, null, null, 0, 0, 499, "cancelled by user");
            return true;
        } catch (SQLException e) {
            log.warn("cancel query {} failed: {}", queryId, e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------- internals

    private int resolveMaxRows(User user, int datasourceId) {
        DatasourceExtRepository.DatasourceExt ext = requireEnabledConsole(datasourceId);
        int cap = properties.getConsole().getMaxDisplayRows();
        if (!user.isAdmin() && ext.getMaxRows() > 0) {
            return Math.min(ext.getMaxRows(), cap);
        }
        return cap;
    }

    private void authorize(User user, int datasourceId, String sql) {
        DataApiProperties.Console cfg = properties.getConsole();
        requireEnabledConsole(datasourceId);
        if (!user.isAdmin() && !dataSourceRepository.canAccess(user.getId(), false, datasourceId)) {
            throw new ConsoleException(403, "no permission for datasource " + datasourceId);
        }
        SqlGuard.Verdict verdict = SqlGuard.check(sql, cfg.getMaxSqlBytes());
        if (!verdict.isAllowed()) {
            throw new ConsoleException(400, verdict.getReason());
        }
        if (!rateLimitService.tryAcquire("console:u:" + user.getId())) {
            throw new ConsoleException(429, "too many console requests");
        }
    }

    private DatasourceExtRepository.DatasourceExt requireEnabledConsole(int datasourceId) {
        DatasourceExtRepository.DatasourceExt ext = extRepository.find(datasourceId).orElse(null);
        if (ext == null || !ext.isConsoleEnabled()) {
            throw new ConsoleException(403, "datasource " + datasourceId + " is not enabled for the console");
        }
        return ext;
    }

    private void acquireSlot(String userId) {
        int limit = Math.max(1, properties.getConsole().getPerUserConcurrency());
        AtomicInteger counter = perUserRunning.computeIfAbsent(userId, k -> new AtomicInteger());
        if (counter.incrementAndGet() > limit) {
            counter.decrementAndGet();
            throw new ConsoleException(429, "another query is already running for this user");
        }
    }

    private void releaseSlot(String userId) {
        AtomicInteger counter = perUserRunning.get(userId);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }

    private RunContext open(User user, int datasourceId, String sql) throws SQLException {
        DataSource pool = resolvePool(datasourceId);
        Connection conn = pool.getConnection();
        try {
            conn.setReadOnly(true);
            DatasourceTypeDescriptor descriptor = descriptorFor(datasourceId);
            Statement statement = conn.createStatement();
            boolean explicitTx = descriptor != null && descriptor.hasReadOnlyTx();
            if (explicitTx) {
                conn.setAutoCommit(false);
                try (Statement tx = conn.createStatement()) {
                    tx.execute(descriptor.getReadOnlyTx());
                }
            }
            return new RunContext(conn, statement, explicitTx);
        } catch (SQLException | RuntimeException e) {
            conn.close();
            throw e;
        }
    }

    private DatasourceTypeDescriptor descriptorFor(int datasourceId) {
        List<DsDataSource> sources = dataSourceRepository.findByIds(List.of(datasourceId));
        if (sources.isEmpty() || sources.get(0).getType() == null) {
            return null;
        }
        return typeRegistry.find(sources.get(0).getType().name());
    }

    /**
     * The console authenticates with the read-only account from da_datasource_ext (plan §6.3);
     * the DS-center pool is only reused when no dedicated account is configured.
     */
    private DataSource resolvePool(int datasourceId) {
        HikariDataSource dsPool = dataSourceLoader.poolByDatasourceId(datasourceId);
        if (dsPool == null) {
            throw new ConsoleException(400, "datasource " + datasourceId + " is not loaded (check its type descriptor)");
        }
        DatasourceExtRepository.DatasourceExt ext = extRepository.find(datasourceId).orElse(null);
        if (ext == null || StringUtils.isBlank(ext.getReadonlyUser())) {
            return dsPool;
        }
        String fingerprint = ext.getReadonlyUser() + "|" + StringUtils.defaultString(ext.getReadonlyPasswordEnc())
                + "|" + ext.getUpdateTime();
        CachedPool cached = readonlyPools.get(datasourceId);
        if (cached != null && cached.fingerprint.equals(fingerprint)) {
            return cached.pool;
        }
        synchronized (this) {
            cached = readonlyPools.get(datasourceId);
            if (cached != null && cached.fingerprint.equals(fingerprint)) {
                return cached.pool;
            }
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dsPool.getJdbcUrl());
            config.setDriverClassName(dsPool.getDriverClassName());
            config.setUsername(ext.getReadonlyUser());
            config.setPassword(DsPasswordCodec.decodePassword(ext.getReadonlyPasswordEnc()));
            config.setPoolName("da-console-" + datasourceId);
            config.setMaximumPoolSize(Math.min(3, properties.getPoolMaximumPoolSize()));
            config.setMinimumIdle(0);
            config.setReadOnly(true);
            config.setInitializationFailTimeout(-1);
            HikariDataSource created = new HikariDataSource(config);
            HikariDataSource old = readonlyPools.containsKey(datasourceId)
                    ? readonlyPools.remove(datasourceId).pool : null;
            readonlyPools.put(datasourceId, new CachedPool(created, fingerprint));
            if (old != null) {
                old.close();
            }
            return created;
        }
    }

    private QueryResult drain(ResultSet rs, int maxRows) throws SQLException {
        QueryResult result = new QueryResult();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("name", columnLabel(meta, i));
            column.put("type", meta.getColumnTypeName(i));
            column.put("nullable", meta.isNullable(i) != ResultSetMetaData.columnNoNulls);
            result.columns.add(column);
        }
        int count = 0;
        while (rs.next()) {
            if (count >= maxRows) {
                result.truncated = true;
                break;
            }
            List<Object> row = new ArrayList<>(meta.getColumnCount());
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.add(stringify(rs.getObject(i)));
            }
            result.rows.add(row);
            count++;
        }
        result.rowCount = count;
        return result;
    }

    private static String columnLabel(ResultSetMetaData meta, int i) throws SQLException {
        String label = meta.getColumnLabel(i);
        return StringUtils.isNotBlank(label) ? label : meta.getColumnName(i);
    }

    /** JSON/CSV friendly value conversion; binary and LOB payloads are summarized, not shipped. */
    static Object stringify(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof java.sql.Date || value instanceof java.sql.Time
                || value instanceof java.util.Date || value instanceof java.time.temporal.Temporal) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return "<binary " + bytes.length + " bytes>";
        }
        if (value instanceof java.sql.Clob clob) {
            try {
                long len = Math.min(clob.length(), 8192);
                return clob.getSubString(1, (int) len);
            } catch (SQLException e) {
                return "<unreadable clob>";
            }
        }
        if (value instanceof java.sql.Blob blob) {
            try {
                return "<binary " + blob.length() + " bytes>";
            } catch (SQLException e) {
                return "<unreadable blob>";
            }
        }
        return String.valueOf(value);
    }

    static String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static String abbreviate(String message) {
        return StringUtils.abbreviate(StringUtils.defaultString(message, "unknown error"), 480);
    }

    /** Connection + statement pair; read-only transactions are always rolled back. */
    private static final class RunContext implements AutoCloseable {

        private final Connection connection;
        private final Statement statement;
        private final boolean explicitTx;

        private RunContext(Connection connection, Statement statement, boolean explicitTx) {
            this.connection = connection;
            this.statement = statement;
            this.explicitTx = explicitTx;
        }

        @Override
        public void close() {
            try {
                statement.close();
            } catch (SQLException ignored) {
                // best effort
            }
            try {
                if (explicitTx && !connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (SQLException ignored) {
                // best effort
            }
            try {
                connection.close();
            } catch (SQLException ignored) {
                // best effort
            }
        }
    }
}
