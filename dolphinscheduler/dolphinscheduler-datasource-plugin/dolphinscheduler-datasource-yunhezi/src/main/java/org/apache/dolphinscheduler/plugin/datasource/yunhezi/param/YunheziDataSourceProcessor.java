package org.apache.dolphinscheduler.plugin.datasource.yunhezi.param;

import com.google.auto.service.AutoService;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.dolphinscheduler.spi.enums.YunheziVersionType;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Locale;

@AutoService(DataSourceProcessor.class)
@Slf4j
public class YunheziDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, YunheziDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        YunheziDataSourceParamDTO httpParam = (YunheziDataSourceParamDTO) baseDataSourceParamDTO;
        if (StringUtils.isEmpty(httpParam.getAccount())) {
            throw new IllegalArgumentException("HTTP account is required");
        }
        if (StringUtils.isEmpty(httpParam.getAppId())) {
            throw new IllegalArgumentException("HTTP appId is required");
        }
        if (StringUtils.isEmpty(httpParam.getAppSecret())) {
            throw new IllegalArgumentException("HTTP appSecret is required");
        }
        if (StringUtils.isEmpty(httpParam.getProtocol())) {
            throw new IllegalArgumentException("HTTP Protocol is required");
        }
        if (StringUtils.isEmpty(httpParam.getVersion())) {
            throw new IllegalArgumentException("HTTP version is required");
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
        YunheziConnectionParam httpParam = (YunheziConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}", dbType.getName(), httpParam.getHost());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        YunheziConnectionParam connectionParams = (YunheziConnectionParam) createConnectionParams(connectionJson);
        YunheziDataSourceParamDTO httpParamDTO = new YunheziDataSourceParamDTO();

        httpParamDTO.setHost(connectionParams.getHost());
        httpParamDTO.setPort(connectionParams.getPort());
        httpParamDTO.setConnectTimeout(connectionParams.getConnectTimeout());
        httpParamDTO.setSocketTimeout(connectionParams.getSocketTimeout());
        httpParamDTO.setAccount(connectionParams.getAccount());
        httpParamDTO.setAppId(connectionParams.getAppId());
        httpParamDTO.setAppSecret(connectionParams.getAppSecret());
        httpParamDTO.setVersion(connectionParams.getVersion());
        httpParamDTO.setProtocol(connectionParams.getProtocol());

        return httpParamDTO;
    }

    @Override
    public YunheziConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        YunheziDataSourceParamDTO httpParam = (YunheziDataSourceParamDTO) dataSourceParam;
        YunheziConnectionParam httpConnectionParam = new YunheziConnectionParam();

        httpConnectionParam.setHost(httpParam.getHost());
        httpConnectionParam.setPort(httpParam.getPort());
        httpConnectionParam.setConnectTimeout(httpParam.getConnectTimeout());
        httpConnectionParam.setSocketTimeout(httpParam.getSocketTimeout());
        httpConnectionParam.setAccount(httpParam.getAccount());
        httpConnectionParam.setAppId(httpParam.getAppId());
        httpConnectionParam.setAppSecret(httpParam.getAppSecret());
        httpConnectionParam.setVersion(httpParam.getVersion());
        httpConnectionParam.setProtocol(httpParam.getProtocol());


        return httpConnectionParam;
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        // 1. 参数验证
        if (connectionParam == null) {
            log.error("Connection parameters cannot be null");
            return false;
        }

        YunheziConnectionParam httpParam = (YunheziConnectionParam) connectionParam;
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

        // 4. 配置请求
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build()) {
            // 5. 创建请求
            YunheziVersionType versionType = YunheziVersionType.getByName(httpParam.getVersion());
            HttpRequestBase request = createValidHttpRequest(httpParam, versionType);
            // 7. 执行请求
            return executeRequest(httpClient, request, httpParam, versionType);
        } catch (Exception e) {
            log.error("HTTP connectivity check failed for host: {}",
                    httpParam.getHost(),
                    e);
            return false;
        }

    }

    /**
     * 生成签名
     *
     * @param appId     应用ID
     * @param account   账号
     * @param timestamp 时间戳（毫秒）
     * @param appSecret 应用密钥
     * @return MD5签名字符串（32位小写）
     */
    public String generateSignature(String appId, String account, long timestamp, String appSecret) {
        // 拼接字符串
        String content = appId + account + timestamp + appSecret;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 字节数组转16进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private HttpRequestBase createValidHttpRequest(YunheziConnectionParam httpParam, YunheziVersionType versionType) {
        String tokenUrl = versionType.getTokenUrl();
        String fullUrl = buildRequestUrl(
                httpParam.getProtocol(),
                httpParam.getHost(),
                httpParam.getPort(),
                StringUtils.defaultIfBlank(tokenUrl, "/")
        );
        Long timestamp = System.currentTimeMillis();
        String tokenBody = fullUrl + "?appId=" + httpParam.getAppId() +
                "&account=" + httpParam.getAccount() +
                "&timestamp=" + timestamp +
                "&sign=" + generateSignature(httpParam.getAppId(), httpParam.getAccount(), timestamp, httpParam.getAppSecret());

        log.error("=======================tokenBody: {}", tokenBody);
        HttpPost post = new HttpPost(tokenBody);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
//        post.setEntity(new StringEntity("{}", "UTF-8"));
        return post;

    }

    private boolean executeRequest(CloseableHttpClient httpClient,
                                   HttpRequestBase request,
                                   YunheziConnectionParam httpParam,
                                   YunheziVersionType versionType) throws Exception {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("获取云盒子返回的值: {}", responseBody);

                // 直接获取 accessToken
                Integer isSuccess = JsonPath.read(responseBody, versionType.getResultCondition());
                log.info("HTTP connectivity check - Host: {}, Status: {}, resultCondition:{}",
                        httpParam.getHost(),
                        statusCode, isSuccess);
                if (-1 == isSuccess) {
                    return true;
                } else if (-3 == isSuccess) {
                    throw new RuntimeException("账号不存在");
                } else if (-4 == isSuccess) {
                    throw new RuntimeException("没有权限");
                } else if (-2 == isSuccess) {
                    throw new RuntimeException("失败");
                }
            }
//
            log.info("HTTP connectivity check - Host: {}, Status: {}",
                    httpParam.getHost(),
                    statusCode);
            return false;
        } finally {
            request.releaseConnection();
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
        return JSONUtils.parseObject(connectionJson, YunheziConnectionParam.class);
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
        return DbType.YUNHEZI;
    }

    @Override
    public DataSourceProcessor create() {
        return new YunheziDataSourceProcessor();
    }
}
