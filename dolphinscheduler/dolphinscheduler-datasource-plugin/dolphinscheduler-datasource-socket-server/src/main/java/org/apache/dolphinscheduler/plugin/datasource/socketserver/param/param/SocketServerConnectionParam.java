package org.apache.dolphinscheduler.plugin.datasource.socketserver.param.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocketServerConnectionParam implements ConnectionParam {
    private String host;
    private Integer port;
    private Integer maxConnections;
    private Integer bufferSize;
    private Integer socketTimeout;
}
