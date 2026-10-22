package org.apache.dolphinscheduler.plugin.datasource.socketserver.param.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

@Data
@EqualsAndHashCode(callSuper = true)
public class SocketServerDataSourceParamDTO extends BaseDataSourceParamDTO {
    private String host;
    private Integer port;
    private Integer socketTimeout = 60000;     // 默认60秒
    private Integer maxConnections = 100;
    private Integer bufferSize = 4196;

    @Override
    public DbType getType() {
        return DbType.SOCKET_SERVER;
    }
}
