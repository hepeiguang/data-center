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

package org.apache.dolphinscheduler.plugin.task.openmetadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SqlLineageParserTest {

    private static final String SOURCE = "agri_satellite_pg.agri_sat.public.ods_satellite_scene";
    private static final String TARGET = "agri_satellite_pg.agri_sat.public.dwd_ndvi_tile";

    @Test
    void parsesInsertSelectColumns() {
        String sql = "insert into dwd_ndvi_tile (tile_id, ndvi_mean) "
                + "select scene_id, ndvi as ndvi_mean from ods_satellite_scene";
        SqlLineageParser.ParsedLineage parsed = SqlLineageParser.parse(sql, SOURCE, TARGET);
        Assertions.assertEquals(2, parsed.getColumnLineage().size());
        Assertions.assertEquals(SOURCE + ".scene_id", parsed.getColumnLineage().get(0).getFromColumns().get(0));
        Assertions.assertEquals(TARGET + ".tile_id", parsed.getColumnLineage().get(0).getToColumn());
        Assertions.assertEquals(TARGET + ".ndvi_mean", parsed.getColumnLineage().get(1).getToColumn());
    }

    @Test
    void extractsAndParsesSeatunnelSqlTransform() {
        String config = "transform {\n"
                + "  SQL {\n"
                + "    query = \"\"\"\n"
                + "      select scene_id as tile_id, ndvi_mean as ndvi_mean\n"
                + "      from ods_satellite_scene\n"
                + "    \"\"\"\n"
                + "  }\n"
                + "}";
        String sql = SeatunnelSqlExtractor.extract(config);
        Assertions.assertNotNull(sql);
        SqlLineageParser.ParsedLineage parsed = SqlLineageParser.parse(sql, SOURCE, TARGET);
        Assertions.assertEquals(2, parsed.getColumnLineage().size());
        Assertions.assertEquals(TARGET + ".tile_id", parsed.getColumnLineage().get(0).getToColumn());
    }

    @Test
    void wildcardYieldsNoColumnLineage() {
        SqlLineageParser.ParsedLineage parsed = SqlLineageParser.parse(
                "insert into dwd_ndvi_tile select * from ods_satellite_scene", SOURCE, TARGET);
        Assertions.assertTrue(parsed.getColumnLineage().isEmpty());
        Assertions.assertTrue(parsed.getSourceTables().contains("ods_satellite_scene"));
    }
}
