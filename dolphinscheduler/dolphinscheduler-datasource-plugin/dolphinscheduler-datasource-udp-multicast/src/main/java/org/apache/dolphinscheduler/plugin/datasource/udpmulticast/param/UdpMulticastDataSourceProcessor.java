package org.apache.dolphinscheduler.plugin.datasource.udpmulticast.param;

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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

@AutoService(DataSourceProcessor.class)
@Slf4j
public class UdpMulticastDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, UdpMulticastDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        UdpMulticastDataSourceParamDTO udpParam = (UdpMulticastDataSourceParamDTO) baseDataSourceParamDTO;
        if (StringUtils.isEmpty(udpParam.getMulticastAddress())) {
            throw new IllegalArgumentException("Multicast address is required");
        }
        if (!isValidMulticastAddress(udpParam.getMulticastAddress())) {
            throw new IllegalArgumentException("Invalid multicast address");
        }
        if (udpParam.getPort() <= 0 || udpParam.getPort() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (udpParam.getConnectTimeout() <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        if (udpParam.getBufferSize() <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
    }

    private boolean isValidMulticastAddress(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            return inetAddress.isMulticastAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        UdpMulticastConnectionParam udpParam = (UdpMulticastConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}:{2}", dbType.getName(),
                udpParam.getMulticastAddress(), udpParam.getPort());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        UdpMulticastConnectionParam connectionParams = (UdpMulticastConnectionParam) createConnectionParams(connectionJson);
        UdpMulticastDataSourceParamDTO udpMulticastDataSourceParamDTO = new UdpMulticastDataSourceParamDTO();
        udpMulticastDataSourceParamDTO.setMulticastAddress(connectionParams.getMulticastAddress());
        udpMulticastDataSourceParamDTO.setPort(connectionParams.getPort());
        udpMulticastDataSourceParamDTO.setUdpMulticastType(connectionParams.getUdpMulticastType());
        udpMulticastDataSourceParamDTO.setConnectTimeout(connectionParams.getConnectTimeout());
        udpMulticastDataSourceParamDTO.setBufferSize(connectionParams.getBufferSize());
        udpMulticastDataSourceParamDTO.setOther(connectionParams.getOther());
        return udpMulticastDataSourceParamDTO;
    }

    @Override
    public UdpMulticastConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        UdpMulticastDataSourceParamDTO udpParam = (UdpMulticastDataSourceParamDTO) datasourceParam;
        UdpMulticastConnectionParam udpConnectionParam = new UdpMulticastConnectionParam();
        udpConnectionParam.setMulticastAddress(udpParam.getMulticastAddress());
        udpConnectionParam.setUdpMulticastType(udpParam.getUdpMulticastType());
        udpConnectionParam.setPort(udpParam.getPort());
        udpConnectionParam.setConnectTimeout(udpParam.getConnectTimeout());
        udpConnectionParam.setBufferSize(udpParam.getBufferSize());
        if (udpParam.getOther() != null && !udpParam.getOther().isEmpty()) {
            udpConnectionParam.setOther(udpParam.getOther());
        }
        return udpConnectionParam;
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        UdpMulticastConnectionParam udpParam = (UdpMulticastConnectionParam) connectionParam;

        try (MulticastSocket socket = new MulticastSocket(udpParam.getPort())) {
            InetAddress group = InetAddress.getByName(udpParam.getMulticastAddress());
            socket.joinGroup(group);
            socket.setSoTimeout(udpParam.getConnectTimeout());

            // Test packet
            byte[] testData = "CONNECTIVITY_TEST".getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(
                    testData, testData.length,
                    group, udpParam.getPort()
            );
            socket.send(packet);
            return true;
        } catch (IOException e) {
            log.error("UDP multicast connectivity check failed", e);
            return false;
        }
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, UdpMulticastConnectionParam.class);
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
        return DbType.UDP_MULTICAST;
    }

    @Override
    public DataSourceProcessor create() {
        return new UdpMulticastDataSourceProcessor();
    }
}
