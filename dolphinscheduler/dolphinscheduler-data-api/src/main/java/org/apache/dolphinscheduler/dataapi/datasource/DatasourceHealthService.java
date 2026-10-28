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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

/**
 * Periodic {@code SELECT 1} against every loaded business datasource, with a state-transition webhook.
 */
@Slf4j
@Service
public class DatasourceHealthService {

    private static final int QUERY_TIMEOUT_SECONDS = 3;

    private final DsMagicDataSourceLoader loader;
    private final MagicDynamicDataSource magicDynamicDataSource;
    private final DataApiProperties properties;
    private final Map<String, HealthView> statuses = new ConcurrentHashMap<>();

    public DatasourceHealthService(DsMagicDataSourceLoader loader,
                                   MagicDynamicDataSource magicDynamicDataSource,
                                   DataApiProperties properties) {
        this.loader = loader;
        this.magicDynamicDataSource = magicDynamicDataSource;
        this.properties = properties;
    }

    @Scheduled(initialDelay = 15000L,
            fixedDelayString = "${data-api.datasource-health-interval-ms:60000}")
    public void checkAll() {
        if (!properties.isDatasourceHealthEnabled()) {
            return;
        }
        for (String key : loader.loadedKeys()) {
            check(key);
        }
    }

    public void check(String key) {
        long start = System.currentTimeMillis();
        boolean ok;
        String error = null;
        try {
            MagicDynamicDataSource.DataSourceNode node = magicDynamicDataSource.getDataSource(key);
            DataSource dataSource = node == null ? null : node.getDataSource();
            if (dataSource == null) {
                throw new IllegalStateException("datasource not loaded");
            }
            try (Connection conn = dataSource.getConnection();
                    Statement st = conn.createStatement()) {
                st.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                st.execute("SELECT 1");
            }
            ok = true;
        } catch (Exception e) {
            ok = false;
            error = e.getMessage();
        }
        long latency = System.currentTimeMillis() - start;
        HealthView previous = statuses.put(key, new HealthView(key, ok, latency, error, System.currentTimeMillis()));
        if (!ok && (previous == null || previous.ok)) {
            log.error("datasource {} is DOWN: {}", key, error);
            notifyWebhook(key, error);
        } else if (ok && previous != null && !previous.ok) {
            log.info("datasource {} recovered, latency={}ms", key, latency);
        }
    }

    public List<Map<String, Object>> views() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (HealthView view : statuses.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", view.key);
            row.put("ok", view.ok);
            row.put("latencyMs", view.latencyMs);
            row.put("error", view.error);
            row.put("checkedAt", view.checkedAt);
            items.add(row);
        }
        return items;
    }

    private void notifyWebhook(String key, String error) {
        String webhook = properties.getDatasourceHealthWebhook();
        if (webhook == null || webhook.trim().isEmpty()) {
            return;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(webhook).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            String payload = "{\"event\":\"datasource_down\",\"key\":\"" + key + "\",\"error\":\""
                    + (error == null ? "" : error.replace("\"", "'")) + "\",\"time\":"
                    + System.currentTimeMillis() + "}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e) {
            log.warn("datasource health webhook failed: {}", e.getMessage());
        }
    }

    private static final class HealthView {

        private final String key;
        private final boolean ok;
        private final long latencyMs;
        private final String error;
        private final long checkedAt;

        private HealthView(String key, boolean ok, long latencyMs, String error, long checkedAt) {
            this.key = key;
            this.ok = ok;
            this.latencyMs = latencyMs;
            this.error = error;
            this.checkedAt = checkedAt;
        }
    }
}
