package org.apache.dolphinscheduler.plugin.datasource.mqtt.param;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@AutoService(DataSourceProcessor.class)
public class MqttDataSourceProcessor extends AbstractDataSourceProcessor {

    private static final String PROTOCOL = "TLSv1.2";

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, MqttDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO datasourceParamDTO) {
        MqttDataSourceParamDTO mqttParam = (MqttDataSourceParamDTO) datasourceParamDTO;
        if (StringUtils.isEmpty(mqttParam.getHost())) {
            throw new IllegalArgumentException("mqtt servers is required");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        MqttConnectionParam mqttParam = (MqttConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}", dbType.getName(),
                mqttParam.getBrokenUrls());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        MqttConnectionParam connectionParams = (MqttConnectionParam) createConnectionParams(connectionJson);
        MqttDataSourceParamDTO mqttParamDTO = new MqttDataSourceParamDTO();

        mqttParamDTO.setHost(connectionParams.getBrokenUrls());
        mqttParamDTO.setUserName(connectionParams.getUserName());
        mqttParamDTO.setPassword(connectionParams.getPassword());
        mqttParamDTO.setConnectionTimeout(connectionParams.getConnectionTimeout());
        mqttParamDTO.setCleanSession(connectionParams.getCleanSession());
        mqttParamDTO.setAutomaticReconnect(connectionParams.getAutomaticReconnect());
        mqttParamDTO.setKeepAliveInterval(connectionParams.getKeepAliveInterval());
        mqttParamDTO.setMaxReconnectDelay(connectionParams.getMaxReconnectDelay());
        mqttParamDTO.setUseSsl(connectionParams.isUseSsl());
        if (connectionParams.isUseSsl()) {
            mqttParamDTO.setSslKeyStore(connectionParams.getSslKeyStore());
            mqttParamDTO.setSslKeyStorePassword(connectionParams.getSslKeyStorePassword());
            mqttParamDTO.setSslTrustStore(connectionParams.getSslTrustStore());
            mqttParamDTO.setSslTrustStorePassword(connectionParams.getSslTrustStorePassword());
        }
        return mqttParamDTO;
    }

    @Override
    public MqttConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        MqttDataSourceParamDTO mqttParam = (MqttDataSourceParamDTO) dataSourceParam;
        MqttConnectionParam mqttConnectionParam = new MqttConnectionParam();

        mqttConnectionParam.setBrokenUrls(mqttParam.getHost());
        mqttConnectionParam.setUserName(mqttParam.getUserName());
        mqttConnectionParam.setPassword(mqttParam.getPassword());
        mqttConnectionParam.setConnectionTimeout(mqttParam.getConnectionTimeout());
        mqttConnectionParam.setCleanSession(mqttParam.getCleanSession());
        mqttConnectionParam.setAutomaticReconnect(mqttParam.getAutomaticReconnect());
        mqttConnectionParam.setKeepAliveInterval(mqttParam.getKeepAliveInterval());
        mqttConnectionParam.setMaxReconnectDelay(mqttParam.getMaxReconnectDelay());
        mqttConnectionParam.setUseSsl(mqttParam.isUseSsl());
        if (mqttParam.isUseSsl()) {
            mqttConnectionParam.setSslKeyStore(mqttParam.getSslKeyStore());
            mqttConnectionParam.setSslKeyStorePassword(mqttParam.getSslKeyStorePassword());
            mqttConnectionParam.setSslTrustStore(mqttParam.getSslTrustStore());
            mqttConnectionParam.setSslTrustStorePassword(mqttParam.getSslTrustStorePassword());
        }
        return mqttConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, MqttConnectionParam.class);
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
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        try {

            MqttConnectionParam mqttParam = (MqttConnectionParam) connectionParam;
            MqttConnectOptions options = new MqttConnectOptions();

            if (mqttParam.getUserName() != null && !mqttParam.getUserName().isEmpty()) {
                options.setUserName(mqttParam.getUserName());
            }
            if (mqttParam.getPassword() != null && !mqttParam.getPassword().isEmpty()) {
                options.setPassword(mqttParam.getPassword().toCharArray());
            }

            // SSL 配置
            if (mqttParam.isUseSsl()) {
                // 1. 创建SSL上下文
                SSLContext sslContext = createSSLContext(mqttParam.getSslTrustStore(), mqttParam.getSslTrustStorePassword(),
                        mqttParam.getSslKeyStore(), mqttParam.getSslKeyStorePassword());
                // 配置SSL
                options.setSocketFactory(sslContext.getSocketFactory());
            }

            String clientId = "client_" + System.currentTimeMillis();
            List<String> breakUrls;
            if (mqttParam.getBrokenUrls().contains(",")) {
                breakUrls = Arrays.asList(mqttParam.getBrokenUrls().split(","));
            } else {
                breakUrls = new ArrayList<>();
                breakUrls.add(mqttParam.getBrokenUrls());
            }
            // 只测试连接，不进行发布订阅
            for (String breakUrl : breakUrls) {
                MqttClient client = new MqttClient(breakUrl, clientId, new MemoryPersistence());

                client.connect(options);

                // 简单的连接测试，不涉及主题操作
                boolean isConnected = client.isConnected();
                if (!isConnected) {
                    log.info("mqtt connection failed brokenUrl:{}", breakUrl);
                    return false;
                }
                // 立即断开连接
                client.disconnect();
                client.close();
            }
            return true;
        } catch (Exception e) {
            log.error("check mqtt connectivity error", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private SSLContext createSSLContext(String trustStorePath,
                                        String trustStorePassword,
                                        String keyStorePath,
                                        String keyStorePassword) throws Exception {

        // 1. 加载信任库（CA证书）
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            trustStore.load(fis, trustStorePassword.toCharArray());
        }

        // 创建信任管理器
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. 加载密钥库（客户端证书）
        KeyManagerFactory kmf = null;
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                keyStore.load(fis, keyStorePassword.toCharArray());
            }

            kmf = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyStorePassword.toCharArray());
        }

        // 3. 创建SSL上下文
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
        sslContext.init(
                kmf != null ? kmf.getKeyManagers() : null,
                tmf.getTrustManagers(),
                new SecureRandom()
        );

        return sslContext;
    }

    @Override
    public DbType getDbType() {
        return DbType.MQTT;
    }

    @Override
    public DataSourceProcessor create() {
        return new MqttDataSourceProcessor();
    }
}