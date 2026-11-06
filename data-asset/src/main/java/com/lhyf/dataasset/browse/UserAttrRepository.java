package com.lhyf.dataasset.browse;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserAttrRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserAttrRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** attr_key -> attr_value rows stored for one user (empty map if none). */
    public Map<String, String> findAttributes(String username) {
        Map<String, String> attrs = new LinkedHashMap<>();
        RowCallbackHandler collect = rs -> attrs.put(rs.getString(1), rs.getString(2));
        jdbc.query("select attr_key, attr_value from user_attr where username = :username",
                new MapSqlParameterSource("username", username), collect);
        return attrs;
    }
}
