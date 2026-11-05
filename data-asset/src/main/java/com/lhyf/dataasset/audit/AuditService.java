package com.lhyf.dataasset.audit;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void record(String user, String action, String target, Object detail, int resultCode,
                       long durationMs, String clientIp) {
        String json;
        try {
            json = objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            json = "{}";
        }
        jdbc.update("insert into audit_event (event_time, username, action, target, detail_json, result_code, "
                        + "duration_ms, client_ip) values (now(),?,?,?,?,?,?,?)",
                user, action, target, json, resultCode, durationMs, clientIp);
    }

    public List<Map<String, Object>> list(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return jdbc.queryForList("select event_time, username, action, target, result_code, duration_ms, "
                + "client_ip from audit_event order by event_time desc limit ?", safeLimit);
    }
}
