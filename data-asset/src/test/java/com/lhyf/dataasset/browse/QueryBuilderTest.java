package com.lhyf.dataasset.browse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class QueryBuilderTest {

    @Test
    void buildsParameterizedSelectWithWhitelistedColumns() {
        QueryBuilder.SqlQuery query = QueryBuilder.build("public", "dwd_ndvi_tile",
                List.of("tile_id", "ndvi_mean"), List.of(new QueryBuilder.Filter("tile_id", "=", "T1")),
                "tile_id", true, 200, 0, null, Map.of());
        assertEquals("select \"tile_id\", \"ndvi_mean\" from \"public\".\"dwd_ndvi_tile\" "
                + "where \"tile_id\" = ? order by \"tile_id\" asc limit ? offset ?", query.sql);
        assertEquals(List.of("T1", 200, 0), query.params);
    }

    @Test
    void expandsRowPolicyIntoParameters() {
        QueryBuilder.SqlQuery query = QueryBuilder.build("public", "t", List.of("id"),
                List.of(), null, true, 10, 0, "region_id = ${user.regionId}", Map.of("regionId", "R1"));
        assertTrue(query.sql.contains("where region_id = ?"));
        assertEquals("R1", query.params.get(0));
    }

    @Test
    void rejectsUnknownFilterColumn() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.build("public", "t",
                List.of("id"), List.of(new QueryBuilder.Filter("secret", "=", "x")),
                null, true, 10, 0, null, Map.of()));
    }

    @Test
    void rejectsInvalidIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.quote("t; drop table x"));
    }

    @Test
    void rejectsMissingPolicyAttribute() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.build("public", "t", List.of("id"),
                List.of(), null, true, 10, 0, "region_id = ${user.regionId}", Map.of()));
    }
}
