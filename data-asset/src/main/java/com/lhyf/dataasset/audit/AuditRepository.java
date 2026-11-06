package com.lhyf.dataasset.audit;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class AuditRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insert(String user, String action, String target, Object detail, int resultCode,
                       long durationMs, String clientIp) {
        String json;
        try {
            json = objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            json = "{}";
        }
        jdbc.update("insert into audit_event (event_time, username, action, target, detail_json, result_code, "
                        + "duration_ms, client_ip) values (now(), :user, :action, :target, :detail, :code, "
                        + ":duration, :ip)",
                new MapSqlParameterSource()
                        .addValue("user", user)
                        .addValue("action", action)
                        .addValue("target", target)
                        .addValue("detail", json)
                        .addValue("code", resultCode)
                        .addValue("duration", durationMs)
                        .addValue("ip", clientIp));
    }

    public List<Map<String, Object>> recent(int limit) {
        return jdbc.query("select event_time, username, action, target, result_code, duration_ms, client_ip "
                        + "from audit_event order by event_time desc limit :limit",
                new MapSqlParameterSource("limit", limit), new ColumnMapRowMapper());
    }
}
