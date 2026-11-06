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
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ConsoleAuditServiceTest {

    @Test
    void excerptKeepsShortSqlIntactAndNullAsNull() {
        Assertions.assertNull(ConsoleAuditService.excerpt(null));
        Assertions.assertEquals("select 1", ConsoleAuditService.excerpt("select 1"));
    }

    @Test
    void excerptCutsAtUtf8CharacterBoundary() {
        // 3-byte Chinese characters: 4096 bytes cannot hold a whole 1366th char
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append('测');
        }
        String excerpt = ConsoleAuditService.excerpt(sb.toString());
        int bytes = excerpt.getBytes(StandardCharsets.UTF_8).length;
        Assertions.assertTrue(bytes <= 4 * 1024, "excerpt must fit 4KB, got " + bytes);
        Assertions.assertEquals(1365, excerpt.length());
        for (int i = 0; i < excerpt.length(); i++) {
            Assertions.assertEquals('测', excerpt.charAt(i));
        }
    }

    @Test
    void recordStoresExcerptAndShaThenHistoryReturnsNewestFirst() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:console_audit_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table da_console_query_log ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "user_id VARCHAR(64),"
                + "datasource_id INT,"
                + "sql_excerpt CLOB,"
                + "sql_sha256 CHAR(64),"
                + "row_count INT,"
                + "latency_ms BIGINT,"
                + "result_code INT,"
                + "reject_reason VARCHAR(512),"
                + "create_time TIMESTAMP)");
        ConsoleAuditService audit = new ConsoleAuditService(jdbc);

        String sql = "select name from t where secret = 'hunter2'";
        audit.record("42", 7, sql, 3, 12L, 200, null);
        audit.record("42", 7, "update t set a = 1", 0, 1L, 400, "forbidden fragment");
        audit.record("99", 7, "select 1", 1, 2L, 200, null);

        List<Map<String, Object>> mine = audit.history("42", 10);
        Assertions.assertEquals(2, mine.size());
        Map<String, Object> newest = mine.get(0);
        Assertions.assertEquals(DigestUtils.sha256Hex("update t set a = 1"), newest.get("sqlSha256"));
        Assertions.assertEquals(400, newest.get("resultCode"));
        Assertions.assertEquals("forbidden fragment", newest.get("rejectReason"));
        Assertions.assertEquals(7, mine.get(1).get("datasourceId"));
        // other users never appear in this user's history
        Assertions.assertEquals(1, audit.history("99", 10).size());
    }
}
