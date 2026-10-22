package org.apache.dolphinscheduler.plugin.datasource.minio.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

/**
 * Minio数据源参数
 *
 * @author ZhaoYuan
 * @date 2025/5/10 10:59
 * @since JDK1.8
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MinioDataSourceParamDTO extends BaseDataSourceParamDTO {

    //是否安全，true为https，false为http
    private Boolean secure;

    @Override
    public DbType getType() {
        return DbType.MINIO;
    }
}
