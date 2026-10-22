package org.apache.dolphinscheduler.plugin.task.minio.dto;

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
    //服务名
    private String endpoint;
    //端口
    private Integer port;
    //ak
    private String accessKey;
    //sk
    private String secretKey;
    //是否安全，true为https，false为http
    private Boolean secure;
}