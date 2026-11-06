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

package org.apache.dolphinscheduler.dataapi.meta;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConnectionParamsTest {

    @Test
    void parsesDolphinschedulerPostgresParams() {
        String json = "{\"type\":\"POSTGRESQL\",\"host\":\"123.57.2.64\",\"port\":5432,"
                + "\"principal\":\"osint\",\"password\":\"enc\","
                + "\"address\":\"jdbc:postgresql://123.57.2.64:5432\","
                + "\"database\":\"osint\",\"jdbcUrl\":\"jdbc:postgresql://123.57.2.64:5432/osint\","
                + "\"other\":{\"currentSchema\":\"public\"}}";
        ConnectionParams params = ConnectionParams.parse(json);
        Assertions.assertNotNull(params);
        Assertions.assertEquals("enc", params.getPassword());
        Assertions.assertEquals("jdbc:postgresql://123.57.2.64:5432/osint?currentSchema=public",
                params.effectiveJdbcUrl());
    }

    @Test
    void appendsWithAmpersandWhenJdbcUrlAlreadyHasQuery() {
        String json = "{\"jdbcUrl\":\"jdbc:mysql://h:3306/db?useSSL=false\",\"other\":"
                + "{\"serverTimezone\":\"UTC\"}}";
        ConnectionParams params = ConnectionParams.parse(json);
        Assertions.assertEquals("jdbc:mysql://h:3306/db?useSSL=false&serverTimezone=UTC",
                params.effectiveJdbcUrl());
    }

    @Test
    void fallsBackToAddressPlusDatabaseWithoutJdbcUrl() {
        String json = "{\"address\":\"jdbc:postgresql://h:5432\",\"database\":\"dw\"}";
        Assertions.assertEquals("jdbc:postgresql://h:5432/dw", ConnectionParams.parse(json).effectiveJdbcUrl());
    }

    @Test
    void blankOrBrokenJsonYieldsNull() {
        Assertions.assertNull(ConnectionParams.parse(null));
        Assertions.assertNull(ConnectionParams.parse("  "));
        Assertions.assertNull(ConnectionParams.parse("{not json"));
    }

    @Test
    void unknownFieldsAreIgnored() {
        String json = "{\"jdbcUrl\":\"jdbc:h2:mem:x\",\"driverClassName\":\"org.h2.Driver\","
                + "\"host\":\"h\",\"port\":1,\"compatibleOtherConfigs\":{\"a\":1}}";
        ConnectionParams params = ConnectionParams.parse(json);
        Assertions.assertNotNull(params);
        Assertions.assertEquals("jdbc:h2:mem:x", params.effectiveJdbcUrl());
    }
}
