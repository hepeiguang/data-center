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

package org.apache.dolphinscheduler.plugin.task.asyncstate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dolphinscheduler.common.enums.HttpCheckCondition;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

/**
 * http parameter
 */
public class AsyncStateParametersTest {

    @Test
    public void testJsonPath() {
        String json = "{\"data\": {\"users\": [{\"name\": \"Alice\", \"age\": 25}, {\"name\": \"Bob\"}]}}";
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(json);
            String text = jsonNode.at("/data/users/0/age").asText();
            //boolean data = jsonNode.path("data").isMissingNode();
            System.out.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGenerator() {
        String paramData = "{\"localParams\":[],\"httpParams\":[],\"url\":\"https://www.baidu.com/\","
                + "\"httpMethod\":\"GET\",\"httpCheckCondition\":\"STATUS_CODE_DEFAULT\",\"condition\":\"\",\"connectTimeout\":\"10000\",\"socketTimeout\":\"10000\"}";
        AsyncStateParameters httpParameters = JSONUtils.parseObject(paramData, AsyncStateParameters.class);

        Assertions.assertEquals(10000, httpParameters.getConnectTimeout());
        Assertions.assertEquals(10000, httpParameters.getSocketTimeout());
        Assertions.assertEquals("https://www.baidu.com/", httpParameters.getUrl());
        Assertions.assertEquals(AsyncStateMethod.GET, httpParameters.getAsyncStateMethod());
        Assertions.assertEquals(HttpCheckCondition.STATUS_CODE_DEFAULT, httpParameters.getAsyncStateCheckCondition());
        Assertions.assertEquals("", httpParameters.getCondition());

    }

    @Test
    public void testCheckParameters() {
        String paramData = "{\"localParams\":[],\"httpParams\":[],\"url\":\"https://www.baidu.com/\","
                + "\"httpMethod\":\"GET\",\"httpCheckCondition\":\"STATUS_CODE_DEFAULT\",\"condition\":\"\",\"connectTimeout\":\"10000\",\"socketTimeout\":\"10000\"}";
        AsyncStateParameters httpParameters = JSONUtils.parseObject(paramData, AsyncStateParameters.class);

        Assertions.assertTrue(httpParameters.checkParameters());
        Assertions.assertEquals(10000, httpParameters.getConnectTimeout());
        Assertions.assertEquals(10000, httpParameters.getSocketTimeout());
        Assertions.assertEquals("https://www.baidu.com/", httpParameters.getUrl());
        Assertions.assertEquals(AsyncStateMethod.GET, httpParameters.getAsyncStateMethod());
        Assertions.assertEquals(HttpCheckCondition.STATUS_CODE_DEFAULT, httpParameters.getAsyncStateCheckCondition());
        Assertions.assertEquals("", httpParameters.getCondition());

    }

    @Test
    public void testCheckValues() {
        String paramData = "{\"localParams\":[],\"httpParams\":[],\"url\":\"https://www.baidu.com/\","
                + "\"httpMethod\":\"GET\",\"httpCheckCondition\":\"STATUS_CODE_DEFAULT\",\"condition\":\"\",\"connectTimeout\":\"10000\",\"socketTimeout\":\"10000\"}";
        AsyncStateParameters httpParameters = JSONUtils.parseObject(paramData, AsyncStateParameters.class);

        Assertions.assertTrue(httpParameters.checkParameters());
        Assertions.assertEquals(10000, httpParameters.getConnectTimeout());
        Assertions.assertEquals(10000, httpParameters.getSocketTimeout());
        Assertions.assertEquals("https://www.baidu.com/", httpParameters.getUrl());
        Assertions.assertEquals(AsyncStateMethod.GET, httpParameters.getAsyncStateMethod());
        Assertions.assertEquals(HttpCheckCondition.STATUS_CODE_DEFAULT, httpParameters.getAsyncStateCheckCondition());
        Assertions.assertEquals("", httpParameters.getCondition());
        Assertions.assertEquals(0, httpParameters.getLocalParametersMap().size());
        Assertions.assertEquals(0, httpParameters.getResourceFilesList().size());
    }

}
