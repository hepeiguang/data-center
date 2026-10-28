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
import org.apache.dolphinscheduler.dataapi.auth.DsTokenAuthorizationInterceptor;
import org.apache.dolphinscheduler.dataapi.security.AppCredentialService;

import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for external application credentials.
 */
@RestController
@RequestMapping("/data-api/apps")
public class DataApiAppController {

    private final AppCredentialService appCredentialService;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;

    public DataApiAppController(AppCredentialService appCredentialService,
                                DsTokenAuthorizationInterceptor authorizationInterceptor) {
        this.appCredentialService = appCredentialService;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "token", required = false) String token,
                                  @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (requireAdmin(token, sessionId) == null) {
            return unauthorized();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (AppCredentialService.AppCredential credential : appCredentialService.list()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", credential.getId());
            row.put("appId", credential.getAppId());
            row.put("userId", credential.getUserId());
            row.put("scopes", credential.getScopes());
            row.put("expireTime", credential.getExpireTime() == null ? null : credential.getExpireTime().toString());
            row.put("createTime", credential.getCreateTime() == null ? null : credential.getCreateTime().toString());
            items.add(row);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader(value = "token", required = false) String token,
                                    @RequestHeader(value = "sessionId", required = false) String sessionId,
                                    @RequestBody Map<String, Object> body) {
        User admin = requireAdmin(token, sessionId);
        if (admin == null) {
            return unauthorized();
        }
        try {
            String appId = str(body.get("appId"));
            int userId = body.get("userId") == null ? admin.getId() : Integer.parseInt(str(body.get("userId")));
            String scopes = str(body.get("scopes"));
            Timestamp expireTime = parseTimestamp(str(body.get("expireTime")));
            AppCredentialService.AppCredential created =
                    appCredentialService.create(appId, userId, scopes, expireTime);
            Map<String, Object> result = new HashMap<>();
            result.put("id", created.getId());
            result.put("appId", created.getAppId());
            // plaintext secret is shown exactly once
            result.put("appSecret", created.getSecret());
            result.put("userId", created.getUserId());
            result.put("scopes", created.getScopes());
            result.put("expireTime", created.getExpireTime() == null ? null : created.getExpireTime().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<?> rotate(@PathVariable("id") long id,
                                    @RequestHeader(value = "token", required = false) String token,
                                    @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (requireAdmin(token, sessionId) == null) {
            return unauthorized();
        }
        try {
            String secret = appCredentialService.rotate(id);
            Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            result.put("appSecret", secret);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") long id,
                                    @RequestHeader(value = "token", required = false) String token,
                                    @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (requireAdmin(token, sessionId) == null) {
            return unauthorized();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("ok", appCredentialService.delete(id));
        return ResponseEntity.ok(result);
    }

    private User requireAdmin(String token, String sessionId) {
        String auth = StringUtils.isNotBlank(token) ? token : sessionId;
        User user = authorizationInterceptor.resolveDsUser(auth);
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            return null;
        }
        return user.getUserType() == UserType.ADMIN_USER ? user : null;
    }

    private static ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("admin required");
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Timestamp parseTimestamp(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Timestamp.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("expireTime must be yyyy-MM-dd HH:mm:ss");
        }
    }
}
