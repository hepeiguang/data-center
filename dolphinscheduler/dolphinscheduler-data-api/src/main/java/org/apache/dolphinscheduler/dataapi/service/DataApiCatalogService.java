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

package org.apache.dolphinscheduler.dataapi.service;

import org.apache.dolphinscheduler.dataapi.acl.DataApiAclService;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * Lists published magic APIs for ACL binding UI.
 */
@Service
public class DataApiCatalogService {

    private final MagicResourceService magicResourceService;

    public DataApiCatalogService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    public List<Map<String, Object>> listApis() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<? extends MagicEntity> files = magicResourceService.files("api");
        if (files == null) {
            return result;
        }
        for (MagicEntity entity : files) {
            if (!(entity instanceof ApiInfo)) {
                continue;
            }
            ApiInfo api = (ApiInfo) entity;
            String groupPath = magicResourceService.getGroupPath(api.getGroupId());
            String full = joinPath(groupPath, api.getPath());
            Map<String, Object> row = new HashMap<>();
            row.put("id", api.getId());
            row.put("name", api.getName());
            row.put("method", api.getMethod());
            row.put("path", DataApiAclService.normalizePathPattern(full));
            row.put("groupPath", groupPath);
            row.put("gatewayUrl", "/data-api/gateway" + DataApiAclService.normalizePathPattern(full));
            result.add(row);
        }
        return result;
    }

    public static String joinPath(String groupPath, String apiPath) {
        String g = StringUtils.defaultString(groupPath);
        String a = StringUtils.defaultString(apiPath);
        if (!g.startsWith("/") && StringUtils.isNotBlank(g)) {
            g = "/" + g;
        }
        if (!a.startsWith("/")) {
            a = "/" + a;
        }
        if (StringUtils.isBlank(g) || "/".equals(g)) {
            return a;
        }
        if (g.endsWith("/")) {
            return g.substring(0, g.length() - 1) + a;
        }
        return g + a;
    }
}
