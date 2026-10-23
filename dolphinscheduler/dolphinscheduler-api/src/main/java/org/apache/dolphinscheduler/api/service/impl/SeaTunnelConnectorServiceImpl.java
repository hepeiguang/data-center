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

package org.apache.dolphinscheduler.api.service.impl;

import org.apache.dolphinscheduler.api.service.SeaTunnelConnectorService;
import org.apache.dolphinscheduler.common.utils.JSONUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SeaTunnelConnectorServiceImpl implements SeaTunnelConnectorService {

    private static final String CATALOG_RESOURCE = "seatunnel-connectors-catalog.json";

    private final AtomicReference<Map<String, Object>> cache = new AtomicReference<>();

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConnectorCatalog() {
        Map<String, Object> cached = cache.get();
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = cache.get();
            if (cached != null) {
                return cached;
            }
            try (InputStream in = new ClassPathResource(CATALOG_RESOURCE).getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String json = reader.lines().collect(Collectors.joining("\n"));
                Map<String, Object> catalog = JSONUtils.parseObject(json, Map.class);
                if (catalog == null) {
                    catalog = emptyCatalog();
                }
                cache.set(Collections.unmodifiableMap(new LinkedHashMap<>(catalog)));
                return cache.get();
            } catch (Exception e) {
                log.error("Failed to load SeaTunnel connector catalog from {}", CATALOG_RESOURCE, e);
                return emptyCatalog();
            }
        }
    }

    private static Map<String, Object> emptyCatalog() {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("sources", Collections.emptyList());
        empty.put("sinks", Collections.emptyList());
        empty.put("transforms", Collections.emptyList());
        return empty;
    }
}
