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

import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskConstants;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Optional integration test against a running OpenMetadata instance.
 * Run with OM_TOKEN (and optionally OM_URL/OM_FROM_FQN/OM_TO_FQN) set.
 */
class OpenMetadataTaskRealTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "OM_TOKEN", matches = ".+")
    void pushesLineageToRealOpenMetadata() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("openMetadataUrl", System.getenv().getOrDefault("OM_URL", "http://127.0.0.1:8585"));
        params.put("token", System.getenv("OM_TOKEN"));
        params.put("fromFqn", System.getenv().getOrDefault("OM_FROM_FQN",
                "agri_satellite_pg.agri_sat.public.dim_satellite"));
        params.put("toFqn", System.getenv().getOrDefault("OM_TO_FQN",
                "agri_satellite_pg.agri_sat.public.ods_satellite_scene"));
        params.put("description", "DolphinScheduler OPENMETADATA plugin integration test");
        params.put("sql", "insert into ods_satellite_scene (scene_id, satellite_id) "
                + "select scene_id, satellite_id from dim_satellite");

        TaskExecutionContext context = new TaskExecutionContext();
        context.setTaskParams(JSONUtils.toJsonString(params));
        OpenMetadataTask task = new OpenMetadataTask(context);
        task.init();
        task.handle(null);
        Assertions.assertEquals(TaskConstants.EXIT_CODE_SUCCESS, task.getExitStatusCode());
    }
}
