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

package org.apache.dolphinscheduler.dataapi.acl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataApiAclPathMatchTest {

    @Test
    void exactMatch() {
        Assertions.assertTrue(DataApiAclService.pathMatches("/smoke/select1", "/smoke/select1"));
        Assertions.assertFalse(DataApiAclService.pathMatches("/smoke/select1", "/smoke/select2"));
    }

    @Test
    void recursiveWildcard() {
        Assertions.assertTrue(DataApiAclService.pathMatches("/smoke/**", "/smoke/select1"));
        Assertions.assertTrue(DataApiAclService.pathMatches("/smoke/**", "/smoke/a/b"));
        Assertions.assertFalse(DataApiAclService.pathMatches("/smoke/**", "/other/x"));
    }

    @Test
    void stripsGatewayPrefix() {
        Assertions.assertEquals("/smoke/select1",
                DataApiAclService.extractGatewayRelativePath("/data-api/gateway/smoke/select1"));
        Assertions.assertEquals("/smoke/select1",
                DataApiAclService.normalizePathPattern("data-api/gateway/smoke/select1"));
    }
}
