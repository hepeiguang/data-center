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

package org.apache.dolphinscheduler.dataapi.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "data-api")
public class DataApiProperties {

    /**
     * Max rows returned by a single query API call.
     */
    private int maxRows = 1000;

    /**
     * Reload interval for DS datasource center → MagicDynamicDataSource (ms).
     */
    private long datasourceRefreshMs = 60000L;

    /**
     * DbType names to load (uppercase). Doris is required for phase-1.
     */
    private Set<String> loadDbTypes = new HashSet<>(Arrays.asList("DORIS", "MYSQL"));

    /**
     * Hikari pool size per business datasource.
     */
    private int poolMaximumPoolSize = 5;

    private int poolMinimumIdle = 1;

    /**
     * Statement timeout seconds for business queries.
     */
    private int queryTimeoutSeconds = 60;

    /**
     * When true, reject non-SELECT SQL in RequestInterceptor.
     */
    private boolean rejectDml = true;

    /**
     * API ACL mode:
     * <ul>
     *   <li>{@code off} — no path-level ACL (auth only)</li>
     *   <li>{@code optional} — if a path has ALLOW rules, only those subjects; else any authenticated</li>
     *   <li>{@code enforce} — non-admin must have explicit ALLOW for the path</li>
     * </ul>
     */
    private String aclMode = "optional";

    /**
     * Cache TTL for {@code da_api_acl} rules evaluated on each gateway call (ms). 0 disables caching.
     */
    private long aclCacheMs = 10000L;

    /**
     * Write invoke audit rows asynchronously through a bounded queue.
     */
    private boolean invokeLogEnabled = true;

    /**
     * Queue capacity for async invoke audit; rows are dropped when full instead of blocking the request.
     */
    private int invokeLogQueueSize = 5000;

    /**
     * Mark business datasource connections read-only (advisory for MySQL protocol drivers).
     */
    private boolean readOnlyConnections = true;

    /**
     * Per-DbType session init SQL, executed by Hikari on every new physical connection.
     * Keys are {@code DbType} names (MYSQL / DORIS / ...); {@code {timeoutSeconds}} is replaced with
     * {@link #queryTimeoutSeconds}. Set an empty value to disable the default for that type.
     */
    private Map<String, String> sessionInitSql = new LinkedHashMap<>();

    /**
     * Browser origins allowed to call {@code /data-api/**}. Empty means no cross-origin access;
     * the console is expected to be served same-origin or through nginx.
     */
    private List<String> corsAllowedOrigins = new ArrayList<>();

    /**
     * Accept {@code X-App-Id} / {@code X-App-Secret} in addition to DS tokens.
     */
    private boolean appAuthEnabled = true;

    /**
     * Requests per minute per subject (user id / app id). 0 disables the limiter.
     */
    private int rateLimitPermitsPerMinute = 600;

    /**
     * Burst capacity of the per-subject token bucket.
     */
    private int rateLimitBurst = 120;

    /**
     * Reject responses larger than this many bytes (0 disables the cap).
     */
    private long responseMaxBytes = 2L * 1024 * 1024;

    /**
     * Field names masked recursively in map/list responses (case-insensitive).
     */
    private Set<String> maskFields = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd", "secret", "appsecret", "token", "phone", "idcard", "id_card"));

    /**
     * Periodically run {@code SELECT 1} against every loaded business datasource.
     */
    private boolean datasourceHealthEnabled = true;

    private long datasourceHealthIntervalMs = 60000L;

    /**
     * Optional HTTP endpoint receiving JSON when a datasource flips UP→DOWN.
     */
    private String datasourceHealthWebhook;

    /**
     * Delete invoke-audit rows older than this many days (0 keeps forever).
     */
    private int auditRetentionDays = 30;

    /**
     * Lifetime of magic console tokens issued by username/password login.
     */
    private long consoleTokenTtlMinutes = 480L;

    /**
     * Upper bound of in-memory console tokens.
     */
    private int consoleTokenMax = 1000;

    /**
     * When false, {@code /data-api/openapi.json} requires a DS token.
     */
    private boolean openApiPublic = false;

    /**
     * Optional absolute base URL written into the OpenAPI {@code servers} block.
     */
    private String openApiServerUrl = "";

    /**
     * Extract table references from published API scripts.
     */
    private boolean lineageEnabled = true;

    /**
     * One-shot: import {@code ./data/magic-api} into {@code magic_api_file} when cluster storage is empty.
     */
    private boolean magicResourceMigrate = false;

    /**
     * Cluster mode: re-read magic resources from the shared store on this interval (ms).
     * 0 disables (single instance).
     */
    private long magicResourceRefreshMs = 0L;
}
