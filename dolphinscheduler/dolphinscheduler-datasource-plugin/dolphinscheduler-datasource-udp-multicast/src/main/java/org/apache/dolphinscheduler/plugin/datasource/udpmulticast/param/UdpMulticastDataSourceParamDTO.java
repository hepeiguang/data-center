package org.apache.dolphinscheduler.plugin.datasource.udpmulticast.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

@Data
@EqualsAndHashCode(callSuper = true)
public class UdpMulticastDataSourceParamDTO extends BaseDataSourceParamDTO {
    private String multicastAddress;
    private Integer port;
    private Integer connectTimeout = 5000;
    private Integer bufferSize = 8192;
    private String udpMulticastType;

    @Override
    public DbType getType() {
        return DbType.UDP_MULTICAST;
    }
}
