package org.apache.dolphinscheduler.api.security.impl.dam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
@Configuration
public class DamService {

    private static final Logger logger = LoggerFactory.getLogger(DamService.class);

    @Resource
    private DamAuthConfig damAuthConfig;

    public DamUser damLogin(String userId, String userPwd) {
        // 1. 设置超时策略
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)       // 连接超时（毫秒）
                .setSocketTimeout(10000)       // 请求超时（毫秒）
                .setConnectionRequestTimeout(3000) // 从连接池获取连接的超时（毫秒）
                .build();
        HttpPost post = new HttpPost(damAuthConfig.getLoginUrl());
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        post.addHeader(damAuthConfig.getTokenKey(), damAuthConfig.getLoginAuthorization());
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            post.setEntity(new UrlEncodedFormEntity(getLoginParams(userId, userPwd)));
            CloseableHttpResponse res = httpClient.execute(post);
            String resStr = EntityUtils.toString(res.getEntity());
            res.close();
            if (res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("user name or password error, detail: {}", resStr);
                return null;
            }
            DamTokenAccess token = JSONUtils.parseObject(resStr, DamTokenAccess.class);
            logger.info("Login dam successful, token: {}, username: {}", token.getAccess_token(), token.getUsername());
            return getUserFromDam(httpClient, token.getAccess_token(), token.getUser_id());
        } catch (Exception e) {
            logger.error("Login dam failed, url: {}, detail: ", damAuthConfig.getLoginUrl(), e);
        }
        return null;
    }

    private List<BasicNameValuePair> getLoginParams(String userId, String userPwd) {
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username", userId));
        params.add(new BasicNameValuePair("password", userPwd));
        params.add(new BasicNameValuePair("grant_type", damAuthConfig.getGrantType()));
        params.add(new BasicNameValuePair("scope", damAuthConfig.getScope()));
        return params;
    }

    public DamUser tokenValidation(String token) {
        // 1. 设置超时策略
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)       // 连接超时（毫秒）
                .setSocketTimeout(10000)       // 请求超时（毫秒）
                .setConnectionRequestTimeout(3000) // 从连接池获取连接的超时（毫秒）
                .build();
        HttpGet httpGet = new HttpGet(damAuthConfig.getTokenValidUrl());
        httpGet.addHeader("Content-Type", "application/json");
        httpGet.addHeader(damAuthConfig.getTokenKey(), damAuthConfig.getTokenPrefix() == null ? token : damAuthConfig.getTokenPrefix().concat(token));
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();
             CloseableHttpResponse res = httpClient.execute(httpGet)) {
            String resStr = EntityUtils.toString(res.getEntity());
            res.close();
            if (res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("Valid token failed, detail: {}", resStr);
                return null;
            }
            DamTokenAccess tokenAccess = JSONUtils.parseObject(resStr, DamTokenAccess.class);
            logger.info("Valid token successful, token: {}, username: {}", token, tokenAccess.getUsername());
            return getUserFromDam(httpClient, token, tokenAccess.getUser_id());
        } catch (Exception e) {
            logger.error("Valid token failed, url: {}, detail: ", damAuthConfig.getTokenValidUrl(), e);
        }
        return null;
    }

    private DamUser getUserFromDam(CloseableHttpClient httpClient, String token, String userId) {
        try {
            HttpGet httpGet = new HttpGet(String.format(damAuthConfig.getGetUserUrl(), userId));
            httpGet.addHeader("Content-Type", "application/json");
            httpGet.addHeader(damAuthConfig.getTokenKey(), damAuthConfig.getTokenPrefix() == null ? token : damAuthConfig.getTokenPrefix().concat(token));
            CloseableHttpResponse res = httpClient.execute(httpGet);
            String resStr = EntityUtils.toString(res.getEntity());
            res.close();
            if (res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("Get user from dam failed, detail: {}", resStr);
                return null;
            }
            ObjectNode damUserJson = JSONUtils.parseObject(resStr);
            if (damUserJson.has("code") && damUserJson.get("code").asInt() == HttpStatus.SC_OK) {
                return JSONUtils.parseObject(damUserJson.get("data").toString(), DamUser.class);
            }
        } catch (IOException | ParseException e) {
            logger.error("Get dam user failed, url: {}, detail: ", damAuthConfig.getGetUserUrl(), e);
            throw new RuntimeException(e);
        }
        return null;
    }


    /***
     * get user type by configured admin userId
     * @param userId login userId
     * @return user type
     */
    public UserType getUserType(String userId) {
        return damAuthConfig.getAdminUserId().equalsIgnoreCase(userId) ? UserType.ADMIN_USER : UserType.GENERAL_USER;
    }

    public static CloseableHttpClient createHttpsClient() {

        CloseableHttpClient httpClient = null;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            //4.3版本之前用这个
            //SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(
            //builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            //4.3版本之后用这个
            SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(builder.build(),
                    new String[]{"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3"}, null, new NoopHostnameVerifier());


            Registry<ConnectionSocketFactory> registry = RegistryBuilder.
                    <ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", sslcsf)
                    .build();
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(2000);

            httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslcsf)
                    .setConnectionManager(cm)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            logger.error("Init http client failed, ", e);
            throw new RuntimeException("Init http client failed, detail: " + e.getMessage());
        }
        return httpClient;
    }
}
