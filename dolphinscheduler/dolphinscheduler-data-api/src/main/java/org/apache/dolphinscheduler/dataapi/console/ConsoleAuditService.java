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

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Append-only audit for every console attempt — including rejections (plan §7.2 layer 6).
 * SQL is stored as a 4KB excerpt + SHA-256 so sensitive constants are not fully duplicated
 * into the log table (plan §5.3 rule 2). No update/delete API exists on purpose.
 */
@Slf4j
@Service
public class ConsoleAuditService {

    private static final int EXCERPT_BYTES = 4 * 1024;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConsoleAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String userId, Integer datasourceId, String sql, int rowCount, long latencyMs,
                       int resultCode, String rejectReason) {
        String excerpt = excerpt(sql);
        String sha = sql == null ? null : DigestUtils.sha256Hex(sql);
        try {
            jdbcTemplate.update("insert into da_console_query_log (user_id, datasource_id, sql_excerpt,"
                    + " sql_sha256, row_count, latency_ms, result_code, reject_reason, create_time)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    userId, datasourceId, excerpt, sha, rowCount, latencyMs, resultCode, rejectReason,
                    new Timestamp(System.currentTimeMillis()));
        } catch (Exception e) {
            // auditing must never mask the business response; log and continue
            log.error("console audit write failed user={} code={}", userId, resultCode, e);
        }
    }

    public List<Map<String, Object>> history(String userId, int limit) {
        return jdbcTemplate.query("select id, datasource_id, sql_excerpt, sql_sha256, row_count,"
                + " latency_ms, result_code, reject_reason, create_time"
                + " from da_console_query_log where user_id = ? order by id desc limit ?",
                (rs, n) -> {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("datasourceId", rs.getObject("datasource_id"));
                    row.put("sqlExcerpt", rs.getString("sql_excerpt"));
                    row.put("sqlSha256", rs.getString("sql_sha256"));
                    row.put("rowCount", rs.getObject("row_count"));
                    row.put("latencyMs", rs.getObject("latency_ms"));
                    row.put("resultCode", rs.getInt("result_code"));
                    row.put("rejectReason", rs.getString("reject_reason"));
                    row.put("createTime", rs.getTimestamp("create_time").toString());
                    return row;
                }, userId, Math.max(1, Math.min(limit, 500)));
    }

    /** UTF-8 safe first {@value EXCERPT_BYTES} bytes. */
    static String excerpt(String sql) {
        if (sql == null) {
            return null;
        }
        String normalized = sql.length() > 10 * EXCERPT_BYTES ? sql.substring(0, 10 * EXCERPT_BYTES) : sql;
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= EXCERPT_BYTES) {
            return normalized;
        }
        int end = EXCERPT_BYTES;
        // avoid cutting a multi-byte character: walk back off UTF-8 continuation bytes
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) {
            end--;
        }
        return StringUtils.stripEnd(new String(bytes, 0, end, StandardCharsets.UTF_8), null);
    }

    String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
