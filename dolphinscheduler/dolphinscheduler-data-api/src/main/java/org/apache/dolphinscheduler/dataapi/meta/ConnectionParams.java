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

package org.apache.dolphinscheduler.dataapi.meta;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

/**
 * Parses the {@code t_ds_datasource.connection_params} JSON written by the DS UI. Replaces
 * {@code DataSourceUtils.buildConnectionParams} + {@code BaseConnectionParam}: the stored JSON
 * already carries {@code jdbcUrl}, so no per-type processor is required for the descriptor-backed
 * types (see datasource-types.yaml). Unknown fields are ignored to stay compatible with
 * type-specific extras (e.g. Doris frontend config).
 */
@Slf4j
@Getter
@Setter
public class ConnectionParams {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private String user;
    /** Still encoded as stored; decode via {@link DsPasswordCodec#decodePassword(String)}. */
    private String password;
    private String address;
    private String database;
    private String jdbcUrl;
    private String driverClassName;
    private String validationQuery;
    private Map<String, String> other = new LinkedHashMap<>();

    public static ConnectionParams parse(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, ConnectionParams.class);
        } catch (Exception e) {
            log.warn("unparseable connection_params: {}", e.getMessage());
            return null;
        }
    }

    /** Mirrors DS processors: {@code jdbcUrl} plus {@code other} appended as query string. */
    public String effectiveJdbcUrl() {
        String url = StringUtils.isNotBlank(jdbcUrl) ? jdbcUrl
                : (StringUtils.isNotBlank(address) && StringUtils.isNotBlank(database)
                        ? address + "/" + database
                        : address);
        if (other == null || other.isEmpty() || url == null) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : other.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(StringUtils.defaultString(entry.getValue()));
            first = false;
        }
        return sb.toString();
    }
}
