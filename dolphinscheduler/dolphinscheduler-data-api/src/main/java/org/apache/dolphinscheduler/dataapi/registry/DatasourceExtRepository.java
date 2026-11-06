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

package org.apache.dolphinscheduler.dataapi.registry;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Governance extension for DS-registered datasources (DATA-API-BOOT3-CONSOLE-PLAN §5.3 / D7):
 * keyed by {@code t_ds_datasource.id}, never modifies DS tables. The console authenticates with
 * the read-only account stored here instead of the DS management account.
 */
@Repository
public class DatasourceExtRepository {

    private static final String COLUMNS =
            "datasource_id, console_enabled, readonly_user, readonly_password_enc, max_rows, masked_columns, owner, update_time";

    private static final String UPSERT_TEMPLATE =
            "MERGE INTO da_datasource_ext (datasource_id, console_enabled, readonly_user, readonly_password_enc,"
                    + " max_rows, masked_columns, owner, update_time) KEY(datasource_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String PG_UPSERT_TEMPLATE =
            "INSERT INTO da_datasource_ext (datasource_id, console_enabled, readonly_user, readonly_password_enc,"
                    + " max_rows, masked_columns, owner, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT (datasource_id) DO UPDATE SET console_enabled = EXCLUDED.console_enabled,"
                    + " readonly_user = EXCLUDED.readonly_user,"
                    + " readonly_password_enc = EXCLUDED.readonly_password_enc,"
                    + " max_rows = EXCLUDED.max_rows, masked_columns = EXCLUDED.masked_columns,"
                    + " owner = EXCLUDED.owner, update_time = EXCLUDED.update_time";

    private final JdbcTemplate jdbcTemplate;

    public DatasourceExtRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Getter
    @Setter
    public static class DatasourceExt {

        private int datasourceId;
        private boolean consoleEnabled;
        private String readonlyUser;
        /** Encrypted with the environment key; never a plaintext password (plan §5.3 rule 3). */
        private String readonlyPasswordEnc;
        private int maxRows;
        private String maskedColumns;
        private String owner;
        private Timestamp updateTime;
    }

    public Optional<DatasourceExt> find(int datasourceId) {
        List<DatasourceExt> rows = jdbcTemplate.query(
                "select " + COLUMNS + " from da_datasource_ext where datasource_id = ?",
                (rs, rowNum) -> map(rs), datasourceId);
        return rows.stream().findFirst();
    }

    private DatasourceExt map(java.sql.ResultSet rs) throws java.sql.SQLException {
        DatasourceExt ext = new DatasourceExt();
        ext.setDatasourceId(rs.getInt("datasource_id"));
        ext.setConsoleEnabled(rs.getInt("console_enabled") == 1);
        ext.setReadonlyUser(rs.getString("readonly_user"));
        ext.setReadonlyPasswordEnc(rs.getString("readonly_password_enc"));
        ext.setMaxRows(rs.getInt("max_rows"));
        ext.setMaskedColumns(rs.getString("masked_columns"));
        ext.setOwner(rs.getString("owner"));
        ext.setUpdateTime(rs.getTimestamp("update_time"));
        return ext;
    }

    public void upsert(DatasourceExt ext) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Object[] args = new Object[] {
                ext.getDatasourceId(),
                ext.isConsoleEnabled() ? 1 : 0,
                ext.getReadonlyUser(),
                ext.getReadonlyPasswordEnc(),
                ext.getMaxRows(),
                ext.getMaskedColumns(),
                ext.getOwner(),
                now
        };
        String product = jdbcTemplate.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName());
        boolean postgres = product != null && product.toLowerCase().contains("postgres");
        jdbcTemplate.update(postgres ? PG_UPSERT_TEMPLATE : UPSERT_TEMPLATE, args);
    }
}
