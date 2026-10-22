package org.apache.dolphinscheduler.plugin.datasource.hdfs.param;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

@AutoService(DataSourceProcessor.class)
@Slf4j
public class HdfsDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, HdfsDataSourceParamDTO.class);
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        HdfsDataSourceParamDTO hdfsParam = (HdfsDataSourceParamDTO) baseDataSourceParamDTO;
        if (StringUtils.isEmpty(hdfsParam.getHost())) {
            throw new IllegalArgumentException("fsDefaultFs address is required");
        }
    }


    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        HdfsConnectionParam hdfsParam = (HdfsConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}", dbType.getName(), hdfsParam.getFsDefault());
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        HdfsConnectionParam connectionParams = (HdfsConnectionParam) createConnectionParams(connectionJson);
        HdfsDataSourceParamDTO hdfsParamDTO = new HdfsDataSourceParamDTO();
        hdfsParamDTO.setHost(connectionParams.getFsDefault());
        hdfsParamDTO.setHdfsSitePath(connectionParams.getHdfsSitePath());
        hdfsParamDTO.setHdfsSitePath(connectionParams.getHdfsSitePath());
        hdfsParamDTO.setOther(connectionParams.getKbsConfig());
        hdfsParamDTO.setRemoteUser(connectionParams.getRemoteUser());
        return hdfsParamDTO;
    }

    @Override
    public HdfsConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        HdfsDataSourceParamDTO hdfsParam = (HdfsDataSourceParamDTO) datasourceParam;
        HdfsConnectionParam hdfsConnectionParam = new HdfsConnectionParam();

        hdfsConnectionParam.setFsDefault(hdfsParam.getHost());
        hdfsConnectionParam.setHdfsSitePath(hdfsParam.getHdfsSitePath());
        hdfsConnectionParam.setHdfsSitePath(hdfsParam.getHdfsSitePath());
        hdfsConnectionParam.setKbsConfig(hdfsParam.getOther());
        hdfsConnectionParam.setRemoteUser(hdfsParam.getRemoteUser());
        return hdfsConnectionParam;
    }

    @Override
    public boolean checkDataSourceConnectivity(ConnectionParam connectionParam) {
        HdfsConnectionParam hdfsConnectionParam = (HdfsConnectionParam) connectionParam;
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsConnectionParam.getFsDefault());
        // 测试HDFS
        try {
            try (FileSystem fs = FileSystem.get(conf)) {
                return fs.exists(new org.apache.hadoop.fs.Path("/"));
            }
        } catch (Exception e) {
            log.error("Check HDFS connectivity failed", e);
            return false;
        }

    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, HdfsConnectionParam.class);
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
        return DbType.HDFS_FILE;
    }

    @Override
    public DataSourceProcessor create() {
        return new HdfsDataSourceProcessor();
    }
}
