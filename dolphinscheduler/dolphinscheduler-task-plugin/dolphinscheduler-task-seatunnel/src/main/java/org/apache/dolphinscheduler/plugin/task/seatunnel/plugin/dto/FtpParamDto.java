package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

/**
 * Ftp/Sftp连接参数
 *
 * @author ZhaoYuan
 * @date 2025/5/13 23:07
 * @since JDK1.8
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FtpParamDto implements ConnectionParam {

    //目标FTP主机地址
    private String host;
    //目标FTP端口
    private Integer port;
    //目标FTP用户名
    private String user;
    //目标FTP密码
    private String password;
}