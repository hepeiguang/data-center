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

import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Read-only replacement for {@code org.apache.dolphinscheduler.dao.mapper.DataSourceMapper}.
 * The authorization query mirrors {@code DataSourceMapper.xml#listAuthorizedDataSource}:
 * visible = granted via t_ds_relation_datasource_user OR owned by the user; userId 0 (admin)
 * sees everything.
 */
@Repository
public class DataSourceRepository {

    private static final String BASE_COLUMNS =
            "id, name, note, type, user_id, connection_params, create_time, update_time";

    private static final RowMapper<DsDataSource> MAPPER = (rs, rowNum) -> {
        DsDataSource ds = new DsDataSource();
        ds.setId(rs.getInt("id"));
        ds.setName(rs.getString("name"));
        ds.setNote(rs.getString("note"));
        ds.setType(DbType.of(rs.getInt("type")));
        ds.setUserId(rs.getInt("user_id"));
        ds.setConnectionParams(rs.getString("connection_params"));
        ds.setCreateTime(rs.getTimestamp("create_time"));
        ds.setUpdateTime(rs.getTimestamp("update_time"));
        return ds;
    };

    private final JdbcTemplate jdbcTemplate;

    public DataSourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DsDataSource> selectAll() {
        return jdbcTemplate.query("select " + BASE_COLUMNS + " from t_ds_datasource", MAPPER);
    }

    /** Datasources visible to {@code userId}; pass {@code admin=true} for the full list. */
    public List<DsDataSource> listAuthorized(int userId, boolean admin) {
        if (admin) {
            return selectAll();
        }
        return jdbcTemplate.query("select " + BASE_COLUMNS + " from t_ds_datasource where id in ("
                + "select datasource_id from t_ds_relation_datasource_user where user_id = ?"
                + " union select id from t_ds_datasource where user_id = ?)", MAPPER, userId, userId);
    }

    public boolean canAccess(int userId, boolean admin, int datasourceId) {
        if (admin) {
            return true;
        }
        Integer hit = jdbcTemplate.queryForObject("select count(1) from t_ds_datasource where id = ? and ("
                + "id in (select datasource_id from t_ds_relation_datasource_user where user_id = ?)"
                + " or user_id = ?)", Integer.class, datasourceId, userId, userId);
        return hit != null && hit > 0;
    }

    public List<DsDataSource> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return jdbcTemplate.query("select " + BASE_COLUMNS + " from t_ds_datasource where id in (" + placeholders + ")",
                MAPPER, ids.toArray());
    }
}
