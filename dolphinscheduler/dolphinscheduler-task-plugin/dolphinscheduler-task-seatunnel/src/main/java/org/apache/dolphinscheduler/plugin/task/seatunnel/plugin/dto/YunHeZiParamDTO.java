package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.hadoop.fs.Path;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-24 11:55
 **/
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YunHeZiParamDTO {
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
    //服务名
    private String host;
    //端口
    private Integer port;
    //sk
    private String connectTimeout;
    //是否安全，true为https，false为http
    private String socketTimeout;

    /**
     * 云盘分配给第三方系统的应用id
     */
    private String appId;
    /**
     * 用户登录账号
     */
    private String account;
    /**
     * 云盘分配给第三方系统的应用id
     */
    private String appSecret;
    /**
     * 对应的版本号
     */
    private String version;

    private String protocol;
}