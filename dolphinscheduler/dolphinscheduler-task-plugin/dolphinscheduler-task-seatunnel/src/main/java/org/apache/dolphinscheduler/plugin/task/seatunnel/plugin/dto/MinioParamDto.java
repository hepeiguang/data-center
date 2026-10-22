package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

/**
 * Minio连接参数
 *
 * @author ZhaoYuan
 * @date 2025/5/10 10:55
 * @since JDK1.8
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MinioParamDto implements ConnectionParam {

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