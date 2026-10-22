package org.apache.dolphinscheduler.plugin.datasource.hdfs.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class HdfsDataSourceParamDTO extends BaseDataSourceParamDTO {

    private String remoteUser;
    private String hdfsSitePath;

    @Override
    public DbType getType() {
        return DbType.HDFS_FILE;
    }
}
