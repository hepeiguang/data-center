package org.apache.dolphinscheduler.plugin.datasource.sftp.param;

import com.google.auto.service.AutoService;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 * Sftp数据源流程类
 *
 * @author ZhaoYuan
 * @date 2025/5/13 20:05
 * @since JDK1.8
 */
@Slf4j
@AutoService(DataSourceProcessor.class)
public class SftpDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, SftpDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        SftpDataSourceParamDTO sftpParamDTO = (SftpDataSourceParamDTO) baseDataSourceParamDTO;
        if (ObjectUtils.isEmpty(sftpParamDTO.getHost())) {
            throw new IllegalArgumentException("Sftp host is required");
        }
        if (!isValidEndpoint(sftpParamDTO.getHost())) {
            throw new IllegalArgumentException("Sftp host invalid");
        }
        if (sftpParamDTO.getPort() <= 0 || sftpParamDTO.getPort() > 65535) {
            throw new IllegalArgumentException("Sftp port must be between 1 and 65535");
        }
        if (ObjectUtils.isEmpty(sftpParamDTO.getUserName())) {
            throw new IllegalArgumentException("Sftp user is required");
        }
        if (ObjectUtils.isEmpty(sftpParamDTO.getPassword())) {
            throw new IllegalArgumentException("Sftp password is required");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        SftpConnectionParam sftpParam = (SftpConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}:{2}", dbType.getName(), sftpParam.getHost(), sftpParam.getPort());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        SftpConnectionParam connectionParams = (SftpConnectionParam) createConnectionParams(connectionJson);

        SftpDataSourceParamDTO sftpParamDTO = new SftpDataSourceParamDTO();
        sftpParamDTO.setHost(connectionParams.getHost());
        sftpParamDTO.setPort(connectionParams.getPort());
        sftpParamDTO.setUserName(connectionParams.getUser());
        sftpParamDTO.setPassword(connectionParams.getPassword());
        return sftpParamDTO;
    }

    @Override
    public SftpConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        SftpDataSourceParamDTO sftpParamDTO = (SftpDataSourceParamDTO) datasourceParam;
        SftpConnectionParam connectionParam = new SftpConnectionParam();
        connectionParam.setHost(sftpParamDTO.getHost());
        connectionParam.setPort(sftpParamDTO.getPort());
        connectionParam.setUser(sftpParamDTO.getUserName());
        connectionParam.setPassword(sftpParamDTO.getPassword());
        return connectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, SftpConnectionParam.class);
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        SftpConnectionParam sftpParam = (SftpConnectionParam) connectionParam;
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            //获取会话
            session = jsch.getSession(sftpParam.getUser(), sftpParam.getHost(), sftpParam.getPort());
            //登录
            session.setPassword(sftpParam.getPassword());
            //配置参数
            session.setConfig("StrictHostKeyChecking", "no");
            //连接
            session.connect();
            //打开sftp通道
            Channel channel = session.openChannel("sftp");
            //连接通道
            channel.connect();
            //转化
            channelSftp = (ChannelSftp) channel;
            return true;
        } catch (Exception e) {
            log.error("Sftp connection test failed!", e);
            return false;
        } finally {
            if (ObjectUtils.isNotEmpty(channelSftp)) {
                channelSftp.disconnect();
            }
            if (ObjectUtils.isNotEmpty(channelSftp)) {
                session.disconnect();
            }
        }
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
        return DbType.SFTP;
    }

    @Override
    public DataSourceProcessor create() {
        return new SftpDataSourceProcessor();
    }

    /**
     * 判斷是否有效地址
     *
     * @author ZhaoYuan
     * @date 2025/5/13 19:50
     * @param endpoint 地址
     * @return 返回值
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