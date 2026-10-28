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

package org.apache.dolphinscheduler.dataapi.openapi;

import org.apache.dolphinscheduler.dataapi.acl.DataApiAclService;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.service.DataApiCatalogService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.BaseDefinition;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * OpenAPI 3.0 export for published magic APIs.
 */
@Service
public class OpenApiService {

    private static final String GATEWAY_PREFIX = "/data-api/gateway";

    private final MagicResourceService magicResourceService;
    private final DataApiProperties properties;

    public OpenApiService(MagicResourceService magicResourceService, DataApiProperties properties) {
        this.magicResourceService = magicResourceService;
        this.properties = properties;
    }

    public Map<String, Object> build() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("openapi", "3.0.1");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "DolphinScheduler Data API");
        info.put("version", "1.0.0");
        info.put("description", "SQL published through magic-api. Authenticate with token / sessionId / X-App-Id + X-App-Secret.");
        root.put("info", info);
        if (StringUtils.isNotBlank(properties.getOpenApiServerUrl())) {
            List<Map<String, Object>> servers = new ArrayList<>();
            Map<String, Object> server = new LinkedHashMap<>();
            server.put("url", properties.getOpenApiServerUrl());
            servers.add(server);
            root.put("servers", servers);
        }

        Map<String, Object> paths = new LinkedHashMap<>();
        for (MagicEntity entity : magicResourceService.files("api")) {
            if (!(entity instanceof ApiInfo)) {
                continue;
            }
            ApiInfo api = (ApiInfo) entity;
            String groupPath = magicResourceService.getGroupPath(api.getGroupId());
            String fullPath = GATEWAY_PREFIX + DataApiAclService.normalizePathPattern(
                    DataApiCatalogService.joinPath(groupPath, api.getPath()));
            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("operationId", api.getId());
            operation.put("summary", StringUtils.defaultIfBlank(api.getName(), api.getPath()));
            if (StringUtils.isNotBlank(api.getDescription())) {
                operation.put("description", api.getDescription());
            }
            operation.put("parameters", parameters(api));
            operation.put("responses", responses());
            Map<String, Object> pathItem = new LinkedHashMap<>();
            pathItem.put(StringUtils.defaultIfBlank(api.getMethod(), "GET").toLowerCase(Locale.ROOT), operation);
            paths.put(fullPath, pathItem);
        }
        root.put("paths", paths);
        return root;
    }

    private List<Map<String, Object>> parameters(ApiInfo api) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (api.getParameters() == null) {
            return result;
        }
        for (BaseDefinition parameter : api.getParameters()) {
            if (StringUtils.isBlank(parameter.getName())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", parameter.getName());
            item.put("in", "query");
            item.put("required", parameter.isRequired());
            if (StringUtils.isNotBlank(parameter.getDescription())) {
                item.put("description", parameter.getDescription());
            }
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", jsonType(parameter));
            item.put("schema", schema);
            result.add(item);
        }
        return result;
    }

    private static String jsonType(BaseDefinition definition) {
        if (definition.getDataType() == null) {
            return "string";
        }
        switch (definition.getDataType().name()) {
            case "INTEGER":
            case "LONG":
                return "integer";
            case "FLOAT":
            case "DOUBLE":
            case "DECIMAL":
                return "number";
            case "BOOLEAN":
                return "boolean";
            default:
                return "string";
        }
    }

    private static Map<String, Object> responses() {
        Map<String, Object> responses = new LinkedHashMap<>();
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("description", "magic-api result envelope: {code, message, data}");
        responses.put("200", ok);
        return responses;
    }
}
