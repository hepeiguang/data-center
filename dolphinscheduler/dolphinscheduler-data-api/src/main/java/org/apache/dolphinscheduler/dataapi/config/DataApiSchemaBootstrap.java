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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * Creates the data-api extension tables when they are missing.
 * MySQL / PostgreSQL / H2 are generated from the same column model.
 */
@Slf4j
@Component
public class DataApiSchemaBootstrap {

    private final DataSource metaDataSource;

    public DataApiSchemaBootstrap(DataSource metaDataSource) {
        this.metaDataSource = metaDataSource;
    }

    @PostConstruct
    public void init() {
        try (Connection conn = metaDataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            String db = product == null ? "" : product.toLowerCase(Locale.ROOT);
            boolean postgres = db.contains("postgres");
            String id = postgres ? "BIGSERIAL PRIMARY KEY" : "BIGINT AUTO_INCREMENT PRIMARY KEY";
            String text = postgres || db.contains("h2") ? "TEXT" : "LONGTEXT";
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS magic_api_file ("
                        + "file_path VARCHAR(255) PRIMARY KEY,"
                        + "file_content " + text + ")");
                st.execute("CREATE TABLE IF NOT EXISTS da_api_release ("
                        + "id " + id + ","
                        + "api_path VARCHAR(255) NOT NULL,"
                        + "version INT NOT NULL,"
                        + "method VARCHAR(16),"
                        + "script " + text + ","
                        + "entity_json " + text + ","
                        + "state VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',"
                        + "remark VARCHAR(512),"
                        + "create_by INT,"
                        + "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                st.execute("CREATE TABLE IF NOT EXISTS da_api_table_ref ("
                        + "id " + id + ","
                        + "api_path VARCHAR(255) NOT NULL,"
                        + "datasource_key VARCHAR(128),"
                        + "table_name VARCHAR(512) NOT NULL,"
                        + "update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                st.execute("CREATE TABLE IF NOT EXISTS da_app_credential ("
                        + "id " + id + ","
                        + "app_id VARCHAR(64) NOT NULL,"
                        + "app_key_hash VARCHAR(128) NOT NULL,"
                        + "user_id INT NOT NULL,"
                        + "scopes VARCHAR(512),"
                        + "expire_time TIMESTAMP,"
                        + "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                st.execute("CREATE TABLE IF NOT EXISTS da_api_invoke_log ("
                        + "id " + id + ","
                        + "api_path VARCHAR(255) NOT NULL,"
                        + "user_id VARCHAR(64),"
                        + "app_id VARCHAR(64),"
                        + "latency_ms BIGINT,"
                        + "row_count INT,"
                        + "code INT,"
                        + "trace_id VARCHAR(64),"
                        + "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                createIndexes(st, postgres, db);
            }
        } catch (SQLException e) {
            log.warn("data-api schema bootstrap skipped: {}", e.getMessage());
        }
    }

    private void createIndexes(Statement st, boolean postgres, String db) {
        if (!db.contains("mysql")) {
            safe(st, "CREATE UNIQUE INDEX IF NOT EXISTS uk_da_release ON da_api_release (api_path, version)");
            safe(st, "CREATE INDEX IF NOT EXISTS idx_da_release_path ON da_api_release (api_path, create_time)");
            safe(st, "CREATE UNIQUE INDEX IF NOT EXISTS uk_da_table_ref"
                    + " ON da_api_table_ref (api_path, datasource_key, table_name)");
            safe(st, "CREATE UNIQUE INDEX IF NOT EXISTS uk_da_app_id ON da_app_credential (app_id)");
            safe(st, "CREATE INDEX IF NOT EXISTS idx_da_invoke_api_time"
                    + " ON da_api_invoke_log (api_path, create_time)");
        } else {
            safe(st, "CREATE UNIQUE INDEX uk_da_release ON da_api_release (api_path, version)");
            safe(st, "CREATE INDEX idx_da_release_path ON da_api_release (api_path, create_time)");
            safe(st, "CREATE UNIQUE INDEX uk_da_table_ref"
                    + " ON da_api_table_ref (api_path, datasource_key, table_name)");
            safe(st, "CREATE UNIQUE INDEX uk_da_app_id ON da_app_credential (app_id)");
            safe(st, "CREATE INDEX idx_da_invoke_api_time ON da_api_invoke_log (api_path, create_time)");
        }
    }

    private void safe(Statement st, String sql) {
        try {
            st.execute(sql);
        } catch (SQLException e) {
            log.debug("schema index skipped: {} ({})", sql, e.getMessage());
        }
    }
}
