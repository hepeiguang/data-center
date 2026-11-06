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

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Replacement for {@code org.apache.dolphinscheduler.common.utils.EncryptionUtils#getMd5},
 * which is literally {@code DigestUtils.md5Hex}. Must stay byte-compatible with the
 * {@code t_ds_user.user_password} digests written by api-server.
 */
public final class DsDigests {

    private DsDigests() {
    }

    public static String md5(String text) {
        return DigestUtils.md5Hex(text);
    }
}
