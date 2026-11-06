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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only replacement for {@code org.apache.dolphinscheduler.dao.repository.UserRepository}.
 * SQL mirrors {@code UserMapper.xml} (queryUserByNamePassword / queryUserByToken / selectById);
 * data-api never writes t_ds_* tables.
 */
@Slf4j
@Repository
public class UserRepository {

    private static final String BASE_COLUMNS = "id, user_name, user_type, state";

    private static final String QUERY_BY_ID =
            "select " + BASE_COLUMNS + " from t_ds_user where id = ?";
    private static final String QUERY_BY_NAME_PASSWORD =
            "select " + BASE_COLUMNS + " from t_ds_user where user_name = ? and user_password = ?";
    private static final String QUERY_BY_TOKEN =
            "select u.id, u.user_name, u.user_type, u.state from t_ds_user u, t_ds_access_token t"
                    + " where u.id = t.user_id and t.token = ? and t.expire_time > ?";

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User queryById(int id) {
        List<User> users = jdbcTemplate.query(QUERY_BY_ID, UserRepository::mapUser, id);
        return users.isEmpty() ? null : users.get(0);
    }

    /** {@code passwordMd5} must be produced by {@link DsDigests#md5(String)} (DS-compatible). */
    public User queryUserByNamePassword(String userName, String passwordMd5) {
        List<User> users =
                jdbcTemplate.query(QUERY_BY_NAME_PASSWORD, UserRepository::mapUser, userName, passwordMd5);
        return users.isEmpty() ? null : users.get(0);
    }

    public User queryUserByToken(String token, Date now) {
        List<User> users =
                jdbcTemplate.query(QUERY_BY_TOKEN, UserRepository::mapUser, token, new Timestamp(now.getTime()));
        return users.isEmpty() ? null : users.get(0);
    }

    private static User mapUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUserName(rs.getString("user_name"));
        user.setUserType(UserType.of(rs.getInt("user_type")));
        user.setState(rs.getInt("state"));
        return user;
    }
}
