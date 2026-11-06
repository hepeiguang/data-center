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
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dataapi.auth.DsTokenAuthorizationInterceptor;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.openapi.OpenApiService;

import org.apache.commons.lang3.StringUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataApiOpenApiController {

    private final OpenApiService openApiService;
    private final DataApiProperties properties;
    private final DsTokenAuthorizationInterceptor authorizationInterceptor;

    public DataApiOpenApiController(OpenApiService openApiService,
                                    DataApiProperties properties,
                                    DsTokenAuthorizationInterceptor authorizationInterceptor) {
        this.openApiService = openApiService;
        this.properties = properties;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @GetMapping(value = "/data-api/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> openapi(@RequestHeader(value = "token", required = false) String token,
                                     @RequestHeader(value = "sessionId", required = false) String sessionId,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!properties.isOpenApiPublic()) {
            String auth = StringUtils.isNotBlank(token) ? token : sessionId;
            if (StringUtils.isBlank(auth) && StringUtils.isNotBlank(authorization)) {
                auth = authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                        ? authorization.substring(7).trim()
                        : authorization.trim();
            }
            User user = authorizationInterceptor.resolveDsUser(auth);
            if (user == null || user.getState() == Flag.NO.ordinal()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("token required");
            }
        }
        return ResponseEntity.ok(openApiService.build());
    }
}
