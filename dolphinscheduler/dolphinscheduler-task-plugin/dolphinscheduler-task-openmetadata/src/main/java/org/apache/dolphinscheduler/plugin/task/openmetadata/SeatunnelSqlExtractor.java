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

package org.apache.dolphinscheduler.plugin.task.openmetadata;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a SQL/query literal from a SeaTunnel job config (HOCON-like or YAML-like).
 */
public final class SeatunnelSqlExtractor {

    private static final Pattern SQL_LITERAL = Pattern.compile(
            "(?is)\\b(?:query|sql)\\s*=\\s*(?:\"\"\"(.*?)\"\"\"|'''(.*?)'''|\"((?:\\\\.|[^\"])*)\"|'((?:\\\\.|[^'])*)')");

    private SeatunnelSqlExtractor() {
    }

    public static String extract(String config) {
        if (StringUtils.isBlank(config)) {
            return null;
        }
        Matcher matcher = SQL_LITERAL.matcher(config);
        if (!matcher.find()) {
            return null;
        }
        for (int group = 1; group <= 4; group++) {
            String value = matcher.group(group);
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
