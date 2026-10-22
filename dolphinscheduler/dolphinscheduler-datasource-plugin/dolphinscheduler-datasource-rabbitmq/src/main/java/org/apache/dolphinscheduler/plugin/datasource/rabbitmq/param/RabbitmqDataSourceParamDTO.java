package org.apache.dolphinscheduler.plugin.datasource.rabbitmq.param;

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
public class RabbitmqDataSourceParamDTO extends BaseDataSourceParamDTO {
    private String virtualHost;

    @Override
    public DbType getType() {
        return DbType.RABBITMQ;
    }
}
