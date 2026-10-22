package org.apache.dolphinscheduler.plugin.datasource.minio.param;

import com.google.auto.service.AutoService;
import io.minio.MinioClient;
import io.minio.messages.Bucket;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minio数据源流程类
 *
 * @author ZhaoYuan
 * @date 2025/5/10 11:02
 * @since JDK1.8
 */
@Slf4j
@AutoService(DataSourceProcessor.class)
public class MinioDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, MinioDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        MinioDataSourceParamDTO minioParamDTO = (MinioDataSourceParamDTO) baseDataSourceParamDTO;
        if (ObjectUtils.isEmpty(minioParamDTO.getHost())) {
            throw new IllegalArgumentException("Minio endpoint is required");
        }
        if (!isValidEndpoint(minioParamDTO.getHost())) {
            throw new IllegalArgumentException("Minio endpoint invalid");
        }
        if (minioParamDTO.getPort() <= 0 || minioParamDTO.getPort() > 65535) {
            throw new IllegalArgumentException("Minio port must be between 1 and 65535");
        }
        if (ObjectUtils.isEmpty(minioParamDTO.getUserName())) {
            throw new IllegalArgumentException("Minio accessKey is required");
        }
        if (ObjectUtils.isEmpty(minioParamDTO.getPassword())) {
            throw new IllegalArgumentException("Minio secretKey is required");
        }
        if (ObjectUtils.isEmpty(minioParamDTO.getSecure())) {
            throw new IllegalArgumentException("Minio secure is required");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        MinioConnectionParam minioParam = (MinioConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}:{2}", dbType.getName(), minioParam.getEndpoint(), minioParam.getPort());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        MinioConnectionParam connectionParams = (MinioConnectionParam) createConnectionParams(connectionJson);

        MinioDataSourceParamDTO minioParamDTO = new MinioDataSourceParamDTO();
        minioParamDTO.setHost(connectionParams.getEndpoint());
        minioParamDTO.setPort(connectionParams.getPort());
        minioParamDTO.setUserName(connectionParams.getAccessKey());
        minioParamDTO.setPassword(connectionParams.getPassword());
        minioParamDTO.setSecure(connectionParams.getSecure());
        return minioParamDTO;
    }

    @Override
    public MinioConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        MinioDataSourceParamDTO minioParamDTO = (MinioDataSourceParamDTO) datasourceParam;
        MinioConnectionParam connectionParam = new MinioConnectionParam();
        connectionParam.setEndpoint(minioParamDTO.getHost());
        connectionParam.setPort(minioParamDTO.getPort());
        connectionParam.setAccessKey(minioParamDTO.getUserName());
        connectionParam.setSecretKey(minioParamDTO.getPassword());
        connectionParam.setSecure(minioParamDTO.getSecure());
        return connectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, MinioConnectionParam.class);
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        MinioConnectionParam minioParam = (MinioConnectionParam) connectionParam;
        try {
            MinioClient client = new MinioClient(minioParam.getEndpoint(), minioParam.getPort(), minioParam.getAccessKey(), minioParam.getSecretKey(), minioParam.getSecure());
            List<Bucket> bucketList = client.listBuckets();
            for (Bucket bucket : bucketList) {
                log.info("Minio bucket: {}", bucket.name());
            }
            return true;
        } catch (Exception e) {
            log.error("Minio connection test failed", e);
            return false;
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
        return DbType.MINIO;
    }

    @Override
    public DataSourceProcessor create() {
        return new MinioDataSourceProcessor();
    }

    @Override
    public List<String> getItems(ConnectionParam connectionParam) {
        MinioConnectionParam minioParam = (MinioConnectionParam) connectionParam;
        try {
            MinioClient client = new MinioClient(minioParam.getEndpoint(), minioParam.getPort(), minioParam.getAccessKey(), minioParam.getSecretKey(), minioParam.getSecure());
            List<Bucket> bucketList = client.listBuckets();
            return bucketList.stream().map(Bucket::name).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Minio getItems failed", e);
            return new ArrayList<>();
        }
    }

    /**
     * 判斷是否有效地址
     *
     * @author ZhaoYuan
     * @date 2025/5/10 11:23
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