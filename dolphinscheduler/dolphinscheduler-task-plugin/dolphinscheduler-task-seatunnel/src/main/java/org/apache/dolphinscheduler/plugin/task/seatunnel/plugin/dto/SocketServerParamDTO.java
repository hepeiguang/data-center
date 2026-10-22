package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocketServerParamDTO {

    private String host;
    private Integer port;
    private Integer bufferSize;
    private Integer socketTimeout;
    private Integer maxConnections;
}
