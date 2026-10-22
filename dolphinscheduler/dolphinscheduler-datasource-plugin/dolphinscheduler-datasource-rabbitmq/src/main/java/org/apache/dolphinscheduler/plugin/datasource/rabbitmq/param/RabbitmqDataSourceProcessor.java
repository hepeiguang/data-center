package org.apache.dolphinscheduler.plugin.datasource.rabbitmq.param;

import com.google.auto.service.AutoService;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 * Ftp数据源流程类
 *
 * @author ZhaoYuan
 * @date 2025/5/13 19:49
 * @since JDK1.8
 */
@Slf4j
@AutoService(DataSourceProcessor.class)
public class RabbitmqDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, RabbitmqDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        RabbitmqDataSourceParamDTO ftpParamDTO = (RabbitmqDataSourceParamDTO) baseDataSourceParamDTO;
        if (ObjectUtils.isEmpty(ftpParamDTO.getHost())) {
            throw new IllegalArgumentException("Rabbitmq host is required");
        }
        if (!isValidEndpoint(ftpParamDTO.getHost())) {
            throw new IllegalArgumentException("Rabbitmq host invalid");
        }
        if (ftpParamDTO.getPort() <= 0 || ftpParamDTO.getPort() > 65535) {
            throw new IllegalArgumentException("Rabbitmq port must be between 1 and 65535");
        }
        if (ObjectUtils.isEmpty(ftpParamDTO.getUserName())) {
            throw new IllegalArgumentException("Rabbitmq user is required");
        }
        if (ObjectUtils.isEmpty(ftpParamDTO.getPassword())) {
            throw new IllegalArgumentException("Rabbitmq password is required");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        RabbitmqConnectionParam ftpParam = (RabbitmqConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}:{2}", dbType.getName(), ftpParam.getHost(), ftpParam.getPort());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        RabbitmqConnectionParam connectionParams = (RabbitmqConnectionParam) createConnectionParams(connectionJson);

        RabbitmqDataSourceParamDTO rabbitmqParamDTO = new RabbitmqDataSourceParamDTO();
        rabbitmqParamDTO.setHost(connectionParams.getHost());
        rabbitmqParamDTO.setPort(connectionParams.getPort());
        rabbitmqParamDTO.setUserName(connectionParams.getUser());
        rabbitmqParamDTO.setPassword(connectionParams.getPassword());
        rabbitmqParamDTO.setVirtualHost(connectionParams.getVirtualHost());
        return rabbitmqParamDTO;
    }

    @Override
    public RabbitmqConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        RabbitmqDataSourceParamDTO rabbitmqDto = (RabbitmqDataSourceParamDTO) datasourceParam;
        RabbitmqConnectionParam connectionParam = new RabbitmqConnectionParam();
        connectionParam.setHost(rabbitmqDto.getHost());
        connectionParam.setPort(rabbitmqDto.getPort());
        connectionParam.setUser(rabbitmqDto.getUserName());
        connectionParam.setPassword(rabbitmqDto.getPassword());
        connectionParam.setVirtualHost(rabbitmqDto.getVirtualHost());
        return connectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, RabbitmqConnectionParam.class);
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        RabbitmqConnectionParam rabbitmqParam = (RabbitmqConnectionParam) connectionParam;
        ConnectionFactory connectionFactory = getConnectionFactory(rabbitmqParam);
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            return connection.isOpen() && channel.isOpen();
        } catch (Exception e) {
            log.error("rabbitmq connection test connect/login failed!", e);
            return false;
        }
    }

    private ConnectionFactory getConnectionFactory(RabbitmqConnectionParam rabbitmqParam) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmqParam.getHost());
        factory.setPort(rabbitmqParam.getPort());
        factory.setUsername(rabbitmqParam.getUser());
        factory.setPassword(rabbitmqParam.getPassword());
        factory.setVirtualHost(rabbitmqParam.getVirtualHost());
        factory.setConnectionTimeout(rabbitmqParam.getConnectionTimeout());
        factory.setRequestedHeartbeat(rabbitmqParam.getRequestedHeartbeat());
        return factory;
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
    public java.sql.Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException, IOException {
        return null;
    }


    @Override
    public DbType getDbType() {
        return DbType.RABBITMQ;
    }

    @Override
    public DataSourceProcessor create() {
        return new RabbitmqDataSourceProcessor();
    }

    /**
     * 判斷是否有效地址
     *
     * @param endpoint 地址
     * @return 返回值
     * @author ZhaoYuan
     * @date 2025/5/13 19:50
     */
    private boolean isValidEndpoint(String endpoint) {
        try {
            InetAddress inetAddress = InetAddress.getByName(endpoint);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}