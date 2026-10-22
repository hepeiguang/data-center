package org.apache.dolphinscheduler.plugin.datasource.kafka.param;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.kafka.clients.admin.AdminClient;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Set;

@Slf4j
@AutoService(DataSourceProcessor.class)
public class KafkaDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, KafkaDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO datasourceParamDTO) {
        KafkaDataSourceParamDTO kafkaParam = (KafkaDataSourceParamDTO) datasourceParamDTO;
        if (StringUtils.isEmpty(kafkaParam.getHost())) {
            throw new IllegalArgumentException("kafka bootstrap servers is required");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        KafkaConnectionParam kafkaParam = (KafkaConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}", dbType.getName(),
                kafkaParam.getBootstrapServers());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        KafkaConnectionParam connectionParams = (KafkaConnectionParam) createConnectionParams(connectionJson);
        KafkaDataSourceParamDTO kafkaParamDTO = new KafkaDataSourceParamDTO();

        kafkaParamDTO.setHost(connectionParams.getBootstrapServers());
        kafkaParamDTO.setOther(connectionParams.getKafkaConfig());
        return kafkaParamDTO;
    }

    @Override
    public KafkaConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        KafkaDataSourceParamDTO kafkaParam = (KafkaDataSourceParamDTO) dataSourceParam;
        KafkaConnectionParam kafkaConnectionParam = new KafkaConnectionParam();

        kafkaConnectionParam.setBootstrapServers(kafkaParam.getHost());
        kafkaConnectionParam.setKafkaConfig(dataSourceParam.getOther());
        return kafkaConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, KafkaConnectionParam.class);
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
        KafkaConnectionParam kafkaParam = (KafkaConnectionParam) connectionParam;
//        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        try (AdminClient adminClient = createAdminClient(kafkaParam)) {
            Set<String> topics = adminClient.listTopics().names().get();
            log.info("Kafka check connectivity");
            return true;
        } catch (Exception e) {
            log.error("Kafka connection test failed", e);
            return false;
        }
    }

    @Override
    public DbType getDbType() {
        return DbType.KAFKA;
    }

    @Override
    public DataSourceProcessor create() {
        return new KafkaDataSourceProcessor();
    }

    private AdminClient createAdminClient(KafkaConnectionParam kafkaParam) {
        return AdminClient.create(kafkaParam.getKafkaProperties());
    }
}