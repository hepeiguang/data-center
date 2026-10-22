package org.apache.dolphinscheduler.plugin.datasource.kafka.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.CommonUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

import java.util.Map;
import java.util.Properties;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaConnectionParam implements ConnectionParam {
    private String bootstrapServers;
    protected Map<String, String> kafkaConfig;

    public Properties getKafkaProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", this.bootstrapServers);
//        props.put("group.id", this.groupId);
//        props.put("security.protocol", this.securityProtocol);

//        if ("SASL_PLAINTEXT".equals(securityProtocol) || "SASL_SSL".equals(securityProtocol)) {
//            props.put("sasl.mechanism", this.saslMechanism);
//            props.put("sasl.jaas.config", this.saslJaasConfig);
//        }
        if (kafkaConfig != null && !kafkaConfig.isEmpty()) {
            kafkaConfig.entrySet().forEach(entity -> {
                kafkaConfig.put(entity.getKey(), entity.getValue());
            });
        }
        if (!props.containsKey("request.timeout.ms")) {
            props.put(KafkaConfigConstants.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        }
        return props;
    }
}