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

package org.apache.dolphinscheduler.dataapi.service;

import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;

import org.apache.commons.lang3.StringUtils;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Best-effort invoke audit into da_api_invoke_log (table created by SQL scripts).
 * <p>
 * Requests must never block on audit IO, so rows go through a bounded queue and a single
 * writer thread batches inserts. When the queue is full the row is dropped and counted.
 */
@Slf4j
@Service
public class DataApiInvokeLogService {

    private static final int BATCH_SIZE = 200;
    private static final long POLL_MILLIS = 500L;

    private final DataSource metaDataSource;
    private final DataApiProperties properties;
    private final BlockingQueue<InvokeLogEntry> queue;
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong written = new AtomicLong();

    private volatile boolean running = true;
    private Thread writer;

    public DataApiInvokeLogService(DataSource metaDataSource, DataApiProperties properties) {
        this.metaDataSource = metaDataSource;
        this.properties = properties;
        this.queue = new LinkedBlockingQueue<>(Math.max(100, properties.getInvokeLogQueueSize()));
    }

    @PostConstruct
    public void start() {
        if (!properties.isInvokeLogEnabled()) {
            log.info("Invoke audit log disabled");
            return;
        }
        writer = new Thread(this::drainLoop, "da-invoke-log-writer");
        writer.setDaemon(true);
        writer.start();
    }

    public void record(String apiPath, String userId, long latencyMs, int code, Integer rowCount, String traceId) {
        record(apiPath, userId, null, latencyMs, code, rowCount, traceId);
    }

    public void record(String apiPath, String userId, String appId, long latencyMs, int code, Integer rowCount,
                       String traceId) {
        if (!properties.isInvokeLogEnabled() || apiPath == null) {
            return;
        }
        if (!queue.offer(new InvokeLogEntry(apiPath, userId, appId, latencyMs, code, rowCount, traceId))) {
            long total = dropped.incrementAndGet();
            if (total == 1 || total % 1000 == 0) {
                log.warn("Invoke audit queue full, dropped={} rows (latest path={})", total, apiPath);
            }
        }
    }

    public long droppedCount() {
        return dropped.get();
    }

    public long writtenCount() {
        return written.get();
    }

    private void drainLoop() {
        List<InvokeLogEntry> batch = new ArrayList<>(BATCH_SIZE);
        while (running || !queue.isEmpty()) {
            try {
                InvokeLogEntry first = queue.poll(POLL_MILLIS, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                queue.drainTo(batch, BATCH_SIZE - 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running) {
                    break;
                }
                continue;
            }
            flush(batch);
        }
        if (!batch.isEmpty()) {
            flush(batch);
        }
    }

    private void flush(List<InvokeLogEntry> batch) {
        if (batch.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO da_api_invoke_log"
                + "(api_path, user_id, app_id, latency_ms, row_count, code, trace_id, create_time) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            for (InvokeLogEntry entry : batch) {
                ps.setString(1, entry.apiPath);
                if (entry.userId == null) {
                    ps.setNull(2, Types.VARCHAR);
                } else {
                    ps.setString(2, entry.userId);
                }
                if (entry.appId == null) {
                    ps.setNull(3, Types.VARCHAR);
                } else {
                    ps.setString(3, entry.appId);
                }
                ps.setLong(4, entry.latencyMs);
                if (entry.rowCount == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, entry.rowCount);
                }
                ps.setInt(6, entry.code);
                if (entry.traceId == null) {
                    ps.setNull(7, Types.VARCHAR);
                } else {
                    ps.setString(7, entry.traceId);
                }
                ps.setTimestamp(8, now);
                ps.addBatch();
            }
            ps.executeBatch();
            written.addAndGet(batch.size());
        } catch (SQLException e) {
            // table may not exist yet in fresh env — audit must not break API calls
            log.debug("skip invoke log batch ({} rows): {}", batch.size(), e.getMessage());
        } finally {
            batch.clear();
        }
    }

    /**
     * Aggregated call statistics per API path.
     */
    public List<Map<String, Object>> stats(String apiPath, long fromMs, long toMs) {
        StringBuilder sql = new StringBuilder("SELECT api_path, COUNT(*) AS calls,"
                + " SUM(CASE WHEN code >= 400 THEN 1 ELSE 0 END) AS errors,"
                + " AVG(latency_ms) AS avg_latency_ms, MAX(latency_ms) AS max_latency_ms"
                + " FROM da_api_invoke_log WHERE create_time BETWEEN ? AND ?");
        if (StringUtils.isNotBlank(apiPath)) {
            sql.append(" AND api_path = ?");
        }
        sql.append(" GROUP BY api_path ORDER BY calls DESC");
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setTimestamp(1, new Timestamp(fromMs));
            ps.setTimestamp(2, new Timestamp(toMs));
            if (StringUtils.isNotBlank(apiPath)) {
                ps.setString(3, apiPath);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("apiPath", rs.getString("api_path"));
                    row.put("calls", rs.getLong("calls"));
                    row.put("errors", rs.getLong("errors"));
                    row.put("avgLatencyMs", Math.round(rs.getDouble("avg_latency_ms")));
                    row.put("maxLatencyMs", rs.getLong("max_latency_ms"));
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            log.warn("audit stats failed: {}", e.getMessage());
        }
        return result;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void scheduledCleanup() {
        cleanup(properties.getAuditRetentionDays());
    }

    public int cleanup(int retentionDays) {
        if (retentionDays <= 0) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - retentionDays * 24L * 3600L * 1000L;
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM da_api_invoke_log WHERE create_time < ?")) {
            ps.setTimestamp(1, new Timestamp(cutoff));
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                log.info("audit cleanup removed {} rows older than {} days", deleted, retentionDays);
            }
            return deleted;
        } catch (SQLException e) {
            log.warn("audit cleanup failed: {}", e.getMessage());
            return 0;
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (writer != null) {
            try {
                writer.join(3000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class InvokeLogEntry {

        private final String apiPath;
        private final String userId;
        private final String appId;
        private final long latencyMs;
        private final int code;
        private final Integer rowCount;
        private final String traceId;

        private InvokeLogEntry(String apiPath, String userId, String appId, long latencyMs, int code,
                               Integer rowCount, String traceId) {
            this.apiPath = apiPath;
            this.userId = userId;
            this.appId = appId;
            this.latencyMs = latencyMs;
            this.code = code;
            this.rowCount = rowCount;
            this.traceId = traceId;
        }
    }
}
