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

package org.apache.dolphinscheduler.dataapi.meta;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only replacement for {@code org.apache.dolphinscheduler.dao.repository.SessionRepository}.
 */
@Repository
public class SessionRepository {

    private static final String QUERY_BY_ID = "select id, user_id, ip, last_login_time from t_ds_session where id = ?";

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Session queryById(String id) {
        List<Session> sessions = jdbcTemplate.query(QUERY_BY_ID, (rs, rowNum) -> {
            Session session = new Session();
            session.setId(rs.getString("id"));
            session.setUserId(rs.getInt("user_id"));
            session.setIp(rs.getString("ip"));
            session.setLastLoginTime(rs.getTimestamp("last_login_time"));
            return session;
        }, id);
        return sessions.isEmpty() ? null : sessions.get(0);
    }
}
