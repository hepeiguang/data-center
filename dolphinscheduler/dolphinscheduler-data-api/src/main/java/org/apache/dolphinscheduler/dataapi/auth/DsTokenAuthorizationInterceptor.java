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
import org.apache.dolphinscheduler.common.utils.EncryptionUtils;
import org.apache.dolphinscheduler.dao.entity.Session;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.repository.SessionDao;
import org.apache.dolphinscheduler.dao.repository.UserDao;
import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;

import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;

/**
 * magic-api console auth backed by DolphinScheduler users / access tokens / UI sessionId
 * (same metadata DB as api-server).
 */
@Component
public class DsTokenAuthorizationInterceptor implements AuthorizationInterceptor {

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final DataApiProperties properties;

    /** Ephemeral tokens issued by username/password login (not persisted, bounded TTL). */
    private final Map<String, TokenEntry> sessionTokens = new ConcurrentHashMap<>();

    public DsTokenAuthorizationInterceptor(UserDao userDao, SessionDao sessionDao, DataApiProperties properties) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.properties = properties;
    }

    @Override
    public boolean requireLogin() {
        return true;
    }

    @Override
    public MagicUser getUserByToken(String token) throws MagicLoginException {
        User user = resolveDsUser(token);
        if (user == null) {
            throw new MagicLoginException("token invalid or expired");
        }
        assertEnabled(user);
        return toMagicUser(user, token);
    }

    @Override
    public MagicUser login(String username, String password) throws MagicLoginException {
        User user = userDao.queryUserByNamePassword(username, EncryptionUtils.getMd5(password));
        if (user == null) {
            throw new MagicLoginException("username or password incorrect");
        }
        assertEnabled(user);
        evictIfNeeded();
        String token = "da-session-" + UUID.randomUUID();
        long ttl = Math.max(1, properties.getConsoleTokenTtlMinutes());
        sessionTokens.put(token, new TokenEntry(user, System.currentTimeMillis() + ttl * 60_000L));
        return toMagicUser(user, token);
    }

    private void evictIfNeeded() {
        long now = System.currentTimeMillis();
        sessionTokens.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        int max = Math.max(1, properties.getConsoleTokenMax());
        if (sessionTokens.size() < max) {
            return;
        }
        sessionTokens.entrySet().stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().expiresAt))
                .ifPresent(entry -> sessionTokens.remove(entry.getKey()));
    }

    public void logout(String token) {
        sessionTokens.remove(token);
    }

    /**
     * Resolve DS user from: ephemeral magic session → access token → UI sessionId.
     */
    public User resolveDsUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenEntry sessionEntry = sessionTokens.get(token);
        if (sessionEntry != null) {
            if (sessionEntry.expiresAt > System.currentTimeMillis()) {
                return sessionEntry.user;
            }
            sessionTokens.remove(token);
            return null;
        }
        User byAccessToken = userDao.queryUserByToken(token, new Date());
        if (byAccessToken != null) {
            return byAccessToken;
        }
        Session session = sessionDao.queryById(token);
        if (session == null) {
            return null;
        }
        return userDao.queryById(session.getUserId());
    }

    private void assertEnabled(User user) throws MagicLoginException {
        if (user.getState() == Flag.NO.ordinal()) {
            throw new MagicLoginException("user disabled");
        }
    }

    private MagicUser toMagicUser(User user, String token) {
        return new MagicUser(String.valueOf(user.getId()), user.getUserName(), token);
    }

    private static final class TokenEntry {

        private final User user;
        private final long expiresAt;

        private TokenEntry(User user, long expiresAt) {
            this.user = user;
            this.expiresAt = expiresAt;
        }
    }
}
