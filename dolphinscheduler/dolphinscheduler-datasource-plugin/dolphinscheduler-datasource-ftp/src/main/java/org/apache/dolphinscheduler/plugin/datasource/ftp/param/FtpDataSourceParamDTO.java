package org.apache.dolphinscheduler.plugin.datasource.ftp.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

/**
 * Ftp数据源参数
 *
 * @author ZhaoYuan
 * @date 2025/5/13 19:48
 * @since JDK1.8
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FtpDataSourceParamDTO extends BaseDataSourceParamDTO {

    @Override
    public DbType getType() {
        return DbType.FTP;
    }
}
