package org.apache.dolphinscheduler.plugin.datasource.mqtt.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

import java.util.Map;
import java.util.Properties;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MqttConnectionParam implements ConnectionParam {
    private String brokenUrls;
    private String userName;
    private String password;
    private Boolean cleanSession ;
    private Integer keepAliveInterval;
    private Integer connectionTimeout;
    private Boolean automaticReconnect;
    private Integer maxReconnectDelay;

    /**
     * 是否使用 SSL。暂时不实现
     */
    private boolean useSsl;
    private String sslKeyStore;
    private String sslKeyStorePassword;
    private String sslTrustStore;
    private String sslTrustStorePassword;
}