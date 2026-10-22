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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.AbstractTask;
import org.apache.dolphinscheduler.plugin.task.api.TaskCallBack;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.DataType;
import org.apache.dolphinscheduler.plugin.task.api.enums.Direct;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.dolphinscheduler.plugin.task.api.utils.ParameterUtils;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class AsyncStateTask extends AbstractTask {

    /**
     * output
     */
    protected String output;
    /**
     * http parameters
     */
    private AsyncStateParameters asyncStateParameters;
    /**
     * taskExecutionContext
     */
    private TaskExecutionContext taskExecutionContext;

    /**
     * constructor
     *
     * @param taskExecutionContext taskExecutionContext
     */
    public AsyncStateTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        this.asyncStateParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), AsyncStateParameters.class);
        log.info("Initialize http task params {}", JSONUtils.toPrettyJsonString(asyncStateParameters));

        if (asyncStateParameters == null || !asyncStateParameters.checkParameters()) {
            throw new RuntimeException("http task params is not valid");
        }
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        long startTime = System.currentTimeMillis();
        String formatTimeStamp = DateUtils.formatTimeStamp(startTime);
        String statusCode = null;
        String body = null;
        int failRetryTime = asyncStateParameters.getFailRetryTime();
        int failRetryIntervals = asyncStateParameters.getFailRetryIntervals();
        boolean failRetry = true;

        while (failRetry && failRetryTime >= 0) {
            try (
                    CloseableHttpClient client = createHttpClient();
                    CloseableHttpResponse response = sendRequest(client)) {
                statusCode = String.valueOf(getStatusCode(response));
                body = getResponseBody(response);
                exitStatusCode = validResponse(body, statusCode);
                addDefaultOutput(body);
                long costTime = System.currentTimeMillis() - startTime;
                log.info(
                        "startTime: {}, httpUrl: {}, httpMethod: {}, costTime : {} milliseconds, statusCode : {}, body : {}, log : {}",
                        formatTimeStamp, asyncStateParameters.getUrl(),
                        asyncStateParameters.getAsyncStateMethod(), costTime, statusCode, body, output);
                failRetry = false;
            } catch (Exception e) {
                failRetryTime--;
                log.error("httpUrl[: {} ] connection failed：{}", asyncStateParameters.getUrl(), output, e);
                try {
                    Thread.sleep((long) failRetryIntervals * 60 * 1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    @Override
    public void cancel() throws TaskException {

    }

    /**
     * send request
     *
     * @param client client
     * @return CloseableHttpResponse
     * @throws IOException io exception
     */
    protected CloseableHttpResponse sendRequest(CloseableHttpClient client) throws IOException {
        RequestBuilder builder = createRequestBuilder();

        // replace placeholder,and combine local and global parameters
        Map<String, Property> paramsMap = taskExecutionContext.getPrepareParamsMap();

        List<AsyncStateProperty> asyncStatePropertyList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(asyncStateParameters.getHttpParams())) {
            for (AsyncStateProperty asyncStateProperty : asyncStateParameters.getHttpParams()) {
                String jsonObject = JSONUtils.toJsonString(asyncStateProperty);
                String params =
                        ParameterUtils.convertParameterPlaceholders(jsonObject, ParameterUtils.convert(paramsMap));
                log.info("http request params：{}", params);
                asyncStatePropertyList.add(JSONUtils.parseObject(params, AsyncStateProperty.class));
            }
        }
        String httpBody = ParameterUtils.convertParameterPlaceholders(asyncStateParameters.getHttpBody(),
                ParameterUtils.convert(paramsMap));
        addRequestParams(builder, asyncStatePropertyList, httpBody);
        String requestUrl =
                ParameterUtils.convertParameterPlaceholders(asyncStateParameters.getUrl(), ParameterUtils.convert(paramsMap));
        HttpUriRequest request = builder.setUri(requestUrl).build();
        setHeaders(request, asyncStatePropertyList);
        return client.execute(request);
    }

    /**
     * get response body
     *
     * @param httpResponse http response
     * @return response body
     * @throws ParseException parse exception
     * @throws IOException io exception
     */
    protected String getResponseBody(CloseableHttpResponse httpResponse) throws ParseException, IOException {
        if (httpResponse == null) {
            return null;
        }
        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return null;
        }
        return EntityUtils.toString(entity, StandardCharsets.UTF_8.name());
    }

    /**
     * get status code
     *
     * @param httpResponse http response
     * @return status code
     */
    protected int getStatusCode(CloseableHttpResponse httpResponse) {
        return httpResponse.getStatusLine().getStatusCode();
    }

    /**
     * valid response
     *
     * @param body body
     * @param statusCode status code
     * @return exit status code
     */
    protected int validResponse(String body, String statusCode) {
        int exitStatusCode = 0;
        switch (asyncStateParameters.getAsyncStateCheckCondition()) {
            case BODY_CONTAINS:
                if (StringUtils.isEmpty(body) || !body.contains(asyncStateParameters.getCondition())) {
                    appendMessage(asyncStateParameters.getUrl() + " doesn contain "
                            + asyncStateParameters.getCondition());
                    exitStatusCode = -1;
                }
                break;
            case BODY_NOT_CONTAINS:
                if (StringUtils.isEmpty(body) || body.contains(asyncStateParameters.getCondition())) {
                    appendMessage(asyncStateParameters.getUrl() + " contains "
                            + asyncStateParameters.getCondition());
                    exitStatusCode = -1;
                }
                break;
            case BODY_CONTAINS_JSON_NODE:
                if (!StringUtils.isEmpty(body) && asyncStateParameters.getCondition().contains("=")) {
                    String condition = asyncStateParameters.getCondition();
                    String path = condition.split("=")[0].trim();
                    String value = condition.split("=")[1].trim();
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        JsonNode jsonNode = mapper.readTree(body);
                        if (!Objects.equals(jsonNode.at(path).asText(), value)) {
                            appendMessage(asyncStateParameters.getUrl() + " body jsonpath " + path + " = " + value);
                            exitStatusCode = -1;
                            break;
                        }
                        break;
                    } catch (Exception e) {
                        appendMessage(asyncStateParameters.getUrl() + " body doesn json");
                        exitStatusCode = -1;
                        break;
                    }
                }
                appendMessage(asyncStateParameters.getUrl() + " body is null or "
                        + asyncStateParameters.getCondition() + " is invalided");
                exitStatusCode = -1;
                break;
            case STATUS_CODE_CUSTOM:
                if (!statusCode.equals(asyncStateParameters.getCondition())) {
                    appendMessage(asyncStateParameters.getUrl() + " statuscode: " + statusCode + ", Must be: "
                            + asyncStateParameters.getCondition());
                    exitStatusCode = -1;
                }
                break;
            default:
                if (!"200".equals(statusCode)) {
                    appendMessage(asyncStateParameters.getUrl() + " statuscode: " + statusCode + ", Must be: 200");
                    exitStatusCode = -1;
                }
                break;
        }
        return exitStatusCode;
    }

    public String getOutput() {
        return output;
    }

    /**
     * append message
     *
     * @param message message
     */
    protected void appendMessage(String message) {
        if (output == null) {
            output = "";
        }
        if (message != null && !message.trim().isEmpty()) {
            output += message;
        }
    }

    /**
     * add request params
     *
     * @param builder buidler
     * @param asyncStatePropertyList http property list
     */
    protected void addRequestParams(RequestBuilder builder, List<AsyncStateProperty> asyncStatePropertyList, String httpBody) {
        if (StringUtils.isNotEmpty(httpBody)) {
            builder.setEntity(new StringEntity(
                    httpBody,
                    ContentType.create(ContentType.APPLICATION_JSON.getMimeType(),
                            StandardCharsets.UTF_8)));
        }

        if (CollectionUtils.isNotEmpty(asyncStatePropertyList)) {
            ObjectNode jsonParam = JSONUtils.createObjectNode();
            for (AsyncStateProperty property : asyncStatePropertyList) {
                if (property.getAsyncStateParametersType() != null) {
                    if (property.getAsyncStateParametersType().equals(AsyncStateParametersType.PARAMETER)) {
                        builder.addParameter(property.getProp(), property.getValue());
                    } else if (property.getAsyncStateParametersType().equals(AsyncStateParametersType.BODY)) {
                        jsonParam.put(property.getProp(), property.getValue());
                    }
                }
            }
            if (builder.getEntity() == null) {
                builder.setEntity(new StringEntity(
                        jsonParam.toString(),
                        ContentType.create(ContentType.APPLICATION_JSON.getMimeType(),
                                StandardCharsets.UTF_8)));
            }
        }
    }

    /**
     * set headers
     *
     * @param request request
     * @param asyncStatePropertyList http property list
     */
    protected void setHeaders(HttpUriRequest request, List<AsyncStateProperty> asyncStatePropertyList) {
        if (CollectionUtils.isNotEmpty(asyncStatePropertyList)) {
            for (AsyncStateProperty property : asyncStatePropertyList) {
                if (AsyncStateParametersType.HEADERS.equals(property.getAsyncStateParametersType())) {
                    request.addHeader(property.getProp(), property.getValue());
                }
            }
        }
    }

    /**
     * create http client
     *
     * @return CloseableHttpClient
     */
    protected CloseableHttpClient createHttpClient() {
        final RequestConfig requestConfig = requestConfig();
        HttpClientBuilder httpClientBuilder;
        httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(requestConfig);
        return httpClientBuilder.build();
    }

    /**
     * request config
     *
     * @return RequestConfig
     */
    private RequestConfig requestConfig() {
        return RequestConfig.custom().setSocketTimeout(asyncStateParameters.getSocketTimeout())
                .setConnectTimeout(asyncStateParameters.getConnectTimeout()).build();
    }

    /**
     * create request builder
     *
     * @return RequestBuilder
     */
    protected RequestBuilder createRequestBuilder() {
        if (asyncStateParameters.getAsyncStateMethod().equals(AsyncStateMethod.GET)) {
            return RequestBuilder.get();
        } else if (asyncStateParameters.getAsyncStateMethod().equals(AsyncStateMethod.POST)) {
            return RequestBuilder.post();
        } else if (asyncStateParameters.getAsyncStateMethod().equals(AsyncStateMethod.HEAD)) {
            return RequestBuilder.head();
        } else if (asyncStateParameters.getAsyncStateMethod().equals(AsyncStateMethod.PUT)) {
            return RequestBuilder.put();
        } else if (asyncStateParameters.getAsyncStateMethod().equals(AsyncStateMethod.DELETE)) {
            return RequestBuilder.delete();
        } else {
            return null;
        }

    }

    @Override
    public AbstractParameters getParameters() {
        return this.asyncStateParameters;
    }

    public void addDefaultOutput(String response) {
        // put response in output
        Property outputProperty = new Property();
        outputProperty.setProp(String.format("%s.%s", taskExecutionContext.getTaskName(), "response"));
        outputProperty.setDirect(Direct.OUT);
        outputProperty.setType(DataType.VARCHAR);
        outputProperty.setValue(response);
        asyncStateParameters.addPropertyToValPool(outputProperty);
    }
}
