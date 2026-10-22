package org.apache.dolphinscheduler.plugin.datasource.socketserver.param.param;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

@AutoService(DataSourceProcessor.class)
@Slf4j
public class SocketServerDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, SocketServerDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        SocketServerDataSourceParamDTO socketParam = (SocketServerDataSourceParamDTO) baseDataSourceParamDTO;
        if (StringUtils.isEmpty(socketParam.getHost())) {
            throw new IllegalArgumentException("Socket server host is required");
        }
        if (socketParam.getPort() <= 0 || socketParam.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid port number");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        SocketServerConnectionParam socketParam = (SocketServerConnectionParam) connectionParam;
        return String.format("%s@%s:%d", dbType.getName(),
                socketParam.getHost(), socketParam.getPort());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        SocketServerConnectionParam connectionParams = (SocketServerConnectionParam) createConnectionParams(connectionJson);
        SocketServerDataSourceParamDTO socketParamDTO = new SocketServerDataSourceParamDTO();
        socketParamDTO.setHost(connectionParams.getHost());
        socketParamDTO.setPort(connectionParams.getPort());
        socketParamDTO.setMaxConnections(connectionParams.getMaxConnections());
        socketParamDTO.setBufferSize(connectionParams.getBufferSize());
        socketParamDTO.setSocketTimeout(connectionParams.getSocketTimeout());
        return socketParamDTO;
    }

    @Override
    public SocketServerConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        SocketServerDataSourceParamDTO socketParam = (SocketServerDataSourceParamDTO) datasourceParam;
        SocketServerConnectionParam connectionParam = new SocketServerConnectionParam();
        connectionParam.setHost(socketParam.getHost());
        connectionParam.setPort(socketParam.getPort());
        connectionParam.setMaxConnections(socketParam.getMaxConnections());
        connectionParam.setBufferSize(socketParam.getBufferSize());
        connectionParam.setSocketTimeout(socketParam.getSocketTimeout());
        return connectionParam;
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        SocketServerConnectionParam socketParam = (SocketServerConnectionParam) connectionParam;
        try (Socket socket = new Socket()) {
            socket.connect(
                    new java.net.InetSocketAddress(
                            socketParam.getHost(),
                            socketParam.getPort()
                    ),
                    socketParam.getSocketTimeout()
            );
            socket.setSoTimeout(socketParam.getSocketTimeout());
            return socket.isConnected();
        } catch (IOException e) {
            log.error("Socket connection test failed", e);
            return false;
        }
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, SocketServerConnectionParam.class);
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
        return DbType.SOCKET_SERVER;
    }

    @Override
    public DataSourceProcessor create() {
        return new SocketServerDataSourceProcessor();
    }
}
