package org.apache.dolphinscheduler.plugin.datasource.rabbitmq.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

/**
 * Ftp连接参数
 *
 * @author ZhaoYuan
 * @date 2025/5/13 19:47
 * @since JDK1.8
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RabbitmqConnectionParam implements ConnectionParam {

    //主机地址
    private String host;
    //端口
    private Integer port;
    //用户名
    private String user;
    //密码
    private String password;

    private String virtualHost = "/";

    private Integer connectionTimeout = 30000;
    private Integer requestedHeartbeat = 60;
    private Boolean publisherReturns = true;
    private String publisherConfirmType = "CORRELATED";
}