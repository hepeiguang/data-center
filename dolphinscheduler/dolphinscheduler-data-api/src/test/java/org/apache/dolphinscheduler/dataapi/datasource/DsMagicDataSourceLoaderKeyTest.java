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

import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DsMagicDataSourceLoaderKeyTest {

    @Test
    void prefersSanitizedName() {
        DataSource a = ds(1, "osint_mysql", DbType.MYSQL);
        Map<Integer, String> keys = DsMagicDataSourceLoader.assignKeys(Arrays.asList(a));
        Assertions.assertEquals("osint_mysql", keys.get(1));
    }

    @Test
    void sanitizesSpecialChars() {
        DataSource a = ds(2, "Osint-MySQL 生产", DbType.MYSQL);
        Map<Integer, String> keys = DsMagicDataSourceLoader.assignKeys(Arrays.asList(a));
        Assertions.assertEquals("Osint_MySQL", keys.get(2));
    }

    @Test
    void collisionAppendsId() {
        DataSource a = ds(1, "demo", DbType.MYSQL);
        DataSource b = ds(2, "demo", DbType.DORIS);
        Map<Integer, String> keys = DsMagicDataSourceLoader.assignKeys(Arrays.asList(a, b));
        Assertions.assertEquals("demo", keys.get(1));
        Assertions.assertEquals("demo_2", keys.get(2));
    }

    @Test
    void chineseNameFallsBackToTypeId() {
        DataSource a = ds(9, "测试库", DbType.MYSQL);
        Map<Integer, String> keys = DsMagicDataSourceLoader.assignKeys(Arrays.asList(a));
        Assertions.assertEquals("mysql_9", keys.get(9));
    }

    @Test
    void displayNameIncludesTypeIdDatabase() {
        DataSource a = ds(3, "osint_mysql", DbType.MYSQL);
        String display = DsMagicDataSourceLoader.buildDisplayName(a, "osint");
        Assertions.assertEquals("osint_mysql [MYSQL#3 / osint]", display);
    }

    private static DataSource ds(int id, String name, DbType type) {
        DataSource d = new DataSource();
        d.setId(id);
        d.setName(name);
        d.setType(type);
        return d;
    }
}
