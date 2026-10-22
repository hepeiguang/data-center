package org.apache.dolphinscheduler.plugin.task.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-24 11:55
 **/
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectParamDTO {
    /**
     * {
     * "host": "192.168.0.201",
     * "port": "8999",
     * "httpEndpoint": "/api",
     * "httpMethod": "GET",
     * "httpAuthType": "NONE",
     * "httpAuthConfig": "",
     * "connectTimeout": "5000",
     * "socketTimeout": "5000",
     * "password": "******"
     * }
     */
    private String protocol;
    //服务名
    private String host;
    //端口
    private Integer port;
    //ak
    private String httpEndpoint;
    //sk
    private String connectTimeout;
    //是否安全，true为https，false为http
    private String socketTimeout;
}