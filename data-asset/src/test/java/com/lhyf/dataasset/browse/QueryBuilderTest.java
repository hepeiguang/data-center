package com.lhyf.dataasset.browse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class QueryBuilderTest {

    @Test
    void buildsParameterizedSelectWithWhitelistedColumns() {
        QueryBuilder.SqlQuery query = QueryBuilder.build(PostgresSqlDialect.INSTANCE, "public", "dwd_ndvi_tile",
                List.of("tile_id", "ndvi_mean"), List.of(new QueryBuilder.Filter("tile_id", "=", "T1")),
                "tile_id", true, 200, 0, null, Map.of());
        assertEquals("select \"tile_id\", \"ndvi_mean\" from \"public\".\"dwd_ndvi_tile\" "
                + "where \"tile_id\" = ? order by \"tile_id\" asc limit ? offset ?", query.sql);
        assertEquals(List.of("T1", 200, 0), query.params);
    }

    @Test
    void expandsRowPolicyIntoParameters() {
        QueryBuilder.SqlQuery query = QueryBuilder.build(PostgresSqlDialect.INSTANCE, "public", "t", List.of("id"),
                List.of(), null, true, 10, 0, "region_id = ${user.regionId}", Map.of("regionId", "R1"));
        assertTrue(query.sql.contains("where region_id = ?"));
        assertEquals("R1", query.params.get(0));
    }

    @Test
    void rejectsUnknownFilterColumn() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.build(PostgresSqlDialect.INSTANCE,
                "public", "t",
                List.of("id"), List.of(new QueryBuilder.Filter("secret", "=", "x")),
                null, true, 10, 0, null, Map.of()));
    }

    @Test
    void rejectsInvalidIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> PostgresSqlDialect.INSTANCE.quote("t; drop table x"));
        assertThrows(IllegalArgumentException.class, () -> H2SqlDialect.INSTANCE.quote("t; drop table x"));
    }

    @Test
    void rejectsMissingPolicyAttribute() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.build(PostgresSqlDialect.INSTANCE,
                "public", "t", List.of("id"),
                List.of(), null, true, 10, 0, "region_id = ${user.regionId}", Map.of()));
    }

    @Test
    void resolvesDialectFromJdbcUrl() {
        assertInstanceOf(H2SqlDialect.class, SqlDialect.forJdbcUrl("jdbc:h2:file:./data/db"));
        assertInstanceOf(PostgresSqlDialect.class, SqlDialect.forJdbcUrl("jdbc:postgresql://host/db"));
        assertInstanceOf(PostgresSqlDialect.class, SqlDialect.forJdbcUrl(null));
    }

    @Test
    void h2DialectBuildsSameShapeSelectAsPostgres() {
        QueryBuilder.SqlQuery pg = QueryBuilder.build(PostgresSqlDialect.INSTANCE, "public", "t", List.of("id"),
                List.of(), "id", false, 10, 5, null, Map.of());
        QueryBuilder.SqlQuery h2 = QueryBuilder.build(H2SqlDialect.INSTANCE, "public", "t", List.of("id"),
                List.of(), "id", false, 10, 5, null, Map.of());
        assertEquals(pg.sql, h2.sql);
        assertEquals("select \"id\" from \"public\".\"t\" order by \"id\" desc limit ? offset ?", h2.sql);
        assertEquals(List.of(10, 5), h2.params);
    }
}
