package org.apache.dolphinscheduler.plugin.task.flexiblehttp;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONUtil;
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
import org.apache.dolphinscheduler.plugin.task.http.HttpMethod;
import org.apache.dolphinscheduler.plugin.task.http.HttpParameters;
import org.apache.dolphinscheduler.plugin.task.http.HttpParametersType;
import org.apache.dolphinscheduler.plugin.task.http.HttpProperty;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class FlexibleHttpTask extends AbstractTask {

    /**
     * output
     */
    protected String output;
    private FlexibleHttpParameters flexibleHttpParameters;
    private TaskExecutionContext taskExecutionContext;

    public FlexibleHttpTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        this.flexibleHttpParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), FlexibleHttpParameters.class);
        log.info("Initialize flexible http task params {}", JSONUtils.toPrettyJsonString(flexibleHttpParameters));

        if (flexibleHttpParameters == null || !flexibleHttpParameters.checkParameters()) {
            throw new RuntimeException("flexible http task params is not valid");
        }

    }

    private void processExpressionExtension() {
        if(flexibleHttpParameters == null || !flexibleHttpParameters.hasExpressionConfig()){
            return;
        }

        Map<String, Property> contextParams = taskExecutionContext.getPrepareParamsMap();
        FlexibleHttpExpressionProcessor processor = new FlexibleHttpExpressionProcessor(
                flexibleHttpParameters.getExpressionConfig(),
                contextParams
        );
        log.info("有表达式，需要处理表达式 >> {}", flexibleHttpParameters.getExpressionConfig());

        processor.process(flexibleHttpParameters);

    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        long startTime = System.currentTimeMillis();
        String formatTimeStamp = DateUtils.formatTimeStamp(startTime);
        String statusCode = null;
        String body = null;
        String responseHeaders = null;
        List<String> allRespones = new ArrayList<>();
        try {
            //循环请求
            if (flexibleHttpParameters.getFlexibleHttpTaskType() == FlexibleHttpTaskType.LOOP_REQUEST) {
                int loopCount = 0;
                boolean conditionMet = false;

                while (loopCount < flexibleHttpParameters.getMaxLoopNum() && !conditionMet) {
                    LocalDateTime executionTime = LocalDateTime.now();
                    try (CloseableHttpClient client = createHttpClient();
                         CloseableHttpResponse response = sendRequest(client)) {
                        LocalDateTime executionEndTime = LocalDateTime.now();
                        statusCode = String.valueOf(getStatusCode(response));//获取状态码
                        body = getResponseBody(response);//获取响应体
                        exitStatusCode = validResponse(body, statusCode);
                        logHttp(statusCode, body, executionTime, executionEndTime);
                        responseHeaders = getResponseHeaders(response);

                        // check loop condition
                        try {
                            Object actualValue = JsonPath.read(body, flexibleHttpParameters.getLoopConditionField());
                            String actualValueStr = String.valueOf(actualValue);
                            String loopConditionValue = flexibleHttpParameters.getLoopConditionValue();
                            String[] loopConditionValues = loopConditionValue.split(",");
                            List<String> loopConditionValueList = Arrays.asList(loopConditionValues);
                            if (actualValue != null && loopConditionValueList.contains(actualValueStr)) {
                                conditionMet = true;
                                appendMessage("Loop condition met after " + (loopCount + 1) + " attempts");
                            } else {
                                appendMessage((loopCount + 1) +" iteration, Loop condition not met, attempting again...");
                                log.info("Loop condition not met, attempting again...");
                                Thread.sleep(flexibleHttpParameters.getLoopInterval());
                            }
                        } catch (Exception e) {
                            appendMessage("Failed to check loop condition, attempting again...");
                            Thread.sleep(flexibleHttpParameters.getLoopInterval());
                        }
                    }
                    loopCount++;
                }
                if (!conditionMet) {
                    appendMessage("Max loop times reached, condition not met");
                    exitStatusCode = -1;
                }

            }

            //普通请求
            if (flexibleHttpParameters.getFlexibleHttpTaskType() == FlexibleHttpTaskType.SIMPLE_REQUEST) {
                // 普通HTTP请求
                LocalDateTime executionTime = LocalDateTime.now();
                try (CloseableHttpClient client = createHttpClient();
                     CloseableHttpResponse response = sendRequest(client)) {
                    LocalDateTime executionEndTime = LocalDateTime.now();
                    statusCode = String.valueOf(getStatusCode(response));
                    body = getResponseBody(response);
                    responseHeaders = getResponseHeaders(response);
                    exitStatusCode = validResponse(body, statusCode);
                    logHttp(statusCode, body, executionTime, executionEndTime);
                }
            }

            //遍历请求
            if(flexibleHttpParameters.getFlexibleHttpTaskType() == FlexibleHttpTaskType.ITERATE_REQUEST) {
                Map<String, Property> paramsMap = taskExecutionContext.getPrepareParamsMap();
                String inputParamsJson = paramsMap.get(flexibleHttpParameters.getIterateParam()).getValue();
                List<Map<String, String>>items = JsonPath.read(inputParamsJson, flexibleHttpParameters.getIterateParamPath());
                log.info("批量请求次数：{}",items.size());
                log.info("批量数据：{}",items);

                //批量原始参数保存一份
                String originUrl = flexibleHttpParameters.getUrl();
                String originHttpBody = flexibleHttpParameters.getHttpBody();
                List<HttpProperty> originHttpParams = new ArrayList<>();
                originHttpParams.addAll(flexibleHttpParameters.getHttpParams());
                for(Map<String, String> item : items) {
                    //replaceBatchPlaceholders(item);
                    //参数 从批量里获取
                    for (HttpProperty httpProperty : flexibleHttpParameters.getHttpParams()) {
                        String jsonObject = JSONUtils.toJsonString(httpProperty);
                        String params =
                                ParameterUtils.convertParameterPlaceholders(jsonObject, item);
                        HttpProperty newHttpProperty = JSONUtils.parseObject(params, HttpProperty.class);
                        httpProperty.setValue(newHttpProperty.getValue());
                    }

                    //body 从批量参数中获取
                    String httpBody = ParameterUtils.convertParameterPlaceholders(flexibleHttpParameters.getHttpBody(),
                            item);

                    //路径批量参数替换
                    String requestUrl =
                            ParameterUtils.convertParameterPlaceholders(flexibleHttpParameters.getUrl(), item);
                    flexibleHttpParameters.setUrl(requestUrl);

                    flexibleHttpParameters.setHttpBody(httpBody);
                    boolean flag = true;
                    LocalDateTime executionTime = LocalDateTime.now();
                    try (CloseableHttpClient client = createHttpClient();
                         CloseableHttpResponse response = sendRequest(client)) {
                        LocalDateTime executionEndTime = LocalDateTime.now();
                        statusCode = String.valueOf(getStatusCode(response));
                        body = getResponseBody(response);
                        responseHeaders = getResponseHeaders(response);
                        exitStatusCode = validResponse(body, statusCode);
                        logHttp(statusCode, body, executionTime, executionEndTime);
                        //只要有一个请求没成功，批量请求就没成功
                        if(exitStatusCode == -1) {
                            flag = false;
                        }
                        allRespones.add(body);
                    }

                    if(!flag) {
                        appendMessage("批量请求有请求没成功！");
                        exitStatusCode = -1;
                    }

                    //每次执行完成后，将参数替换成原始参数
                    flexibleHttpParameters.setUrl(originUrl);
                    flexibleHttpParameters.setHttpBody(originHttpBody);
                    flexibleHttpParameters.setHttpParams(originHttpParams);
                }



            }

            // 提取输出参数并存储到任务上下文中
            if(flexibleHttpParameters.getFlexibleHttpTaskType() == FlexibleHttpTaskType.ITERATE_REQUEST){
                if (flexibleHttpParameters.getOutputParameters() != null && !flexibleHttpParameters.getOutputParameters().isEmpty()) {
                    extractAndStoreOutputParameters(allRespones);
                    extractAndStoreOutputParameters(responseHeaders);
                } else {
                    // 如果未定义特定输出参数，则存储整个响应内容
                    addDefaultOutput(allRespones);
                    addDefaultHeadersOutput(responseHeaders);
                }
            } else {
                if (flexibleHttpParameters.getOutputParameters() != null && !flexibleHttpParameters.getOutputParameters().isEmpty()) {
                    extractAndStoreOutputParameters(body);
                    extractAndStoreOutputParameters(responseHeaders);
                } else {
                    // 如果未定义特定输出参数，则存储整个响应内容
                    addDefaultOutput(body);
                    addDefaultHeadersOutput(responseHeaders);
                }
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info(
                    "startTime: {}, httpUrl: {}, httpMethod: {}, costTime : {} milliseconds, statusCode : {}, body : {}, log : {}",
                    formatTimeStamp, flexibleHttpParameters.getUrl(),
                    flexibleHttpParameters.getHttpMethod(), costTime, statusCode, body, output);
        } catch (Exception e) {
            appendMessage(e.toString());
            exitStatusCode = -1;
            log.error("httpUrl[" + flexibleHttpParameters.getUrl() + "] connection failed：" + output, e);
            throw new TaskException("Execute flexible http task failed", e);
        }
    }

    private void logHttp(String stateCode, String body, LocalDateTime executionTime, LocalDateTime executionEndTime) {
        if(flexibleHttpParameters.getLogUrl() != null && flexibleHttpParameters.getIsLog()) {
            log.info("需要记录接口调用日志：logUrl：{}", flexibleHttpParameters.getLogUrl());
            try{
                ExecutorService executorService = Executors.newFixedThreadPool(2);
                Map<String, Property> prepareParamsMap = taskExecutionContext.getPrepareParamsMap();
                Map<String, String> map = ParameterUtils.convert(prepareParamsMap);
                executorService.submit(() -> {
                    Map<String, Object> logInfo = new HashMap<>();
                    String opeName = taskExecutionContext.getTaskName();
                    String taskId = map.get("taskId");
                    Integer logType = 2;
                    String reqUrl = flexibleHttpParameters.getUrl();
                    logInfo.put("opeName", opeName);
                    logInfo.put("taskId", taskId);
                    logInfo.put("logType", logType);
                    logInfo.put("reqUrl", reqUrl);
                    /**
                     * 记录请求参数分为httpParameters|body
                     */
                    List<HttpProperty> httpParams = flexibleHttpParameters.getHttpParams();
                    Map<String, Object> opeContent = new HashMap<>();
                    opeContent.put("httpParams", httpParams);
                    opeContent.put("body", flexibleHttpParameters.getHttpBody());
                    logInfo.put("opeContent", JSONUtils.toJsonString(opeContent));
                    logInfo.put("resultCode", stateCode);
                    logInfo.put("resultData", body);
                    if(exitStatusCode == 0) {
                        logInfo.put("resultStatus", 0);
                    }else {
                        logInfo.put("resultStatus", 1);
                    }
                    logInfo.put("executionTime", executionTime);
                    logInfo.put("executionEndTime", executionEndTime);
                    try {
                        CloseableHttpClient client = createHttpClient();
                        RequestBuilder builder = RequestBuilder.post();
                        builder.setEntity(new StringEntity(
                                JSONUtils.toJsonString(logInfo),
                                ContentType.create(ContentType.APPLICATION_JSON.getMimeType(),
                                        StandardCharsets.UTF_8)));
                        HttpUriRequest request = builder.setUri(flexibleHttpParameters.getLogUrl()).build();
                        CloseableHttpResponse response = client.execute(request);
                        String responseBody = getResponseBody(response);
                        log.info("调用记录日志接口情况：{}", responseBody);

                    } catch (Exception e) {
                        log.error("logUrl 记录日志出错！ logUrl:{}", flexibleHttpParameters.getLogUrl());
                    }

                });
            }catch (Exception e) {
                log.error("logUrl 记录日志出错！ logUrl:{}", flexibleHttpParameters.getLogUrl());
            }
        }
    }


    /**
     * 批量请求 动态替换
     * @param item
     */
    private void replaceBatchPlaceholders(Map<String, Object> item) {
        if(flexibleHttpParameters.getHttpParams() != null) {
            for(HttpProperty param : flexibleHttpParameters.getHttpParams()) {
                String value = JSONUtils.toJsonString(item.get(param.getProp()));
                param.setValue(value);
            }
        }

        //TODO body
//        if(flexibleHttpParameters.getHttpBody() != null) {
//            JSONUtils.
//        }
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
        //处理扩展表达式
        processExpressionExtension();
        RequestBuilder builder = createRequestBuilder();
        // replace placeholder,and combine local and global parameters
        Map<String, Property> paramsMap = taskExecutionContext.getPrepareParamsMap();

        List<HttpProperty> httpPropertyList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(flexibleHttpParameters.getHttpParams())) {
            for (HttpProperty httpProperty : flexibleHttpParameters.getHttpParams()) {
                String jsonObject = JSONUtils.toJsonString(httpProperty);
                String params =
                        ParameterUtils.convertParameterPlaceholders(jsonObject, ParameterUtils.convert(paramsMap));
                log.info("http request params：{}", params);
                httpPropertyList.add(JSONUtils.parseObject(params, HttpProperty.class));
            }
        }
        log.info("httpPropertyList : {}", httpPropertyList);
        //设置替换后的参数
        flexibleHttpParameters.setHttpParams(httpPropertyList);
        String httpBody = ParameterUtils.convertParameterPlaceholders(flexibleHttpParameters.getHttpBody(),
                ParameterUtils.convert(paramsMap));
        //设置替换后的Body
        flexibleHttpParameters.setHttpBody(httpBody);
        log.info("http request body：{}", httpBody);
        addRequestParams(builder, httpPropertyList, httpBody);
        String requestUrl =
                ParameterUtils.convertParameterPlaceholders(flexibleHttpParameters.getUrl(), ParameterUtils.convert(paramsMap));
        HttpUriRequest request = builder.setUri(requestUrl).build();
        setHeaders(request, httpPropertyList);
        List<Header> headers = Arrays.asList(request.getAllHeaders());
        log.info("http request headers：{}", headers);
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

    private String getResponseHeaders(CloseableHttpResponse httpResponse) {
        if (httpResponse == null) {
            return null;
        }

        Map<String, Object> headers = new HashMap<>();
        Header[] allHeaders = httpResponse.getAllHeaders();
        for(Header header:allHeaders) {
            String name = header.getName();
            String value = header.getValue();
            headers.put(name, value);
        }
        return JSONUtils.toJsonString(headers);
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
        switch (flexibleHttpParameters.getHttpCheckCondition()) {
            case BODY_CONTAINS:
                if (StringUtils.isEmpty(body) || !body.contains(flexibleHttpParameters.getCondition())) {
                    appendMessage(flexibleHttpParameters.getUrl() + " doesn contain "
                            + flexibleHttpParameters.getCondition());
                    exitStatusCode = -1;
                }
                break;
            case BODY_NOT_CONTAINS:
                if (StringUtils.isEmpty(body) || body.contains(flexibleHttpParameters.getCondition())) {
                    appendMessage(flexibleHttpParameters.getUrl() + " contains "
                            + flexibleHttpParameters.getCondition());
                    exitStatusCode = -1;
                }
                break;
            case STATUS_CODE_CUSTOM:
                if (!statusCode.equals(flexibleHttpParameters.getCondition())) {
                    appendMessage(flexibleHttpParameters.getUrl() + " statuscode: " + statusCode + ", Must be: "
                            + flexibleHttpParameters.getCondition());
                    exitStatusCode = -1;
                }
                break;
            default:
                if (!"200".equals(statusCode)) {
                    appendMessage(flexibleHttpParameters.getUrl() + " statuscode: " + statusCode + ", Must be: 200");
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
     * @param httpPropertyList http property list
     */
    protected void addRequestParams(RequestBuilder builder, List<HttpProperty> httpPropertyList, String httpBody) {
        if (StringUtils.isNotEmpty(httpBody)) {
            builder.setEntity(new StringEntity(
                    httpBody,
                    ContentType.create(ContentType.APPLICATION_JSON.getMimeType(),
                            StandardCharsets.UTF_8)));
        }

        if (CollectionUtils.isNotEmpty(httpPropertyList)) {
            ObjectNode jsonParam = JSONUtils.createObjectNode();
            for (HttpProperty property : httpPropertyList) {
                if (property.getHttpParametersType() != null) {
                    if (property.getHttpParametersType().equals(HttpParametersType.PARAMETER)) {
                        builder.addParameter(property.getProp(), property.getValue());
                    } else if (property.getHttpParametersType().equals(HttpParametersType.BODY)) {
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
     * @param httpPropertyList http property list
     */
    protected void setHeaders(HttpUriRequest request, List<HttpProperty> httpPropertyList) {
        if (CollectionUtils.isNotEmpty(httpPropertyList)) {
            for (HttpProperty property : httpPropertyList) {
                if (HttpParametersType.HEADERS.equals(property.getHttpParametersType())) {
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
    protected CloseableHttpClient createHttpClient() throws Exception {
        final RequestConfig requestConfig = requestConfig();
        HttpClientBuilder httpClientBuilder;
        httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(requestConfig);

        //配置SSL
        SSLContext sslContext;
        if(flexibleHttpParameters.isTrustAllCerts()) {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, ((chain, authType) -> true))
                    .build();
        }else {
            sslContext = SSLContexts.createDefault();
        }

        httpClientBuilder.setSSLContext(sslContext);
        if(StringUtils.isNotEmpty(flexibleHttpParameters.getSslProtocol())) {
            httpClientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(
                    sslContext,
                    flexibleHttpParameters.getCipherSuites(),
                    null,
                    NoopHostnameVerifier.INSTANCE
            ));
        }else {
            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        return httpClientBuilder.build();
    }

    /**
     * request config
     *
     * @return RequestConfig
     */
    private RequestConfig requestConfig() {
        return RequestConfig.custom().setSocketTimeout(flexibleHttpParameters.getSocketTimeout())
                .setConnectTimeout(flexibleHttpParameters.getConnectTimeout()).build();
    }

    /**
     * create request builder
     *
     * @return RequestBuilder
     */
    protected RequestBuilder createRequestBuilder() {
        if (flexibleHttpParameters.getHttpMethod().equals(HttpMethod.GET)) {
            return RequestBuilder.get();
        } else if (flexibleHttpParameters.getHttpMethod().equals(HttpMethod.POST)) {
            return RequestBuilder.post();
        } else if (flexibleHttpParameters.getHttpMethod().equals(HttpMethod.HEAD)) {
            return RequestBuilder.head();
        } else if (flexibleHttpParameters.getHttpMethod().equals(HttpMethod.PUT)) {
            return RequestBuilder.put();
        } else if (flexibleHttpParameters.getHttpMethod().equals(HttpMethod.DELETE)) {
            return RequestBuilder.delete();
        } else if (flexibleHttpParameters.getHttpMethod().equals(HttpMethod.PATCH)){
            return RequestBuilder.patch();
        } else {
            return null;
        }

    }

    @Override
    public AbstractParameters getParameters() {
        return this.flexibleHttpParameters;
    }

    /**
     * 批量指定请求参数输出
     * @param allResponse
     */
    private void extractAndStoreOutputParameters(List<String> allResponse) {
        for (FlexibleHttpOutputParameter param : flexibleHttpParameters.getOutputParameters()) {
            log.info("BATCH HTTP response body: {}", allResponse);
            List<Object> values = new ArrayList<>();
            for(String response : allResponse) {
                try {
                    Object value = JsonPath.read(response, param.getJsonPath());
                    values.add(value);
                }catch (Exception e) {
                    log.error("输出参数，获取输出参数失败! {} 》》{}", response, param.getJsonPath());
                }

            }
            if(values.size() != 0) {
                Property outputProperty = new Property();
//            outputProperty.setProp(String.format("%s.%s", taskExecutionContext.getTaskName(), param.getName()));
                outputProperty.setProp(String.format("%s.%s", flexibleHttpParameters.getAlias(), param.getName()));
                outputProperty.setDirect(Direct.OUT);
                outputProperty.setType(DataType.LIST);
                outputProperty.setValue(JSONUtils.toJsonString(values));
                flexibleHttpParameters.addPropertyToValPool(outputProperty);
                log.info("输出参数:{}",outputProperty);
            }
        }
    }

    /**
     * 制定输出参数值
     * @param response
     */
    private void extractAndStoreOutputParameters(String response) {
        try {
            for (FlexibleHttpOutputParameter param : flexibleHttpParameters.getOutputParameters()) {
                log.info("HTTP response body: {}", response);
                Object value=null;
                try {
                    value = JsonPath.read(response, param.getJsonPath());
                }catch (Exception e) {
                    log.error("输出参数，获取输出参数失败! {} 》》{}", response, param.getJsonPath());
                }
                log.info("JsonPath 读取参数:{} >> {}", value, param.getJsonPath());
                if(value != null) {
                    Property outputProperty = new Property();
//                outputProperty.setProp(String.format("%s.%s", taskExecutionContext.getTaskName(), param.getName()));
                    outputProperty.setProp(String.format("%s.%s", flexibleHttpParameters.getAlias(), param.getName()));
                    outputProperty.setDirect(Direct.OUT);
                    outputProperty.setType(DataType.VARCHAR);
                    if(value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Float || value instanceof Boolean){
                        outputProperty.setValue(value.toString());
                    }else {
                        outputProperty.setValue(JSONUtils.toJsonString(value));
                    }
                    flexibleHttpParameters.addPropertyToValPool(outputProperty);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract output parameters from response", e);
        }
    }

    public static void main(String[] args) {
        JsonPath.read("{\"Server\":\"api-gateway\",\"X-Request-Id\":\"a563be0b0a388372d7de0ea47fd0f4a1\",\"X-Content-Type-Options\":\"nosniff\",\"Connection\":\"keep-alive\",\"X-Download-Options\":\"noopen\",\"X-Subject-Token\":\"MIIErgYJKoZIhvcNAQcCoIIEnzCCBJsCAQExDTALBglghkgBZQMEAgEwggJ8BgkqhkiG9w0BBwGgggJtBIICaXsidG9rZW4iOnsiZXhwaXJlc19hdCI6IjIwMjUtMDYtMjNUMTQ6Mjg6MDkuNDg1MDAwWiIsIm1ldGhvZHMiOlsicGFzc3dvcmQiXSwiY2F0YWxvZyI6W10sInJvbGVzIjpbeyJuYW1lIjoidmRjX2FkbSIsImlkIjoiMCJ9LHsibmFtZSI6InZkY19vd25lciIsImlkIjoiMCJ9LHsibmFtZSI6InRhZ19hZG0iLCJpZCI6IjAifSx7Im5hbWUiOiJhcHByb3ZfYWRtIiwiaWQiOiIwIn0seyJuYW1lIjoidGVfYWRtaW4iLCJpZCI6IjAifV0sInByb2plY3QiOnsiZG9tYWluIjp7Im5hbWUiOiJodHpoLW1vbml0b3IiLCJpZCI6IjFmMzEzMjhkNmMyNzQzMWU4N2Y0ODVhMDdhYmNjN2VjIn0sIm5hbWUiOiJjYXN0LWlzYXNlLTFfaHR6aC1tb25pdG9yIiwiaWQiOiIwMThiOWU5MmI1Y2M0ZTRjODkzZDQ2MmYxNWE0NjEyNCJ9LCJpc3N1ZWRfYXQiOiIyMDI1LTA2LTIyVDE0OjI4OjA5LjQ4NTAwMFoiLCJ1c2VyIjp7ImRvbWFpbiI6eyJuYW1lIjoiaHR6aC1tb25pdG9yIiwiaWQiOiIxZjMxMzI4ZDZjMjc0MzFlODdmNDg1YTA3YWJjYzdlYyJ9LCJuYW1lIjoiaHR6aC1tb25pdG9yIiwicGFzc3dvcmRfZXhwaXJlc19hdCI6IiIsImlkIjoiMmZkMzg5ZDVmZDhmNGMxODk2NzUxZWY2MTlkMTFhZGYifX19MYICBTCCAgECAQEwXDBWMQswCQYDVQQGEwJDTjELMAkGA1UECAwCc2MxCzAJBgNVBAcMAmNkMQswCQYDVQQKDAJIVzEQMA4GA1UECwwHQ2xvdWRCVTEOMAwGA1UEAwwFdG9rZW4CAhAAMAsGCWCGSAFlAwQCATANBgkqhkiG9w0BAQEFAASCAYCKPgMv5VPee5MnKoHF5wjUfWcxjZfhPwkp03j8Dj5bbWvY2ZHmhofDpHWz3KU4dquC1MoBimcq9j6XZhnR8SSsGeJtQzemw00TX9UrrK4remD4MxhOBVFRMUy2SQzDym+1OJ19P4qLaXAcKnnpO6ARWiK463evjfyKTf9cMuKUAK+V4ZdUSCiZDWPkjZ6Q89BTP781ITqJUC62AtyymUWNXhTQhREfIN8AJhtGW3H-+yA+BS3DnQSZcSpCkxr74lEC+06ZNXT-EVXXHdEfppxI+X2JUTDaiHLTSLEIIaJacqMA+RHAPMi9vwH63Tqh-7E6LnZkz3H6OF5SsJz9Ib6yDiQdRmXA6t+UUvGGjc9Oy+dcmsT5wJ5sMbaRi6YD7bWCKNbiexlKU+bb8dnvnaAevEA8camegL82pAHh8Sa7Yd1tpXduh+o-o5Mhezi6CJnowta0GPRN+8SbdBxuFnmb3cYj8JSm3jK9yGz4v0eeepceWvgpxIRTTpZ1KhfZY2Q=\",\"Date\":\"Sun, 22 Jun 2025 14:28:09 GMT\",\"X-Frame-Options\":\"SAMEORIGIN\",\"Strict-Transport-Security\":\"max-age=31536000; includeSubdomains;\",\"Content-Length\":\"6351\",\"X-XSS-Protection\":\"1; mode=block;\",\"X-IAM-Trace-Id\":\"token_cast-isase-1_null_baf9f05a2dd677fa024a15d2f5006944\",\"Content-Type\":\"application/json; charset=UTF-8\"}", "$.X-Subject-Token");
        isJson("11111");
    }

    public static boolean isJson(String str){
        JsonElement jsonElement = JsonParser.parseString(str);
        if(jsonElement == null) {
            return false;
        }else {
            return true;
        }
    }

    /**
     * 输出参数
     * @param response
     */
    public void addDefaultOutput(String response) {
        // put response in output
        Property outputProperty = new Property();
//        outputProperty.setProp(String.format("%s.%s", taskExecutionContext.getTaskName(), "response"));
        outputProperty.setProp(String.format("%s.%s", flexibleHttpParameters.getAlias(), "response"));
        outputProperty.setDirect(Direct.OUT);
        outputProperty.setType(DataType.VARCHAR);
        outputProperty.setValue(response);
        flexibleHttpParameters.addPropertyToValPool(outputProperty);
        log.info("输出参数:{}",outputProperty);
    }

    /**
     *
     * 批量请求输出参数
     * @param allResponse
     */
    public void addDefaultOutput(List<String> allResponse) {
        // put response in output
        Property outputProperty = new Property();
//        outputProperty.setProp(String.format("%s.%s", taskExecutionContext.getTaskName(), "response"));
        outputProperty.setProp(String.format("%s.%s", flexibleHttpParameters.getAlias(), "response"));
        outputProperty.setDirect(Direct.OUT);
        outputProperty.setType(DataType.LIST);
        outputProperty.setValue(JSONUtils.toJsonString(allResponse));
        flexibleHttpParameters.addPropertyToValPool(outputProperty);
        log.info("输出参数:{}",outputProperty);
    }

    private void addDefaultHeadersOutput(String responseHeaders) {
        Property outputProperty = new Property();
//        outputProperty.setProp(String.format("%s.%s", taskExecutionContext.getTaskName(), "response"));
        outputProperty.setProp(String.format("%s.%s", flexibleHttpParameters.getAlias(), "headers"));
        outputProperty.setDirect(Direct.OUT);
        outputProperty.setType(DataType.LIST);
        outputProperty.setValue(JSONUtils.toJsonString(responseHeaders));
        flexibleHttpParameters.addPropertyToValPool(outputProperty);
        log.info("输出响应头参数:{}",outputProperty);
    }


}