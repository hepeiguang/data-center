package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpServerParamDTO {
    private String protocol;
    private String host;
    private Integer port;
    private String httpMethod;
    private String httpEndpoint;
    private String httpAuthType;
    private String httpAuthConfig;
    private Integer connectTimeout;
}
