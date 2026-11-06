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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DsPasswordCodecTest {

    @BeforeEach
    @AfterEach
    void clearSwitches() {
        System.clearProperty(DsPasswordCodec.ENCRYPTION_ENABLE_KEY);
        System.clearProperty(DsPasswordCodec.ENCRYPTION_SALT_KEY);
    }

    @Test
    void plainModePassesThrough() {
        Assertions.assertEquals("secret", DsPasswordCodec.decodePassword("secret"));
        Assertions.assertEquals("secret", DsPasswordCodec.encodePassword("secret"));
        Assertions.assertEquals("", DsPasswordCodec.decodePassword(null));
    }

    @Test
    void encryptedModeRoundTripsLikeDsPasswordUtils() {
        System.setProperty(DsPasswordCodec.ENCRYPTION_ENABLE_KEY, "true");
        String encoded = DsPasswordCodec.encodePassword("p@ss测试");
        Assertions.assertNotEquals("p@ss测试", encoded);
        Assertions.assertEquals("p@ss测试", DsPasswordCodec.decodePassword(encoded));
    }

    @Test
    void saltMismatchKeepsOriginalValue() {
        System.setProperty(DsPasswordCodec.ENCRYPTION_ENABLE_KEY, "true");
        System.setProperty(DsPasswordCodec.ENCRYPTION_SALT_KEY, "other-salt");
        String encodedWithDefaultSalt = java.util.Base64.getEncoder().encodeToString(
                (DsPasswordCodec.ENCRYPTION_SALT_DEFAULT
                        + java.util.Base64.getEncoder().encodeToString("x".getBytes(
                                java.nio.charset.StandardCharsets.UTF_8)))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Assertions.assertEquals(encodedWithDefaultSalt, DsPasswordCodec.decodePassword(encodedWithDefaultSalt));
    }

    @Test
    void customSaltRoundTrips() {
        System.setProperty(DsPasswordCodec.ENCRYPTION_ENABLE_KEY, "true");
        System.setProperty(DsPasswordCodec.ENCRYPTION_SALT_KEY, "my-salt");
        String encoded = DsPasswordCodec.encodePassword("abc123");
        Assertions.assertEquals("abc123", DsPasswordCodec.decodePassword(encoded));
    }
}
