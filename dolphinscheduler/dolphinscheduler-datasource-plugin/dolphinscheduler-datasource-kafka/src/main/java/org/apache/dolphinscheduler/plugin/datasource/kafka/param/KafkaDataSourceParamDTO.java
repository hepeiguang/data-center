package org.apache.dolphinscheduler.plugin.datasource.kafka.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaDataSourceParamDTO extends BaseDataSourceParamDTO {
    @Override
    public DbType getType() {
        return DbType.KAFKA;
    }
}
