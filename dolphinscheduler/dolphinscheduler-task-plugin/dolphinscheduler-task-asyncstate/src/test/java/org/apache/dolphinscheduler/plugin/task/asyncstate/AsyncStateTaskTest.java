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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.DataType;
import org.apache.dolphinscheduler.plugin.task.api.enums.Direct;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_FAILURE;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_SUCCESS;

@ExtendWith(MockitoExtension.class)
public class AsyncStateTaskTest {
    private static final String CONTENT_TYPE = "Content-Type";

    private static final String APPLICATION_JSON_VALUE = "application/json";

    private static final String MOCK_DISPATCH_PATH_REQ_BODY_TO_RES_BODY = "/requestBody/to/responseBody";

    private static final String MOCK_DISPATCH_PATH_REQ_PARAMS_TO_RES_BODY = "/requestParams/to/responseBody";

    private final List<MockWebServer> mockWebServers = new ArrayList<>();

    @AfterEach
    public void after() {
        mockWebServers.forEach(IOUtils::closeQuietly);
        mockWebServers.clear();
    }

    @Test
    public void testHandleCheckCodeDefaultSuccess() throws Exception {
        AsyncStateTask getAsyncStateTask = generateAsyncStateTask(AsyncStateMethod.GET, HttpStatus.SC_OK);
        AsyncStateTask postAsyncStateTask = generateAsyncStateTask(AsyncStateMethod.POST, HttpStatus.SC_OK);
        AsyncStateTask headAsyncStateTask = generateAsyncStateTask(AsyncStateMethod.HEAD, HttpStatus.SC_OK);
        AsyncStateTask putAsyncStateTask = generateAsyncStateTask(AsyncStateMethod.PUT, HttpStatus.SC_OK);
        AsyncStateTask deleteAsyncStateTask = generateAsyncStateTask(AsyncStateMethod.DELETE, HttpStatus.SC_OK);
        getAsyncStateTask.handle(null);
        postAsyncStateTask.handle(null);
        headAsyncStateTask.handle(null);
        putAsyncStateTask.handle(null);
        deleteAsyncStateTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_SUCCESS, getAsyncStateTask.getExitStatusCode());
        Assertions.assertEquals(EXIT_CODE_SUCCESS, postAsyncStateTask.getExitStatusCode());
        Assertions.assertEquals(EXIT_CODE_SUCCESS, headAsyncStateTask.getExitStatusCode());
        Assertions.assertEquals(EXIT_CODE_SUCCESS, putAsyncStateTask.getExitStatusCode());
        Assertions.assertEquals(EXIT_CODE_SUCCESS, deleteAsyncStateTask.getExitStatusCode());
    }

    @Test
    public void testHandleCheckCodeDefaultError() throws Exception {
        AsyncStateTask getAsyncStateTask = generateAsyncStateTask(AsyncStateMethod.GET, HttpStatus.SC_BAD_REQUEST);
        getAsyncStateTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_FAILURE, getAsyncStateTask.getExitStatusCode());
    }

    @Test
    public void testHandleCheckCodeCustom() throws Exception {
        String condition = HttpStatus.SC_CREATED + "";
        AsyncStateTask httpTask = generateAsyncStateTask(AsyncStateMethod.GET, AsyncStateCheckCondition.STATUS_CODE_CUSTOM,
                condition, HttpStatus.SC_CREATED, "");
        AsyncStateTask httpErrorTask = generateAsyncStateTask(AsyncStateMethod.GET, AsyncStateCheckCondition.STATUS_CODE_CUSTOM,
                condition, HttpStatus.SC_OK, "");
        httpTask.handle(null);
        httpErrorTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_SUCCESS, httpTask.getExitStatusCode());
        Assertions.assertEquals(EXIT_CODE_FAILURE, httpErrorTask.getExitStatusCode());
    }

    @Test
    public void testHandleCheckBodyContains() throws Exception {
        AsyncStateTask httpTask = generateAsyncStateTask(AsyncStateMethod.GET, AsyncStateCheckCondition.BODY_CONTAINS,
                "success", HttpStatus.SC_OK, "{\"status\": \"success\"}");
        AsyncStateTask httpErrorTask = generateAsyncStateTask(AsyncStateMethod.GET, AsyncStateCheckCondition.BODY_CONTAINS,
                "success", HttpStatus.SC_OK, "{\"status\": \"failed\"}");
        httpTask.handle(null);
        httpErrorTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_SUCCESS, httpTask.getExitStatusCode());
        Assertions.assertEquals(EXIT_CODE_FAILURE, httpErrorTask.getExitStatusCode());
    }

    @Test
    public void testHandleCheckBodyNotContains() throws Exception {
        AsyncStateTask httpTask = generateAsyncStateTask(AsyncStateMethod.GET, AsyncStateCheckCondition.BODY_NOT_CONTAINS,
                "failed", HttpStatus.SC_OK, "{\"status\": \"success\"}");
        AsyncStateTask httpErrorTask = generateAsyncStateTask(AsyncStateMethod.GET, AsyncStateCheckCondition.BODY_NOT_CONTAINS,
                "failed", HttpStatus.SC_OK, "{\"status\": \"failed\"}");
        httpTask.handle(null);
        httpErrorTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_SUCCESS, httpTask.getExitStatusCode());
        Assertions.assertEquals(EXIT_CODE_FAILURE, httpErrorTask.getExitStatusCode());
    }

    @Test
    public void testHandleWithHttpBodyParams() throws Exception {
        List<AsyncStateProperty> httpParams = new ArrayList<>();
        AsyncStateProperty property = new AsyncStateProperty();
        property.setProp("day");
        property.setValue("${day}");
        property.setAsyncStateParametersType(AsyncStateParametersType.BODY);
        httpParams.add(property);

        Map<String, String> prepareParamsMap = new HashMap<>();
        prepareParamsMap.put("day", "20220812");
        // The MockWebServer will return the request body as response body directly
        // So we just need to check if the response body contains string "20220812"
        AsyncStateTask httpTask = generateAsyncStateTask(MOCK_DISPATCH_PATH_REQ_BODY_TO_RES_BODY, AsyncStateMethod.POST, null,
                httpParams, prepareParamsMap, AsyncStateCheckCondition.BODY_CONTAINS, "20220812",
                HttpStatus.SC_OK, "");
        httpTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_SUCCESS, httpTask.getExitStatusCode());
    }

    @Test
    public void testHandleWithHttpBody() throws Exception {
        String httpBody = "{\"day\": ${day}}";

        Map<String, String> prepareParamsMap = new HashMap<>();
        prepareParamsMap.put("day", "20220812");
        // The MockWebServer will return the request body as response body directly
        // So we just need to check if the response body contains string "20220812"
        AsyncStateTask httpTask = generateAsyncStateTask(MOCK_DISPATCH_PATH_REQ_BODY_TO_RES_BODY, AsyncStateMethod.POST, httpBody,
                null, prepareParamsMap, AsyncStateCheckCondition.BODY_CONTAINS, "20220812",
                HttpStatus.SC_OK, "");
        httpTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_SUCCESS, httpTask.getExitStatusCode());
    }

    @Test
    public void testHandleWithHttpParameterParams() throws Exception {
        List<AsyncStateProperty> httpParams = new ArrayList<>();
        AsyncStateProperty property = new AsyncStateProperty();
        property.setProp("day");
        property.setValue("${day}");
        property.setAsyncStateParametersType(AsyncStateParametersType.PARAMETER);
        httpParams.add(property);

        Map<String, String> prepareParamsMap = new HashMap<>();
        prepareParamsMap.put("day", "20220812");
        // The MockWebServer will return the request parameter as response body directly
        // So we just need to check if the response body contains string "20220812"
        AsyncStateTask httpTask = generateAsyncStateTask(MOCK_DISPATCH_PATH_REQ_PARAMS_TO_RES_BODY, AsyncStateMethod.POST, null,
                httpParams, prepareParamsMap, AsyncStateCheckCondition.BODY_CONTAINS, "20220812",
                HttpStatus.SC_OK, "");
        httpTask.handle(null);
        Assertions.assertEquals(EXIT_CODE_SUCCESS, httpTask.getExitStatusCode());
    }

    @Test
    public void testAddDefaultOutput() throws Exception {
        AsyncStateTask httpTask = generateAsyncStateTask(AsyncStateMethod.GET, HttpStatus.SC_OK);
        AbstractParameters httpParameters = httpTask.getParameters();
        String response = "{\"status\": \"success\"}";
        httpTask.addDefaultOutput(response);

        List<Property> varPool = httpParameters.getVarPool();
        Assertions.assertEquals(1, varPool.size());
        Property property = varPool.get(0);
        Assertions.assertEquals("null.response", property.getProp());
        Assertions.assertEquals(Direct.OUT, property.getDirect());
        Assertions.assertEquals(DataType.VARCHAR, property.getType());
        Assertions.assertEquals(response, property.getValue());
    }

    private String withMockWebServer(String path, int actualResponseCode,
                                     String actualResponseBody) throws IOException {
        MockWebServer server = new MockWebServer();
        mockWebServers.add(server);
        server.start();
        server.setDispatcher(generateMockDispatcher(path, actualResponseCode, actualResponseBody));
        return server.url(path).toString();
    }

    private AsyncStateTask generateAsyncStateTask(AsyncStateMethod httpMethod, int actualResponseCode) throws IOException {
        return generateAsyncStateTask("/test", httpMethod, null, null, null,
                AsyncStateCheckCondition.STATUS_CODE_DEFAULT, "", actualResponseCode, "");
    }

    private AsyncStateTask generateAsyncStateTask(AsyncStateMethod httpMethod, AsyncStateCheckCondition asyncStateCheckConditionType,
                                      String condition, int actualResponseCode,
                                      String actualResponseBody) throws IOException {
        return generateAsyncStateTask("/test", httpMethod, null, null, null,
                asyncStateCheckConditionType, condition, actualResponseCode, actualResponseBody);
    }
    private AsyncStateTask generateAsyncStateTask(String mockPath, AsyncStateMethod httpMethod, String httpBody,
                                      List<AsyncStateProperty> httpParams,
                                      Map<String, String> prepareParamsMap, AsyncStateCheckCondition asyncStateCheckConditionType,
                                      String condition, int actualResponseCode,
                                      String actualResponseBody) throws IOException {
        String url = withMockWebServer(mockPath, actualResponseCode, actualResponseBody);
        String paramData =
                generateAsyncStateParameters(url, httpMethod, httpBody, httpParams, asyncStateCheckConditionType, condition);
        return generateAsyncStateTaskFromParamData(paramData, prepareParamsMap);
    }

    private AsyncStateTask generateAsyncStateTaskFromParamData(String paramData, Map<String, String> prepareParamsMap) {
        TaskExecutionContext taskExecutionContext = Mockito.mock(TaskExecutionContext.class);
        Mockito.when(taskExecutionContext.getTaskParams()).thenReturn(paramData);
        if (prepareParamsMap != null) {
            Map<String, Property> propertyParamsMap = new HashMap<>();
            prepareParamsMap.forEach((k, v) -> {
                Property property = new Property();
                property.setProp(k);
                property.setValue(v);
                propertyParamsMap.put(k, property);
            });
            Mockito.when(taskExecutionContext.getPrepareParamsMap()).thenReturn(propertyParamsMap);
        }
        AsyncStateTask httpTask = new AsyncStateTask(taskExecutionContext);
        httpTask.init();
        return httpTask;
    }

    private String generateAsyncStateParameters(String url, AsyncStateMethod httpMethod, String httpBody,
                                          List<AsyncStateProperty> httpParams,
                                          AsyncStateCheckCondition asyncStateCheckConditionType,
                                          String condition) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        AsyncStateParameters httpParameters = new AsyncStateParameters();
        httpParameters.setUrl(url);
        httpParameters.setAsyncStateMethod(httpMethod);
        httpParameters.setHttpBody(httpBody);
        httpParameters.setAsyncStateCheckCondition(asyncStateCheckConditionType);
        httpParameters.setCondition(condition);
        httpParameters.setConnectTimeout(10000);
        httpParameters.setSocketTimeout(10000);
        httpParameters.setHttpParams(httpParams);
        return mapper.writeValueAsString(httpParameters);
    }

    private Dispatcher generateMockDispatcher(String path, int actualResponseCode, String actualResponseBody) {
        return new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) {
                MockResponse mockResponse = new MockResponse()
                        .setResponseCode(actualResponseCode)
                        .setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);

                if (request.getPath().startsWith(MOCK_DISPATCH_PATH_REQ_BODY_TO_RES_BODY)) {
                    // return request body as mock response body
                    mockResponse.setBody(request.getBody().readUtf8());
                } else if (request.getPath().startsWith(MOCK_DISPATCH_PATH_REQ_PARAMS_TO_RES_BODY)) {
                    // return request params as mock response body
                    ObjectMapper mapper = new ObjectMapper();
                    HttpUrl httpUrl = request.getRequestUrl();
                    Set<String> parameterNames = httpUrl.queryParameterNames();
                    Map<String, String> resBodyMap = new HashMap<>();
                    parameterNames.forEach(name -> resBodyMap.put(name, httpUrl.queryParameter(name)));
                    try {
                        mockResponse.setBody(mapper.writeValueAsString(resBodyMap));
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else if (request.getPath().startsWith(path)) {
                    mockResponse.setBody(actualResponseBody);
                } else {
                    mockResponse.setResponseCode(HttpStatus.SC_NOT_FOUND);
                }
                return mockResponse;
            }
        };
    }
}
