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

package org.apache.dolphinscheduler.dataapi.datasource;

import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DsMagicDataSourceLoaderPoolTest {

    @Test
    void masksCredentialsInJdbcUrl() {
        String masked = DsMagicDataSourceLoader.maskJdbcUrl(
                "jdbc:mysql://h:3306/db?user=root&password=secret&useSSL=false");
        Assertions.assertFalse(masked.contains("secret"));
        Assertions.assertFalse(masked.contains("user=root"));
        Assertions.assertTrue(masked.contains("password=***"));
        Assertions.assertTrue(masked.contains("useSSL=false"));
    }

    @Test
    void buildsDialectSpecificSessionTimeoutSql() {
        DataApiProperties properties = new DataApiProperties();
        properties.setQueryTimeoutSeconds(30);
        DsMagicDataSourceLoader loader = new DsMagicDataSourceLoader(null, properties, null);

        Assertions.assertEquals("SET SESSION max_execution_time = 30000",
                loader.resolveSessionInitSql(DbType.MYSQL));
        Assertions.assertEquals("SET query_timeout = 30", loader.resolveSessionInitSql(DbType.DORIS));
        Assertions.assertEquals("SET statement_timeout = 30000",
                loader.resolveSessionInitSql(DbType.POSTGRESQL));
        Assertions.assertNull(loader.resolveSessionInitSql(DbType.H2));
    }

    @Test
    void sessionInitSqlOverrideAndDisable() {
        DataApiProperties properties = new DataApiProperties();
        properties.setQueryTimeoutSeconds(15);
        properties.getSessionInitSql().put("DORIS", "SET query_timeout = {timeoutSeconds}");
        properties.getSessionInitSql().put("MYSQL", "");
        DsMagicDataSourceLoader loader = new DsMagicDataSourceLoader(null, properties, null);

        Assertions.assertEquals("SET query_timeout = 15", loader.resolveSessionInitSql(DbType.DORIS));
        Assertions.assertNull(loader.resolveSessionInitSql(DbType.MYSQL));
    }
}
