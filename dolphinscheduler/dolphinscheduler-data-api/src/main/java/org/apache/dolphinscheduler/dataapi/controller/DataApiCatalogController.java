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
import org.apache.dolphinscheduler.dataapi.lineage.DataApiLineageService;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API lineage derived from published scripts (input for OpenMetadata).
 */
@RestController
@RequestMapping("/data-api/catalog")
public class DataApiCatalogController {

    private final DataApiLineageService lineageService;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;

    public DataApiCatalogController(DataApiLineageService lineageService,
                                    DsTokenAuthorizationInterceptor authorizationInterceptor) {
        this.lineageService = lineageService;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @GetMapping("/lineage")
    public ResponseEntity<?> lineage(@RequestHeader(value = "token", required = false) String token,
                                     @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (resolveUser(token, sessionId) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("token required");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", lineageService.views());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/lineage/refresh")
    public ResponseEntity<?> refresh(@RequestHeader(value = "token", required = false) String token,
                                     @RequestHeader(value = "sessionId", required = false) String sessionId) {
        User user = resolveUser(token, sessionId);
        if (user == null || user.getUserType() != UserType.ADMIN_USER) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("admin required");
        }
        lineageService.refresh();
        Map<String, Object> body = new HashMap<>();
        body.put("items", lineageService.views());
        return ResponseEntity.ok(body);
    }

    private User resolveUser(String token, String sessionId) {
        String auth = StringUtils.isNotBlank(token) ? token : sessionId;
        User user = authorizationInterceptor.resolveDsUser(auth);
        if (user == null || user.getState() == Flag.NO.ordinal()) {
            return null;
        }
        return user;
    }
}
