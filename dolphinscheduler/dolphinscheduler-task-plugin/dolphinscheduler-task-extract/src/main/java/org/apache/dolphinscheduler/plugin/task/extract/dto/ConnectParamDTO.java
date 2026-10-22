package org.apache.dolphinscheduler.plugin.task.extract.dto;

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
    protected String user;

    protected String password;

    protected String host;

    protected String port;

    protected String publicKey;
}