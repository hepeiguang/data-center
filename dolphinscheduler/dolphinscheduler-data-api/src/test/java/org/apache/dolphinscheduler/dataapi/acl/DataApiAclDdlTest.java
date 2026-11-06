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

package org.apache.dolphinscheduler.dataapi.acl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataApiAclDdlTest {

    @Test
    void mysqlDdlUsesAutoIncrementAndInlineUniqueKey() {
        String ddl = DataApiAclService.buildCreateTableDdl("MySQL");
        Assertions.assertTrue(ddl.contains("AUTO_INCREMENT"));
        Assertions.assertTrue(ddl.contains("UNIQUE KEY uk_da_acl"));
        Assertions.assertFalse(ddl.contains("BIGSERIAL"));
    }

    @Test
    void postgresDdlUsesBigSerialAndNoInlineUniqueKey() {
        String ddl = DataApiAclService.buildCreateTableDdl("PostgreSQL");
        Assertions.assertTrue(ddl.contains("BIGSERIAL PRIMARY KEY"));
        Assertions.assertTrue(ddl.contains("SMALLINT"));
        Assertions.assertFalse(ddl.contains("AUTO_INCREMENT"));
        Assertions.assertFalse(ddl.contains("UNIQUE KEY"));
    }

    @Test
    void h2DdlUsesAutoIncrement() {
        String ddl = DataApiAclService.buildCreateTableDdl("H2");
        Assertions.assertTrue(ddl.contains("AUTO_INCREMENT"));
        Assertions.assertFalse(ddl.contains("BIGSERIAL"));
    }
}
