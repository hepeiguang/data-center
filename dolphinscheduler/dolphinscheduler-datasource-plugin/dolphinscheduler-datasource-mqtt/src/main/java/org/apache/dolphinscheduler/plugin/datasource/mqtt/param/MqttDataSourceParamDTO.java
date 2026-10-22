package org.apache.dolphinscheduler.plugin.datasource.mqtt.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

@Data
@EqualsAndHashCode(callSuper = true)
public class MqttDataSourceParamDTO extends BaseDataSourceParamDTO {
    private Boolean cleanSession = true;
    private Integer keepAliveInterval = 60;
    private Integer connectionTimeout = 30;
    private Boolean automaticReconnect = false;
    private Integer maxReconnectDelay = 128000;

    /**
     * 是否使用 SSL。暂时不实现
     */
    private boolean useSsl;
    private String sslKeyStore;
    private String sslKeyStorePassword;
    private String sslTrustStore;
    private String sslTrustStorePassword;
    @Override
    public DbType getType() {
        return DbType.MQTT;
    }
}
