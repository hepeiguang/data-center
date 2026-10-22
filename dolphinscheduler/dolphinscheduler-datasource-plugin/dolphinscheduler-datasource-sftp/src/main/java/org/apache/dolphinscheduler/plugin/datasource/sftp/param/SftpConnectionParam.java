package org.apache.dolphinscheduler.plugin.datasource.sftp.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

/**
 * Sftp连接参数
 *
 * @author ZhaoYuan
 * @date 2025/5/13 20:03
 * @since JDK1.8
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SftpConnectionParam implements ConnectionParam {

    //目标SFTP主机地址
    private String host;
    //目标SFTP端口
    private Integer port;
    //目标SFTP用户名
    private String user;
    //目标SFTP密码
    private String password;
}