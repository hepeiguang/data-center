package org.apache.dolphinscheduler.plugin.datasource.httpserver.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpServerConnectionParam implements ConnectionParam {
    private String host;
    private Integer port;
    private String httpEndpoint;
    private String httpMethod;
    private String httpAuthType;
    private String httpAuthConfig;
    private Integer connectTimeout;
    private Integer socketTimeout;

    private String protocol;
}
