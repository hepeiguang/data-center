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

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Byte-compatible port of DS {@code PasswordUtils} (base64 + salt), so passwords written by
 * api-server can be decoded here. Switch and salt must match the running DS configuration;
 * resolution order is JVM system property, then environment variable
 * ({@code DATASOURCE_ENCRYPTION_ENABLE} / {@code DATASOURCE_ENCRYPTION_SALT}).
 */
@Slf4j
public final class DsPasswordCodec {

    public static final String ENCRYPTION_ENABLE_KEY = "datasource.encryption.enable";
    public static final String ENCRYPTION_SALT_KEY = "datasource.encryption.salt";
    public static final String ENCRYPTION_SALT_DEFAULT = "!@#$%^&*";

    private static final Base64 BASE64 = new Base64();

    private DsPasswordCodec() {
    }

    public static String decodePassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return StringUtils.EMPTY;
        }
        if (!encryptionEnabled()) {
            return password;
        }
        String salt = salt();
        String passwordWithSalt = new String(BASE64.decode(password), StandardCharsets.UTF_8);
        if (!passwordWithSalt.startsWith(salt)) {
            log.warn("There is a password and salt mismatch");
            return password;
        }
        return new String(BASE64.decode(passwordWithSalt.substring(salt.length())), StandardCharsets.UTF_8);
    }

    public static String encodePassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return StringUtils.EMPTY;
        }
        if (!encryptionEnabled()) {
            return password;
        }
        String passwordWithSalt = salt() + new String(BASE64.encode(password.getBytes(StandardCharsets.UTF_8)));
        return new String(BASE64.encode(passwordWithSalt.getBytes(StandardCharsets.UTF_8)));
    }

    private static boolean encryptionEnabled() {
        return Boolean.parseBoolean(setting(ENCRYPTION_ENABLE_KEY, "false"));
    }

    private static String salt() {
        return setting(ENCRYPTION_SALT_KEY, ENCRYPTION_SALT_DEFAULT);
    }

    private static String setting(String dottedKey, String defaultValue) {
        String envKey = dottedKey.replace('.', '_').toLowerCase();
        String value = System.getProperty(dottedKey);
        if (StringUtils.isBlank(value)) {
            value = System.getenv(envKey.toUpperCase());
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv(envKey);
        }
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
}
