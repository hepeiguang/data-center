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

package org.apache.dolphinscheduler.dataapi.security;

import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;
import org.apache.dolphinscheduler.dataapi.governance.ResponseGovernor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.JsonBean;

class SecurityComponentsTest {

    @Test
    void appScopeMatchesExactAndPrefix() {
        AppCredentialService.AppPrincipal exact = new AppCredentialService.AppPrincipal(
                "cms", 100, new LinkedHashSet<>(Arrays.asList("/smoke/select1")));
        Assertions.assertTrue(exact.allows("/smoke/select1"));
        Assertions.assertFalse(exact.allows("/smoke/select2"));

        AppCredentialService.AppPrincipal prefix = new AppCredentialService.AppPrincipal(
                "cms", 100, new LinkedHashSet<>(Arrays.asList("/smoke/**")));
        Assertions.assertTrue(prefix.allows("/smoke/a/b"));
        Assertions.assertFalse(prefix.allows("/other/x"));

        AppCredentialService.AppPrincipal all = new AppCredentialService.AppPrincipal(
                "cms", 100, new LinkedHashSet<>());
        Assertions.assertTrue(all.allows("/anything"));
    }

    @Test
    void tokenBucketHonorsBurstAndRefills() {
        DataApiProperties properties = new DataApiProperties();
        properties.setRateLimitPermitsPerMinute(60);
        properties.setRateLimitBurst(2);
        RateLimitService service = new RateLimitService(properties);

        Assertions.assertTrue(service.tryAcquire("u:1"));
        Assertions.assertTrue(service.tryAcquire("u:1"));
        Assertions.assertFalse(service.tryAcquire("u:1"));
        Assertions.assertTrue(service.tryAcquire("u:2"));

        properties.setRateLimitPermitsPerMinute(0);
        Assertions.assertTrue(service.tryAcquire("u:1"));
    }

    @Test
    void responseGovernorMasksAndCaps() {
        DataApiProperties properties = new DataApiProperties();
        properties.setMaskFields(new LinkedHashSet<>(Arrays.asList("password")));
        properties.setResponseMaxBytes(0);
        ResponseGovernor governor = new ResponseGovernor(properties, new ObjectMapper());

        Map<String, Object> row = new HashMap<>();
        row.put("user", "bob");
        row.put("password", "secret");
        Assertions.assertNull(governor.apply(new JsonBean<>(Arrays.asList(row))));
        Assertions.assertEquals("***", row.get("password"));

        properties.setResponseMaxBytes(10);
        Object result = governor.apply(new JsonBean<>(Arrays.asList(row)));
        Assertions.assertTrue(result instanceof JsonBean);
        Assertions.assertEquals(413, ((JsonBean<?>) result).getCode());
    }
}
