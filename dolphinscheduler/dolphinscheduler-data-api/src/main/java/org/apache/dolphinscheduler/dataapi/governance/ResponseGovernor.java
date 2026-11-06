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

package org.apache.dolphinscheduler.dataapi.governance;

import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.model.JsonBean;

/**
 * Masks configured field names in query results and rejects oversized responses.
 * Masking is applied in place; the size check runs on the serialized magic result.
 */
@Slf4j
@Component
public class ResponseGovernor {

    private final DataApiProperties properties;
    private final ObjectMapper objectMapper;

    public ResponseGovernor(DataApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * @return an error bean when the response exceeds the configured limit, otherwise {@code null}
     */
    public Object apply(Object value) {
        Object data = value instanceof JsonBean ? ((JsonBean<?>) value).getData() : value;
        mask(data);
        long maxBytes = properties.getResponseMaxBytes();
        if (maxBytes <= 0) {
            return null;
        }
        try {
            long size = objectMapper.writeValueAsBytes(value).length;
            if (size > maxBytes) {
                log.warn("response rejected by size governor: {} bytes > {}", size, maxBytes);
                return new JsonBean<>(413, "response too large (" + size + " bytes), please narrow the query or paginate");
            }
        } catch (Exception e) {
            log.debug("response size check skipped: {}", e.getMessage());
        }
        return null;
    }

    private void mask(Object node) {
        if (node instanceof Map) {
            Set<String> fields = properties.getMaskFields();
            Map<?, ?> map = (Map<?, ?>) node;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
                if (fields.contains(key)) {
                    setValue(map, entry.getKey(), "***");
                } else {
                    mask(entry.getValue());
                }
            }
        } else if (node instanceof Collection) {
            for (Object item : (Collection<?>) node) {
                mask(item);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void setValue(Map<?, ?> map, Object key, Object value) {
        ((Map<Object, Object>) map).put(key, value);
    }
}
