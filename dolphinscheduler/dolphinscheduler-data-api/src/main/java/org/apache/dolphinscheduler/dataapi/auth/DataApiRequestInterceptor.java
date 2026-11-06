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

package org.apache.dolphinscheduler.dataapi.auth;

import org.apache.dolphinscheduler.common.enums.Flag;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.dao.repository.UserDao;
import org.apache.dolphinscheduler.dataapi.acl.DataApiAclService;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.datasource.DsMagicDataSourceLoader;
import org.apache.dolphinscheduler.dataapi.governance.ResponseGovernor;
import org.apache.dolphinscheduler.dataapi.security.AppCredentialService;
import org.apache.dolphinscheduler.dataapi.security.RateLimitService;
import org.apache.dolphinscheduler.dataapi.service.DataApiInvokeLogService;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.script.MagicScriptContext;

/**
 * Call-side guard: token/app credentials, rate limit, DML reject, path ACL, datasource ACL,
 * response governance and invoke audit.
 */
@Order(1)
@Component
public class DataApiRequestInterceptor implements RequestInterceptor {

    private static final String ATTR_START = "_da_start";
    private static final String ATTR_CODE = "_da_code";
    private static final String ATTR_USER_ID = "_da_user_id";
    private static final String ATTR_APP_ID = "_da_app_id";

    /** {@code db.<key>.<method>(...)} — every referenced business datasource. */
    private static final Pattern DATASOURCE_CALL = Pattern.compile(
            "\\bdb\\.([a-zA-Z][a-zA-Z0-9_]*)\\s*\\.\\s*"
                    + "(?:select|selectOne|selectInt|selectValue|page|insert|update|delete|batchUpdate|call|transaction)"
                    + "\\s*\\(",
            Pattern.CASE_INSENSITIVE);
    /** {@code db.use('key')} — explicit datasource switch. */
    private static final Pattern DATASOURCE_USE = Pattern.compile(
            "\\bdb\\.use\\s*\\(\\s*['\"]([a-zA-Z][a-zA-Z0-9_]*)['\"]",
            Pattern.CASE_INSENSITIVE);
    /** magic-api write methods on {@code db} / {@code db.<key>}. */
    private static final Pattern WRITE_CALL = Pattern.compile(
            "\\bdb(?:\\.([a-zA-Z][a-zA-Z0-9_]*))?\\s*\\.\\s*"
                    + "(insert|update|delete|batchUpdate|execute|call)\\s*\\(",
            Pattern.CASE_INSENSITIVE);
    /** {@code db.table('t').insert(...)} row-DSL writes. */
    private static final Pattern TABLE_WRITE_CALL = Pattern.compile(
            "\\btable\\s*\\([^)]*\\)\\s*\\.\\s*(insert|update|delete|save|batchUpdate)\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> FORBIDDEN_STATEMENT_HEADS = new LinkedHashSet<>();

    static {
        FORBIDDEN_STATEMENT_HEADS.addAll(java.util.Arrays.asList(
                "insert", "update", "delete", "drop", "truncate", "alter", "create",
                "grant", "revoke", "merge", "replace", "rename", "call", "load",
                "handler", "lock", "unlock", "set", "use", "prepare", "execute",
                "deallocate", "comment", "analyze", "optimize", "repair", "flush",
                "kill", "shutdown", "do"));
    }

    private final DsTokenAuthorizationInterceptor authorizationInterceptor;
    private final DataSourceMapper dataSourceMapper;
    private final DsMagicDataSourceLoader dataSourceLoader;
    private final DataApiAclService aclService;
    private final DataApiProperties properties;
    private final DataApiInvokeLogService invokeLogService;
    private final UserDao userDao;
    private final AppCredentialService appCredentialService;
    private final RateLimitService rateLimitService;
    private final ResponseGovernor responseGovernor;

    public DataApiRequestInterceptor(DsTokenAuthorizationInterceptor authorizationInterceptor,
                                     DataSourceMapper dataSourceMapper,
                                     DsMagicDataSourceLoader dataSourceLoader,
                                     DataApiAclService aclService,
                                     DataApiProperties properties,
                                     DataApiInvokeLogService invokeLogService,
                                     UserDao userDao,
                                     AppCredentialService appCredentialService,
                                     RateLimitService rateLimitService,
                                     ResponseGovernor responseGovernor) {
        this.authorizationInterceptor = authorizationInterceptor;
        this.dataSourceMapper = dataSourceMapper;
        this.dataSourceLoader = dataSourceLoader;
        this.aclService = aclService;
        this.properties = properties;
        this.invokeLogService = invokeLogService;
        this.userDao = userDao;
        this.appCredentialService = appCredentialService;
        this.rateLimitService = rateLimitService;
        this.responseGovernor = responseGovernor;
    }

    @Override
    public Object preHandle(ApiInfo info, MagicScriptContext context, MagicHttpServletRequest request,
                            MagicHttpServletResponse response) {
        long start = System.currentTimeMillis();
        request.setAttribute(ATTR_START, start);

        String relativePath = DataApiAclService.extractGatewayRelativePath(request.getRequestURI());
        String token = resolveToken(request);
        String appId = null;
        User user = null;

        if (properties.isAppAuthEnabled()) {
            String headerAppId = StringUtils.trimToNull(request.getHeader("X-App-Id"));
            String headerSecret = StringUtils.trimToNull(request.getHeader("X-App-Secret"));
            if (headerAppId != null || headerSecret != null) {
                AppCredentialService.AppPrincipal principal =
                        appCredentialService.authenticate(headerAppId, headerSecret);
                if (principal == null) {
                    return reject(request, response, 401, "app credential invalid or expired");
                }
                user = userDao.queryById(principal.getUserId());
                if (user == null || user.getState() == Flag.NO.ordinal()) {
                    return reject(request, response, 401, "app owner user disabled");
                }
                if (!principal.allows(relativePath)) {
                    return reject(request, response, 403, "app scope does not cover " + relativePath);
                }
                appId = principal.getAppId();
                request.setAttribute(ATTR_APP_ID, appId);
                context.set("daAppId", appId);
            }
        }

        if (user == null) {
            boolean requireLogin = !"false".equalsIgnoreCase(info.getOptionValue(Options.REQUIRE_LOGIN));
            if (requireLogin && StringUtils.isBlank(token)) {
                return reject(request, response, 401, "token required");
            }
            if (StringUtils.isNotBlank(token)) {
                user = authorizationInterceptor.resolveDsUser(token);
                if (user == null || user.getState() == Flag.NO.ordinal()) {
                    return reject(request, response, 401, "token invalid or user disabled");
                }
            }
        }

        if (user != null) {
            request.setAttribute(ATTR_USER_ID, String.valueOf(user.getId()));
            context.set("daUserId", user.getId());
            context.set("daUserName", user.getUserName());
            String subject = appId != null ? "app:" + appId : "u:" + user.getId();
            if (!rateLimitService.tryAcquire(subject)) {
                return reject(request, response, 429, "too many requests");
            }
        }

        context.set("daMaxRows", properties.getMaxRows());
        context.set("daQueryTimeoutSeconds", properties.getQueryTimeoutSeconds());

        String script = info.getScript();
        if (properties.isRejectDml() && containsDml(script)) {
            return reject(request, response, 403, "only SELECT queries are allowed");
        }

        if (user != null && !aclService.isAllowed(user, token, appId, relativePath)) {
            return reject(request, response, 403, "no permission for api " + relativePath);
        }

        if (user != null && user.getUserType() != UserType.ADMIN_USER) {
            for (String dsKey : extractDatasourceKeys(script)) {
                Integer dsId = dataSourceLoader.resolveDatasourceId(dsKey);
                if (dsId == null) {
                    dsId = parseDatasourceId(dsKey);
                }
                if (dsId != null && !canAccessDatasource(user.getId(), dsId)) {
                    return reject(request, response, 403, "no permission for datasource " + dsKey);
                }
            }
        }

        return null;
    }

    @Override
    public Object postHandle(ApiInfo info, MagicScriptContext context, Object returnValue,
                             MagicHttpServletRequest request, MagicHttpServletResponse response) {
        Object governed = responseGovernor.apply(returnValue);
        if (governed instanceof JsonBean && ((JsonBean<?>) governed).getCode() >= 400) {
            applyHttpStatus(response, ((JsonBean<?>) governed).getCode());
        }
        return governed;
    }

    /**
     * One audit row per call. Runs for successful calls, pre-handle rejections, and thrown exceptions.
     */
    @Override
    public void afterCompletion(RequestEntity requestEntity, Object returnValue, Throwable throwable) {
        ApiInfo info = requestEntity.getApiInfo();
        if (info == null || requestEntity.isRequestedFromTest()) {
            return;
        }
        MagicHttpServletRequest request = requestEntity.getRequest();
        Object startAttr = request.getAttribute(ATTR_START);
        long latency = startAttr instanceof Long ? System.currentTimeMillis() - (Long) startAttr : -1L;
        Object userId = request.getAttribute(ATTR_USER_ID);
        Object appId = request.getAttribute(ATTR_APP_ID);
        int code = resolveAuditCode(request, returnValue, throwable);
        if (code >= 400) {
            applyHttpStatus(requestEntity.getResponse(), code);
        }
        // info.getPath() is group-relative; audit must use the same path shape as ACL (/group/api)
        invokeLogService.record(
                DataApiAclService.extractGatewayRelativePath(request.getRequestURI()),
                userId == null ? null : String.valueOf(userId),
                appId == null ? null : String.valueOf(appId),
                latency,
                code,
                rowCount(returnValue),
                requestEntity.getRequestId());
    }

    private static Object reject(MagicHttpServletRequest request, MagicHttpServletResponse response, int code,
                                 String message) {
        request.setAttribute(ATTR_CODE, code);
        applyHttpStatus(response, code);
        return new JsonBean<>(code, message);
    }

    private static void applyHttpStatus(MagicHttpServletResponse response, int code) {
        if (response == null || code < 400) {
            return;
        }
        try {
            Object raw = response.getResponse();
            if (raw instanceof javax.servlet.http.HttpServletResponse) {
                ((javax.servlet.http.HttpServletResponse) raw).setStatus(code);
            }
        } catch (Exception ignored) {
            // response may already be committed; body still carries the magic code
        }
    }

    private static int resolveAuditCode(MagicHttpServletRequest request, Object returnValue, Throwable throwable) {
        Object rejected = request.getAttribute(ATTR_CODE);
        if (rejected instanceof Integer) {
            return (Integer) rejected;
        }
        if (throwable != null) {
            return 500;
        }
        if (returnValue instanceof JsonBean) {
            int code = ((JsonBean<?>) returnValue).getCode();
            if (code == 1) {
                return 200;
            }
            return code > 0 ? code : 500;
        }
        return 200;
    }

    private static Integer rowCount(Object returnValue) {
        Object data = returnValue instanceof JsonBean ? ((JsonBean<?>) returnValue).getData() : returnValue;
        if (data instanceof Collection) {
            return ((Collection<?>) data).size();
        }
        if (data instanceof Object[]) {
            return ((Object[]) data).length;
        }
        return null;
    }

    private static String resolveToken(MagicHttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("magic-token");
        }
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("sessionId");
        }
        if (StringUtils.isBlank(token)) {
            token = bearerToken(request.getHeader("Authorization"));
        }
        return token;
    }

    static String bearerToken(String authorization) {
        if (StringUtils.isBlank(authorization)) {
            return null;
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return StringUtils.trimToNull(value.substring(7));
        }
        return value;
    }

    private boolean canAccessDatasource(int userId, int datasourceId) {
        List<DataSource> authorized =
                dataSourceMapper.listAuthorizedDataSource(userId, new Integer[]{datasourceId});
        return authorized != null && !authorized.isEmpty();
    }

    private static Integer parseDatasourceId(String key) {
        int idx = key.lastIndexOf('_');
        if (idx < 0 || idx == key.length() - 1) {
            return null;
        }
        try {
            return Integer.parseInt(key.substring(idx + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * All datasource keys referenced by the script ({@code db.<key>.*} and {@code db.use('key')}).
     * A script may join across datasources, so every key has to be authorized.
     */
    public static Set<String> extractDatasourceKeys(String script) {
        Set<String> keys = new LinkedHashSet<>();
        if (StringUtils.isBlank(script)) {
            return keys;
        }
        collect(DATASOURCE_CALL, script, keys);
        collect(DATASOURCE_USE, script, keys);
        return keys;
    }

    private static void collect(Pattern pattern, String script, Set<String> keys) {
        Matcher matcher = pattern.matcher(script);
        while (matcher.find()) {
            String key = StringUtils.trimToNull(matcher.group(1));
            if (key != null) {
                keys.add(key);
            }
        }
    }

    /**
     * Heuristic read-only guard: rejects magic-api write methods, row-DSL writes, and any SQL
     * statement whose first keyword is a known write/DDL/DCL verb. String literals and comments
     * are stripped first so that {@code like '%update%'} and {@code select ... for update} pass.
     */
    static boolean containsDml(String script) {
        if (StringUtils.isBlank(script)) {
            return false;
        }
        String normalized = stripLiteralsAndComments(script).toLowerCase(Locale.ROOT);
        if (WRITE_CALL.matcher(normalized).find() || TABLE_WRITE_CALL.matcher(normalized).find()) {
            return true;
        }
        if (normalized.contains("into outfile") || normalized.contains("into dumpfile")) {
            return true;
        }
        for (String statement : normalized.split(";")) {
            String head = firstWord(statement);
            if (head != null && FORBIDDEN_STATEMENT_HEADS.contains(head)) {
                return true;
            }
        }
        return false;
    }

    private static String stripLiteralsAndComments(String script) {
        StringBuilder out = new StringBuilder(script.length());
        int i = 0;
        int n = script.length();
        while (i < n) {
            char c = script.charAt(i);
            if (c == '\'' || c == '"') {
                char quote = c;
                boolean triple = i + 2 < n && script.charAt(i + 1) == quote && script.charAt(i + 2) == quote;
                i += triple ? 3 : 1;
                while (i < n) {
                    char ch = script.charAt(i);
                    if (ch == '\\' && !triple) {
                        i += 2;
                        continue;
                    }
                    if (ch == quote) {
                        if (triple) {
                            if (i + 2 < n && script.charAt(i + 1) == quote && script.charAt(i + 2) == quote) {
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

    private static String firstWord(String statement) {
        int i = 0;
        int n = statement.length();
        while (i < n) {
            char c = statement.charAt(i);
            if (Character.isWhitespace(c) || c == '(') {
                i++;
                continue;
            }
            break;
        }
        int start = i;
        while (i < n && (Character.isLetterOrDigit(statement.charAt(i)) || statement.charAt(i) == '_')) {
            i++;
        }
        if (i == start) {
            return null;
        }
        return statement.substring(start, i);
    }
}
