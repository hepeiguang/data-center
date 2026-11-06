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

package org.apache.dolphinscheduler.dataapi.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.Data;

/**
 * Per-{@code DbType} capabilities consumed by the datasource registry, the query console and
 * metadata endpoints (docs/DATA-API-BOOT3-CONSOLE-PLAN §6.2). Loaded from
 * {@code datasource-types.yaml}; adding a type must not require Java changes.
 */
@Data
public class DatasourceTypeDescriptor {

    /** Upper-case {@code DbType} name, e.g. POSTGRESQL. */
    private String type;
    private String driver;
    private String validationQuery = "select 1";
    /** Identifier quote: " / ` / [] */
    private String quote = "\"";
    private String limitOffset = "LIMIT {limit} OFFSET {offset}";
    /** catalog | schema | schema-as-user */
    private String catalogMode = "schema";
    /** Dialect read-only transaction statement; blank = setReadOnly only. */
    private String readOnlyTx = "";
    /** jdbc = generic DatabaseMetaData; other values need dedicated implementations. */
    private String metadataStrategy = "jdbc";
    private String sessionInitSql;
    private List<String> systemSchemas = new ArrayList<>();

    public String normalizedType() {
        return type == null ? "" : type.toUpperCase(Locale.ROOT);
    }

    public boolean hasReadOnlyTx() {
        return readOnlyTx != null && !readOnlyTx.isBlank();
    }
}
