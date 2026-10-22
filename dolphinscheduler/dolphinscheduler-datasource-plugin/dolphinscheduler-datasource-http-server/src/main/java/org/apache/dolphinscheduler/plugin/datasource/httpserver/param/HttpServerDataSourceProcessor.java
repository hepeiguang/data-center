package org.apache.dolphinscheduler.plugin.datasource.httpserver.param;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@AutoService(DataSourceProcessor.class)
@Slf4j
public class HttpServerDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, HttpServerDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        HttpServerDataSourceParamDTO httpParam = (HttpServerDataSourceParamDTO) baseDataSourceParamDTO;
        if (StringUtils.isEmpty(httpParam.getProtocol())) {
            throw new IllegalArgumentException("HTTP protocol is required");
        }
//        if (StringUtils.isEmpty(httpParam.getHttpMethod())) {
//            throw new IllegalArgumentException("HTTP method is required");
//        }
        if (httpParam.getHost() == null || httpParam.getHost().isEmpty()) {
            throw new IllegalArgumentException("HTTP host must be positive");
        }
        if (httpParam.getConnectTimeout() == null || httpParam.getConnectTimeout() <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        if (httpParam.getSocketTimeout() == null || httpParam.getSocketTimeout() <= 0) {
            throw new IllegalArgumentException("Socket timeout must be positive");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        HttpServerConnectionParam httpParam = (HttpServerConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}", dbType.getName(), httpParam.getHttpEndpoint());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        HttpServerConnectionParam connectionParams = (HttpServerConnectionParam) createConnectionParams(connectionJson);
        HttpServerDataSourceParamDTO httpParamDTO = new HttpServerDataSourceParamDTO();

        httpParamDTO.setProtocol(connectionParams.getProtocol());
        httpParamDTO.setHost(connectionParams.getHost());
        httpParamDTO.setPort(connectionParams.getPort());
        httpParamDTO.setHttpEndpoint(connectionParams.getHttpEndpoint());
        httpParamDTO.setHttpMethod(connectionParams.getHttpMethod());
        httpParamDTO.setHttpAuthType(connectionParams.getHttpAuthType());
        httpParamDTO.setHttpAuthConfig(connectionParams.getHttpAuthConfig());
        httpParamDTO.setConnectTimeout(connectionParams.getConnectTimeout());
        httpParamDTO.setSocketTimeout(connectionParams.getSocketTimeout());

        return httpParamDTO;
    }

    @Override
    public HttpServerConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        HttpServerDataSourceParamDTO httpParam = (HttpServerDataSourceParamDTO) dataSourceParam;
        HttpServerConnectionParam httpConnectionParam = new HttpServerConnectionParam();

        httpConnectionParam.setProtocol(httpParam.getProtocol());
        httpConnectionParam.setHost(httpParam.getHost());
        httpConnectionParam.setPort(httpParam.getPort());
        httpConnectionParam.setHttpEndpoint(httpParam.getHttpEndpoint());
        httpConnectionParam.setHttpMethod(httpParam.getHttpMethod());
        httpConnectionParam.setHttpAuthType(httpParam.getHttpAuthType());
        httpConnectionParam.setHttpAuthConfig(httpParam.getHttpAuthConfig());
        httpConnectionParam.setConnectTimeout(httpParam.getConnectTimeout());
        httpConnectionParam.setSocketTimeout(httpParam.getSocketTimeout());

        return httpConnectionParam;
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        // 1. 参数验证
        if (connectionParam == null) {
            log.error("Connection parameters cannot be null");
            return false;
        }

        HttpServerConnectionParam httpParam = (HttpServerConnectionParam) connectionParam;
        // 2. 关键参数验证
        if (StringUtils.isBlank(httpParam.getHost())) {
            log.error("Host must be specified for connectivity check");
            return false;
        }
        // 3. 设置默认值
        int connectTimeout = httpParam.getConnectTimeout() != null ?
                httpParam.getConnectTimeout() : 5000;
        int socketTimeout = httpParam.getSocketTimeout() != null ?
                httpParam.getSocketTimeout() : 10000;
        if (StringUtils.isEmpty(httpParam.getHttpEndpoint()) || StringUtils.isEmpty(httpParam.getHttpMethod())
                || StringUtils.isEmpty(httpParam.getHttpAuthType())) {
            return checkSocketConnectivity(httpParam.getHost(), httpParam.getPort(), connectTimeout);
        } else {
            // 4. 配置请求
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build()) {
                // 5. 创建请求
                HttpRequestBase request = createValidHttpRequest(httpParam);
                // 6. 添加认证头
                addAuthHeaders(request, httpParam);
                // 7. 执行请求
                return executeRequest(httpClient, request, httpParam);
            } catch (Exception e) {
                log.error("HTTP connectivity check failed for host: {}, endpoint: {}",
                        httpParam.getHost(),
                        httpParam.getHttpEndpoint(),
                        e);
                return false;
            }
        }
    }

    /**
     * 检查IP和端口是否可达
     *
     * @param host    IP地址或域名
     * @param port    端口号
     * @param timeout 超时时间(毫秒)
     * @return true-可达, false-不可达
     */
    public static boolean checkSocketConnectivity(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private HttpRequestBase createValidHttpRequest(HttpServerConnectionParam httpParam) {
        String fullUrl = buildRequestUrl(
                httpParam.getProtocol(),
                httpParam.getHost(),
                httpParam.getPort(),
                StringUtils.defaultIfBlank(httpParam.getHttpEndpoint(), "/")
        );
        String method = StringUtils.defaultString(httpParam.getHttpMethod(), "GET").toUpperCase();
        switch (method) {
            case "GET":
                return new HttpGet(fullUrl);
            case "POST":
                HttpPost post = new HttpPost(fullUrl);
                post.setEntity(new StringEntity("{}", "UTF-8"));
                return post;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    private void addAuthHeaders(HttpRequestBase request, HttpServerConnectionParam httpParam) {
        if (StringUtils.isNotEmpty(httpParam.getHttpAuthType()) &&
                StringUtils.isNotEmpty(httpParam.getHttpAuthConfig())) {
            try {
                Map<String, String> authHeaders = buildAuthHeaders(httpParam);
                authHeaders.forEach(request::addHeader);
            } catch (Exception e) {
                log.warn("Failed to build auth headers, proceeding without authentication", e);
            }
        }
    }

    private boolean executeRequest(CloseableHttpClient httpClient,
                                   HttpRequestBase request,
                                   HttpServerConnectionParam httpParam) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            log.info("HTTP connectivity check - Host: {}, Endpoint: {}, Status: {}",
                    httpParam.getHost(),
                    httpParam.getHttpEndpoint(),
                    statusCode);
            return statusCode >= 200 && statusCode < 300;
        } finally {
            request.releaseConnection();
        }
    }

    // 构造请求头认证信息
    private Map<String, String> buildAuthHeaders(HttpServerConnectionParam httpParam) {
        try {
            Map<String, String> authConfig = JSONUtils.parseObject(httpParam.getHttpAuthConfig(), Map.class);
            String authType = httpParam.getHttpAuthType().toUpperCase();
            Map<String, String> headers = new HashMap<>();
            switch (authType) {
                case "BASIC":
                    String credentials = authConfig.get("credentials");
                    if (StringUtils.isEmpty(credentials)) {
                        throw new IllegalArgumentException("Basic auth requires 'credentials' field");
                    }
                    //String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
                    headers.put("Authorization", "Basic " + credentials);
                    break;
                case "BEARER":
                    String token = authConfig.get("token");
                    if (StringUtils.isEmpty(token)) {
                        throw new IllegalArgumentException("Bearer auth requires 'token' field");
                    }
                    headers.put("Authorization", "Bearer " + token);
                    break;
                case "API_KEY":
                    String headerName = authConfig.get("header_name");
                    String apiKey = authConfig.get("api_key");
                    if (StringUtils.isEmpty(headerName) || StringUtils.isEmpty(apiKey)) {
                        throw new IllegalArgumentException("API_KEY auth requires both 'header_name' and 'api_key' fields");
                    }
                    headers.put(headerName, apiKey);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported auth type: " + authType);
            }
            return headers;
        } catch (Exception e) {
            log.error("Failed to build auth headers", e);
            throw new IllegalArgumentException("Invalid auth configuration: " + e.getMessage());
        }
    }

    // 构造请求路径
    private String buildRequestUrl(String protocol, String host, Integer port, String endpoint) {
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (StringUtils.isEmpty(protocol)) {
            protocol = "http";
        }
        // 处理端口
        String portPart = (port != null && port != 80 && port != 443) ? ":" + port : "";
        // 处理端点路径
        String path = StringUtils.defaultString(endpoint, "");
        path = path.startsWith("/") ? path : "/" + path;
        // 构建完整URL
        return protocol.toLowerCase(Locale.ROOT) + "://" + host + portPart + path;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, HttpServerConnectionParam.class);
    }

    @Override
    public String getDatasourceDriver() {
        return "";
    }

    @Override
    public String getValidationQuery() {
        return "";
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        return "";
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException, IOException {
        return null;
    }

    @Override
    public DbType getDbType() {
        return DbType.HTTP;
    }

    @Override
    public DataSourceProcessor create() {
        return new HttpServerDataSourceProcessor();
    }
}
