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
import org.apache.dolphinscheduler.dataapi.release.DataApiReleaseService;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for API publish history.
 */
@RestController
@RequestMapping("/data-api/releases")
public class DataApiReleaseController {

    private final DataApiReleaseService releaseService;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;

    public DataApiReleaseController(DataApiReleaseService releaseService,
                                    DsTokenAuthorizationInterceptor authorizationInterceptor) {
        this.releaseService = releaseService;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @PostMapping("/snapshot")
    public ResponseEntity<?> snapshot(@RequestHeader(value = "token", required = false) String token,
                                      @RequestHeader(value = "sessionId", required = false) String sessionId,
                                      @RequestBody(required = false) Map<String, Object> body) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return unauthorized();
        }
        String remark = body == null ? null : str(body.get("remark"));
        Map<String, Object> result = new HashMap<>();
        result.put("snapshotCount", releaseService.snapshot(user.getId(), remark));
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(value = "apiPath", required = false) String apiPath,
                                  @RequestHeader(value = "token", required = false) String token,
                                  @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (requireAdmin(token, sessionId) == null) {
            return unauthorized();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("items", releaseService.list(apiPath));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<?> rollback(@PathVariable("id") long id,
                                      @RequestHeader(value = "token", required = false) String token,
                                      @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return unauthorized();
        }
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("newReleaseId", releaseService.rollback(id, user.getId()));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/offline")
    public ResponseEntity<?> offline(@PathVariable("id") long id,
                                     @RequestHeader(value = "token", required = false) String token,
                                     @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = requireAdmin(token, sessionId);
        if (user == null) {
            return unauthorized();
        }
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("newReleaseId", releaseService.offline(id, user.getId()));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
}
