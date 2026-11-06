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

package org.apache.dolphinscheduler.dataapi.lineage;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SqlTableExtractorTest {

    @Test
    void extractsFromAndJoin() {
        String script = "return db.osint_mysql.select(\"\"\"\n"
                + "select a.id from osint.dwd_orders a join dim_customer c on a.cid=c.id\n"
                + "\"\"\");";
        Set<String> tables = SqlTableExtractor.extractTables(script);
        Assertions.assertTrue(tables.contains("osint.dwd_orders"));
        Assertions.assertTrue(tables.contains("dim_customer"));
    }

    @Test
    void extractsDbTableDslAndSkipsSystemSchemas() {
        String script = "db.table('ads_result').select('select 1');\n"
                + "return db.select('select table_name from information_schema.tables');";
        Set<String> tables = SqlTableExtractor.extractTables(script);
        Assertions.assertTrue(tables.contains("ads_result"));
        Assertions.assertFalse(tables.stream().anyMatch(t -> t.startsWith("information_schema.")));
    }

    @Test
    void dynamicSqlYieldsNoFalseTable() {
        Assertions.assertTrue(SqlTableExtractor.extractTables("return db.select(sqlVar)").isEmpty());
        Assertions.assertTrue(SqlTableExtractor.extractTables("return db.select('select 1')").isEmpty());
    }
}
