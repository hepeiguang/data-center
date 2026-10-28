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

package org.apache.dolphinscheduler.dataapi.controller;

import org.apache.dolphinscheduler.common.enums.Flag;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dataapi.acl.AclSubjectType;
import org.apache.dolphinscheduler.dataapi.acl.DataApiAclEntry;
import org.apache.dolphinscheduler.dataapi.acl.DataApiAclService;
import org.apache.dolphinscheduler.dataapi.auth.DsTokenAuthorizationInterceptor;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.service.DataApiCatalogService;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-api/acl")
public class DataApiAclController {

    private final DataApiAclService aclService;
    private final DataApiCatalogService catalogService;
    private final DataApiProperties properties;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;

    public DataApiAclController(DataApiAclService aclService,
                                DataApiCatalogService catalogService,
                                DataApiProperties properties,
                                DsTokenAuthorizationInterceptor authorizationInterceptor) {
        this.aclService = aclService;
        this.catalogService = catalogService;
        this.properties = properties;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "apiPath", required = false) String apiPath) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("admin required");
        }
        List<Map<String, Object>> items = aclService.list(apiPath).stream()
                .map(DataApiAclEntry::toMap)
                .collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("aclMode", properties.getAclMode());
        body.put("items", items);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId,
            @RequestBody Map<String, Object> body) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("admin required");
        }
        try {
            String apiPath = str(body.get("apiPath"));
            String subjectTypeName = str(body.get("subjectType"));
            if (StringUtils.isBlank(subjectTypeName) && body.get("subjectTypeCode") != null) {
                subjectTypeName = AclSubjectType.fromCode(Integer.parseInt(String.valueOf(body.get("subjectTypeCode"))))
                        .name();
            }
            String subjectId = str(body.get("subjectId"));
            int effect = 1;
            if (body.get("effect") != null) {
                String e = String.valueOf(body.get("effect"));
                if ("0".equals(e) || "DENY".equalsIgnoreCase(e)) {
                    effect = 0;
                }
            }
            String remark = str(body.get("remark"));
            DataApiAclEntry created = aclService.create(apiPath, AclSubjectType.fromName(subjectTypeName), subjectId,
                    effect, remark, user.getId());
            return ResponseEntity.ok(created.toMap());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") long id,
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId,
            @RequestBody Map<String, Object> body) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("admin required");
        }
        try {
            Integer effect = null;
            if (body.get("effect") != null) {
                String e = String.valueOf(body.get("effect"));
                effect = ("0".equals(e) || "DENY".equalsIgnoreCase(e)) ? 0 : 1;
            }
            String remark = body.containsKey("remark") ? str(body.get("remark")) : null;
            boolean ok = aclService.update(id, effect, remark);
            Map<String, Object> result = new HashMap<>();
            result.put("ok", ok);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable("id") long id,
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("admin required");
        }
        boolean ok = aclService.delete(id);
        Map<String, Object> body = new HashMap<>();
        body.put("ok", ok);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/apis")
    public ResponseEntity<?> listApis(
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("admin required");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", catalogService.listApis());
        body.put("aclMode", properties.getAclMode());
        return ResponseEntity.ok(body);
    }

    private User requireAdmin(String token, String sessionId) {
        String auth = StringUtils.isNotBlank(token) ? token : sessionId;
        User user = authorizationInterceptor.resolveDsUser(auth);
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            return null;
        }
        if (user.getUserType() != UserType.ADMIN_USER) {
            return null;
        }
        return user;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
