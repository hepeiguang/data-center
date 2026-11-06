package com.lhyf.dataasset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:asset-flyway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.clean-disabled=false"
})
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v1CreatesAllConfigTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);
        assertTrue(tables.containsAll(List.of("browse_entry", "approval_event", "audit_event",
                "user_attr", "key_mapping", "row_switch")), "missing tables: " + tables);
    }

    @Test
    void auditActionColumnUsesWidenedV32() {
        Integer width = jdbcTemplate.queryForObject(
                "select character_maximum_length from information_schema.columns"
                        + " where table_name = 'audit_event' and column_name = 'action'",
                Integer.class);
        assertEquals(32, width, "V1 must ship the widened column that schema.sql patched via ALTER");
    }

    @Test
    void flywayHistoryRecordsV1() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = true",
                Integer.class);
        assertEquals(1, count);
    }
}
