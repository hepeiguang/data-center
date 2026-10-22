package org.apache.dolphinscheduler.plugin.datasource.udpmulticast.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UdpMulticastConnectionParam implements ConnectionParam {
    private String multicastAddress;
    private Integer port;
    private Integer bufferSize;
    private Integer connectTimeout;
    private String udpMulticastType;
    private Map<String, String> other;
}