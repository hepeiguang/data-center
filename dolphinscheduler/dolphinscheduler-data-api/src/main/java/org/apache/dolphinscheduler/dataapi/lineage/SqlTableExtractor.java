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

package org.apache.dolphinscheduler.dataapi.lineage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Best-effort table extraction from magic scripts.
 * Only literal SQL blocks and {@code db.table('x')} are considered; dynamic SQL yields no lineage
 * rather than a wrong edge.
 */
public final class SqlTableExtractor {

    private static final Pattern FROM_JOIN = Pattern.compile(
            "(?i)\\b(?:from|join)\\s+([A-Za-z_`\"$][A-Za-z0-9_`\"$.]*)");
    private static final Pattern DB_TABLE = Pattern.compile(
            "\\bdb(?:\\.[A-Za-z0-9_]+)?\\s*\\.\\s*table\\s*\\(\\s*['\"]([A-Za-z0-9_.]+)['\"]");
    private static final Set<String> NOT_TABLES = new LinkedHashSet<>();

    static {
        NOT_TABLES.add("select");
        NOT_TABLES.add("lateral");
        NOT_TABLES.add("unnest");
        NOT_TABLES.add("dual");
    }

    private SqlTableExtractor() {
    }

    public static Set<String> extractTables(String script) {
        Set<String> tables = new LinkedHashSet<>();
        if (StringUtils.isBlank(script)) {
            return tables;
        }
        for (String literal : literals(script)) {
            Matcher matcher = FROM_JOIN.matcher(literal);
            while (matcher.find()) {
                add(tables, matcher.group(1));
            }
        }
        Matcher dbTable = DB_TABLE.matcher(script);
        while (dbTable.find()) {
            add(tables, dbTable.group(1));
        }
        return tables;
    }

    private static void add(Set<String> tables, String raw) {
        String table = raw.replace("`", "").replace("\"", "").replace(",", "").trim();
        if (table.isEmpty() || table.startsWith("(")) {
            return;
        }
        String lower = table.toLowerCase(Locale.ROOT);
        if (NOT_TABLES.contains(lower)) {
            return;
        }
        if (lower.startsWith("information_schema.") || lower.startsWith("performance_schema.")
                || lower.startsWith("mysql.") || lower.startsWith("sys.")) {
            return;
        }
        tables.add(table);
    }

    /**
     * Extracts single, double and triple quoted string literals (escapes handled for single quotes).
     */
    static List<String> literals(String script) {
        List<String> result = new ArrayList<>();
        int i = 0;
        int n = script.length();
        while (i < n) {
            char c = script.charAt(i);
            if (c != '\'' && c != '"') {
                i++;
                continue;
            }
            char quote = c;
            boolean triple = i + 2 < n && script.charAt(i + 1) == quote && script.charAt(i + 2) == quote;
            i += triple ? 3 : 1;
            int start = i;
            int end = -1;
            while (i < n) {
                char ch = script.charAt(i);
                if (ch == '\\' && !triple) {
                    i += 2;
                    continue;
                }
                if (ch == quote) {
                    if (triple) {
                        if (i + 2 < n && script.charAt(i + 1) == quote && script.charAt(i + 2) == quote) {
                            end = i;
                            i += 3;
                            break;
                        }
                        i++;
                        continue;
                    }
                    if (i + 1 < n && script.charAt(i + 1) == quote) {
                        i += 2;
                        continue;
                    }
                    end = i;
                    i++;
                    break;
                }
                i++;
            }
            if (end > start) {
                result.add(script.substring(start, end));
            }
        }
        return result;
    }
}
