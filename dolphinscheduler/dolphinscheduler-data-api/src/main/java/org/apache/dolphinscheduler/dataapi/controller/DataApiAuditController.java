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
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.service.DataApiInvokeLogService;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for invoke-audit statistics and retention.
 */
@RestController
@RequestMapping("/data-api/audit")
public class DataApiAuditController {

    private final DataApiInvokeLogService invokeLogService;
    private final DataApiProperties properties;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;

    public DataApiAuditController(DataApiInvokeLogService invokeLogService,
                                  DataApiProperties properties,
                                  DsTokenAuthorizationInterceptor authorizationInterceptor) {
        this.invokeLogService = invokeLogService;
        this.properties = properties;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@RequestParam(value = "apiPath", required = false) String apiPath,
                                   @RequestParam(value = "from", required = false) Long from,
                                   @RequestParam(value = "to", required = false) Long to,
                                   @RequestHeader(value = "token", required = false) String token,
                                   @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (requireAdmin(token, sessionId) == null) {
            return unauthorized();
        }
        long end = to == null ? System.currentTimeMillis() : to;
        long start = from == null ? end - 24L * 3600L * 1000L : from;
        Map<String, Object> body = new HashMap<>();
        body.put("from", start);
        body.put("to", end);
        body.put("items", invokeLogService.stats(apiPath, start, end));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanup(@RequestParam(value = "retentionDays", required = false) Integer retentionDays,
                                     @RequestHeader(value = "token", required = false) String token,
                                     @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (requireAdmin(token, sessionId) == null) {
            return unauthorized();
        }
        int days = retentionDays == null ? properties.getAuditRetentionDays() : retentionDays;
        Map<String, Object> body = new HashMap<>();
        body.put("deleted", invokeLogService.cleanup(days));
        body.put("retentionDays", days);
        return ResponseEntity.ok(body);
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
}
