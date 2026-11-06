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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads {@code datasource-types.yaml} once at startup. Unknown db types simply have no
 * descriptor: the registry skips them (with a log line) instead of failing the boot, which
 * mirrors the previous plugin-manager behavior for unsupported connectors.
 */
@Slf4j
@Component
public class DatasourceTypeRegistry {

    private static final String RESOURCE = "datasource-types.yaml";

    private final Map<String, DatasourceTypeDescriptor> descriptors;

    public DatasourceTypeRegistry() {
        this.descriptors = load();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, DatasourceTypeDescriptor> load() {
        Map<String, DatasourceTypeDescriptor> result = new LinkedHashMap<>();
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            Object types = root == null ? null : root.get("types");
            if (types instanceof Map) {
                ((Map<String, Object>) types).forEach((name, spec) -> {
                    DatasourceTypeDescriptor descriptor = new DatasourceTypeDescriptor();
                    descriptor.setType(name.toUpperCase(Locale.ROOT));
                    if (spec instanceof Map) {
                        ((Map<String, Object>) spec).forEach((key, value) -> apply(descriptor, key, value));
                    }
                    result.put(descriptor.getType(), descriptor);
                });
            }
        } catch (Exception e) {
            log.error("failed to load {}: datasource registry will reject all types", RESOURCE, e);
        }
        log.info("datasource type descriptors loaded: {}", result.keySet());
        return Collections.unmodifiableMap(result);
    }

    private static void apply(DatasourceTypeDescriptor d, String key, Object value) {
        switch (key) {
            case "driver" -> d.setDriver(str(value));
            case "validationQuery" -> d.setValidationQuery(str(value));
            case "quote" -> d.setQuote(str(value));
            case "limitOffset" -> d.setLimitOffset(str(value));
            case "catalogMode" -> d.setCatalogMode(str(value));
            case "readOnlyTx" -> d.setReadOnlyTx(str(value));
            case "metadataStrategy" -> d.setMetadataStrategy(str(value));
            case "sessionInitSql" -> d.setSessionInitSql(str(value));
            case "systemSchemas" -> {
                List<String> schemas = new ArrayList<>();
                if (value instanceof List) {
                    for (Object item : (List<?>) value) {
                        schemas.add(String.valueOf(item));
                    }
                }
                d.setSystemSchemas(schemas);
            }
            default -> log.warn("unknown datasource-types.yaml key '{}' for type {}", key, d.getType());
        }
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** @return descriptor or null when the db type is not described (unsupported here). */
    public DatasourceTypeDescriptor find(String dbTypeName) {
        return dbTypeName == null ? null : descriptors.get(dbTypeName.toUpperCase(Locale.ROOT));
    }

    public Map<String, DatasourceTypeDescriptor> all() {
        return descriptors;
    }
}
