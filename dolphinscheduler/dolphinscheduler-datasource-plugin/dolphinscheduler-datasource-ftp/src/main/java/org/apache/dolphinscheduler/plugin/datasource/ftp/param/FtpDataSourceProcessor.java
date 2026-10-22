package org.apache.dolphinscheduler.plugin.datasource.ftp.param;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.net.ftp.FTPClient;
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
 * Ftp数据源流程类
 *
 * @author ZhaoYuan
 * @date 2025/5/13 19:49
 * @since JDK1.8
 */
@Slf4j
@AutoService(DataSourceProcessor.class)
public class FtpDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, FtpDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        FtpDataSourceParamDTO ftpParamDTO = (FtpDataSourceParamDTO) baseDataSourceParamDTO;
        if (ObjectUtils.isEmpty(ftpParamDTO.getHost())) {
            throw new IllegalArgumentException("Ftp host is required");
        }
        if (!isValidEndpoint(ftpParamDTO.getHost())) {
            throw new IllegalArgumentException("Ftp host invalid");
        }
        if (ftpParamDTO.getPort() <= 0 || ftpParamDTO.getPort() > 65535) {
            throw new IllegalArgumentException("Ftp port must be between 1 and 65535");
        }
        if (ObjectUtils.isEmpty(ftpParamDTO.getUserName())) {
            throw new IllegalArgumentException("Ftp user is required");
        }
        if (ObjectUtils.isEmpty(ftpParamDTO.getPassword())) {
            throw new IllegalArgumentException("Ftp password is required");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        FtpConnectionParam ftpParam = (FtpConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}:{2}", dbType.getName(), ftpParam.getHost(), ftpParam.getPort());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        FtpConnectionParam connectionParams = (FtpConnectionParam) createConnectionParams(connectionJson);

        FtpDataSourceParamDTO ftpParamDTO = new FtpDataSourceParamDTO();
        ftpParamDTO.setHost(connectionParams.getHost());
        ftpParamDTO.setPort(connectionParams.getPort());
        ftpParamDTO.setUserName(connectionParams.getUser());
        ftpParamDTO.setPassword(connectionParams.getPassword());
        return ftpParamDTO;
    }

    @Override
    public FtpConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        FtpDataSourceParamDTO ftpParamDTO = (FtpDataSourceParamDTO) datasourceParam;
        FtpConnectionParam connectionParam = new FtpConnectionParam();
        connectionParam.setHost(ftpParamDTO.getHost());
        connectionParam.setPort(ftpParamDTO.getPort());
        connectionParam.setUser(ftpParamDTO.getUserName());
        connectionParam.setPassword(ftpParamDTO.getPassword());
        return connectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, FtpConnectionParam.class);
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        FtpConnectionParam ftpParam = (FtpConnectionParam) connectionParam;
        FTPClient ftpClient = new FTPClient();
        try {
            //连接服务
            ftpClient.connect(ftpParam.getHost(), ftpParam.getPort());
            //登录
            return ftpClient.login(ftpParam.getUser(), ftpParam.getPassword());
        } catch (Exception e) {
            log.error("Ftp connection test connect/login failed!", e);
            return false;
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (Exception e) {
                    log.error("Ftp connection test logout/disconnect failed!", e);
                }
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
        return DbType.FTP;
    }

    @Override
    public DataSourceProcessor create() {
        return new FtpDataSourceProcessor();
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