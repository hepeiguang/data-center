package org.apache.dolphinscheduler.plugin.datasource.sftp.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

/**
 * Sftp数据源参数
 *
 * @author ZhaoYuan
 * @date 2025/5/13 20:04
 * @since JDK1.8
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SftpDataSourceParamDTO extends BaseDataSourceParamDTO {

    @Override
    public DbType getType() {
        return DbType.SFTP;
    }
}
