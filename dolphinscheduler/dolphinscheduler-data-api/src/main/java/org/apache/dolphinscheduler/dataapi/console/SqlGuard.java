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

package org.apache.dolphinscheduler.dataapi.console;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

import org.apache.commons.lang3.StringUtils;

/**
 * Statement layer of the console guard rails (plan §7.2 layer 3): a single {@code SELECT / WITH /
 * EXPLAIN <select>} must parse, and dangerous fragments (locking reads, INTO, COPY, SET ROLE,
 * sleep/dblink helpers) are rejected even inside otherwise valid statements.
 */
public final class SqlGuard {

    public static final class Verdict {

        private final boolean allowed;
        private final String reason;

        private Verdict(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }
    }

    private static final Verdict OK = new Verdict(true, null);

    /** Functions/verbs that escape the read-only sandbox or burn resources (plan §7.2). */
    private static final Pattern[] BLACKLIST = {
            Pattern.compile("\\bfor\\s+update\\b"),
            Pattern.compile("\\binto\\s+(outfile|dumpfile)\\b"),
            Pattern.compile("\\binto\\s+[a-z_@]"),
            Pattern.compile("\\bset\\s+role\\b"),
            Pattern.compile("\\bcopy\\b"),
            Pattern.compile("\\bcall\\s+"),
            Pattern.compile("\\b(pg_sleep|dblink|dblink_exec|dblink_connect|lo_import|lo_export"
                    + "|pg_read_file|pg_ls_dir|pg_stat_file|benchmark|sleep|sys_exec|load_file)\\s*\\("),
    };

    private SqlGuard() {
    }

    public static Verdict check(String sql, int maxSqlBytes) {
        if (StringUtils.isBlank(sql)) {
            return deny("empty statement");
        }
        if (sql.getBytes(StandardCharsets.UTF_8).length > maxSqlBytes) {
            return deny("sql exceeds " + maxSqlBytes + " bytes");
        }
        // scrubbed text (literals/comments blanked) is only for guard rails; the original text
        // is what gets parsed, otherwise `where a = 'x'` would turn into unparseable `where a =`
        String scrub = stripLiteralsAndComments(sql).trim();
        if (scrub.endsWith(";")) {
            scrub = scrub.substring(0, scrub.length() - 1).trim();
        }
        if (scrub.isEmpty()) {
            return deny("empty statement");
        }
        if (scrub.indexOf(';') >= 0) {
            return deny("multi-statement is not allowed");
        }
        String lower = scrub.toLowerCase(Locale.ROOT);
        for (Pattern pattern : BLACKLIST) {
            if (pattern.matcher(lower).find()) {
                return deny("statement contains a forbidden fragment");
            }
        }
        String parseable = sql.trim();
        if (parseable.endsWith(";")) {
            parseable = parseable.substring(0, parseable.length() - 1);
        }
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(parseable);
        } catch (Exception e) {
            return deny("statement is not parseable read-only SQL: " + shortMessage(e));
        }
        if (statement instanceof ExplainStatement) {
            Statement inner = ((ExplainStatement) statement).getStatement();
            if (!(inner instanceof Select)) {
                return deny("EXPLAIN only accepts a SELECT");
            }
            return checkSelectShape((Select) inner);
        }
        if (!(statement instanceof Select)) {
            return deny("only SELECT / WITH / EXPLAIN statements are allowed");
        }
        return checkSelectShape((Select) statement);
    }

    private static Verdict checkSelectShape(Select select) {
        // FOR UPDATE / INTO also show up on the plain select body (jsqlparser 4.x: Select -> SelectBody)
        SelectBody body = select.getSelectBody();
        if (body instanceof PlainSelect) {
            PlainSelect plain = (PlainSelect) body;
            if (plain.getIntoTables() != null && !plain.getIntoTables().isEmpty()) {
                return deny("SELECT INTO is not allowed");
            }
        }
        return OK;
    }

    private static Verdict deny(String reason) {
        return new Verdict(false, reason);
    }

    private static String shortMessage(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return StringUtils.abbreviate(msg.replaceAll("\\s+", " "), 160);
    }

    /** Blank out quoted literals and comments so guards neither spoof nor mis-detect (§7.2 layer 3). */
    static String stripLiteralsAndComments(String script) {
        StringBuilder out = new StringBuilder(script.length());
        int i = 0;
        int n = script.length();
        while (i < n) {
            char c = script.charAt(i);
            if (c == '\'' || c == '"' || c == '`') {
                char quote = c;
                i++;
                while (i < n) {
                    char ch = script.charAt(i);
                    if (ch == '\\' && quote != '`') {
                        i += 2;
                        continue;
                    }
                    if (ch == quote) {
                        if (i + 1 < n && script.charAt(i + 1) == quote) {
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    i++;
                }
                out.append(' ');
                continue;
            }
            if (c == '-' && i + 1 < n && script.charAt(i + 1) == '-') {
                while (i < n && script.charAt(i) != '\n') {
                    i++;
                }
                out.append(' ');
                continue;
            }
            if (c == '#') {
                while (i < n && script.charAt(i) != '\n') {
                    i++;
                }
                out.append(' ');
                continue;
            }
            if (c == '/' && i + 1 < n && script.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(script.charAt(i) == '*' && script.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
                out.append(' ');
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }
}
