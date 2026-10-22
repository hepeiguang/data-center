package org.apache.dolphinscheduler.plugin.datasource.yunhezi.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YunheziConnectionParam implements ConnectionParam {
    private String host;
    private Integer port;
    private String protocol;
    private Integer connectTimeout;
    private Integer socketTimeout;
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
}
